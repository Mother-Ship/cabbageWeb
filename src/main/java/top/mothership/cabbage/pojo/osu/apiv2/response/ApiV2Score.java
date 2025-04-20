package top.mothership.cabbage.pojo.osu.apiv2.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户成绩响应模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiV2Score {
    private long id;
    private long userId;
    private float accuracy;
    private String rank;  // SS, S, A, B, C, D
    private long totalScore;
    private int maxCombo;
    private boolean perfect;
    private String createdAt;
    private int countMiss;
    private int count50;
    private int count100;
    private int count300;
    private int countKatu;
    private int countGeki;

    private Beatmap beatmap;
    private BeatmapSet beatmapSet;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Beatmap {
        private long id;
        private String title;
        private String difficulty;
        private float starRating;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BeatmapSet {
        private long id;
        private String title;
        private String artist;
        private String creator;
    }
}