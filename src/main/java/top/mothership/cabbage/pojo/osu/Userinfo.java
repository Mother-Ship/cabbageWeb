package top.mothership.cabbage.pojo.osu;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.LocalDate;
@Data
public class Userinfo {
    /**
     * 这个字段不写入数据库
     */
    @SerializedName("username")
    private String userName;
    private Integer mode;
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
    private int countRankSsh;
    private int countRankS;
    private int countRankSh;
    private int countRankA;
    private LocalDate queryDate;

}
