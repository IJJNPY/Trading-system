package com.gudy.engine.bean;

import com.alipay.disruptor.RingBuffer;
import com.alipay.sofa.jraft.rhea.client.DefaultRheaKVStore;
import com.alipay.sofa.jraft.rhea.client.RheaKVStore;
import com.alipay.sofa.jraft.rhea.options.PlacementDriverOptions;
import com.alipay.sofa.jraft.rhea.options.RegionRouteTableOptions;
import com.alipay.sofa.jraft.rhea.options.RheaKVStoreOptions;
import com.alipay.sofa.jraft.rhea.options.configured.MultiRegionRouteTableOptionsConfigured;
import com.alipay.sofa.jraft.rhea.options.configured.PlacementDriverOptionsConfigured;
import com.alipay.sofa.jraft.rhea.options.configured.RheaKVStoreOptionsConfigured;
import com.google.common.collect.Lists;
import com.gudy.engine.bean.orderbook.GOrderBookImpl;
import com.gudy.engine.bean.orderbook.IOrderBook;
import com.gudy.engine.core.EngineApi;
import com.gudy.engine.core.EngineCore;
import com.gudy.engine.db.DbQuery;
import com.gudy.engine.handler.BaseHandler;
import com.gudy.engine.handler.match.StockMatchHandler;
import com.gudy.engine.handler.pub.L1PubHandler;
import com.gudy.engine.handler.risk.ExistRiskHandler;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.dbutils.QueryRunner;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;
import thirdpart.bean.CmdPack;
import thirdpart.bus.IBusSender;
import thirdpart.bus.MqttBusSender;
import thirdpart.checksum.ICheckSum;
import thirdpart.codec.IBodyCodec;
import thirdpart.codec.IMsgCodec;
import thirdpart.hq.MatchData;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.*;

@Getter
@RequiredArgsConstructor
@ToString
@Log4j2
public class EngineConfig {


    private short id;

    private String orderRecvIp;

    private int orderRecvPort;

    private String seqUrlList;

    private String pubIp;

    private int pubPort;

    @NonNull
    private String filename;

    @NonNull
    private IBodyCodec bodyCodec;

    @NonNull
    private ICheckSum cs;

    @NonNull
    private IMsgCodec msgCodec;

    private Vertx vertx = Vertx.vertx();

    public void startup() throws Exception{
        //1.读取配置文件
        initConfig();

        //2.数据库连接
        initDB();

        //3.启动撮合核心
        startEngine();

        //4.建立跟总线的连接
        initPub();

        //5.初始化跟排队机的连接
        startSeqConn();

    }

    @Getter
    private IBusSender busSender;

    private void initPub() {
        busSender = new MqttBusSender(pubIp,pubPort,msgCodec,vertx);
        busSender.startup();
    }

    private void startEngine() throws Exception {
        //1.前置风控处理器
        final BaseHandler riskHandler = new ExistRiskHandler(
                db.queryAllBalance().keySet(),
                db.queryAllStockCode()
        );

        //2.撮合处理器（订单簿*****）撮合/提供行情查询
        IntObjectHashMap<IOrderBook> orderBookMap = new IntObjectHashMap<>();
        db.queryAllStockCode().forEach(code -> orderBookMap.put(code,new GOrderBookImpl(code)));
        final BaseHandler matchHandler = new StockMatchHandler(orderBookMap);

        //3.行情发布系统
        ShortObjectHashMap<List<MatchData>> matcherEventMap = new ShortObjectHashMap<>();
        for(short id: db.queryAllMemberIds()){
            matcherEventMap.put(id,Lists.newArrayList());
        }

        final BaseHandler pubHandler = new L1PubHandler(matcherEventMap,this);

        engineApi = new EngineCore(
            riskHandler,
            matchHandler,
            pubHandler
        ).getApi();

    }

    private DbQuery db;

    //数据库查询
    private void initDB(){
        QueryRunner runner = new QueryRunner(new ComboPooledDataSource());
        db = new DbQuery(runner);
    }

