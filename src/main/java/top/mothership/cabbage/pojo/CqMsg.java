package top.mothership.cabbage.pojo;

import com.google.gson.annotations.SerializedName;
//酷Q收到消息之后通过HTTPAPI给白菜的POST请求体
public class CqMsg {
    @SerializedName("post_type")
    private String postType;
    private String event;
    @SerializedName("request_type")
    private String requestType;
    @SerializedName("message_type")
    private String messageType;
    @SerializedName("sub_type")
    private String subType;
    @SerializedName("group_id")
    private Integer groupId;
    @SerializedName("user_id")
    private Integer userId;
    private String message;
    @SerializedName("operator_id")
    private Integer operator_id;
    private Integer duration;

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getOperator_id() {
        return operator_id;
    }

    public void setOperator_id(Integer operator_id) {
        this.operator_id = operator_id;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}
