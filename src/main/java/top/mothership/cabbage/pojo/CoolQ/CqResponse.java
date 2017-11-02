package top.mothership.cabbage.pojo.CoolQ;

import com.google.gson.annotations.SerializedName;

//调用HTTPAPI发送消息之后的返回体
public class CqResponse <T>{

    private String status;
    @SerializedName("retcode")
    private int retCode;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    private T data;


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRetCode() {
        return retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

}