package top.mothership.cabbage.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Score {
    private Integer beatmapId;

    private String beatmapName;

    private Long score;
@SerializedName("maxcombo")
    private Integer maxCombo;

    private Integer count50;

    private Integer count100;

    private Integer count300;
@SerializedName("countmiss")
    private Integer countMiss;
@SerializedName("countkatu")
    private Integer countKatu;
@SerializedName("countgeki")
    private Integer countGeki;

    private Integer perfect;

    private Integer enabledMods;

    private Date date;

    private String rank;

    private Float pp;
    //md ppysb
    @SerializedName("username")
    private String userName;

    private Integer userId;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {

        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getBeatmapId() {
        return beatmapId;
    }

    public void setBeatmapId(Integer beatmapId) {
        this.beatmapId = beatmapId;
    }

    public String getBeatmapName() {
        return beatmapName;
    }

    public void setBeatmapName(String beatmapName) {
        this.beatmapName = beatmapName;
    }

    public Long getScore() {
        return score;
    }

    public void setScore(Long score) {
        this.score = score;
    }

    public Integer getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(Integer maxCombo) {
        this.maxCombo = maxCombo;
    }

    public Integer getCount50() {
        return count50;
    }

    public void setCount50(Integer count50) {
        this.count50 = count50;
    }

    public Integer getCount100() {
        return count100;
    }

    public void setCount100(Integer count100) {
        this.count100 = count100;
    }

    public Integer getCount300() {
        return count300;
    }

    public void setCount300(Integer count300) {
        this.count300 = count300;
    }

    public Integer getCountMiss() {
        return countMiss;
    }

    public void setCountMiss(Integer countMiss) {
        this.countMiss = countMiss;
    }

    public Integer getCountKatu() {
        return countKatu;
    }

    public void setCountKatu(Integer countKatu) {
        this.countKatu = countKatu;
    }

    public Integer getCountGeki() {
        return countGeki;
    }

    public void setCountGeki(Integer countGeki) {
        this.countGeki = countGeki;
    }

    public Integer getPerfect() {
        return perfect;
    }

    public void setPerfect(Integer perfect) {
        this.perfect = perfect;
    }

    public Integer getEnabledMods() {
        return enabledMods;
    }

    public void setEnabledMods(Integer enabledMods) {
        this.enabledMods = enabledMods;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public Float getPp() {
        return pp;
    }

    public void setPp(Float pp) {
        this.pp = pp;
    }
}
