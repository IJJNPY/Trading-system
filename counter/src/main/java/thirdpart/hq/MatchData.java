package thirdpart.hq;

import lombok.Builder;
import thirdpart.order.OrderStatus;

import java.io.Serializable;

@Builder
public class MatchData implements Serializable {

    //时间戳
    public long timestamp;

    //会员号
    public short mid;

    //委托编号
    public long oid;

    //委托状态
    public OrderStatus status;

    //成交编号
    public long tid;

    //撤单数量 成交数量
    public long volume;

    //成交价格
    public long price;

}
