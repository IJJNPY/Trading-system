package com.gudy.counter.controller;

import com.gudy.counter.bean.res.*;
import com.gudy.counter.cache.CacheType;
import com.gudy.counter.cache.RedisStringCache;
import com.gudy.counter.service.AccountService;
import com.gudy.counter.service.OrderService;
import com.gudy.counter.thirdpart.uuid.GudyUuid;
import com.gudy.counter.util.Captcha;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api")
@Log4j2
public class OrderController {

    @Autowired
    private OrderService orderService;

    @RequestMapping("/balance")
    public CounterRes balanceQuery(@RequestParam long uid)
            throws Exception{
        long balance = orderService.getBalance(uid);
        return new CounterRes(balance);
    }

    @RequestMapping("/posiinfo")
    public CounterRes posiQuery(@RequestParam long uid)
            throws Exception{
        List<PosiInfo> postList = orderService.getPostList(uid);
        return new CounterRes(postList);
    }

    @RequestMapping("/orderinfo")
    public CounterRes orderQuery(@RequestParam long uid)
            throws Exception{
        List<OrderInfo> orderList = orderService.getOrderList(uid);
        System.out.println(orderList);
        return new CounterRes(orderList);
    }

    @RequestMapping("/tradeinfo")
    public CounterRes tradeQuery(@RequestParam long uid)
            throws Exception{
        List<TradeInfo> tradeList = orderService.getTradeList(uid);
        return new CounterRes(tradeList);
    }

}
