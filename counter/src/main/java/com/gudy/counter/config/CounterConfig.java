package com.gudy.counter.config;

import io.vertx.core.Vertx;
import thirdpart.checksum.ICheckSum;
import thirdpart.codec.IBodyCodec;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import thirdpart.codec.IMsgCodec;

import javax.annotation.PostConstruct;

@Getter
@Component
@Log4j2
public class CounterConfig {

    //会员号
    @Value("${counter.id}")
    private short id;

    //UUID相关配置
    @Value("${counter.dataCenterId}")
    private long dataCenterId;

    @Value("${counter.workerId}")
    private long workerId;

    ///////////网关配置/////////////////////////
    @Value("${counter.sendip}")
    private String sendIp;

    @Value("${counter.sendport}")
    private int sendPort;

    @Value("${counter.gatewayid}")
    private short gatewayId;

    //////////////////////////////////////////
    private Vertx vertx = Vertx.vertx();


//////////////编码相关配置///////////////////////////////
    @Value("${counter.checksum}")
    private String checkSumClass;

    @Value("${counter.bodycodec}")
    private String bodyCodecClass;

    @Value("${counter.msgcodec}")
    private String msgCodecClass;

    private ICheckSum cs;

    private IBodyCodec bodyCodec;

    private IMsgCodec msgCodec;

    @PostConstruct
    private void init(){
        Class<?> clz;

        try {
            clz = Class.forName(checkSumClass);

            cs = (ICheckSum) clz.getDeclaredConstructor().newInstance();

            clz = Class.forName(bodyCodecClass);
            bodyCodec = (IBodyCodec) clz.getDeclaredConstructor().newInstance();

            clz = Class.forName(msgCodecClass);
            msgCodec = (IMsgCodec) clz.getDeclaredConstructor().newInstance();

        } catch (Exception e) {
            log.error("init config error ", e);
        }
    }
}
