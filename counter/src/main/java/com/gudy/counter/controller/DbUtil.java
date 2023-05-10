package com.gudy.counter.controller;

import com.google.common.collect.ImmutableMap;
import com.gudy.counter.bean.res.Account;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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


}
