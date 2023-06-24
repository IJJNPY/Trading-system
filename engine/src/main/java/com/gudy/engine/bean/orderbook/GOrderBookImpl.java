package com.gudy.engine.bean.orderbook;

import com.google.common.collect.Lists;
import com.gudy.engine.bean.command.CmdResultCode;
import com.gudy.engine.bean.command.RbCmd;
import io.netty.util.collection.LongObjectHashMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import thirdpart.hq.L1MarketData;
import thirdpart.order.OrderDirection;
import thirdpart.order.OrderStatus;

import java.util.*;

@Log4j2
@RequiredArgsConstructor
public class GOrderBookImpl implements IOrderBook {

    @NonNull
    private int code;

    //<价格，orderbucket>
    private final NavigableMap<Long,IOrderBucket> sellBuckets = new TreeMap<>();
    private final NavigableMap<Long,IOrderBucket> buyBuckets = new TreeMap<>(Collections.reverseOrder());

    private final LongObjectHashMap<Order> oidMap = new LongObjectHashMap<>();


    @Override
    public CmdResultCode newOrder(RbCmd cmd) {

        //1.判断重复
        if(oidMap.containsKey(cmd.oid)){
            return CmdResultCode.DUPLICATE_ORDER_ID;
        }

        //2.生成新Order
        //2.1预撮合
        NavigableMap<Long,IOrderBucket> subMatchBuckets = (cmd.direction == OrderDirection.SELL?buyBuckets:sellBuckets)
                .headMap(cmd.price,true);

        long tVolume = preMatch(cmd,subMatchBuckets);
        //如果成交的委托量等于这笔委托的委托量则不需要生成新的委托单放在orderbucket中
        if(tVolume == cmd.volume){
            return CmdResultCode.SUCCESS;
        }

        final Order order = Order.builder()
                .mid(cmd.mid)
                .uid(cmd.uid)
                .code(cmd.code)
                .direction(cmd.direction)
                .price(cmd.price)
                .volume(cmd.volume)
                .tvolume(tVolume)
                .oid(cmd.oid)
                .timestamp(cmd.timestamp)
                .build();

        if(tVolume == 0){
            genMatchEvent(cmd,OrderStatus.ORDER_ED);
        }else {
            genMatchEvent(cmd,OrderStatus.PART_TRADE);
        }

        //3.加入orderBucket
        final IOrderBucket bucket = (cmd.direction == OrderDirection.SELL?sellBuckets:buyBuckets)
                //判断当前bucket中价格是否存在，如果不存在需要new一个bucket
                .computeIfAbsent(cmd.price,p->{
                    final IOrderBucket b = IOrderBucket.create(IOrderBucket.OrderBucketImplType.GUDY);
                    b.setPrice(p);
                    return b;
                });
        bucket.put(order);
        //在缓存中将该委托加入进去
        oidMap.put(cmd.oid,order);


        return CmdResultCode.SUCCESS;
    }

    private long preMatch(RbCmd cmd, NavigableMap<Long, IOrderBucket> matchingBuckets) {
        int tVol = 0;
        if(matchingBuckets.size() == 0){
            return tVol;
        }

        List<Long> emptyBuckets = Lists.newArrayList();

        for(IOrderBucket bucket:matchingBuckets.values()){

            tVol += bucket.match(cmd.volume - tVol, cmd, order -> oidMap.remove(order.getOid()));

            //如果某个bucket所有委托都耗尽了，则将其加入空列表中,最后全部进行移除
            if(bucket.getTotalVolume() == 0){
                emptyBuckets.add(bucket.getPrice());
            }

            if(tVol == cmd.volume){
                break;
            }
        }

        emptyBuckets.forEach(matchingBuckets::remove);

        //返回预撮合量
        return tVol;
    }

    //生成matchevent
    private void genMatchEvent(RbCmd cmd, OrderStatus status) {
        long now = System.currentTimeMillis();
        MatchEvent event = new MatchEvent();
        event.timestamp = now;
        event.mid = cmd.mid;
        event.oid = cmd.oid;
        event.status = status;
        event.volume = 0;
        cmd.matchEventList.add(event);

    }

    @Override
    public CmdResultCode cancelOrder(RbCmd cmd) {
        //1.从缓存中移除委托
        Order order = oidMap.get(cmd.oid);
        if(order == null){
            return CmdResultCode.INVALID_ORDER_ID;
        }
        oidMap.remove(order.getOid());

        //2.从orderbucket中移除委托
        final NavigableMap<Long, IOrderBucket> buckets = order.getDirection() == OrderDirection.SELL ? sellBuckets : buyBuckets;
        IOrderBucket OrderBucket = buckets.get(order.getPrice());
        OrderBucket.remove(order.getOid());
        if(OrderBucket.getTotalVolume() == 0){
            buckets.remove(order.getPrice());
        }

        //3.发送撤单MatchEvent
        MatchEvent cancelEvent = new MatchEvent();
        cancelEvent.timestamp = System.currentTimeMillis();
        cancelEvent.mid = order.getMid();
        cancelEvent.oid = order.getOid();
        cancelEvent.status = order.getTvolume() == 0? OrderStatus.CANCEL_ED:OrderStatus.PART_CANCEL;
        cancelEvent.volume = order.getTvolume() - order.getVolume();
        cmd.matchEventList.add(cancelEvent);

        return CmdResultCode.SUCCESS;
    }

    @Override
    public void fillCode(L1MarketData data) {
        data.code = code;
    }

    @Override
    public void fillSells(int sellSize, L1MarketData data) {
        if(sellSize == 0){
            data.sellSize = 0;
            return;
        }
        int i = 0;
        for(IOrderBucket bucket:sellBuckets.values()){
            data.sellPrices[i] = bucket.getPrice();
            data.sellVolumes[i] = bucket.getTotalVolume();
            if(++i == sellSize){
                break;
            }
        }

        data.sellSize = i;
    }

    @Override
    public void fillBuys(int buySize, L1MarketData data) {
        if(buySize == 0){
            data.buySize = 0;
            return;
        }
        int i = 0;
        for(IOrderBucket bucket:buyBuckets.values()){
            data.buyPrices[i] = bucket.getPrice();
            data.buyVolumes[i] = bucket.getTotalVolume();
            if(++i == buySize){
                break;
            }
        }

        data.buySize = i;
    }

    @Override
    public int limitBuyBucketSize(int maxSize) {
        return Math.min(maxSize,buyBuckets.size());
    }

    @Override
    public int limitSellBucketSize(int maxSize) {
        return Math.min(maxSize,sellBuckets.size());
    }
}
