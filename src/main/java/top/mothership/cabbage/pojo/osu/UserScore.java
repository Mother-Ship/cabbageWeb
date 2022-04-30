package top.mothership.cabbage.pojo.osu;

import com.google.gson.annotations.SerializedName;

public class UserScore {
    private Integer mods;
    private Integer combo;
    private Integer mode;
    private Integer count50;
    private Integer count100;
    private Integer count300;
    private Integer countMiss;

    public Integer getMods() {
        return mods;
    }

    public void setMods(Integer mods) {
        this.mods = mods;
    }

    public Integer getCombo() {
        return combo;
    }

    public void setCombo(Integer combo) {
        this.combo = combo;
    }

    public Integer getMode() {
        return mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
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
}
