package com.gudy.counter.bean.res;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

//成交信息
@Setter
@Getter
@NoArgsConstructor
@ToString
public class TradeInfo {
    private int id;
    private long uid;
    private int code;
    private String name;
    private int direction;
    private long price;
    private long tcount;
    private int status;
    private int oid;

}
