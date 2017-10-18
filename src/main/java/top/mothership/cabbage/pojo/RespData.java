package top.mothership.cabbage.pojo;

import com.google.gson.annotations.SerializedName;

public class RespData {
    //API消息中的返回消息体，目前没有作用
    private Integer id;
    private String nickname;
    @SerializedName("group_name")
    private String groupName;
    @SerializedName("group_id")
    private Long groupId;

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

}