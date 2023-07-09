package com.gudy.engine.handler.pub;

import com.gudy.engine.bean.EngineConfig;
import com.gudy.engine.bean.command.RbCmd;
import com.gudy.engine.bean.orderbook.MatchEvent;
import com.gudy.engine.handler.BaseHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.collections.api.tuple.primitive.ShortObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;
import thirdpart.bean.CommonMsg;
import thirdpart.hq.L1MarketData;
import thirdpart.hq.MatchData;
import thirdpart.order.CmdType;

import java.util.List;

import static thirdpart.bean.MsgConstants.*;

@Log4j2
@RequiredArgsConstructor
public class L1PubHandler extends BaseHandler {

    //向外广播行情，向柜台委托成交有变化的先推过去
    public static final int HQ_PUB_RATE = 1000;

    //存储发向柜台的数据
    @NonNull
    private final ShortObjectHashMap<List<MatchData>> matcherEventMap;

    @NonNull
    private EngineConfig config;

    @Override
    public void onEvent(RbCmd cmd, long sequence, boolean endOfBatch) throws Exception {
        final CmdType cmdType = cmd.command;

        if (cmdType == CmdType.NEW_ORDER || cmdType == CmdType.CANCEL_ORDER) {
            for (MatchEvent e : cmd.matchEventList) {
                log.info(e);
                matcherEventMap.get(e.mid).add(e.copy());
            }
        } else if (cmdType == CmdType.HQ_PUB) {
//            log.info(cmd);
            //1.五档行情
            pubMarketData(cmd.marketDataMap);
            //2.给柜台发送MatchData
            pubMatcherData();
        }
    }

    private void pubMatcherData() {
        if(matcherEventMap.size() == 0){
            return;
        }
        log.info(matcherEventMap);

        try {
            for (ShortObjectPair<List<MatchData>> s: matcherEventMap.keyValuesView()){
                if(CollectionUtils.isEmpty(s.getTwo())){
                    continue;
                }
                byte[] serialize = config.getBodyCodec().serialize(s.getTwo().toArray(new MatchData[0]));
                pubData(serialize,s.getOne(),MATCH_ORDER_DATA);

                s.getTwo().clear();
            }
        }catch (Exception e){
            log.error(e);
        }
    }

    public static final short HQ_ADDRESS = -1;

    private void pubMarketData(IntObjectHashMap<L1MarketData> marketDataMap) {
        log.info(marketDataMap);
        byte[] serialize = null;
        try{
            serialize = config.getBodyCodec().serialize(marketDataMap.values().toArray(new L1MarketData[0]));
        }catch (Exception e){
            log.error(e);
        }

        if(serialize == null) {
            return;
        }
        pubData(serialize,HQ_ADDRESS,MATCH_HQ_DATA);
    }


    private void pubData(byte[] serialize, short dst, short msgType) {
        CommonMsg msg = new CommonMsg();
        msg.setBodyLength(serialize.length);
        msg.setChecksum(config.getCs().getChecksum(serialize));
        msg.setMsgSrc(config.getId());
        msg.setMsgDst(dst);
        msg.setMsgType(msgType);
        msg.setStatus(NORMAL);
        msg.setBody(serialize);
        config.getBusSender().publish(msg);
    }




}
