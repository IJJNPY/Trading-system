package com.gudy.engine.bean.orderbook;

import com.gudy.engine.bean.command.RbCmd;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import thirdpart.order.OrderStatus;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@Log4j2
@ToString
public class GOrderBucketImpl implements IOrderBucket{

    //增加三个成员变量，价格，量，委托列表
    @Getter
    @Setter
    private long price;

    @Getter
    private long totalVolume = 0;

    //3.委托列表，list行不通
    private final LinkedHashMap<Long,Order> entries = new LinkedHashMap<>();

    @Override
    public void put(Order order) {
        entries.put(order.getOid(),order);
        totalVolume += order.getVolume() - order.getTvolume();
    }

    @Override
    public Order remove(long oid) {
        //防止重读执行，删除订单的请求
        Order order = entries.get(oid);
        if(order == null){
            return null;
        }
        entries.remove(oid);

        //已经有的委托-已经成交的委托
        totalVolume -= order.getVolume()- order.getTvolume();

        return order;
    }

    //撮合业务
    @Override
    public long match(long volumeLeft, RbCmd triggerCmd, Consumer<Order> removeOrderCallback) {

        Iterator<Map.Entry<Long, Order>> iterator = entries.entrySet().iterator();

        long volumeMatch = 0;

        //剩余需要撮合的委托量大于零
        while (iterator.hasNext() && volumeLeft>0){
            Map.Entry<Long,Order> next = iterator.next();
            Order order = next.getValue();
            //计算order可以吃多少量
            long traded = Math.min(volumeLeft, order.getVolume()-order.getTvolume());
            volumeMatch += traded;

            //1.order自身的量 2.volumeleft 3.bucket总委托量
            order.setTvolume(order.getTvolume()+ traded);
            volumeLeft -= traded;
            totalVolume -= traded;

            //生成事件
            boolean fullMatch = order.getVolume() == order.getTvolume();
            genMatchEvent(order,triggerCmd,fullMatch,volumeLeft==0,traded);

            //如果当前这笔委托已经成交完成了
            if(fullMatch){
                removeOrderCallback.accept(order);
                //为什么使用iterator的方式来进行遍历：只有使用iterator的方法才能在遍历的过程中进行删除，用for不行
                iterator.remove();
            }
        }
        return volumeMatch;
    }

    private void genMatchEvent(final Order order, final RbCmd cmd, boolean fullMatch, boolean cmdFullMatch, long traded) {

        long now = System.currentTimeMillis();

        long tid = IOrderBucket.tidGen.getAndIncrement();

        //两个MatchEvent
        MatchEvent bidEvent = new MatchEvent();
        bidEvent.timestamp = now;
        bidEvent.mid = cmd.mid;
        bidEvent.oid = cmd.oid;
        bidEvent.status = cmdFullMatch? OrderStatus.TRADE_ED:OrderStatus.PART_TRADE;
        bidEvent.tid = tid;
        bidEvent.volume = traded;
        bidEvent.price = order.getPrice();
        cmd.matchEventList.add(bidEvent);

        MatchEvent ofrEvent = new MatchEvent();
        ofrEvent.timestamp = now;
        ofrEvent.mid = cmd.mid;
        ofrEvent.oid = cmd.oid;
        ofrEvent.status = fullMatch? OrderStatus.TRADE_ED:OrderStatus.PART_TRADE;
        ofrEvent.tid = tid;
        ofrEvent.volume = traded;
        ofrEvent.price = order.getPrice();
        cmd.matchEventList.add(ofrEvent);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;

        if(o == null||getClass()!=o.getClass()) return false;

        GOrderBucketImpl that = (GOrderBucketImpl) o;

        return new EqualsBuilder()
                .append(price,that.price)
                .append(entries, that.entries)
                .isEquals();
    }

    @Override
    public int hashCode(){
        return new HashCodeBuilder(17,37)
                .append(price)
                .append(entries)
                .toHashCode();
    }

    @Override
    public long getPrice() {
        return 0;
    }

    @Override
    public void setPrice(long price) {

    }

    @Override
    public long getTotalVolume() {
        return 0;
    }
}
