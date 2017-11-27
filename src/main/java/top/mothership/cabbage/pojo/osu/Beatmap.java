package top.mothership.cabbage.pojo.osu;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Beatmap {
    //提供兼容osu search的API
    @SerializedName("beatmapset_id")
    private Integer beatmapSetId;

    private Integer beatmapId;
    @SerializedName(value = "beatmap_status",alternate = {"approved"})
    private Integer approved;

    private String totalLength;
    @SerializedName(value = "play_length",alternate = {"hit_length"})
    private String hitLength;
    @SerializedName(value ="difficulty_name",alternate = {"version"})
    private String version;

    private String fileMd5;
    @SerializedName(value ="difficulty_cs",alternate = {"diff_size"})
    private String diffSize;
    @SerializedName(value ="difficulty_od",alternate = {"diff_overall"})
    private String diffOverall;
    @SerializedName(value ="difficulty_ar",alternate = {"diff_approach"})
    private String diffApproach;
    @SerializedName(value ="difficulty_hp",alternate = {"diff_drain"})
    private String diffDrain;
    @SerializedName(value ="gamemode",alternate = {"mode"})
    private String mode;
    @SerializedName(value ="date",alternate = {"approved_date"})
    private String approvedDate;

    private String lastUpdate;

    private String artist;

    private String title;
    @SerializedName(value ="mapper",alternate = {"creator"})
    private String creator;

    private String bpm;

    private String source;

    private String tags;

    private String genreId;

    private String languageId;
    @SerializedName(value ="favorites",alternate = {"favourite_count"})
    private String favouriteCount;
    @SerializedName("playcount")
    private String playCount;
    @SerializedName("passcount")
    private String passCount;

    private String maxCombo;
    @SerializedName(value = "difficultyrating", alternate = {"difficulty"})
    private String difficultyRating;
}
