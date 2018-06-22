package top.mothership.cabbage.pojo.coolq.osu;

import lombok.Data;

import java.util.List;

@Data
public class OsuSearchResp {
    private Integer resultCount;
    private List<Beatmap> beatmaps;
}
