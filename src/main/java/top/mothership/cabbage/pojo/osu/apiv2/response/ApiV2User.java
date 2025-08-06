package top.mothership.cabbage.pojo.osu.apiv2.response;

import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import lombok.Data;
import java.time.OffsetDateTime;
import java.net.URI;

public class ApiV2User {

    @Data
    public static class UserExtended extends User {
        @SerializedName("discord")
        private String discord;

        @SerializedName("has_supported")
        private boolean hasSupported;

        @SerializedName("interests")
        private String interests;

        @SerializedName("join_date")
        private String joinDate;

        @SerializedName("kudosu")
        private JsonObject kudosu;

        @SerializedName("location")
        private String location;

        @SerializedName("max_blocks")
        private long maxBlocks;

        @SerializedName("max_friends")
        private long maxFriends;

        @SerializedName("occupation")
        private String occupation;

        /**
         * 官网用户的默认游玩模式
         */
        @SerializedName("playmode")
        private String mode;

        @SerializedName("playstyle")
        private String[] playstyle;

        @SerializedName("post_count")
        private long postCount;

        @SerializedName("profile_hue")
        private Long profileHue;

        @SerializedName("profile_order")
        private String[] profileOrder;

        @SerializedName("title")
        private String title;

        @SerializedName("title_url")
        private String titleUrl;

        @SerializedName("twitter")
        private String twitter;

        @SerializedName("website")
        private String website;

        @SerializedName("comments_count")
        private long commentsCount;

        @SerializedName("mapping_follower_count")
        private Long mappingFollowerCount;

        @Override
        public UserStatistics getStatistics() {
            if (getStatisticsCurrent() != null) {
                return getStatisticsCurrent();
            }

            switch (mode) {
                case "osu":
                    return getStatisticsModes().getOsu();
                case "taiko":
                    return getStatisticsModes().getTaiko();
                case "fruits":
                    return getStatisticsModes().getFruits();
                case "mania":
                    return getStatisticsModes().getMania();
                default:
                    throw new IllegalArgumentException("Invalid mode: " + mode);
            }
        }
    }

    @Data
    public static class User {
        @SerializedName("avatar_url")
        private URI avatarUrl;

        @SerializedName("country_code")
        private String countryCode;

        @SerializedName("default_group")
        private String defaultGroup;

        @SerializedName("id")
        private long id;

        @SerializedName("is_active")
        private boolean isActive;

        @SerializedName("is_bot")
        private boolean isBot;

        @SerializedName("is_deleted")
        private boolean isDeleted;

        @SerializedName("is_online")
        private boolean isOnline;

        @SerializedName("is_supporter")
        private boolean isSupporter;

        @SerializedName("last_visit")
        private String lastVisit;

        @SerializedName("pm_friends_only")
        private boolean pmFriendsOnly;

        @SerializedName("profile_colour")
        private String profileColor;

        @SerializedName("username")
        private String username;

        // UserJsonAvailableIncludes
        @SerializedName("account_history")
        private UserAccountHistory[] accountHistory;

        @SerializedName("badges")
        private UserBadge[] badges;

        @SerializedName("beatmap_playcounts_count")
        private Long beatmapPlaycountsCount;

        @SerializedName("country")
        private Country country;

        @SerializedName("cover")
        private UserCover cover;

        @SerializedName("favourite_beatmapset_count")
        private Long favouriteBeatmapsetCount;

        @SerializedName("follower_count")
        private Long followerCount;

        @SerializedName("graveyard_beatmapset_count")
        private Long graveyardBeatmapsetCount;

        @SerializedName("guest_beatmapset_count")
        private Long guestBeatmapsetCount;

        @SerializedName("loved_beatmapset_count")
        private Long lovedBeatmapsetCount;

        @SerializedName("pending_beatmapset_count")
        private Long pendingBeatmapsetCount;

        @SerializedName("ranked_beatmapset_count")
        private Long rankedBeatmapsetCount;

        @SerializedName("groups")
        private UserGroup[] groups;

        @SerializedName("rank_highest")
        private UserHighestRank highestRank;

        @SerializedName("is_admin")
        private Boolean isAdmin;

        @SerializedName("is_bng")
        private Boolean isBng;

        @SerializedName("is_full_bn")
        private Boolean isFullBn;

        @SerializedName("is_gmt")
        private Boolean isGmt;

        @SerializedName("is_limited_bn")
        private Boolean isLimitedBn;

        @SerializedName("is_moderator")
        private Boolean isModerator;

        @SerializedName("is_nat")
        private Boolean isNat;

        @SerializedName("is_restricted")
        private Boolean isRestricted;

        @SerializedName("is_silenced")
        private Boolean isSilenced;

        @SerializedName("medals")
        private MedalCompact[] medals;

        @SerializedName("monthly_playcounts")
        private MonthlyCount[] monthlyPlaycounts;

        @SerializedName("page")
        private UserPage page;

        @SerializedName("previous_usernames")
        private String[] previousUsernames;

        // 搞不懂为啥这里ppy要给两个rankhistory
        @SerializedName("rank_history")
        private RankHistory rankHistory;

        @SerializedName("replays_watched_counts")
        private MonthlyCount[] replaysWatchedCounts;

