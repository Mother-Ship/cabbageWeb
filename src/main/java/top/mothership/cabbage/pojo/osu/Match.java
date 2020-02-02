package top.mothership.cabbage.pojo.osu;

import lombok.Data;

import java.util.Date;

@Data
public class Match {
    private Integer matchId;
    private String name;
    private Date startTime;
    private Date endTime;
}
