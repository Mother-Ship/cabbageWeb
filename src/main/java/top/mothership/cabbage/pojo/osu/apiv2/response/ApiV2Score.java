package top.mothership.cabbage.pojo.osu.apiv2.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.mothership.cabbage.pojo.osu.apiv2.constant.ApiV2Mode;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;

import static top.mothership.cabbage.pojo.osu.apiv2.constant.ApiV2Mode.*;

/**
 * 用户成绩响应模型
 */

@Data
@NoArgsConstructor
public class ApiV2Score {

    @Data
    public static class BeatmapScoreLazer {
        @SerializedName("position")
        private int position;

        @SerializedName("score")
        private ScoreLazer score;
    }

    @Data
    public static class ScoreLazer {
        @SerializedName("accuracy")
        private double accuracy;

        @SerializedName("beatmap_id")
        private long beatmapId;

        @SerializedName("best_id")
        private Long bestId;

        @SerializedName("build_id")
        private Long buildId;

        @SerializedName("classic_total_score")
        private long classicTotalScore;

        @SerializedName("ended_at")
        private String endedAt;

        @SerializedName("has_replay")
        private boolean hasReplay;

        @SerializedName("id")
        private long id;

        @SerializedName("is_perfect_combo")
        private boolean isPerfectCombo;

        @SerializedName("legacy_perfect")
        private boolean legacyPerfect;

        @SerializedName("legacy_score_id")
        private Long legacyScoreId;

        @SerializedName("legacy_total_score")
        private long legacyTotalScore;

        @SerializedName("max_combo")
        private long maxCombo;

        @SerializedName("maximum_statistics")
        private ScoreStatisticsLazer maximumStatistics;

        @SerializedName("mods")
        private Mod[] mods;

        @SerializedName("passed")
        private boolean passed;

        @SerializedName("pp")
        private Double pp;

        @SerializedName("preserve")
        private boolean preserve;

        @SerializedName("processed")
        private boolean processed;

        @SerializedName("rank")
        private String rank;

        @SerializedName("ranked")
        private boolean ranked;

        @SerializedName("ruleset_id")
        private int modeInt;

        @SerializedName("started_at")
        private String startedAt;

        @SerializedName("statistics")
        private ScoreStatisticsLazer statistics;

        @SerializedName("total_score")
        private long score;

        @SerializedName("type")
        private String kind;

        @SerializedName("user_id")
        private long userId;

        // SoloScoreJsonAttributesMultiplayer
        @SerializedName("playlist_item_id")
        private Long playlistItemId;

        @SerializedName("room_id")
        private Long roomId;

        @SerializedName("solo_score_id")
        private Long soloScoreId;

        // ScoreJsonAvailableIncludes
        @SerializedName("beatmap")
        private ApiV2Beatmap.Beatmap beatmap;

        @SerializedName("beatmapset")
        private ApiV2BeatmapSet.Beatmapset beatmapset;

        @SerializedName("user")
        private ApiV2User.User user;

        @SerializedName("weight")
        private ScoreWeight weight;

        @SerializedName("match")
        private Match match;

        @SerializedName("rank_country")
        private Long rankCountry;

        @SerializedName("rank_global")
        private Long rankGlobal;

        // ScoreJsonDefaultIncludes
        @SerializedName("current_user_attributes")
        private CurrentUserAttributes currentUserAttributes;

        // Tool properties (transient - not serialized)
        private boolean convertFromOld = false;

        // Cached values
        private String cachedJsonMods;
        private Double cachedLegacyAcc;
        private String cachedLegacyRank;
        private ScoreStatisticsLazer cachedConvertStatistics;

        // Computed properties
        public String getMode() {
            return ApiV2Mode.fromInt(modeInt);
        }

        public boolean isLazer() {
            return startedAt != null;
        }

        public boolean isClassic() {
            return startedAt == null;
        }

        public long getScoreAuto() {
            return isClassic() ? legacyTotalScore : score;
        }

        public String getRankAuto() {
            return isClassic() ? getLegacyRank() : rank;
        }

        public double getAccAuto() {
            return isClassic() ? getLegacyAcc() : accuracy;
        }

        public String getJsonMods() {
            if (cachedJsonMods != null) {
                return cachedJsonMods;
            }
            Gson gson = new Gson();
            cachedJsonMods = gson.toJson(mods);
            return cachedJsonMods;
        }

