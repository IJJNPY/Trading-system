package com.gudy.gateway;

import com.gudy.gateway.bean.GatewayConfig;
import lombok.extern.log4j.Log4j2;
import thirdpart.checksum.ByteCheckSum;
import thirdpart.codec.BodyCodec;

import java.net.URI;

@Log4j2
public class GatewayStartup {
    public static void main(String[] args) throws Exception {
        String configFileName = "gateway.xml";

        GatewayConfig config = new GatewayConfig();
        //getResource方法中如果有中文或者空格，其会被替代掉，如果new一个file会出现找不到路径的错误，因为URL对空格，特殊字符，中文进行了编码处理
        //官方采用的办法是使用uri类再将其解码出来
        URI uri = new URI(GatewayStartup.class.getResource("/").getPath().toString());

        config.initConfig(uri.getPath() + configFileName);

        config.setCs(new ByteCheckSum());
        config.setBodyCodec(new BodyCodec());
        config.startup();
    }
}
