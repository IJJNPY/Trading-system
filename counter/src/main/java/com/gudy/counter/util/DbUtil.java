package com.gudy.counter.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gudy.counter.bean.res.Account;
import com.gudy.counter.bean.res.OrderInfo;
import com.gudy.counter.bean.res.PosiInfo;
import com.gudy.counter.bean.res.TradeInfo;
import com.gudy.counter.cache.CacheType;
import com.gudy.counter.cache.RedisStringCache;
import thirdpart.order.OrderCmd;
import thirdpart.order.OrderStatus;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Component
public class DbUtil {
    //如何在静态调用的工具类中注入spring管理的对象
    private static DbUtil dbUtil = null;

    private DbUtil(){}

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    private void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate){
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    private SqlSessionTemplate getSqlSessionTemplate(){
        return this.sqlSessionTemplate;
    }

    @PostConstruct
    private void init(){
        dbUtil = new DbUtil();
        dbUtil.setSqlSessionTemplate(this.sqlSessionTemplate);
    }

    //身份认证
    public static Account queryAccount(long uid, String password){
        return dbUtil.getSqlSessionTemplate().selectOne(
                "userMapper.queryAccount",
                ImmutableMap.of("UId",uid,"Password",password)
        );
    }

    public static void updateLoginTime(long uid, String nowDate, String nowTime){
        dbUtil.getSqlSessionTemplate().update(
                "userMapper.updateAccountLoginTime",
                ImmutableMap.of(
                        "UId",uid,
                        "ModifyDate",nowDate,
                        "ModifyTime",nowTime
                )
        );
    }

    //修改密码
    public static int updatePwd(long uid,String oldPwd,String newPwd){
        return dbUtil.getSqlSessionTemplate().update(
                "userMapper.updatePwd",
                ImmutableMap.of(
                        "UId",uid,
                        "NewPwd",newPwd,
                        "OldPwd",oldPwd));
    }

    //资金类
    public static long getBalance(long uid){
        Long res = dbUtil.getSqlSessionTemplate().selectOne(
                "orderMapper.queryBalance",
                ImmutableMap.of("UId",uid));
        if(res == null){
            return -1;
        }else {
            return res;
        }
    }

    public static void addBalance(long uid,long balance){
        dbUtil.getSqlSessionTemplate().update("orderMapper.updateBalance",
                ImmutableMap.of("UId", uid, "Balance", balance));
    }

    public static void minusBalance(long uid,long balance){
        addBalance(uid, -balance);
    }

    //持仓类，由于需要频繁查询，所以将数据存入缓存来缓解数据库的压力
    public static List<PosiInfo> getPosiList(long uid){
        //1.查缓存
        String suid = Long.toString(uid);
        String posiS = RedisStringCache.get(suid, CacheType.POSI);
        if(StringUtils.isEmpty(posiS)){
            //2.未查到
            List<PosiInfo> temp = dbUtil.getSqlSessionTemplate().selectList(
                    "orderMapper.queryPosi",
                    ImmutableMap.of("UId",uid)
            );
            List<PosiInfo> result = CollectionUtils.isEmpty(temp)? Lists.newArrayList():temp;

            //更新缓存
            RedisStringCache.cache(suid, JsonUtil.toJson(result),CacheType.POSI);
            return result;
        }else{
            //3.查到 命中缓存
            return JsonUtil.fromJsonArr(posiS,PosiInfo.class);
        }
    }
    public static PosiInfo getPosi(long uid,int code){
        return dbUtil.getSqlSessionTemplate().selectOne(
                "orderMapper.queryPosi",
                ImmutableMap.of("UId",uid,"Code",code));
    }

    public static void addPosi(long uid,int code,long volume,long price){
        //持仓是否存在
        PosiInfo posiInfo = getPosi(uid,code);
        if(posiInfo == null){
            //新增一条持仓
            insertPosi(uid,code,volume,price);
        }else {
            //修改持仓
            posiInfo.setCount(posiInfo.getCount()+volume);
            posiInfo.setCost(posiInfo.getCost()+price*volume);
            updatePosi(posiInfo);
        }
    }

    public static void minusPosi(long uid,int code,long volume,long price){
        addPosi(uid,code,-volume,price);
    }

    private static void insertPosi(long uid,int code,long volume,long price){
        dbUtil.getSqlSessionTemplate().insert("orderMapper.insertPosi",
                ImmutableMap.of("UId",uid,
                "Code",code,
                "Count",volume,
                "Cost",volume*price));
    }

    private static void updatePosi(PosiInfo posiInfo){
        dbUtil.getSqlSessionTemplate().insert("orderMapper.updatePosi",
                ImmutableMap.of("UId",posiInfo.getUid(),
                        "Code",posiInfo.getCode(),
                        "Count",posiInfo.getCount(),
                        "Cost",posiInfo.getCost())
        );
    }



    //委托类
    public static List<OrderInfo> getOrderList(long uid){
        //1.查缓存
        String suid = Long.toString(uid);
        String orderS = RedisStringCache.get(suid, CacheType.ORDER);
        if(StringUtils.isEmpty(orderS)){
            //2.未查到
            List<OrderInfo> temp = dbUtil.getSqlSessionTemplate().selectList(
                    "orderMapper.queryOrder",
                    ImmutableMap.of("UId",uid)
            );
            List<OrderInfo> result = CollectionUtils.isEmpty(temp)? Lists.newArrayList():temp;

            //更新缓存
            RedisStringCache.cache(suid, JsonUtil.toJson(result),CacheType.ORDER);
            return result;
        }else{
            //3.查到
            return JsonUtil.fromJsonArr(orderS,OrderInfo.class);
        }
    }

    //成交类
    public static List<TradeInfo> getTradeList(long uid){
        //1.查缓存
        String suid = Long.toString(uid);
        String tradeS = RedisStringCache.get(suid, CacheType.TRADE);
        if(StringUtils.isEmpty(tradeS)){
            //2.未查到
            List<TradeInfo> temp = dbUtil.getSqlSessionTemplate().selectList(
                    "orderMapper.queryTrade",
                    ImmutableMap.of("UId",uid)
            );
            List<TradeInfo> result = CollectionUtils.isEmpty(temp)? Lists.newArrayList():temp;

            //更新缓存
            RedisStringCache.cache(suid, JsonUtil.toJson(result),CacheType.TRADE);
            return result;
        }else{
            //3.查到
            return JsonUtil.fromJsonArr(tradeS,TradeInfo.class);
        }
    }
    //订单处理类
    public static int saveOrder(OrderCmd ordercmd){
        Map<String,Object> param = Maps.newHashMap();
        param.put("UId",ordercmd.uid);
        param.put("Code",ordercmd.code);
        param.put("Direction",ordercmd.direction.getDirection());
        param.put("Type",ordercmd.orderType.getType());
        param.put("Price",ordercmd.price);
        param.put("OCount",ordercmd.volume);
        param.put("TCount",0);
        param.put("Status", OrderStatus.NOT_SET.getCode());

        param.put("Data",TimeformatUtil.yyyyMMdd(ordercmd.timestamp));
        param.put("Time",TimeformatUtil.hhMMss(ordercmd.timestamp));

        int count = dbUtil.getSqlSessionTemplate().insert(
                "orderMapper.saveOrder",param
        );
        //判断是否成功
        if(count>0){
            return Integer.parseInt(param.get("ID").toString());
        }else{
            return -1;
        }
    }

    //股票信息查询
    public static List<Map<String,Object>> queryAllStockInfo(){
        return dbUtil.getSqlSessionTemplate()
                .selectList("stockMapper.queryStock");
    }


}
