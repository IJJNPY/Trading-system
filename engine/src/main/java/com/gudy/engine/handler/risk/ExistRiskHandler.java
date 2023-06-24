package com.gudy.engine.handler.risk;

import com.gudy.engine.bean.command.CmdResultCode;
import com.gudy.engine.bean.command.RbCmd;
import com.gudy.engine.handler.BaseHandler;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.mutable.MutableInt;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import thirdpart.order.CmdType;

@Log4j2
@RequiredArgsConstructor
public class ExistRiskHandler extends BaseHandler {
    @NonNull
    private MutableLongSet uidSet;

    @NonNull
    private MutableIntSet codeSet;

    @Override
    public void onEvent(RbCmd cmd, long sequence, boolean endOfBatch) throws Exception {

        //如果指令为行情发布的指令，即指令由系统产生就不需要前置风控判断了
        if(cmd.command == CmdType.HQ_PUB){
            return;
        }
        if(cmd.command == CmdType.NEW_ORDER||cmd.command == CmdType.CANCEL_ORDER){
            //1.用户是否存在
            if(!uidSet.contains(cmd.uid)){
                log.error("illegal uid[{}]",cmd.uid);
                cmd.resultCode = CmdResultCode.RISK_INVALID_USER;
                return;
            }

            //2.股票代码是否合法
            if(!codeSet.contains(cmd.code)){
                log.error("illegal code[{}]",cmd.code);
                cmd.resultCode = CmdResultCode.RISK_INVALID_CODE;
                return;
            }
        }

    }
}
