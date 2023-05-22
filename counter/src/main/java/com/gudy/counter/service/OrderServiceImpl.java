package com.gudy.counter.service;

import com.gudy.counter.bean.res.OrderInfo;
import com.gudy.counter.bean.res.PosiInfo;
import com.gudy.counter.bean.res.TradeInfo;
import com.gudy.counter.config.CounterConfig;
import com.gudy.counter.config.GatewayConn;
import thirdpart.order.CmdType;
import thirdpart.order.OrderCmd;
import thirdpart.order.OrderDirection;
import thirdpart.order.OrderType;
import com.gudy.counter.util.DbUtil;
import com.gudy.counter.util.IDConverter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Log4j2
@Component
public class OrderServiceImpl implements OrderService {
    @Override
    public Long getBalance(long uid) {
        return DbUtil.getBalance(uid);
    }

    @Override
    public List<PosiInfo> getPostList(long uid) {
        return DbUtil.getPosiList(uid);
    }

    @Override
    public List<OrderInfo> getOrderList(long uid) {
        return DbUtil.getOrderList(uid);
    }

    @Override
    public List<TradeInfo> getTradeList(long uid) {
        return DbUtil.getTradeList(uid);
    }

    @Autowired
    private CounterConfig config;

    @Autowired
    private GatewayConn gatewayConn;

    @Override
    public boolean sendOrder(long uid, short type, long timestamp, int code, byte direction, long price, long volume, byte ordertype) {
        final OrderCmd orderCmd = OrderCmd.builder()
                .type(CmdType.of(type))
                .timestamp(timestamp)
                .mid(config.getId())
                .uid(uid)
                .code(code)
                .direction(OrderDirection.of(ordertype))
                .price(price)
                .volume(volume)
                .orderType(OrderType.of(ordertype))
                .build();
        //1.入库
        int oid = DbUtil.saveOrder(orderCmd);
        if(oid<0){
            return false;
        }else{
            //1.调整资金持仓数据
            if(orderCmd.direction == OrderDirection.BUY){
                //减少资金
                DbUtil.minusBalance(orderCmd.uid,orderCmd.price*orderCmd.volume);
            }else if(orderCmd.direction == OrderDirection.SELL){
                //减少持仓
                DbUtil.minusPosi(orderCmd.uid,orderCmd.code,orderCmd.volume,orderCmd.price);
            }else {
                log.error("wrong direction[{}],ordercmd:{}",orderCmd.direction,orderCmd);
                return false;
            }
            //2.生成全局ID 组装ID long[柜台ID，委托ID]
            orderCmd.oid = IDConverter.combineInt2Long(config.getId(),oid);

            //3.打包委托发送数据(ordercmd->网关模板类commonmsg->-tcp数据流)
            //4.发送
            gatewayConn.sendOrder(orderCmd);
            log.info(orderCmd);
            return true;
        }

    }
}
