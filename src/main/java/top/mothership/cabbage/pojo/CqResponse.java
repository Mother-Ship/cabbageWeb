package top.mothership.cabbage.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.List;

//调用HTTPAPI发送消息之后的返回体
public class CqResponse {

    private String status;
@SerializedName("retcode")
    private int retCode;

    private RespData respData;

    public List<QQInfo> getData() {
        return data;
    }

    public void setData(List<QQInfo> data) {
        this.data = data;
    }

    private List<QQInfo> data;

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

    public RespData getRespData() {
        return respData;
    }

    public void setRespData(RespData respData) {
        this.respData = respData;
    }
}