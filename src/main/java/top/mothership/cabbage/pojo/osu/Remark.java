package top.mothership.cabbage.pojo.osu;

import lombok.Data;

@Data
public class Remark {
    private Long userId;
    private Long beatmapId;
    private String text;
}
