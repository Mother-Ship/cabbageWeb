package top.mothership.cabbage.pojo.elo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Elo {
    private Integer code;
    private Integer user_id;
    private Integer rank;
    private BigDecimal elo;
    private BigDecimal init_elo;
}
