package com.gudy.counter.controller;

import com.gudy.counter.bean.res.CaptchaRes;
import com.gudy.counter.bean.res.CounterRes;
import com.gudy.counter.cache.CacheType;
import com.gudy.counter.cache.RedisStringCache;
import com.gudy.counter.thirdpart.uuid.GudyUuid;
import com.gudy.counter.util.Captcha;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/login")
@Log4j2
public class LoginController {

//    @RequestMapping("/captcha")
//    public CounterRes captcha() throws Exception{
//        //1.生成验证码
//        Captcha captcha = new Captcha(120,40,4,10);
//        //2.将验证码的id与数值放入缓存
//        String uuid = String.valueOf(GudyUuid.getInstance().getUUID());
//        RedisStringCache.cache(uuid,captcha.getCode(), CacheType.CAPTCHA);
//        //3.使用base64编码图片，并返回给前台
//        CaptchaRes res = new CaptchaRes(uuid,captcha.getBase64ByteStr());
//        return new CounterRes(res);
//    }

    @RequestMapping("/captcha")
    public CounterRes captcha() throws Exception{
        //1.生成验证码 120 40 4个字符 噪点+线条
        Captcha captcha = new Captcha(120,40, 4,10);
        //2.将验证码<ID,验证码数值>放入缓存
        String uuid = String.valueOf(GudyUuid.getInstance().getUUID());
        RedisStringCache.cache(uuid,captcha.getCode(), CacheType.CAPTCHA);

        //3.使用base64编码图片，并返回给前台
        //uuid,base64
        CaptchaRes res = new CaptchaRes(uuid,captcha.getBase64ByteStr());
        return new CounterRes(res);
    }
//    @RequestMapping("/userlogin")
//    public CounterRes login() throws Exception{
//
//    }
}
