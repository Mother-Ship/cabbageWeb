package top.mothership.cabbage.pojo;

import java.util.Date;

public class User {
    private String username;
    private int user_id;
    private int count300;
    private int count100;
    private int count50;
    private int playcount;
    private float accuracy;
    private float pp_raw;
    private long ranked_score;
    private long total_score;
    private float level;
    private int pp_rank;
    private int count_rank_ss;
    private int count_rank_s;
    private int count_rank_a;
    private Date queryDate;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getCount300() {
        return count300;
    }

    public void setCount300(int count300) {
        this.count300 = count300;
    }

    public int getCount100() {
        return count100;
    }

    public void setCount100(int count100) {
        this.count100 = count100;
    }

    public int getCount50() {
        return count50;
    }

    public void setCount50(int count50) {
        this.count50 = count50;
    }

    public int getPlaycount() {
        return playcount;
    }

    public void setPlaycount(int playcount) {
        this.playcount = playcount;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public float getPp_raw() {
        return pp_raw;
    }

    public void setPp_raw(float pp_raw) {
        this.pp_raw = pp_raw;
    }

    public long getRanked_score() {
        return ranked_score;
    }

    public void setRanked_score(long ranked_score) {
        this.ranked_score = ranked_score;
    }

    public long getTotal_score() {
        return total_score;
    }

    public void setTotal_score(long total_score) {
        this.total_score = total_score;
    }

    public float getLevel() {
        return level;
    }

    public void setLevel(float level) {
        this.level = level;
    }

    public int getPp_rank() {
        return pp_rank;
    }

    public void setPp_rank(int pp_rank) {
        this.pp_rank = pp_rank;
    }

    public int getCount_rank_ss() {
        return count_rank_ss;
    }

    public void setCount_rank_ss(int count_rank_ss) {
        this.count_rank_ss = count_rank_ss;
    }

    public int getCount_rank_s() {
        return count_rank_s;
    }

    public void setCount_rank_s(int count_rank_s) {
        this.count_rank_s = count_rank_s;
    }

    public int getCount_rank_a() {
        return count_rank_a;
    }

    public void setCount_rank_a(int count_rank_a) {
        this.count_rank_a = count_rank_a;
    }

    public Date getQueryDate() {
        return queryDate;
    }

    public void setQueryDate(Date queryDate) {
        this.queryDate = queryDate;
    }
}
