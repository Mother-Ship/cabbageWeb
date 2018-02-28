package top.mothership.cabbage.util.osu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Userinfo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class UserUtil {
    private final UserInfoDAO userInfoDAO;
    private final UserDAO userDAO;
    private final ApiManager apiManager;
    private Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    public UserUtil(UserInfoDAO userInfoDAO, UserDAO userDAO, ApiManager apiManager) {
        this.userInfoDAO = userInfoDAO;
        this.userDAO = userDAO;
        this.apiManager = apiManager;
    }


    public void registerUser(Integer userId, Integer mode, Long QQ, String role) {
        //构造User对象写入数据库，如果指定了mode就使用指定mode
        Userinfo userFromAPI = null;
        for (int i = 0; i < 4; i++) {
            userFromAPI = apiManager.getUser(i, userId);
            if (LocalTime.now().isAfter(LocalTime.of(4, 0))) {
                userFromAPI.setQueryDate(LocalDate.now());
            } else {
                userFromAPI.setQueryDate(LocalDate.now().minusDays(1));
            }
            //写入一行userinfo
            userInfoDAO.addUserInfo(userFromAPI);
        }
        User user = new User(userId, role, QQ, "[]", userFromAPI.getUserName(), false, mode, null, null, 0L, 0L);
        userDAO.addUser(user);
    }


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
            //2018-1-29 15:30:59 兼容mp3
            if (o1.contains("mp4s") && o2.equals("mp4") || o2.equals("mp3")) {
                return -1;
            }
            if (o2.contains("mp4s") && o1.equals("mp4") || o1.equals("mp3")) {
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
