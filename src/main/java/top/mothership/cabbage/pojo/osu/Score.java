package top.mothership.cabbage.pojo.osu;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Date;

/**
 * The type Score.
 */
@Data
public class Score {
    //仅用于get_user_best,其他API没有
    private Integer beatmapId;
    //这个不在API返回值，画BP的时候手动拼接的
    private String beatmapName;
    //这六个仅仅用于db解析
    private Byte mode;
    private Integer scoreVersion;
    private String mapMd5;
    private String repMd5;
    //永远是-1,在osr文件中代表LZMA流大小？
    private Integer size;
    //这个可能是get_scores的score_id值
    private Long onlineId;
    //用于存储BP的位数
    private Integer bpId;
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
    @SerializedName("enabled_mods")
    private Integer enabledMods;
    //更换为LocalDateTime会出反序列化异常
    private Date date;
    private String rank;
    //recent的API里压根没有这个字段
    private Float pp;
    //md ppysb
    @SerializedName("username")
    private String userName;
    private Integer userId;
    //为兼容get_match
    private Integer slot;
    private Integer team;
    private Integer pass;
}
