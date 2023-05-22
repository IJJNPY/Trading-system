package com.gudy.gateway.bean;

import com.gudy.gateway.bean.handler.ConnHandler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import thirdpart.checksum.ICheckSum;
import thirdpart.codec.IBodyCodec;

import java.io.File;

@Getter
@Log4j2
public class GatewayConfig {

    //网关ID
    private short id;

    //端口
    private int recvPort;

    //TODO 柜台列表 数据库连接

    @Setter
    private IBodyCodec bodyCodec;

    @Setter
    private ICheckSum cs;

    private Vertx vertx = Vertx.vertx();


    public void initConfig(String fileName)throws Exception{

        //创建dom4j解析器
        SAXReader reader = new SAXReader();
        System.out.println(fileName);
        Document document = reader.read(new File(fileName));

        Element root = document.getRootElement();

        //1.端口
        id = Short.parseShort(root.element("id").getText());
        recvPort = Integer.parseInt(root.element("recvport").getText());
        log.info("Gateway ID:{},Port:{}",id,recvPort);

        //TODO数据库连接 柜台连接列表

    }

    public void startup() throws Exception{
        //1.启动TCP服务监听
        initRecv();
        //TODO排队机交互
    }

    private void initRecv(){
        NetServer server = vertx.createNetServer();
        server.connectHandler(new ConnHandler(this));
        server.listen(recvPort, res->{
            if(res.succeeded()){
                log.info("gateway startup success at port:{}",recvPort);
            }else {
                log.error("gateway startup fail");
            }
        });
    }
}