        @SerializedName("scores_best_count")
        private Long scoresBestCount;

        @SerializedName("scores_first_count")
        private Long scoresFirstCount;

        @SerializedName("scores_recent_count")
        private Long scoresRecentCount;

        @SerializedName("scores_pinned_count")
        private Long scoresPinnedCount;

        @SerializedName("statistics")
        private UserStatistics statisticsCurrent;

        @SerializedName("statistics_rulesets")
        private UserStatisticsModes statisticsModes;

        @SerializedName("support_level")
        private Long supportLevel;

        @SerializedName("active_tournament_banners")
        private JsonArray activeTournamentBanners;

        @SerializedName("active_tournament_banner")
        private JsonObject activeTournamentBanner;

        public UserStatistics getStatistics() {
            return statisticsCurrent != null ? statisticsCurrent : statisticsModes.getOsu();
        }
    }

    @Data
    public static class Country {
        @SerializedName("code")
        private String code;

        @SerializedName("display")
        private String display;

        @SerializedName("name")
        private String name;
    }

    @Data
    public static class UserCover {
        @SerializedName("custom_url")
        private URI customUrl;

        @SerializedName("url")
        private URI url;

        @SerializedName("id")
        private String id;
    }

    @Data
    public static class UserPage {
        @SerializedName("html")
        private String html;

        @SerializedName("raw")
        private String raw;
    }

    @Data
    public static class UserHighestRank {
        @SerializedName("rank")
        private long rank;

        @SerializedName("updated_at")
        private String updatedAt;
    }

    @Data
    public static class RankHistory {
        @SerializedName("mode")
        private String  mode;

        @SerializedName("data")
        private long[] data;
    }

    @Data
    public static class UserStatisticsModes {
        @SerializedName("osu")
        private UserStatistics osu;

        @SerializedName("taiko")
        private UserStatistics taiko;

        @SerializedName("fruits")
        private UserStatistics fruits;

        @SerializedName("mania")
        private UserStatistics mania;
    }

    @Data
    public static class UserStatistics {
        @SerializedName("level")
        private UserLevel level;

        @SerializedName("global_rank")
        private long globalRank;

        @SerializedName("pp")
        private double pp;

        @SerializedName("ranked_score")
        private long rankedScore;

        @SerializedName("hit_accuracy")
        private double hitAccuracy;

        @SerializedName("play_count")
        private long playCount;

        @SerializedName("play_time")
        private long playTime;

        @SerializedName("total_score")
        private long totalScore;

        @SerializedName("total_hits")
        private long totalHits;

        @SerializedName("maximum_combo")
        private long maximumCombo;

        @SerializedName("replays_watched_by_others")
        private long replaysWatchedByOthers;

        @SerializedName("is_ranked")
        private boolean isRanked;

        @SerializedName("grade_counts")
        private UserGradeCounts gradeCounts;

        @SerializedName("country_rank")
        private long countryRank;

        @SerializedName("rank")
        private UserRank rank;
    }

    @Data
    public static class UserGradeCounts {
        @SerializedName("ss")
        private int ss;

        @SerializedName("ssh")
        private int ssh;

        @SerializedName("s")
        private int s;

        @SerializedName("sh")
        private int sh;

        @SerializedName("a")
        private int a;
    }

    @Data
    public static class UserGroup {
        @SerializedName("colour")
        private String color;

        @SerializedName("description")
        private String description;

        @SerializedName("has_playmodes")
        private boolean hasModes;

        @SerializedName("id")
        private long id;

        @SerializedName("identifier")
        private String identifier;

        @SerializedName("is_probationary")
        private boolean isProbationary;

        @SerializedName("playmodes")
        private String[] modes;

        @SerializedName("name")
        private String name;

        @SerializedName("short_name")
        private String shortName;
    }

    @Data
    public static class UserLevel {
        @SerializedName("current")
        private int current;

        @SerializedName("progress")
        private int progress;
    }

    @Data
    public static class MonthlyCount {
        @SerializedName("start_date")
        private String startDate;

        @SerializedName("count")
        private int count;
    }

    @Data
    public static class UserRank {
        @SerializedName("country")
        private int country;
    }


    @Data
    public static class UserBadge {
        @SerializedName("awarded_at")
        private String awardedAt;

        @SerializedName("description")
        private String description;

        @SerializedName("image_url")
        private URI imageUrl;

        @SerializedName("url")
        private URI url;
    }

    @Data
    public static class UserAccountHistory {
        @SerializedName("id")
        private Long id;

        @SerializedName("timestamp")
        private String time;

        @SerializedName("description")
        private String description;

        @SerializedName("type")
        private HistoryType historyType;

        @SerializedName("length")
        private long seconds;

        @SerializedName("permanent")
        private boolean permanent;
    }

    public enum HistoryType {
        NOTE("note"),
        RESTRICTION("restriction"),
        TOURNAMENT_BAN("tournament_ban"),
        SILENCE("silence");

        private final String description;

        HistoryType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static HistoryType fromDescription(String description) {
            for (HistoryType type : values()) {
                if (type.description.equals(description)) {
                    return type;
                }
            }
            return NOTE; // default
        }
    }

    // Placeholder class that would need to be implemented
    public static class MedalCompact { }
}