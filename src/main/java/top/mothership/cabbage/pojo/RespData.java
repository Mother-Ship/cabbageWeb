package top.mothership.cabbage.pojo;

public class RespData {
    //API消息中的返回消息体，目前没有作用
    private int id;

    private String nickname;

    public void setId(int id){
        this.id = id;
    }
    public int getId(){
        return this.id;
    }
    public void setNickname(String nickname){
        this.nickname = nickname;
    }
    public String getNickname(){
        return this.nickname;
    }

}