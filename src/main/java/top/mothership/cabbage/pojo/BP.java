package top.mothership.cabbage.pojo;

import java.util.Date;

public class BP {
    private Integer beatmap_id;

    private String beatmap_name;

    private Long score;

    private Integer maxcombo;

    private Integer count50;

    private Integer count100;

    private Integer count300;

    private Integer countmiss;

    private Integer countkatu;

    private Integer countgeki;

    private Integer perfect;

    private Integer enabled_mods;

    private Date date;

    private String rank;

    private Float pp;

    public String getBeatmap_name() {
        return beatmap_name;
    }

    public void setBeatmap_name(String beatmap_name) {
        this.beatmap_name = beatmap_name;
    }

    public Integer getBeatmap_id() {
        return beatmap_id;
    }

    public void setBeatmap_id(Integer beatmap_id) {
        this.beatmap_id = beatmap_id;
    }

    public Long getScore() {
        return score;
    }

    public void setScore(Long score) {
        this.score = score;
    }

    public Integer getMaxcombo() {
        return maxcombo;
    }

    public void setMaxcombo(Integer maxcombo) {
        this.maxcombo = maxcombo;
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

    public Integer getCountmiss() {
        return countmiss;
    }

    public void setCountmiss(Integer countmiss) {
        this.countmiss = countmiss;
    }

    public Integer getCountkatu() {
        return countkatu;
    }

    public void setCountkatu(Integer countkatu) {
        this.countkatu = countkatu;
    }

    public Integer getCountgeki() {
        return countgeki;
    }

    public void setCountgeki(Integer countgeki) {
        this.countgeki = countgeki;
    }

    public Integer getPerfect() {
        return perfect;
    }

    public void setPerfect(Integer perfect) {
        this.perfect = perfect;
    }

    public Integer getEnabled_mods() {
        return enabled_mods;
    }

    public void setEnabled_mods(Integer enabled_mods) {
        this.enabled_mods = enabled_mods;
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
