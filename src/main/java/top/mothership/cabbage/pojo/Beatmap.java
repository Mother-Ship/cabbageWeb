package top.mothership.cabbage.pojo;

import com.google.gson.annotations.SerializedName;

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

    @Override
    public String toString() {
        return "Beatmap{" +
                "beatmapSetId='" + beatmapSetId + '\'' +
                ", beatmapId='" + beatmapId + '\'' +
                ", artist='" + artist + '\'' +
                ", title='" + title + '\'' +
                ", version='" + version + '\'' +
                ", creator='" + creator + '\'' +
                ", bpm='" + bpm + '\'' +
                '}';
    }

    public String getBeatmapId() {
        return beatmapId;
    }

    public void setBeatmapId(String beatmapId) {
        this.beatmapId = beatmapId;
    }


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

    public String getBeatmapSetId() {
        return beatmapSetId;
    }

    public void setBeatmapSetId(String beatmapSetId) {
        this.beatmapSetId = beatmapSetId;
    }

    public String getHitLength() {
        return hitLength;
    }

    public void setHitLength(String hitLength) {
        this.hitLength = hitLength;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(String approvedDate) {
        this.approvedDate = approvedDate;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getGenreId() {
        return genreId;
    }

    public void setGenreId(String genreId) {
        this.genreId = genreId;
    }

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId;
    }

    public String getFavouriteCount() {
        return favouriteCount;
    }

    public void setFavouriteCount(String favouriteCount) {
        this.favouriteCount = favouriteCount;
    }

    public String getPlayCount() {
        return playCount;
    }

    public void setPlayCount(String playCount) {
        this.playCount = playCount;
    }

    public String getPassCount() {
        return passCount;
    }

    public void setPassCount(String passCount) {
        this.passCount = passCount;
    }

    public String getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(String maxCombo) {
        this.maxCombo = maxCombo;
    }

    public String getDifficultyRating() {
        return difficultyRating;
    }

    public void setDifficultyRating(String difficultyRating) {
        this.difficultyRating = difficultyRating;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(String totalLength) {
        this.totalLength = totalLength;
    }

    public String getBpm() {
        return bpm;
    }

    public void setBpm(String bpm) {
        this.bpm = bpm;
    }
    public String getDiffSize() {
        return diffSize;
    }

    public void setDiffSize(String diffSize) {
        this.diffSize = diffSize;
    }

    public String getDiffOverall() {
        return diffOverall;
    }

    public void setDiffOverall(String diffOverall) {
        this.diffOverall = diffOverall;
    }

    public String getDiffApproach() {
        return diffApproach;
    }

    public void setDiffApproach(String diffApproach) {
        this.diffApproach = diffApproach;
    }

    public String getDiffDrain() {
        return diffDrain;
    }

    public void setDiffDrain(String diffDrain) {
        this.diffDrain = diffDrain;
    }
}