    @Getter
    private EngineApi engineApi;

    @Getter
    @ToString.Exclude
    private final RheaKVStore orderKvStore = new DefaultRheaKVStore();

    //连接排队机
    private void startSeqConn() throws Exception{
        //路由表
        final List<RegionRouteTableOptions> regionRouteTableOptions = MultiRegionRouteTableOptionsConfigured
                .newConfigured()
                .withInitialServerList(-1L,seqUrlList)
                .config();

        final PlacementDriverOptions pdOpts = PlacementDriverOptionsConfigured
                .newConfigured()
                .withFake(true)
                .withRegionRouteTableOptionsList(regionRouteTableOptions)
                .config();

        final RheaKVStoreOptions opts = RheaKVStoreOptionsConfigured
                .newConfigured()
                .withPlacementDriverOptions(pdOpts)
                .config();

        orderKvStore.init(opts);

        /////////////////////////////////////////////////////////////////

        //委托指令处理

        //1.接收来自排队机的数据
        CmdPacketQueue.getInstance().init(orderKvStore,bodyCodec, engineApi);

        //2.组播的方式传输，允许多个Socket接收同一份数据
        DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());

        socket.listen(orderRecvPort,"0.0.0.0",asyncRes->{
            if(asyncRes.succeeded()){

                socket.handler(packet->{
                    Buffer udpData = packet.data();
                    if(udpData.length()>0){
                        try {
                            CmdPack cmdPack = bodyCodec.deserialize(udpData.getBytes(), CmdPack.class);
                            CmdPacketQueue.getInstance().cache(cmdPack);
                        } catch (Exception e) {
                            log.error("decode packet error",e);
                        }
                    }else{
                        log.error("recv empty udp packet from client : {}", packet.sender().toString());
                    }
                });
                try{
                    //参数分别为 监听的组播的ip，网卡名称，发消息的源地址，异步处理器
                    socket.listenMulticastGroup(orderRecvIp,mainInterface().getName(),null,asyncRes2->{
                        log.info("listen succeed {}", asyncRes2.succeeded());
                    });
                }catch (Exception e){
                    log.error(e);
                }
            }else {
                log.error("Listen failed,", asyncRes.cause());
            }
        });
    }

    //得到唯一的一块网卡适合UDP组播包的接收
    private static NetworkInterface mainInterface() throws Exception{
        //拿到机器所有的网卡，并放入链表
        final ArrayList<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

        //对链表进行过滤
        final NetworkInterface networkInterface = interfaces.stream().filter(t->{
           try{
               //需要满足的条件
               //1.地址是否为环回地址
               final boolean isLoopback = t.isLoopback();
               //2.是否支持multicast
               final boolean supportMulticast = t.supportsMulticast();
               //3.是否为虚拟机地址
               final boolean isVirtualBox = t.getDisplayName().contains("VirtualBox")||t.getDisplayName().contains("Host-only");
               //4.是否支持IPV4协议
               final boolean hasIpv4 = t.getInterfaceAddresses().stream().anyMatch(ia -> ia.getAddress() instanceof Inet4Address);

               return !isLoopback&&supportMulticast&&!isVirtualBox&&hasIpv4;

           }catch (Exception e){
               log.error("fine net interface error", e);
           }
           return false;
        }).sorted(Comparator.comparing(NetworkInterface::getName)).findFirst().orElse(null);

        return networkInterface;
    }

    private void initConfig() throws IOException {
        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/"+filename));

        id = Short.parseShort(properties.getProperty("id"));
        orderRecvIp = properties.getProperty("orderrecvip");
        orderRecvPort = Integer.parseInt(properties.getProperty("orderrecvport"));
        seqUrlList = properties.getProperty("sequrllist");
        pubIp = properties.getProperty("pubip");
        pubPort = Integer.parseInt(properties.getProperty("pubport"));

        log.info(this);

    }

}
