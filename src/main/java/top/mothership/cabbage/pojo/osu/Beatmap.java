package top.mothership.cabbage.pojo.osu;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Beatmap {
    @SerializedName("beatmapset_id")
    private String beatmapSetId;

    private String beatmapId;

    private Integer approved;

    private String totalLength;

    private String hitLength;

    private String version;

    private String fileMd5;

    private String diffSize;

    private String diffOverall;

    private String diffApproach;

    private String diffDrain;

    private String mode;

    private String approvedDate;

    private String lastUpdate;

    private String artist;

    private String title;

    private String creator;

    private String bpm;

    private String source;

    private String tags;

    private String genreId;

    private String languageId;

    private String favouriteCount;
    @SerializedName("playcount")
    private String playCount;
    @SerializedName("passCount")
    private String passCount;

    private String maxCombo;
    @SerializedName("difficultyrating")
    private String difficultyRating;
}
