package top.mothership.cabbage.pojo.CoolQ;

import com.google.gson.annotations.SerializedName;

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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public Long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(Long joinTime) {
        this.joinTime = joinTime;
    }

    public Long getLastSentTime() {
        return lastSentTime;
    }

    public void setLastSentTime(Long lastSentTime) {
        this.lastSentTime = lastSentTime;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getUnfriendly() {
        return unfriendly;
    }

    public void setUnfriendly(Boolean unfriendly) {
        this.unfriendly = unfriendly;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getTitleExpireTime() {
        return titleExpireTime;
    }

    public void setTitleExpireTime(Long titleExpireTime) {
        this.titleExpireTime = titleExpireTime;
    }

    public Boolean getCardChangeable() {
        return cardChangeable;
    }

    public void setCardChangeable(Boolean cardChangeable) {
        this.cardChangeable = cardChangeable;
    }
}
