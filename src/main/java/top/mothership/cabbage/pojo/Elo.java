package top.mothership.cabbage.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Elo {
    private Integer rank;
    private BigDecimal elo;
    private BigDecimal init_elo;
}
