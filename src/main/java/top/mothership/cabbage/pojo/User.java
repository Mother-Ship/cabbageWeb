package top.mothership.cabbage.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Integer userId;
    private String role;
    private Long qq;
    private String legacyUname;
    private String currentUname;
    private boolean banned;
    private Integer mode;
    //这两个仅用于网页登录
    private String username;
    private String pwd;
    private Long repeatCount;
    private Long speakingCount;

}