        public ScoreStatisticsLazer getConvertStatistics() {
            if (cachedConvertStatistics != null) {
                return cachedConvertStatistics;
            }

            if (convertFromOld) {
                cachedConvertStatistics = statistics;
                return statistics;
            }

            if (Objects.equals(getMode(), FRUITS)) {
                cachedConvertStatistics = new ScoreStatisticsLazer();
                cachedConvertStatistics.setCountGreat(statistics.getCountGreat());
                cachedConvertStatistics.setCountOk(statistics.getLargeTickHit());
                cachedConvertStatistics.setCountMeh(statistics.getSmallTickHit());
                cachedConvertStatistics.setCountKatu(statistics.getSmallTickMiss());
                cachedConvertStatistics.setCountGeki(statistics.getCountGeki());
                cachedConvertStatistics.setCountMiss(statistics.getCountMiss() + statistics.getLargeTickMiss());
            } else {
                cachedConvertStatistics = statistics;
            }

            return cachedConvertStatistics;
        }

        public String getLegacyRank() {
            if (cachedLegacyRank != null) {
                return cachedLegacyRank;
            }

            if ("F".equals(this.rank)) {
                cachedLegacyRank = "F";
                return cachedLegacyRank;
            }

            switch (getMode()) {
                case OSU: {
                    long totalHits = statistics.totalHits(getMode());
                    double greatRate = totalHits > 0 ? (double) statistics.getCountGreat() / totalHits : 1.0;
                    double mehRate = totalHits > 0 ? (double) statistics.getCountMeh() / totalHits : 1.0;

                    if (greatRate == 1.0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "XH" : "X";
                    } else if (greatRate > 0.9 && mehRate <= 0.01 && statistics.getCountMiss() == 0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "SH" : "S";
                    } else if ((greatRate > 0.8 && statistics.getCountMiss() == 0) || greatRate > 0.9) {
                        cachedLegacyRank = "A";
                    } else if ((greatRate > 0.7 && statistics.getCountMiss() == 0) || greatRate > 0.8) {
                        cachedLegacyRank = "B";
                    } else if (greatRate > 0.6) {
                        cachedLegacyRank = "C";
                    } else {
                        cachedLegacyRank = "D";
                    }
                    break;
                }
                case TAIKO: {
                    long totalHits = statistics.totalHits(getMode());
                    double greatRate = totalHits > 0 ? (double) statistics.getCountGreat() / totalHits : 1.0;

                    if (greatRate == 1.0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "XH" : "X";
                    } else if (greatRate > 0.9 && statistics.getCountMiss() == 0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "SH" : "S";
                    } else if ((greatRate > 0.8 && statistics.getCountMiss() == 0) || greatRate > 0.9) {
                        cachedLegacyRank = "A";
                    } else if ((greatRate > 0.7 && statistics.getCountMiss() == 0) || greatRate > 0.8) {
                        cachedLegacyRank = "B";
                    } else if (greatRate > 0.6) {
                        cachedLegacyRank = "C";
                    } else {
                        cachedLegacyRank = "D";
                    }
                    break;
                }
                case FRUITS: {
                    double acc = statistics.accuracy(getMode());

                    if (acc == 1.0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "XH" : "X";
                    } else if (acc > 0.98) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "SH" : "S";
                    } else if (acc > 0.94) {
                        cachedLegacyRank = "A";
                    } else if (acc > 0.9) {
                        cachedLegacyRank = "B";
                    } else if (acc > 0.85) {
                        cachedLegacyRank = "C";
                    } else {
                        cachedLegacyRank = "D";
                    }
                    break;
                }
                case MANIA: {
                    double acc = statistics.accuracy(getMode());

                    if (acc == 1.0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "XH" : "X";
                    } else if (acc > 0.95) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "SH" : "S";
                    } else if (acc > 0.9) {
                        cachedLegacyRank = "A";
                    } else if (acc > 0.8) {
                        cachedLegacyRank = "B";
                    } else if (acc > 0.7) {
                        cachedLegacyRank = "C";
                    } else {
                        cachedLegacyRank = "D";
                    }
                    break;
                }
                default: {
                    cachedLegacyRank = rank;
                    break;
                }
            }

