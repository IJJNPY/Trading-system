package com.gudy.counter.controller;

import com.gudy.counter.bean.res.Account;
import com.gudy.counter.bean.res.CaptchaRes;
import com.gudy.counter.bean.res.CounterRes;
import com.gudy.counter.cache.CacheType;
import com.gudy.counter.cache.RedisStringCache;
import com.gudy.counter.service.AccountService;
import thirdpart.uuid.GudyUuid;
import com.gudy.counter.util.Captcha;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/login")
@Log4j2
public class LoginController {

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

    @Autowired
    private AccountService accountService;

    @RequestMapping("/userlogin")
    public CounterRes login(@RequestParam long uid,
                            @RequestParam String password,
                            @RequestParam String captcha,
                            @RequestParam String captchaid) throws Exception{
        Account account = accountService.login(uid,password,captcha,captchaid);

        if(account == null){
            return new CounterRes(CounterRes.FAIL,"Login failed,username/password/captcha incorrect",null);
        }else{
            return new CounterRes(account);
        }
    }

    @RequestMapping("/loginfail")
    public CounterRes loginFail(){
        return new CounterRes(CounterRes.RELOGIN,"Please Login again",null);

    }

    @RequestMapping("/logout")
    public CounterRes logout(@RequestParam String token){
        accountService.logout(token);
        return new CounterRes(CounterRes.SUCCESS,"退出成功",null);
    }

    @RequestMapping("/pwdupdate")
    public CounterRes pwdUpdate(@RequestParam int uid,
                                @RequestParam String oldpwd,
                                @RequestParam String newpwd){
        boolean res = accountService.updatePwd(uid,oldpwd,newpwd);
        if(res){
            return new CounterRes(CounterRes.SUCCESS,"密码更新成功",null);
        }else {
            return new CounterRes(CounterRes.FAIL,"旧密码输入错误",null);
        }
    }

}
