package top.mothership.cabbage.pojo.CoolQ;

import com.google.gson.annotations.SerializedName;

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

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return this.id;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return this.nickname;
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    public Long getToken() {
        return token;
    }

    public void setToken(Long token) {
        this.token = token;
    }

    public String getCoolqEdition() {
        return coolqEdition;
    }

    public void setCoolqEdition(String coolqEdition) {
        this.coolqEdition = coolqEdition;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }
}