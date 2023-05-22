package com.gudy.gateway.bean;

import com.google.common.collect.Lists;
import thirdpart.order.OrderCmd;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class OrderCmdContainer {

    private static OrderCmdContainer ourInstance = new OrderCmdContainer();

    private OrderCmdContainer(){}

    public static OrderCmdContainer getInstance(){
        return ourInstance;
    }

    private final BlockingQueue<OrderCmd> queue = new LinkedBlockingDeque<>();

    public boolean cache(OrderCmd cmd){
        return queue.offer(cmd);
    }

    public int size(){
        return queue.size();
    }

    public List<OrderCmd> getAll(){
        List<OrderCmd> msgList = Lists.newArrayList();

        int count = queue.drainTo(msgList);

        if(count == 0){
            return null;
        }else{
            return msgList;
        }
    }
}
