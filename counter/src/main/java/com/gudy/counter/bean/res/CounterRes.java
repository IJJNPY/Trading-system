package com.gudy.counter.bean.res;

import lombok.AllArgsConstructor;

//通用返回格式
@AllArgsConstructor
public class CounterRes {


    private int code;


    private String message;


    private Object data;

    public CounterRes(Object data){
        this(0,"",data);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
