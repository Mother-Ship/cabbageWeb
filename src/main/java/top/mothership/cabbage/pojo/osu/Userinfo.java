package top.mothership.cabbage.pojo.osu;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Date;
@Data
public class Userinfo {
    @SerializedName("username")
    private String userName;
    private int userId;
    private int count300;
    private int count100;
    private int count50;
    @SerializedName("playcount")
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

}
