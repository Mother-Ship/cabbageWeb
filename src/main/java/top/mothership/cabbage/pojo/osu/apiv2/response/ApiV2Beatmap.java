package top.mothership.cabbage.pojo.osu.apiv2.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.time.OffsetDateTime;
import java.net.URI;

public class ApiV2Beatmap {

    @Data
    public static class BeatmapList {
        @SerializedName("beatmaps")
        private Beatmap[] beatmaps;
    }

    @Data
    public static class Beatmap {
        @SerializedName("beatmapset_id")
        private long beatmapsetId;

        @SerializedName("difficulty_rating")
        private double difficultyRating;

        @SerializedName("id")
        private long beatmapId;

        @SerializedName("mode")
        private String mode;

        @SerializedName("status")
        private Status status;

        @SerializedName("total_length")
        private long totalLength;

        @SerializedName("user_id")
        private long userId;

        @SerializedName("version")
        private String version;

        // Accuracy = OD
        @SerializedName("accuracy")
        private double od;

        @SerializedName("ar")
        private double ar;

        @SerializedName("bpm")
        private Double bpm;

        @SerializedName("convert")
        private boolean convert;

        @SerializedName("count_circles")
        private long countCircles;

        @SerializedName("count_sliders")
        private long countSliders;

        @SerializedName("count_spinners")
        private long countSpinners;

        @SerializedName("cs")
        private double cs;

        @SerializedName("deleted_at")
        private String deletedAt;

        @SerializedName("drain")
        private double hpDrain;

        @SerializedName("hit_length")
        private long hitLength;

        @SerializedName("is_scoreable")
        private boolean isScoreable;

        @SerializedName("last_updated")
        private String lastUpdated;

        @SerializedName("mode_int")
        private int modeInt;

        @SerializedName("passcount")
        private long passcount;

        @SerializedName("playcount")
        private long playcount;

        @SerializedName("ranked")
        private long ranked;

        @SerializedName("url")
        private URI url;

        @SerializedName("checksum")
        private String checksum;

        @SerializedName("beatmapset")
        private ApiV2BeatmapSet.Beatmapset beatmapset;

        @SerializedName("failtimes")
        private BeatmapFailtimes failtimes;

        @SerializedName("max_combo")
        private Long maxCombo;
    }

    @Data
    public static class BeatmapFailtimes {
        @SerializedName("fail")
        private int[] fail;

        @SerializedName("exit")
        private int[] exit;
    }

    public enum Status {
        UNKNOWN(""),
        GRAVEYARD("graveyard"),
        WIP("wip"),
        PENDING("pending"),
        RANKED("ranked"),
        APPROVED("approved"),
        QUALIFIED("qualified"),
        LOVED("loved");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static Status fromDescription(String description) {
            for (Status status : values()) {
                if (status.description.equals(description)) {
                    return status;
                }
            }
            return UNKNOWN;
        }
    }

}