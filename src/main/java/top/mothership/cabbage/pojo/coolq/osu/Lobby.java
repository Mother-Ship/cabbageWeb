package top.mothership.cabbage.pojo.coolq.osu;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用于存储本地房间的实体类
 */
@Data
public class Lobby {
    /**
     * 比赛的发起人
     */
    private Integer creator;
    private LocalDateTime reservedStartTime;
    private Match match;
    private List<Game> games;
    private String group;
}
