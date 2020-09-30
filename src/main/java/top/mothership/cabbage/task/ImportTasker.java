package top.mothership.cabbage.task;

import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.RedisDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.coolq.CqResponse;
import top.mothership.cabbage.pojo.coolq.QQInfo;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.util.osu.ScoreUtil;
import top.mothership.cabbage.util.osu.UserUtil;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
@Component
public class ImportTasker {
    private Logger logger = LogManager.getLogger(this.getClass());
    private  RedisDAO redisDAO;
    private  UserInfoDAO userInfoDAO;
    private  UserDAO userDAO;
    private ApiManager apiManager;
@Autowired
    public void setApiManager(ApiManager apiManager) {
        this.apiManager = apiManager;
    }

    @Autowired
    public void setRedisDAO(RedisDAO redisDAO) {
        this.redisDAO = redisDAO;
    }
    @Autowired
    public void setUserInfoDAO(UserInfoDAO userInfoDAO) {
        this.userInfoDAO = userInfoDAO;
    }
    @Autowired
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    private ScoreUtil scoreUtil;

    @Autowired
    public void setScoreUtil(ScoreUtil scoreUtil) {
        this.scoreUtil = scoreUtil;
    }


    private UserUtil userUtil;

    @Autowired
    public void setUserUtil(UserUtil userUtil) {
        this.userUtil = userUtil;
    }


    private CqManager cqManager;

    @Autowired
    public void setCqManager(CqManager cqManager) {
        this.cqManager = cqManager;
    }


