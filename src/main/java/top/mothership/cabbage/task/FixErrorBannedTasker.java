package top.mothership.cabbage.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Userinfo;

import java.time.LocalDate;
import java.util.List;
@Component
public class FixErrorBannedTasker {
    private Logger logger = LogManager.getLogger(this.getClass());

    private UserDAO userDAO;

    @Autowired
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }


    private ApiManager apiManager;

    @Autowired
    public void setApiManager(ApiManager apiManager) {
        this.apiManager = apiManager;
    }


    private UserInfoDAO userInfoDAO;

    @Autowired
    public void setUserInfoDAO(UserInfoDAO userInfoDAO) {
        this.userInfoDAO = userInfoDAO;
    }


//    @Scheduled(cron = "0 0 * * * ?")
    public void refreshBannedStatus() {
        List<User> list = userDAO.listBannedUser();
        for (User user : list) {
            Userinfo userinfo = apiManager.getUser(0, user.getUserId());
            if (userinfo != null) {
                //将日期改为一天前写入
                userinfo.setQueryDate(LocalDate.now().minusDays(1));
                userInfoDAO.addUserInfo(userinfo);
                logger.info("将" + userinfo.getUserName() + "的数据补录成功");
                if (!userinfo.getUserName().equals(user.getCurrentUname())) {
                    //如果检测到用户改名，取出数据库中的现用名加入到曾用名，并且更新现用名和曾用名
                    List<String> legacyUname = new GsonBuilder().create().fromJson(user.getLegacyUname(), new TypeToken<List<String>>() {
                    }.getType());
                    if (user.getCurrentUname() != null) {
                        legacyUname.add(user.getCurrentUname());
                    }
                    user.setLegacyUname(new Gson().toJson(legacyUname));
                    user.setCurrentUname(userinfo.getUserName());
                    logger.info("检测到玩家" + userinfo.getUserName() + "改名，已登记");
                }
                //如果能获取到userinfo，就把banned设置为0
                user.setBanned(false);
                userDAO.updateUser(user);
            }

        }

    }
}
