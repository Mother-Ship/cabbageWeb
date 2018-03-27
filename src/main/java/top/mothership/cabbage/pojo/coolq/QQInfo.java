package top.mothership.cabbage.pojo.coolq;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class QQInfo {
    @SerializedName("group_id")
    private Long groupId;
    @SerializedName("user_id")
    private Long userId;
    private String nickname;
    private String card;
    private String sex;
    private Integer age;
    private String area;
    @SerializedName("join_time")
    private Long joinTime;
    @SerializedName("last_sent_time")
    private Long lastSentTime;
    private String level;
    private String role;
    private Boolean unfriendly;
    private String title;
    @SerializedName("title_expire_time")
    private Long titleExpireTime;
    @SerializedName("card_changeable")
    private Boolean cardChangeable;

}
