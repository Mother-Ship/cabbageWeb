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
    private Long repeatCount;
    private Long speakingCount;
    private String mainRole;
    private Boolean useEloBorder;
}
