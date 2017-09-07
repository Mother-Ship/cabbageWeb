package top.mothership.cabbage.pojo;

//调用HTTPAPI发送消息之后的返回体
public class CqResponse {

    private String status;

    private int retcode;

    private RespData RespData;

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public RespData getRespData() {
        return RespData;
    }

    public void setRespData(RespData RespData) {
        this.RespData = RespData;
    }

    public void setRetcode(int retcode) {
        this.retcode = retcode;
    }

    public int getRetcode() {
        return this.retcode;
    }



}