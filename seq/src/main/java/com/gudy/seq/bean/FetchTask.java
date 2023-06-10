package com.gudy.seq.bean;

import com.alipay.sofa.common.utils.ArrayUtil;
import com.alipay.sofa.jraft.util.Bits;
import com.alipay.sofa.jraft.util.BytesUtil;
import com.google.common.collect.Lists;
import io.vertx.core.buffer.Buffer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.logging.log4j.core.config.Order;
import thirdpart.bean.CmdPack;
import thirdpart.fetchsurv.IFetchService;
import thirdpart.order.CmdType;
import thirdpart.order.OrderCmd;
import thirdpart.order.OrderDirection;

import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.stream.Collectors;

@Log4j2
@RequiredArgsConstructor
public class FetchTask extends TimerTask {

    @NonNull
    private SeqConfig config;

    @Override
    public void run() {
        //1.从map中遍历所有网关的连接，获取数据
        if (!config.getNode().isLeader()) {
            return;
        }

        Map<String, IFetchService> fetchServiceMap = config.getFetchServiceMap();
        if (MapUtils.isEmpty(fetchServiceMap)) {
            return;
        }

        //获取数据
        List<OrderCmd> cmds = collectAllOrders(fetchServiceMap);
        if (CollectionUtils.isEmpty(cmds)) {
            return;
        }

        log.info(cmds);

        //排序 时间优先 价格优先 量优先
        cmds.sort((o1, o2) -> {
            int res = compareTime(o1, o2);
            if (res != 0) {
                return res;
            }
            res = comparePrice(o1, o2);
            if (res != 0) {
                return res;
            }
            res = compareVolume(o1, o2);
            return res;

        });

        //存储到KV Store， 发送到撮合核心

        try {
            //1.生成Packtno
            long packetNo = getPacketNoFromStore();

            //2.入库
            CmdPack pack = new CmdPack(packetNo, cmds);
            byte[] serialize = config.getCodec().serialize(pack);
            insertToKvStore(packetNo, serialize);

            //3.更新packetno+1
            updatePacketNoInstore(packetNo + 1);
            log.info(serialize);
            log.info(config.getNode().getRheaKVStore());

            //4.发送
            config.getMulticastSender().send(
                    Buffer.buffer(serialize),
                    config.getMulticastPort(),
                    config.getMulticastIp(),
                    null
            );
            log.info("5");


        } catch (Exception e) {
            log.error("encoder cmd packet error", e);
        }



    }
    private void updatePacketNoInstore(long packetNo){
        byte[] bytes = new byte[8];
        Bits.putLong(bytes,0,packetNo);
        config.getNode().getRheaKVStore().put(PACKET_NO_KEY,bytes);
    }

    //保存所有的数据到kvstore
    private void insertToKvStore(long packetNo, byte[] serialize) {
        byte[] key = new byte[8];
        Bits.putLong(key,0,packetNo);
        config.getNode().getRheaKVStore().put(key,serialize);
    }

    private static final byte[] PACKET_NO_KEY = BytesUtil.writeUtf8("seq_pqcket_no");


    //获取packetNo
    private long getPacketNoFromStore() {
        final byte[] bPacketNo = config.getNode().getRheaKVStore().bGet(PACKET_NO_KEY);
        long packetNo = 0;
        if(ArrayUtils.isNotEmpty(bPacketNo)){
            packetNo = Bits.getLong(bPacketNo,0);
        }
        return packetNo;
    }

    private int compareVolume(OrderCmd o1, OrderCmd o2) {
        if(o1.volume > o2.volume){
            return -1;
        }else if(o1.volume < o2.volume){
            return 1;
        }else {
            return 0;
        }
    }

    private int comparePrice(OrderCmd o1, OrderCmd o2) {
        if(o1.direction == o2.direction){
            if(o1.price > o2.price){
                return o1.direction == OrderDirection.BUY?-1:1;
            }else if(o1.price < o2.price){
                return o1.direction == OrderDirection.BUY?1:-1;
            }else {
                return 0;
            }
        }
        return 0;
    }

    private int compareTime(OrderCmd o1, OrderCmd o2) {
        if(o1.timestamp > o2.timestamp){
            return 1;
        }else if(o1.timestamp < o2.timestamp){
            return -1;
        }else {
            return 0;
        }
    }

    private List<OrderCmd> collectAllOrders(Map<String, IFetchService> fetchServiceMap) {
//        List<OrderCmd> res = fetchServiceMap.values().stream()
//                .map(t -> t.fetchData())
//                .filter(msg -> CollectionUtils.isEmpty(msg))
//                .flatMap(List::stream)
//                .collect(Collectors.toList());
        List<OrderCmd> msgs = Lists.newArrayList();
        fetchServiceMap.values().forEach(t->{
            List<OrderCmd> orderCmds = t.fetchData();
            if(CollectionUtils.isNotEmpty(orderCmds)){
                msgs.addAll(orderCmds);
//                log.info(msgs);
            }
        });

        return msgs;
    }
}
