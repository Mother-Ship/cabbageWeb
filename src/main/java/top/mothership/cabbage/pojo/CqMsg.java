package top.mothership.cabbage.pojo;

import com.google.gson.annotations.SerializedName;
//酷Q收到消息之后通过HTTPAPI给白菜的POST请求体
//public class CqMsg implements Comparable<CqMsg> {
public class CqMsg  {
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
    private Long groupId;
    @SerializedName("user_id")
    private Long userId;
    private String message;
    @SerializedName("operator_id")
    private Long operatorId;
    private Integer duration;
    @SerializedName("discuss_id")
    private Long discussId;
    private String flag;
    private String type;
    private Boolean approve;
    private String reason;
    private Boolean enable;
    private Long time;

    @Override
    public String toString() {
        return "CqMsg{" +
                "postType='" + postType + '\'' +
                ", event='" + event + '\'' +
                ", messageType='" + messageType + '\'' +
                ", subType='" + subType + '\'' +
                ", groupId=" + groupId +
                ", userId=" + userId +
                ", message='" + message + '\'' +
                ", operatorId=" + operatorId +
                ", discussId=" + discussId +
                ", time=" + time +
                '}';
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getApprove() {
        return approve;
    }

    public void setApprove(boolean approve) {
        this.approve = approve;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

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

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public Long getDiscussId() {
        return discussId;
    }

    public void setDiscussId(Long discussId) {
        this.discussId = discussId;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

//    @Override
//    public int compareTo(CqMsg o) {
//        return this.getTime().compareTo(o.getTime());
//    }
}
