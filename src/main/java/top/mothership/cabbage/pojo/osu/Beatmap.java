package top.mothership.cabbage.pojo.osu;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Date;

@Data
public class Beatmap {
    //提供兼容osu search的API
    @SerializedName("beatmapset_id")
    private Integer beatmapSetId;
    @SerializedName("beatmap_id")
    private Integer beatmapId;
    @SerializedName(value = "beatmap_status", alternate = {"approved"})
    private Integer approved;
    @SerializedName("total_length")
    private Integer totalLength;
    @SerializedName(value = "play_length", alternate = {"hit_length"})
    private Integer hitLength;
    @SerializedName(value = "difficulty_name", alternate = {"version"})
    private String version;
    @SerializedName("file_md5")
    private String fileMd5;
    @SerializedName(value = "difficulty_cs", alternate = {"diff_size"})
    private Float diffSize;
    @SerializedName(value = "difficulty_od", alternate = {"diff_overall"})
    private Float diffOverall;
    @SerializedName(value = "difficulty_ar", alternate = {"diff_approach"})
    private Float diffApproach;
    @SerializedName(value = "difficulty_hp", alternate = {"diff_drain"})
    private Float diffDrain;
    @SerializedName(value = "gamemode", alternate = {"mode"})
    private Integer mode;
    @SerializedName(value = "date", alternate = {"approved_date"})
    private Date approvedDate;
    @SerializedName("last_update")
    private Date lastUpdate;

    private String artist;

    private String title;
    @SerializedName(value = "mapper", alternate = {"creator"})
    private String creator;

    private Double bpm;

    private String source;

    private String tags;

    private Integer genreId;

    private Integer languageId;
    @SerializedName(value = "favorites", alternate = {"favourite_count"})
    private Integer favouriteCount;
    @SerializedName("playcount")
    private Long playCount;
    @SerializedName("passcount")
    private Long passCount;
    @SerializedName("max_combo")
    private Integer maxCombo;
    @SerializedName(value = "difficultyrating", alternate = {"difficulty"})
    private Double difficultyRating;

    @SerializedName("artist_unicode")
    private String artistUnicode;
    @SerializedName("title_unicode")
    private String titleUnicode;
}

