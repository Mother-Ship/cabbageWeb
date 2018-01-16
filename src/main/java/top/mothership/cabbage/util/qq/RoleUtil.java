package top.mothership.cabbage.util.qq;

import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.User;

import java.util.ArrayList;
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

    public User addRole(String role, User user) {
        String newRole;
        //如果当前的用户组是creep，就直接改成现有的组
        if ("creep".equals(user.getRole())) {
            newRole = role;
        } else {
            //当用户不在想要添加的用户组的时候才添加 2017-11-27 20:45:20
            if (!Arrays.asList(user.getRole().split(",")).contains(role)) {
                newRole = user.getRole() + "," + role;
            } else {
                newRole = user.getRole();
            }

        }
        user.setRole(newRole);
        return user;
    }

    public User delRole(String role, User user) {
        //拿到原先的user，把role去掉
        String newRole;
        //这里如果不把Arrays.asList传入构造函数，而是直接使用会有个Unsupported异常
        //因为Arrays.asList做出的List是不可变的
        List<String> roles = new ArrayList<>(Arrays.asList(user.getRole().split(",")));
        //2017-11-27 21:04:36 增强健壮性，只有在含有这个role的时候才进行移除
        if (roles.contains(role)) {
            roles.remove(role);
        }
        if ("all".equals(role) || roles.size() == 0) {
            newRole = "creep";
        } else {
            //转换为字符串，此处得去除空格（懒得遍历+拼接了）
            newRole = roles.toString().replace(" ", "").
                    substring(1, roles.toString().replace(" ", "").indexOf("]"));
        }

        user.setRole(newRole);
        return user;
    }
}
