package top.mothership.cabbage.pojo;

import lombok.Data;

@Data
public class User {
    private int userId;
    private String role;
    private String QQ;
    //这两个仅用于网页登录
    private String username;
    private String pwd;
}
