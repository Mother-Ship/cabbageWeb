package top.mothership.cabbage.util.qq;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class RoleUtil {

    public List<String> sortRoles(String role) {
        List<String> roles = Arrays.asList(role.split(","));
        //此处自定义实现排序方法
        //dev>分群>主群>比赛
        roles.sort((o1, o2) -> {
//            mp5s优先级得低于mp5和各个分部，考虑到比赛选手刷超了超过mp4的，也得低于mp4
            if (o1.contains("mp5s") && (
                    o2.equals("mp5") || o2.equals("mp4")
                            || o2.equals("mp5mc") || o2.equals("mp5chart"))) {
                return -1;
            }
            if (o2.contains("mp5s") && (
                    o1.equals("mp5") || o1.equals("mp4")
                            || o1.equals("mp5mc") || o1.equals("mp5chart"))) {
                return 1;
            }
//            //比赛期间mp5s优先级比mp5高，只比mc和chart低
//            if (o1.contains("mp5s") && (o2.equals("mp5mc") || o2.equals("mp5chart"))) {
//                return -1;
//            }
            //mp4s<mp4
            if (o1.contains("mp4s") && o2.equals("mp4")) {
                return -1;
            }
            if (o2.contains("mp4s") && o1.equals("mp4")) {
                return 1;
            }
            //dev大于一切
            if (o1.equals("dev")) {
                return 1;
            }
            if (o2.equals("dev")) {
                return -1;
            }
            return o1.compareTo(o2);
        });
        Collections.reverse(roles);
        return roles;
    }

}
