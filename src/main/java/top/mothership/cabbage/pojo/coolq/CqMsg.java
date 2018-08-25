package top.mothership.cabbage.pojo.coolq;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Arg;
import top.mothership.cabbage.consts.ParameterEnum;

//酷Q收到消息之后通过HTTPAPI给白菜的POST请求体
//public class CqMsg implements Comparable<CqMsg> {
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CqMsg  {
    private Argument argument;
    private ParameterEnum[] required;
    private ParameterEnum[] optional;
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
    @SerializedName("self_id")
    private Long selfId;


}
