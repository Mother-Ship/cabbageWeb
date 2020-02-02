package top.mothership.cabbage.pojo.osu;

import lombok.Data;

import java.util.Date;
import java.util.List;
//MP API中的某一场游戏
@Data
public class Game {
    private List<Score> scores;
    private Integer gameId;
    private Date startTime;
    private Date endTime;
    private Integer beatmapId;
    private Integer playMode;
    private Integer matchType;
    private Integer scoringType;
    private Integer teamType;
    private Integer mods;
}
