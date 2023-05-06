package com.gudy.counter.service;

import com.gudy.counter.bean.res.Account;
import org.springframework.stereotype.Component;

@Component
public interface AccountService {
    //login
    Account login(long uid,String password,
                  String captcha,String captchaId) throws Exception;

    //缓存中是否存在登录信息
    boolean accountExistInCache(String token);
}
