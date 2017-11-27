package top.mothership.cabbage.pojo;

import lombok.Data;

@Data
public class User {
    private Integer userId;
    private String role;
    private Long QQ;
    private String legacyUname;
    private String currentUname;
    private Integer banned;
    //这两个仅用于网页登录
    private String username;
    private String pwd;
}
