package top.mothership.cabbage.pojo.elo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class EloChange {
    private Integer user_id;
    private BigDecimal elo_change;
    private BigDecimal match_id;
}
