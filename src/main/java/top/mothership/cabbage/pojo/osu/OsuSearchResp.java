package top.mothership.cabbage.pojo.osu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class OsuSearchResp {
    private int id;
    private String title;
    private String titleUnicode;
    private String artist;
    private String artistUnicode;
    private String creator;
    private String source;
    private String tags;
    private Covers covers;
    private int favouriteCount;
    private Object hype;
    private boolean nsfw;
    private int offset;
    private int playCount;
    private String previewUrl;
    private boolean spotlight;
    private String status;
    private Object trackId;
    private int userId;
    private boolean video;
    private double bpm;
    private boolean canBeHyped;
    private Object deletedAt;
    private boolean discussionEnabled;
    private boolean discussionLocked;
    private boolean isScoreable;
    private String lastUpdated;
    private String legacyThreadUrl;
    private NominationsSummary nominationsSummary;
    private int ranked;
    private String rankedDate;
    private boolean storyboard;
    private String submittedDate;
    private Availability availability;
    private boolean hasFavourited;
    private List<Beatmap> beatmaps;
    private List<String> packTags;
    private List<Integer> modes;
    private String lastChecked;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Covers {
        private String cover;
        private String cover2x;
        private String card;
        private String card2x;
        private String list;
        private String list2x;
        private String slimcover;
        private String slimcover2x;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NominationsSummary {
        private int current;
        private List<String> eligibleMainRulesets;
        private RequiredMeta requiredMeta;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RequiredMeta {
            private int mainRuleset;
            private int nonMainRuleset;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Availability {
        private boolean downloadDisabled;
        private Object moreInformation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Beatmap {
        private int beatmapsetId;
        private double difficultyRating;
        private int id;
        private String mode;
        private String status;
        private int totalLength;
        private int userId;
        private String version;
        private double accuracy;
        private double ar;
        private double bpm;
        private boolean convert;
        private int countCircles;
        private int countSliders;
        private int countSpinners;
        private double cs;
        private Object deletedAt;
        private double drain;
        private int hitLength;
        private boolean isScoreable;
        private String lastUpdated;
        private int modeInt;
        private int passcount;
        private int playcount;
        private int ranked;
        private String url;
        private String checksum;
        private int maxCombo;
    }
}
