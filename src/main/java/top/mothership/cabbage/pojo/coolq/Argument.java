package top.mothership.cabbage.pojo.coolq;

import lombok.Data;
import top.mothership.cabbage.pojo.osu.SearchParam;

import java.util.List;

/**
 * 从命令中解析出的参数
 */
@Data
public class Argument {

    private String subCommandLowCase;
    private Integer mode;
    private Integer day;
    private Integer num;
    private boolean text;
    private String username;
    private Integer userId;
    private Long qq;
    private Long hour;
    private List<String> usernames;
    private Long groupId;
    private String role;
    private String flag;
    private SearchParam searchParam;
    private String fileName;
    private String url;
    private Integer second;
    private Integer beatmapId;
    private String remark;
    private String bound;
}
