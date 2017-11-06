package top.mothership.cabbage.pojo.osu;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Date;
@Data
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

}
