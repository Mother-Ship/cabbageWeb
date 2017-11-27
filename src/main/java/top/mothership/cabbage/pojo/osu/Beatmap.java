package top.mothership.cabbage.pojo.osu;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Beatmap {
    //提供兼容osu search的API
    @SerializedName("beatmapset_id")
    private Integer beatmapSetId;

    private Integer beatmapId;
    @SerializedName("beatmap_status")
    private Integer approved;

    private String totalLength;
    @SerializedName("play_length")
    private String hitLength;
    @SerializedName("difficulty_name")
    private String version;

    private String fileMd5;
    @SerializedName("difficulty_cs")
    private String diffSize;
    @SerializedName("difficulty_od")
    private String diffOverall;
    @SerializedName("difficulty_ar")
    private String diffApproach;
    @SerializedName("difficulty_hp")
    private String diffDrain;
    @SerializedName("gamemode")
    private String mode;
    @SerializedName("date")
    private String approvedDate;

    private String lastUpdate;

    private String artist;

    private String title;
    @SerializedName("mapper")
    private String creator;

    private String bpm;

    private String source;

    private String tags;

    private String genreId;

    private String languageId;
    @SerializedName("favorites")
    private String favouriteCount;
    @SerializedName("playcount")
    private String playCount;
    @SerializedName("passcount")
    private String passCount;

    private String maxCombo;
    @SerializedName(value = "difficultyrating", alternate = {"difficulty"})
    private String difficultyRating;
}
