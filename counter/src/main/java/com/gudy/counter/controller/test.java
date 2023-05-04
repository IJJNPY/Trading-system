package com.gudy.counter.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class test {

    @RequestMapping("/test1")
    public String hello(){
        return "hello everybody now test";
    }
}
