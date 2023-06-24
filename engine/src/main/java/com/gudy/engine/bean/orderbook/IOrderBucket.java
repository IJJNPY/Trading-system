package com.gudy.engine.bean.orderbook;

import com.gudy.engine.bean.command.RbCmd;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public interface IOrderBucket extends Comparable<IOrderBucket>{

    AtomicLong tidGen = new AtomicLong(0);

    //1.新增订单
    void put(Order order);

    //2.移除订单
    Order remove(long oid);

    //3.match
    //当前bucket中需要匹配的数量，传入的委托，
    long match(long volumeLeft, RbCmd triggerCmd, Consumer<Order> removeOrderCallback);

    //4.行情发布
    long getPrice();
    void setPrice(long price);
    long getTotalVolume();

    //5.初始化选项
    static IOrderBucket create(OrderBucketImplType type){
        switch (type){
            case GUDY:
                return new GOrderBucketImpl();
            default:
                throw new IllegalArgumentException();
        }
    }

    @Getter
    enum OrderBucketImplType{
        GUDY(0);

        private byte code;

        OrderBucketImplType(int code){
            this.code = (byte)code;
        }
    }

    //6.比较排序
    default int compareTo(IOrderBucket other){
        return Long.compare(this.getPrice(),other.getPrice());
    }
}
