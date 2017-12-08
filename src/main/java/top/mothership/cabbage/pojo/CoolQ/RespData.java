package top.mothership.cabbage.pojo.CoolQ;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class RespData {
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