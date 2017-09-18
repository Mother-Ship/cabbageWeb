package top.mothership.cabbage.pojo;

import com.google.gson.annotations.SerializedName;

public class Beatmap {
    private String beatmapsetId;

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

    private String difficultyRating;


    public String getCreator() {
        return creator;
    }

    public Integer getApproved() {
        return approved;
    }

    public void setApproved(Integer approved) {
        this.approved = approved;
    }
    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getArtist() {

        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
