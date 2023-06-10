package com.gudy.seq.bean;

import com.alipay.sofa.jraft.rhea.options.PlacementDriverOptions;
import com.alipay.sofa.jraft.rhea.options.RheaKVStoreOptions;
import com.alipay.sofa.jraft.rhea.options.StoreEngineOptions;
import com.alipay.sofa.jraft.rhea.options.configured.MemoryDBOptionsConfigured;
import com.alipay.sofa.jraft.rhea.options.configured.PlacementDriverOptionsConfigured;
import com.alipay.sofa.jraft.rhea.options.configured.RheaKVStoreOptionsConfigured;
import com.alipay.sofa.jraft.rhea.options.configured.StoreEngineOptionsConfigured;
import com.alipay.sofa.jraft.rhea.storage.StorageType;
import com.alipay.sofa.jraft.util.Endpoint;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.listener.ChannelListener;
import com.alipay.sofa.rpc.transport.AbstractChannel;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import thirdpart.codec.IBodyCodec;
import thirdpart.fetchsurv.IFetchService;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;

@Log4j2
@ToString
@RequiredArgsConstructor
public class SeqConfig {

    private String dataPath;

    private String serveUrl;

    private String serveList;

    @NonNull
    private String fileName;


    public void startup() throws Exception{
        //1.读取配置文件
        initConfig();

        //2.初始化集群
        startSeqDbCluster();

        //TODO3.启动下游广播
        startMultiCast();

        //4.初始化网关连接
        startupFetch();
    }


    /////////////////////广播/////////////////////////////

    @Getter
    private String multicastIp;

    @Getter
    private int multicastPort;

    @Getter
    private DatagramSocket multicastSender;

    private void startMultiCast(){
        multicastSender = Vertx.vertx().createDatagramSocket(new DatagramSocketOptions());
    }



    ///////////////////////抓取逻辑//////////////////////////
    private String fetchurls;

    @ToString.Exclude
    @Getter
    private Map<String, IFetchService> fetchServiceMap = Maps.newConcurrentMap();

    @NonNull
    @ToString.Exclude
    @Getter
    private IBodyCodec codec;

    @RequiredArgsConstructor
    private class FetchChannelListener implements ChannelListener{

        @NonNull
        private ConsumerConfig<IFetchService> config;

        @Override
        public void onConnected(AbstractChannel abstractChannel) {
            //连接成功打印日志并加入map
            String remoteAddr = abstractChannel.remoteAddress().toString();
            log.info("connect to gateway : {}",remoteAddr);
            fetchServiceMap.put(remoteAddr,config.refer());
        }

        @Override
        public void onDisconnected(AbstractChannel abstractChannel) {
            String remoteAddr = abstractChannel.remoteAddress().toString();
            log.info("disconnect from gateway:{}",remoteAddr);
            fetchServiceMap.remove(remoteAddr);
        }
    }

    //1.从哪些网关抓
    //2.通信方式
    private void startupFetch(){
        //1.将所有网关的连接放入map中
        String[] urls = fetchurls.split(";");
        for(String url:urls){
            ConsumerConfig<IFetchService> consumerConfig = new ConsumerConfig<IFetchService>()
                    //通信接口
                    .setInterfaceId(IFetchService.class.getName())
                    //上下游通信的协议
                    .setProtocol("bolt")
                    .setTimeout(5000)
                    //直连地址
                    .setDirectUrl(url);
            //对其加上监听器，如果当前连接从网关断开，应该将其从map中剔除
            consumerConfig.setOnConnect(Lists.newArrayList(new FetchChannelListener(consumerConfig)));
            fetchServiceMap.put(url,consumerConfig.refer());
        }
        //2.启动定时任务定时抓取网关数据
        new Timer().schedule(new FetchTask(this),5000,1000);

    }

    @Getter
    private Node node;

    //启动KV Store
    private void startSeqDbCluster(){
        //为一个集群中有多个raft集群的情况使用
        final PlacementDriverOptions pdOpts = PlacementDriverOptionsConfigured.newConfigured()
                //这里只有一个raft集群，参数传入true书名pdopts不生效
                .withFake(true)
                .config();

        //127.0.0.1：8891
        String[] split = serveUrl.split(":");
        final StoreEngineOptions storeOpts = StoreEngineOptionsConfigured.newConfigured()
                //数据存储类型用内存的方式来存储的
                .withStorageType(StorageType.Memory)
                .withMemoryDBOptions(MemoryDBOptionsConfigured.newConfigured().config())
                .withRaftDataPath(dataPath)
//                .withServerAddress(new Endpoint("127.0.0.1",8891))
                .withServerAddress(new Endpoint(split[0],Integer.parseInt(split[1])))
                .config();

        //kv的option依赖于 pdopts以及storeopts，因此需要将他们提前定义出来
        final RheaKVStoreOptions opts = RheaKVStoreOptionsConfigured.newConfigured()
                .withInitialServerList(serveList)
                .withStoreEngineOptions(storeOpts)
                .withPlacementDriverOptions(pdOpts)
                .config();

        node = new Node(opts);
        node.start();
        //把节点的stop方法挂到系统的shutdown流程当中去
        Runtime.getRuntime().addShutdownHook(new Thread(node::stop));
        log.info("start seq node success on port{}",split[1]);
    }

    private void initConfig() throws IOException{
        Properties properties = new Properties();
        properties.load(Object.class.getResourceAsStream("/"+fileName));

        dataPath = properties.getProperty("datapath");
        serveUrl = properties.getProperty("serveurl");
        serveList = properties.getProperty("serverlist");
        fetchurls = properties.getProperty("fetchurl");
        multicastIp = properties.getProperty("multicastip");
        multicastPort = Integer.parseInt(properties.getProperty("multicastport"));

        log.info("read config: {}",this);
    }
}