            return cachedLegacyRank;
        }

        public double getLegacyAcc() {
            if (cachedLegacyAcc != null) {
                return cachedLegacyAcc;
            }

            if (convertFromOld) {
                cachedLegacyAcc = accuracy;
                return accuracy;
            }

            cachedLegacyAcc = statistics.accuracy(getMode());
            return cachedLegacyAcc;
        }
    }

    @Data
    public static class Match implements Serializable {
        @SerializedName("pass")
        private boolean pass;

        @SerializedName("slot")
        private long slot;

        @SerializedName("team")
        private long team;
    }

    @Data
    public static class CurrentUserAttributes implements Serializable{
        @SerializedName("pin")
        private CurrentUserPin pin;
    }

    @Data
    public static class CurrentUserPin implements Serializable{
        @SerializedName("is_pinned")
        private boolean isPinned;

        @SerializedName("score_id")
        private long scoreId;
    }
    @Data
    public static class ScoreWeight implements Serializable {
        @SerializedName("percentage")
        public double Percentage;

        @SerializedName("pp")
        public double PP ;
    }
    @Data
    public static class ScoreStatisticsLazer implements Serializable{
        @SerializedName("ok")
        private long countOk;

        @SerializedName("great")
        private long countGreat;

        @SerializedName("meh")
        private long countMeh;

        @SerializedName("perfect")
        private long countGeki;

        @SerializedName("good")
        private long countKatu;

        @SerializedName("miss")
        private long countMiss;

        @SerializedName("large_tick_hit")
        private long largeTickHit;

        @SerializedName("large_tick_miss")
        private long largeTickMiss;

        @SerializedName("small_tick_hit")
        private long smallTickHit;

        @SerializedName("small_tick_miss")
        private long smallTickMiss;

        @SerializedName("ignore_hit")
        private long ignoreHit;

        @SerializedName("ignore_miss")
        private long ignoreMiss;

        @SerializedName("large_bonus")
        private long largeBonus;

        @SerializedName("small_bonus")
        private long smallBonus;

        @SerializedName("slider_tail_hit")
        private long sliderTailHit;

        @SerializedName("combo_break")
        private long comboBreak;

        @SerializedName("legacy_combo_increase")
        private long legacyComboIncrease;

        public long passedObjects(String mode) {
            switch (mode) {
                case OSU:
                    return countGreat + countOk + countMeh + countMiss;
                case TAIKO:
                    return countGreat + countOk + countMiss;
                case FRUITS:
                    return countGreat + countOk + countMiss;
                case MANIA:
                    return countGeki + countKatu + countGreat + countOk + countMeh + countMiss;
                default:
                    return 0;
            }
        }

        public long totalHits(String mode) {
            switch (mode) {
                case OSU:
                    return countGreat + countOk + countMeh + countMiss;
                case TAIKO:
                    return countGreat + countOk + countMiss;
                case FRUITS:
                    return smallTickHit + largeTickHit + countGreat + countMiss + smallTickMiss + largeTickMiss;
                case MANIA:
                    return countGeki + countKatu + countGreat + countOk + countMeh + countMiss;
                default:
                    return 0;
            }
        }

        public double accuracy(String mode) {
            long totalHits = totalHits(mode);

            if (totalHits == 0) {
                return 0.0;
            }

            switch (mode) {
                case OSU:
                    return (double) ((6 * countGreat) + (2 * countOk) + countMeh) / (double) (6 * totalHits);
                case TAIKO:
                    return (double) ((2 * countGreat) + countOk) / (double) (2 * totalHits);
                case FRUITS:
                    return (double) (smallTickHit + largeTickHit + countGreat) / (double) totalHits;
                case MANIA:
                    return (double) (6 * (countGeki + countGreat) + 4 * countKatu + 2 * countOk + countMeh) / (double) (6 * totalHits);
                default:
                    return 0;
            }
        }
    }


    @Data
    public static class Mod {
        @SerializedName("acronym")
        private String acronym;

        @SerializedName("settings")
        private JsonObject settings;

        public boolean isClassic() {
            return "CL".equals(acronym);
        }

        public boolean isVisualMod() {
            return "HD".equals(acronym) || "FL".equals(acronym);
        }

        public boolean isSpeedChangeMod() {
            return "DT".equals(acronym) || "NC".equals(acronym) ||
                    "HT".equals(acronym) || "DC".equals(acronym);
        }

        public static Mod fromString(String mod) {
            Mod modObj = new Mod();
            modObj.setAcronym(mod);
            return modObj;
        }
    }
}