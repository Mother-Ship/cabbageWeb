package top.mothership.cabbage.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class EloChange {
    private Integer match_id;
    private BigDecimal last_elo;
    private BigDecimal sum_elo;
    private String tourney_name;
    private String time;
}
