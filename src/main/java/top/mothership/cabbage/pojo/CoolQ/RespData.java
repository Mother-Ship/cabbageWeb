package top.mothership.cabbage.pojo.CoolQ;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class RespData {
    //API消息中，除了获取某个QQ详情之外情况的返回消息体，目前没有作用
    private Integer id;
    private String nickname;
    @SerializedName("group_name")
    private String groupName;
    @SerializedName("group_id")
    private Long groupId;
    private String cookies;
    private Long token;
    @SerializedName("coolq_edition")
    private String coolqEdition;
    @SerializedName("plugin_version")
    private String pluginVersion;

}