    @Scheduled(cron = "0 8 4 * * ?")
    @SneakyThrows
    public void importUserInfo() {
        //似乎每分钟并发也就600+，不需要加延迟……
        java.util.Date start = Calendar.getInstance().getTime();
        //清掉前一天全部信息
        redisDAO.flushDb();
        userInfoDAO.clearTodayInfo(LocalDate.now().minusDays(1));
        cqManager.warn("开始进行每日登记");
        List<String> bannedList = new ArrayList<>();
        Integer successCount = 0;
        Integer bindedCount = 0;
        List<Integer> list = userDAO.listUserIdByRole(null, false);
        for (Integer aList : list) {
            Thread.sleep(100);
            User user = userDAO.getUser(null, aList);
            //如果上次活跃在一个月之前，或者没绑定不录入
            if (LocalDate.now().minusDays(30).isAfter(user.getLastActiveDate()) || user.getQq() == 0){
                continue;
            }
            //这里四个模式都要更新，但是只有主模式的才判断PP超限
            for (int i = 0; i < 4; i++) {
                Userinfo userinfo = apiManager.getUser(i, aList);
                if (userinfo != null) {
                    //将日期改为一天前写入
                    userinfo.setQueryDate(LocalDate.now().minusDays(1));
                    userInfoDAO.addUserInfo(userinfo);
                    //2018-3-16 17:47:51实验性特性：加入redis缓存
                    redisDAO.add(aList, userinfo);
                    redisDAO.expire(aList, 1, TimeUnit.DAYS);
                    logger.info("将" + userinfo.getUserName() + "在模式" + scoreUtil.convertGameModeToString(i) + "的数据录入成功");
                    if (!userinfo.getUserName().equals(user.getCurrentUname())) {
                        //如果检测到用户改名，取出数据库中的现用名加入到曾用名，并且更新现用名和曾用名
                        user = userUtil.renameUser(user, userinfo.getUserName());
                        userDAO.updateUser(user);
                    }
                    if (i == 0) {
                        handlePPOverflow(user, userinfo);
                        //借着这个if，每个玩家只计算一次模式
                        successCount++;
                        if(!user.getQq().equals(0L)){
                            bindedCount++;
                        }
                    }
                    //如果能获取到userinfo，就把banned设置为0
//                    if(user.isBanned()) {
                    user.setBanned(false);
                    userDAO.updateUser(user);
//                    }
                } else {
                    //将null的用户直接设为banned
                    if (!user.isBanned()) {
                        user.setBanned(true);
                        logger.info("检测到玩家" + user.getUserId() + "被Ban，已登记");
                        userDAO.updateUser(user);
                    }
                    if (!bannedList.contains(user.getCurrentUname())) {
                        //避免重复添加
                        bannedList.add(user.getCurrentUname());
                    }
                }
            }
        }
        CqMsg cqMsg = new CqMsg();
        cqMsg.setSelfId(1335734629L);
        cqMsg.setMessageType("private");
        cqMsg.setUserId(1335734657L);
        cqMsg.setMessage("录入完成，录入的玩家数量：" + successCount + "，以下玩家本次被标明已封禁：" + bannedList+"，绑定QQ的玩家数量："+bindedCount);
        cqManager.sendMsg(cqMsg);
    }
    private void handlePPOverflow(User user, Userinfo userinfo) {
        //如果用户在mp4组
        List<String> roles = new ArrayList<>(Arrays.asList(user.getRole().split(",")));
        if (roles.contains("mp5")) {
            CqMsg cqMsg = new CqMsg();
            cqMsg.setMessageType("group");
            cqMsg.setSelfId(1020640876L);
            cqMsg.setGroupId(201872650L);
            CqResponse<QQInfo> cqResponse = cqManager.getGroupMember(201872650L, user.getQq());
            if (cqResponse != null) {
                if (cqResponse.getData() != null) {
                    if (!cqResponse.getData().getCard().toLowerCase(Locale.CHINA).replace("_", " ")
                            .contains(user.getCurrentUname().toLowerCase(Locale.CHINA).replace("_", " "))) {
                        cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的群名片没有包含完整id。请修改名片。");
                        cqManager.sendMsg(cqMsg);
                    }
                }
            }
            //并且刷超了
            if (userinfo.getPpRaw() > 4500 + 0.49) {

                //回溯昨天这时候检查到的pp
                Userinfo lastDayUserinfo = userInfoDAO.getUserInfo(0, userinfo.getUserId(), LocalDate.now().minusDays(2));
                //如果昨天这时候的PP存在，并且也超了
                if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > 4500 + 0.49) {
                    //继续回溯前天这时候的PP
                    lastDayUserinfo = userInfoDAO.getUserInfo(0, userinfo.getUserId(), LocalDate.now().minusDays(3));
                    //如果前天这时候的PP存在，并且也超了
                    if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > 4500 + 0.49) {
                        //回溯大前天的PP
                        lastDayUserinfo = userInfoDAO.getUserInfo(0, userinfo.getUserId(), LocalDate.now().minusDays(4));
                        //如果大前天这个时候也超了，就飞了
                        if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > 4500 + 0.49) {
                            if (!user.getQq().equals(0L)) {
                                //2018-3-16 13:13:53似乎现在白菜踢人不会自动删组？在这里补上试试
                                user = userUtil.delRole("mp5", user);
                                userDAO.updateUser(user);
                                cqMsg.setUserId(user.getQq());
                                cqMsg.setMessageType("kick");
                                cqManager.sendMsg(cqMsg);
                                cqMsg.setMessageType("private");
                                cqMsg.setMessage("由于PP超限，已将你移出MP5群。");
                                cqManager.sendMsg(cqMsg);
                            }
                        } else {
                            //大前天没超
                            if (!user.getQq().equals(0L)) {
                                cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在1天后将你移除。");
                                cqManager.sendMsg(cqMsg);
                            }
                        }
                    } else {
                        //前天没超
                        if (!user.getQq().equals(0L)) {
                            cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在2天后将你移除。");
                            cqManager.sendMsg(cqMsg);
                        }
                    }
                } else {
                    //昨天没超
                    if (!user.getQq().equals(0L)) {
                        cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在3天后将你移除。");
                        cqManager.sendMsg(cqMsg);
                    }

                }
            }

        }
    }

}
