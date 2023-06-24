package com.gudy.engine.handler.match;

import com.gudy.engine.bean.command.CmdResultCode;
import com.gudy.engine.bean.command.RbCmd;
import com.gudy.engine.bean.orderbook.IOrderBook;
import com.gudy.engine.handler.BaseHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

@RequiredArgsConstructor
public class StockMatchHandler extends BaseHandler {

    @NonNull
    private final IntObjectHashMap<IOrderBook> orderBookMap;

    @Override
    public void onEvent(RbCmd cmd, long sequnce, boolean endOfBatch) throws Exception {
        //1.对每笔委托首先判断是否通过了前置风控的校验
        if(cmd.resultCode.getCode()<0){
            return;
        }

        cmd.resultCode = processCmd(cmd);

    }

    private CmdResultCode processCmd(RbCmd cmd) {
        switch (cmd.command){
            case NEW_ORDER:
                return orderBookMap.get(cmd.code).newOrder(cmd);
            case CANCEL_ORDER:
                return orderBookMap.get(cmd.code).cancelOrder(cmd);
            case HQ_PUB:
                //遍历所有的orderbook将其中的快照都拿出来，一次性丢到外面去
                orderBookMap.forEachKeyValue((code,orderBookMap)->{
                    cmd.marketDataMap.put(code,orderBookMap.getL1MarketDataSnapshot());
                });
            default:
                return CmdResultCode.SUCCESS;
        }
    }
}
