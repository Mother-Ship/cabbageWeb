package top.mothership.cabbage.pojo;

import java.util.Date;

public class Userinfo {
    private String userName;
    private int userId;
    private int count300;
    private int count100;
    private int count50;
    private int playCount;
    private float accuracy;
    private float ppRaw;
    private long rankedScore;
    private long totalScore;
    private float level;
    private int ppRank;
    private int countRankSs;
    private int countRankS;
    private int countRankA;
    private Date queryDate;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public float getPpRaw() {
        return ppRaw;
    }

    public void setPpRaw(float ppRaw) {
        this.ppRaw = ppRaw;
    }

    public long getRankedScore() {
        return rankedScore;
    }

    public void setRankedScore(long rankedScore) {
        this.rankedScore = rankedScore;
    }

    public long getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(long totalScore) {
        this.totalScore = totalScore;
    }

    public float getLevel() {
        return level;
    }

    public void setLevel(float level) {
        this.level = level;
    }

    public int getPpRank() {
        return ppRank;
    }

    public void setPpRank(int ppRank) {
        this.ppRank = ppRank;
    }

    public int getCountRankSs() {
        return countRankSs;
    }

    public void setCountRankSs(int countRankSs) {
        this.countRankSs = countRankSs;
    }

    public int getCountRankS() {
        return countRankS;
    }

    public void setCountRankS(int countRankS) {
        this.countRankS = countRankS;
    }

    public int getCountRankA() {
        return countRankA;
    }

    public void setCountRankA(int countRankA) {
        this.countRankA = countRankA;
    }

    public Date getQueryDate() {
        return queryDate;
    }

    public void setQueryDate(Date queryDate) {
        this.queryDate = queryDate;
    }
}
