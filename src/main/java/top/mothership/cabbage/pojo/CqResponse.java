package top.mothership.cabbage.pojo;

//调用HTTPAPI发送消息之后的返回体
public class CqResponse {

    private String status;

    private int retCode;

    private RespData respData;

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