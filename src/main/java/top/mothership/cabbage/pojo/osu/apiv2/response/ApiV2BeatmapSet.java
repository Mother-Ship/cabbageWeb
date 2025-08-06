package top.mothership.cabbage.pojo.osu.apiv2.response;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.time.OffsetDateTime;
import java.net.URI;

public class ApiV2BeatmapSet {

    @Data
    public static class Beatmapset {
        @SerializedName("artist")
        private String artist;

        @SerializedName("artist_unicode")
        private String artistUnicode;

        @SerializedName("covers")
        private BeatmapCovers covers;

        @SerializedName("creator")
        private String creator;

        @SerializedName("favourite_count")
        private long favouriteCount;

        @SerializedName("hype")
        private BeatmapHype hype;

        @SerializedName("id")
        private long id;

        @SerializedName("nsfw")
        private boolean isNsfw;

        @SerializedName("offset")
        private long offset;

        @SerializedName("play_count")
        private long playCount;

        @SerializedName("preview_url")
        private String previewUrl;

        @SerializedName("source")
        private String source;

        @SerializedName("spotlight")
        private boolean spotlight;

        @SerializedName("status")
        private String status;

        @SerializedName("title")
        private String title;

        @SerializedName("title_unicode")
        private String titleUnicode;

        @SerializedName("user_id")
        private long userId;

        @SerializedName("video")
        private boolean video;

        @SerializedName("availability")
        private BeatmapAvailability availability;

        @SerializedName("bpm")
        private long bpm;

        @SerializedName("can_be_hyped")
        private boolean canBeHyped;

        @SerializedName("discussion_enabled")
        private boolean discussionEnabled;

        @SerializedName("discussion_locked")
        private boolean discussionLocked;

        @SerializedName("is_scoreable")
        private boolean isScoreable;

        @SerializedName("last_updated")
        private String lastUpdated;

        @SerializedName("legacy_thread_url")
        private URI legacyThreadUrl;

        @SerializedName("nominations_summary")
        private NominationsSummary nominationsSummary;

        @SerializedName("ranked")
        private long ranked;

        @SerializedName("ranked_date")
        private String rankedDate;

        @SerializedName("storyboard")
        private boolean storyboard;

        @SerializedName("submitted_date")
        private String submittedDate;

        @SerializedName("tags")
        private String tags;

        @SerializedName("ratings")
        private long[] ratings;

        @SerializedName("beatmaps")
        private ApiV2Beatmap[] beatmaps;

    }

    @Data
    public static class BeatmapAvailability {
        @SerializedName("download_disabled")
        private boolean downloadDisabled;

        @SerializedName("more_information")
        private String moreInformation;
    }

    @Data
    public static class BeatmapCovers {
        @SerializedName("cover")
        private String cover;

        @SerializedName("cover@2x")
        private String cover2x;

        @SerializedName("card")
        private String card;

        @SerializedName("card@2x")
        private String card2x;

        @SerializedName("list")
        private String list;

        @SerializedName("list@2x")
        private String list2x;

        @SerializedName("slimcover")
        private String slimCover;

        @SerializedName("slimcover@2x")
        private String slimCover2x;
    }

    @Data
    public static class BeatmapHype {
        @SerializedName("current")
        private int current;

        @SerializedName("required")
        private int required;
    }

    @Data
    public static class NominationsSummary {
        @SerializedName("current")
        private int current;

        @SerializedName("required")
        private int required;
    }


}