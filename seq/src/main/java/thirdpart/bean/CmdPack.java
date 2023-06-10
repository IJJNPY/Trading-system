package thirdpart.bean;

import lombok.*;
import thirdpart.order.OrderCmd;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class CmdPack implements Serializable {

    private long packNo;

    private List<OrderCmd> orderCmds;
}
