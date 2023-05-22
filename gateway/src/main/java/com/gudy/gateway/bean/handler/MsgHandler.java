package com.gudy.gateway.bean.handler;

import com.gudy.gateway.bean.OrderCmdContainer;
import com.sun.org.apache.xpath.internal.operations.Or;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import thirdpart.bean.CommonMsg;
import thirdpart.codec.IBodyCodec;
import thirdpart.order.OrderCmd;

@Log4j2
@AllArgsConstructor
public class MsgHandler implements IMsgHandler {

    private IBodyCodec bodyCodec;

    @Override
    public void onCounterData(CommonMsg msg) {
        OrderCmd orderCmd;

        try {
            orderCmd = bodyCodec.deserialize(msg.getBody(), OrderCmd.class);
            log.info("recv cmd: {}",orderCmd);

            if(!OrderCmdContainer.getInstance().cache(orderCmd)){
                log.error("gateway queue insert fail,queue length:{},order:{}",
                        OrderCmdContainer.getInstance().size(),
                        orderCmd);
            }


        } catch (Exception e) {
            log.error("decode order cmd error", e);
        }
    }
}
