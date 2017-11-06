package top.mothership.cabbage.pojo.CoolQ;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

//调用HTTPAPI发送消息之后的返回体
@Data
public class CqResponse <T>{

    private String status;
    @SerializedName("retcode")
    private int retCode;
    private T data;


}