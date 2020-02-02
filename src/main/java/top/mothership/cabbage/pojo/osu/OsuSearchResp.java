package top.mothership.cabbage.pojo.osu;

import lombok.Data;

import java.util.List;

@Data
public class OsuSearchResp {
    private Integer resultCount;
    private List<Beatmap> beatmaps;
}
