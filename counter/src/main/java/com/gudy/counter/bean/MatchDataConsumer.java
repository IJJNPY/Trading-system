package com.gudy.counter.bean;

import com.google.common.collect.ImmutableMap;
import com.gudy.counter.config.CounterConfig;
import com.gudy.counter.util.DbUtil;
import com.gudy.counter.util.IDConverter;
import com.gudy.counter.util.JsonUtil;
import io.netty.util.collection.LongObjectHashMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import thirdpart.hq.MatchData;
import thirdpart.order.OrderCmd;
import thirdpart.order.OrderDirection;
import thirdpart.order.OrderStatus;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.gudy.counter.bean.MqttBusConsumer.INNER_MATCH_DATA_ADDR;
import static com.gudy.counter.config.WebSocketConfig.ORDER_NOTIFY_ADDR_PREFIX;
import static com.gudy.counter.config.WebSocketConfig.TRADE_NOTIFY_ADDR_PREFIX;


@Log4j2
@Component
public class MatchDataConsumer {

    public static final String ORDER_DATA_CACHE_ADDR = "order_data_cache_addr";

    @Autowired
    private CounterConfig config;

    //<委托编号，OrderCmd>
    private LongObjectHashMap<OrderCmd> oidOrderMap = new LongObjectHashMap<>();

    @PostConstruct
    private void init(){
        EventBus eventBus = config.getVertx().eventBus();

        eventBus.consumer(ORDER_DATA_CACHE_ADDR)
                .handler(buffer -> {
                    Buffer body = (Buffer) buffer.body();
                    try {
                        OrderCmd om = config.getBodyCodec().deserialize(body.getBytes(), OrderCmd.class);
                        log.info("cache order:{}", om);
                        oidOrderMap.put(om.oid, om);
                    } catch (Exception e) {
                        log.error(e);
                    }
                });

        eventBus.consumer(INNER_MATCH_DATA_ADDR)
                .handler(buffer -> {
                    Buffer body = (Buffer) buffer.body();
                    if(body.length() == 0){
                        return;
                    }

                    MatchData[] matchData = null;
                    try{
                        matchData = config.getBodyCodec().deserialize(body.getBytes(),MatchData[].class);
                    } catch (Exception e) {
                        log.error(e);
                    }

                    if(ArrayUtils.isEmpty(matchData)){
                        return;
                    }

                    //按照oid进行分类
                    Map<Long, List<MatchData>> collect = Arrays.asList(matchData)
                            .stream().collect(Collectors.groupingBy(t->t.oid));

                    for(Map.Entry<Long,List<MatchData>> entry:collect.entrySet()){
                        if(CollectionUtils.isEmpty(entry.getValue())){
                            continue;
                        }

                        //拆分获取柜台内部委托编号
                        long oid = entry.getKey();
                        int counterOId = IDConverter.seperateLong2Int(oid)[1];

                        updateAndNotify(counterOId,entry.getValue(),oidOrderMap.get(oid));

                    }
                });

    }

    private void updateAndNotify(int counterOId, List<MatchData> value, OrderCmd orderCmd) {
        if(CollectionUtils.isEmpty(value)){
            return;
        }

        //成交和委托变动
        //成交
        for(MatchData md:value){
            OrderStatus status = md.status;
            if(status == OrderStatus.TRADE_ED||status == OrderStatus.PART_TRADE){
                //更新成交
                DbUtil.saveTrade(counterOId,md,orderCmd);

                //持仓资金多退少补
                if(orderCmd.direction == OrderDirection.BUY){

                    if(orderCmd.price>md.price){
                        DbUtil.addBalance(orderCmd.uid,(orderCmd.price-md.price)*md.volume);
                    }
                    DbUtil.addPosi(orderCmd.uid,orderCmd.code,md.volume,md.price);
                }else if(orderCmd.direction == OrderDirection.SELL){
                    DbUtil.addBalance(orderCmd.uid,md.price*md.volume);
                }else {
                    log.error("wrong direction[{}]", orderCmd.direction);;
                }

                //发生成交时的主动推送
                config.getVertx().eventBus().publish(
                        TRADE_NOTIFY_ADDR_PREFIX + orderCmd.uid,
                        JsonUtil.toJson(
                                ImmutableMap.of("code", orderCmd.code,
                                        "direction", orderCmd.direction,
                                        "volume", md.volume)
                        )
                );
            }
        }

        //委托变动
        //根据最后一笔Match处理委托
        MatchData finalMatchData = value.get(value.size()-1);
        OrderStatus finalOrderStatus = finalMatchData.status;
        DbUtil.updateOrder(orderCmd.uid,counterOId,finalOrderStatus);
        if(finalOrderStatus == OrderStatus.CANCEL_ED||finalOrderStatus == OrderStatus.PART_CANCEL){
            oidOrderMap.remove(orderCmd.oid);
            if(orderCmd.direction == OrderDirection.BUY){
                //撤买
                DbUtil.addBalance(orderCmd.uid, -(orderCmd.price*finalMatchData.volume));
            }else if(orderCmd.direction == OrderDirection.SELL){
                //增加持仓 撤卖单
                DbUtil.addPosi(orderCmd.uid,orderCmd.code,-finalMatchData.volume,orderCmd.price);
            }else {
                log.error("wrong direction[{}]", orderCmd.direction);
            }
        }

        //通知委托终端
        config.getVertx().eventBus().publish(
                ORDER_NOTIFY_ADDR_PREFIX + orderCmd.uid,
                ""
        );
    }

}
