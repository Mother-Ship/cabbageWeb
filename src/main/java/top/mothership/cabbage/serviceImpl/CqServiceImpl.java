package top.mothership.cabbage.serviceImpl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.annotation.GroupRoleControl;
import top.mothership.cabbage.consts.Base64Consts;
import top.mothership.cabbage.consts.OverallConsts;
import top.mothership.cabbage.consts.PatternConsts;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.CoolQ.CqResponse;
import top.mothership.cabbage.pojo.CoolQ.QQInfo;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.pojo.osu.SearchParam;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.util.osu.ScoreUtil;
import top.mothership.cabbage.util.qq.ImgUtil;
import top.mothership.cabbage.util.qq.RoleUtil;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;

/**
 * 普通命令进行业务处理的类
 *
 * @author QHS
 */
@Service
public class CqServiceImpl {
    private final ApiManager apiManager;
    private final CqManager cqManager;
    private final WebPageManager webPageManager;
    private final UserInfoDAO userInfoDAO;
    private final UserDAO userDAO;
    private final ImgUtil imgUtil;
    private final ScoreUtil scoreUtil;
    private final RoleUtil roleUtil;
    private Logger logger = LogManager.getLogger(this.getClass());

    /**
     * Instantiates a new Cq service.
     *
     * @param apiManager     the api manager
     * @param cqManager      the cq manager
     * @param webPageManager 网页相关抓取工具
     * @param userDAO        the user dao
     * @param userInfoDAO    the user info dao
     * @param imgUtil        the img util
     * @param scoreUtil      the score util
     * @param roleUtil
     */
    @Autowired
    public CqServiceImpl(ApiManager apiManager, CqManager cqManager, WebPageManager webPageManager, UserDAO userDAO, UserInfoDAO userInfoDAO, ImgUtil imgUtil, ScoreUtil scoreUtil, RoleUtil roleUtil) {
        this.apiManager = apiManager;
        this.cqManager = cqManager;
        this.webPageManager = webPageManager;
        this.userDAO = userDAO;
        this.userInfoDAO = userInfoDAO;
        this.imgUtil = imgUtil;
        this.scoreUtil = scoreUtil;
        this.roleUtil = roleUtil;
    }


    /**
     * 处理statu/stat/statme的方法。
     *
     * @param cqMsg QQ消息体
     */
    public void statUserInfo(CqMsg cqMsg) {

        //明天好好用文字整理下逻辑吧……
        String msg = cqMsg.getMessage();
        Matcher m = PatternConsts.REG_CMD_REGEX_NUM_PARAM.matcher(msg);
        if (!m.find()) {
            m = PatternConsts.REG_CMD_REGEX.matcher(msg);
            m.find();
        }
        String username;
        //预定义变量，day默认为1，这样才能默认和昨天的比较
        int day = 1;
        User user;
        Userinfo userFromAPI = null;
        boolean near = false;
        Userinfo userInDB = null;
        String role = null;
        int scoreRank;
        List<String> roles;
        //首先尝试解析数字，对各种数字异常情况进行处理并返回
        if (m.groupCount() == 3) {
            try {
                day = Integer.valueOf(m.group(3));
                if (day < 0) {
                    cqMsg.setMessage("白菜不会预知未来。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (LocalDate.now().minusDays(day).isBefore(LocalDate.of(2007, 9, 16))) {
                    cqMsg.setMessage("你要找史前时代的数据吗。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
            } catch (java.lang.NumberFormatException e) {
                cqMsg.setMessage("假使这些完全……不能用的参数，你再给他传一遍，你等于……你也等于……你也有泽任吧？");
                cqManager.sendMsg(cqMsg);
                return;
            }
        }



        /*
        statme:从数据库根据QQ取出user，为null则返回需要绑定的消息，否则将role取出；
        如果被ban则从数据库获取最近的作为最新信息，而day置为0；
        没有被ban则去官网根据user.id取出最新信息，判断day，从数据库根据user.id和day取出对比信息
         */
        switch (m.group(1).toLowerCase(Locale.CHINA)) {
            case "statme":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage("你没有绑定osu!id。请使用!setid 你的osuid 命令。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                role = user.getRole();
                if (user.isBanned()) {
                    //当数据库查到该玩家，并且被ban时，从数据库里取出最新的一份userinfo伪造
                    userFromAPI = userInfoDAO.getNearestUserInfo(user.getUserId(), LocalDate.now());
                    //尝试补上当前用户名
                    if (user.getCurrentUname() != null) {
                        userFromAPI.setUserName(user.getCurrentUname());
                    } else {
                        List<String> list = new GsonBuilder().create().fromJson(user.getLegacyUname(), new TypeToken<List<String>>() {
                        }.getType());
                        if (list.size() > 0) {
                            userFromAPI.setUserName(list.get(0));
                        } else {
                            userFromAPI.setUserName(String.valueOf(user.getUserId()));
                        }
                    }
                    day = 0;
                } else {
                    userFromAPI = apiManager.getUser(null, user.getUserId());
                    if (day > 0) {
                        userInDB = userInfoDAO.getUserInfo(userFromAPI.getUserId(), LocalDate.now().minusDays(day));
                        if (userInDB == null) {
                            userInDB = userInfoDAO.getNearestUserInfo(userFromAPI.getUserId(), LocalDate.now().minusDays(day));
                            near = true;
                        }
                    }
                }
                break;
            case "statu":
//                statu：从数据库根据uid取出user（为了兼容被ban玩家），为null则判断官网能否获取到、day是否大于0，
//                  如果能取到并大于0则添加到数据库，为0则什么也不做；取不到则返回错误信息
//                不为null则判断是否被ban，被ban则从数据库获取最近的作为最新信息，而day置为0；
//                没有被ban则从数据库根据user.id和day取出对比信息
                username = m.group(2);
                if ("3".equals(username)) {
                    cqMsg.setMessage("你们总是想查BanchoBot。\n可是BanchoBot已经很累了，她不想被查。\n她想念自己的小ppy，而不是被逼着查PP。\n你有考虑过这些吗？没有！你只考虑过你自己。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user = userDAO.getUser(null, Integer.valueOf(username));
                userFromAPI = apiManager.getUser(null, Integer.valueOf(username));
                if (user == null) {
                    if (userFromAPI == null) {
                        cqMsg.setMessage("没有从osu!api获取到uid为" + username + "的玩家信息，并且数据库也没有记载。");
                        cqManager.sendMsg(cqMsg);
                        return;
                    } else {
                        logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
                        //构造User对象写入数据库
                        user = new User(userFromAPI.getUserId(), "creep", 0L, "[]", userFromAPI.getUserName(), false, null, null, 0L, 0L);
                        userDAO.addUser(user);
                        if (LocalTime.now().isAfter(LocalTime.of(4, 0))) {
                            userFromAPI.setQueryDate(LocalDate.now());
                        } else {
                            userFromAPI.setQueryDate(LocalDate.now().minusDays(1));
                        }
                        //写入一行userinfo
                        userInfoDAO.addUserInfo(userFromAPI);
                        userInDB = userFromAPI;
                    }
                    role = "creep";
                } else if (user.isBanned()) {
                    //当数据库查到该玩家，并且被ban时，从数据库里取出最新的一份userinfo伪造
                    userFromAPI = userInfoDAO.getNearestUserInfo(user.getUserId(), LocalDate.now());
                    //尝试补上当前用户名
                    if (user.getCurrentUname() != null) {
                        userFromAPI.setUserName(user.getCurrentUname());
                    } else {
                        List<String> list = new GsonBuilder().create().fromJson(user.getLegacyUname(), new TypeToken<List<String>>() {
                        }.getType());
                        if (list.size() > 0) {
                            userFromAPI.setUserName(list.get(0));
                        } else {
                            userFromAPI.setUserName(String.valueOf(user.getUserId()));
                        }
                    }
                    day = 0;
                    role = user.getRole();
                } else {
                    role = user.getRole();
                    if (day > 0) {
                        userInDB = userInfoDAO.getUserInfo(userFromAPI.getUserId(), LocalDate.now().minusDays(day));
                        if (userInDB == null) {
                            userInDB = userInfoDAO.getNearestUserInfo(userFromAPI.getUserId(), LocalDate.now().minusDays(day));
                            near = true;
                        }
                    }
                }
                break;
            case "stat":
//                stat：从官网根据用户名取出最新信息，为null则返回消息；再根据返回的uid去数据库取出user，这里不需要做被ban检查，如果user为null
//                就判断day，大于0则添加到数据库，小于0则置用户组为creep；而如果user不为null就根据day取出数据库中的对比信息
                username = m.group(2);
                //处理彩蛋
                if ("白菜".equals(username)) {
                    cqMsg.setMessage("唉，没人疼没人爱，我是地里一颗小白菜。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有从osu!api获取到用户名为" + username + "的玩家信息。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (userFromAPI.getUserId() == 3) {
                    cqMsg.setMessage("你们总是想查BanchoBot。\n可是BanchoBot已经很累了，她不想被查。\n她想念自己的小ppy，而不是被逼着查PP。\n你有考虑过这些吗？没有！你只考虑过你自己。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user = userDAO.getUser(null, userFromAPI.getUserId());
                if (user == null) {

                    logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
                    //构造User对象写入数据库
                    user = new User(userFromAPI.getUserId(), "creep", 0L, "[]", userFromAPI.getUserName(), false, null, null, 0L, 0L);
                    userDAO.addUser(user);
                    if (LocalTime.now().isAfter(LocalTime.of(4, 0))) {
                        userFromAPI.setQueryDate(LocalDate.now());
                    } else {
                        userFromAPI.setQueryDate(LocalDate.now().minusDays(1));
                    }
                    //写入一行userinfo
                    userInfoDAO.addUserInfo(userFromAPI);
                    userInDB = userFromAPI;

                    role = "creep";
                } else {
                    role = user.getRole();
                    if (day > 0) {
                        userInDB = userInfoDAO.getUserInfo(userFromAPI.getUserId(), LocalDate.now().minusDays(day));
                        if (userInDB == null) {
                            userInDB = userInfoDAO.getNearestUserInfo(userFromAPI.getUserId(), LocalDate.now().minusDays(day));
                            near = true;
                        }
                    }
                }
                break;
            default:
                break;

        }


        roles = roleUtil.sortRoles(role);
        //获取score rank
        //gust？
        if (userFromAPI.getUserId() == 1244312
                || userFromAPI.getUserId() == 6149313
                || userFromAPI.getUserId() == 3213720
                //MFA
                || userFromAPI.getUserId() == 6854920) {
            scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 10000);
        } else {
            scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
        }
        //调用绘图类绘图(2017-10-19 14:09:04 roles改为List，排好序后直接取第一个)
        String result = imgUtil.drawUserInfo(userFromAPI, userInDB, roles.get(0), day, near, scoreRank);
        cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
        cqManager.sendMsg(cqMsg);

    }

    public void printBP(CqMsg cqMsg) {
        String username;
        Userinfo userFromAPI = null;
        User user;
        int num = 0;
        boolean text = true;
        Matcher m = PatternConsts.REG_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        switch (m.group(1).toLowerCase(Locale.CHINA)) {
            case "bp":
                //bp和bps是图片/文字的区别，如果是bp会更改这个text的bool值，用于后续控制
                text = false;
            case "bps":
                username = m.group(2);
                if ("白菜".equals(username)) {
                    cqMsg.setMessage("大白菜（学名：Brassica rapa pekinensis，异名Brassica campestris pekinensis或Brassica pekinensis）" +
                            "是一种原产于中国的蔬菜，又称“结球白菜”、“包心白菜”、“黄芽白”、“胶菜”等。(via 维基百科)");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有从osu!api获取到用户名为" + username + "的玩家信息。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "bpu":
                text = false;
            case "bpus":
                username = m.group(2);
                userFromAPI = apiManager.getUser(null, Integer.valueOf(username));
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有从osu!api获取到用户名为" + username + "的玩家信息。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "bpme":
            case "mybp":
                text = false;
            case "bpmes":
            case "mybps":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (user.isBanned()) {
                    cqMsg.setMessage("……期待你回来的那一天。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(null, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有获取到" + cqMsg.getUserId() + "绑定的uid为" + user.getUserId() + "的玩家信息。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            default:
                break;
        }

        List<Score> bps = apiManager.getBP(userFromAPI.getUserName(), null);

        ArrayList<Score> todayBP = new ArrayList<>();

        for (int i = 0; i < bps.size(); i++) {
            //对BP进行遍历，如果产生时间在24小时内
            if (bps.get(i).getDate().after(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))) {
                bps.get(i).setBpId(i);
                todayBP.add(bps.get(i));
            }
        }


        if (todayBP.size() == 0) {
            cqMsg.setMessage("[CQ:record,file=base64://" + Base64Consts.WAN_BU_LIAO_LA + "]");
            cqManager.sendMsg(cqMsg);
            cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "今天还。。\n这么悲伤的事情，不忍心说啊。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (text) {
            cqMsg.setMessage("请加上#n参数。以文本形式展现今日BP可能会造成刷屏。");
            cqManager.sendMsg(cqMsg);
            return;
        } else {
            for (Score aList : todayBP) {
                //对BP进行遍历，请求API将名称写入
                Beatmap map = apiManager.getBeatmap(aList.getBeatmapId());
                aList.setBeatmapName(map.getArtist() + " - " + map.getTitle() + " [" + map.getVersion() + "]");
            }
            String result = imgUtil.drawUserBP(userFromAPI, todayBP);
            cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
            cqManager.sendMsg(cqMsg);
        }
    }

    //很迷啊，在printBP里传userinfo cqmsg text等参数，aop拦截不到，只能让代码重复了_(:з」∠)_
    @GroupRoleControl(banned = {112177148L, 677545541L, 234219559L, 201872650L, 564679329L, 532783765L, 558518324L})
    public void printSpecifiedBP(CqMsg cqMsg) {
        String username;
        Userinfo userFromAPI = null;
        User user;
        int num = 0;
        boolean text = true;
        Matcher m = PatternConsts.REG_CMD_REGEX_NUM_PARAM.matcher(cqMsg.getMessage());
        m.find();
        try {
            num = Integer.valueOf(m.group(3));
            if (num < 0 || num > 100) {
                cqMsg.setMessage("其他人看不到的东西，白菜也看不到啦。");
                cqManager.sendMsg(cqMsg);
                return;
            }
        } catch (java.lang.NumberFormatException e) {
            cqMsg.setMessage("[CQ:record,file=base64://" + Base64Consts.AYA_YA_YA + "]");
            cqManager.sendMsg(cqMsg);
            return;
        }
        switch (m.group(1).toLowerCase(Locale.CHINA)) {
            case "bp":
                //bp和bps是图片/文字的区别，如果是bp会更改这个text的bool值，用于后续控制
                text = false;
            case "bps":
                username = m.group(2);
                if ("白菜".equals(username)) {
                    cqMsg.setMessage("大白菜（学名：Brassica rapa pekinensis，异名Brassica campestris pekinensis或Brassica pekinensis）" +
                            "是一种原产于中国的蔬菜，又称“结球白菜”、“包心白菜”、“黄芽白”、“胶菜”等。(via 维基百科)");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有从osu!api获取到用户名为" + username + "的玩家信息。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "bpu":
                text = false;
            case "bpus":
                username = m.group(2);
                userFromAPI = apiManager.getUser(null, Integer.valueOf(username));
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有从osu!api获取到用户名为" + username + "的玩家信息。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "bpme":
            case "mybp":
                text = false;
            case "bpmes":
            case "mybps":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (user.isBanned()) {
                    cqMsg.setMessage("……期待你回来的那一天。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(null, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有获取到" + cqMsg.getUserId() + "绑定的uid为" + user.getUserId() + "的玩家信息。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            default:
                break;
        }

        List<Score> bps = apiManager.getBP(userFromAPI.getUserName(), null);


        if (num > bps.size()) {
            cqMsg.setMessage("该玩家没有打出指定的bp……");
            cqManager.sendMsg(cqMsg);
            return;
        } else {
            if (text) {
                //list基于0，得-1
                Score score = bps.get(num - 1);
                logger.info("获得了玩家" + userFromAPI.getUserName() + "的第" + num + "个BP：" + score.getBeatmapId() + "，正在获取歌曲名称");
                Beatmap beatmap = apiManager.getBeatmap(score.getBeatmapId());
                cqMsg.setMessage(scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName()));
                cqManager.sendMsg(cqMsg);
            } else {
                //list基于0，得-1
                Score score = bps.get(num - 1);
                logger.info("获得了玩家" + userFromAPI.getUserName() + "的第" + num + "个BP：" + score.getBeatmapId() + "，正在获取歌曲名称");
                Beatmap map = apiManager.getBeatmap(score.getBeatmapId());
                String result = imgUtil.drawResult(userFromAPI, score, map);
                cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
                cqManager.sendMsg(cqMsg);
            }
        }
    }


    public void setId(CqMsg cqMsg) {
        String username;
        Userinfo userFromAPI = null;
        User user;
        Matcher m = PatternConsts.REG_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();

        username = m.group(2);
        userFromAPI = apiManager.getUser(username, null);
        if (userFromAPI == null) {
            cqMsg.setMessage("没有从osu!api获取到用户名为" + username + "的玩家信息。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        logger.info("尝试将" + userFromAPI.getUserName() + "绑定到QQ：" + cqMsg.getUserId() + "上");

        //只有这个QQ对应的id是null
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            //只有这个id对应的QQ是null
            user = userDAO.getUser(null, userFromAPI.getUserId());
            if (user == null) {
                //如果没有使用过白菜的话
                user = new User(userFromAPI.getUserId(), "creep", cqMsg.getUserId(), "[]", userFromAPI.getUserName(), false, null, null, 0L, 0L);
                userDAO.addUser(user);
                if (LocalTime.now().isAfter(LocalTime.of(4, 0))) {
                    userFromAPI.setQueryDate(LocalDate.now());
                } else {
                    userFromAPI.setQueryDate(LocalDate.now().minusDays(1));
                }
                //写入一行userinfo
                userInfoDAO.addUserInfo(userFromAPI);
                cqMsg.setMessage("将" + userFromAPI.getUserName() + "绑定到" + cqMsg.getUserId() + "成功。");
            } else {
                if (user.getQq() == 0) {
                    //由于reg方法中已经进行过登记了,所以这用的应该是update操作
                    user.setUserId(userFromAPI.getUserId());
                    user.setQq(cqMsg.getUserId());
                    userDAO.updateUser(user);
                    cqMsg.setMessage("将" + userFromAPI.getUserName() + "绑定到" + cqMsg.getUserId() + "成功。");
                } else {
                    cqMsg.setMessage("你的osu!账号已经绑定了QQ：" + user.getQq() + "，如果发生错误请联系妈妈船。");
                }
            }
        } else {
            userFromAPI = apiManager.getUser(null, user.getUserId());
            cqMsg.setMessage("你的QQ已经绑定了玩家：" + userFromAPI.getUserName() + "，如果发生错误请联系妈妈船。");
        }
        cqManager.sendMsg(cqMsg);

    }

    public void recent(CqMsg cqMsg) {
        Matcher m = PatternConsts.REG_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        Userinfo userFromAPI = null;
        User user;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (user.isBanned()) {
            cqMsg.setMessage("……期待你回来的那一天。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        userFromAPI = apiManager.getUser(null, user.getUserId());
        if (userFromAPI == null) {
            cqMsg.setMessage("没有获取到QQ" + cqMsg.getUserId() + "绑定的uid为" + user.getUserId() + "玩家的信息。");
            cqManager.sendMsg(cqMsg);
            return;
        }

        logger.info("检测到对" + userFromAPI.getUserName() + "的最近游戏记录查询");
        Score score = apiManager.getRecent(null, userFromAPI.getUserId());
        if (score == null) {
            cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "最近没有游戏记录。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        Beatmap beatmap = apiManager.getBeatmap(score.getBeatmapId());
        if (beatmap == null) {
            cqMsg.setMessage("网络错误：没有获取到Bid为" + score.getBeatmapId() + "的谱面信息。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        switch (m.group(1).toLowerCase(Locale.CHINA)) {
            case "rs":
                String resp = scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName());
                cqMsg.setMessage(resp);
                cqManager.sendMsg(cqMsg);
                break;
            default:
                break;
        }
        Matcher recentQianeseMatcher = PatternConsts.QIANESE_RECENT.matcher(m.group(1).toLowerCase(Locale.CHINA));
        if (recentQianeseMatcher.find()) {
            String filename = imgUtil.drawResult(userFromAPI, score, beatmap);
            cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
            cqManager.sendMsg(cqMsg);
        }
    }

    public void help(CqMsg cqMsg) {
        String img;
        if ((int) (Math.random() * 20) == 1) {
            img = imgUtil.drawImage(ImgUtil.images.get("helpTrick.png"));
            cqMsg.setMessage("[CQ:image,file=base64://" + img + "]");
        } else {
            img = imgUtil.drawImage(ImgUtil.images.get("help.png"));
        }
        cqMsg.setMessage("[CQ:image,file=base64://" + img + "]");
        cqManager.sendMsg(cqMsg);

    }

    public void sleep(CqMsg cqMsg) {
        Matcher m = PatternConsts.REG_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        Long hour;
        try {
            hour = Long.valueOf(m.group(2));
        } catch (java.lang.Exception e) {
            hour = 6L;
        }
        if (hour > 13) {
            hour = 13L;
        }
        if (hour == 0) {
            if (cqMsg.getUserId() == 546748348) {
                hour = 720L;
            } else {
                hour = 6L;
            }
        }
        if (hour < 0) {
            hour = 6L;
        }
        logger.info(cqMsg.getUserId() + "被自己禁言" + hour + "小时。");
        cqMsg.setMessage("[CQ:record,file=base64://" + Base64Consts.ZOU_HAO_BU_SONG + "]");
        cqManager.sendMsg(cqMsg);
        cqMsg.setMessageType("smoke");
        cqMsg.setDuration((int) (hour * 3600));
        cqManager.sendMsg(cqMsg);
    }

    public void firstPlace(CqMsg cqMsg) {
        Matcher m = PatternConsts.REG_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        Beatmap beatmap;
        Score score;
        int bid;
        try {
            bid = Integer.valueOf(m.group(2));
        } catch (java.lang.NumberFormatException e) {
            cqMsg.setMessage("It's a disastah!!");
            cqManager.sendMsg(cqMsg);
            return;
        }
        beatmap = apiManager.getBeatmap(bid);
        if (beatmap == null) {
            cqMsg.setMessage("提供的bid没有找到谱面信息。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        //一次性取2个
        List<Score> scores = apiManager.getFirstScore(bid, 2);
        if (scores.size() == 0) {
            cqMsg.setMessage("提供的bid没有找到#1成绩。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (!scores.get(0).getUserId().equals(userDAO.getUser(cqMsg.getUserId(), null).getUserId())) {
            cqMsg.setMessage("不是你打的#1不给看哦。\n如果你确定是你打的，看看是不是没登记osu!id？(使用!setid命令)");
            cqManager.sendMsg(cqMsg);
            return;
        }
        Userinfo userFromAPI = apiManager.getUser(null, scores.get(0).getUserId());
        //为了日志+和BP的PP计算兼容，补上get_score的API缺失的部分
        scores.get(0).setBeatmapName(beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]");
        scores.get(0).setBeatmapId(Integer.valueOf(beatmap.getBeatmapId()));
        String filename = imgUtil.drawFirstRank(beatmap, scores.get(0), userFromAPI, scores.get(0).getScore() - scores.get(1).getScore());
        cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
        cqManager.sendMsg(cqMsg);
    }


    @GroupRoleControl(banned = {112177148L, 677545541L, 234219559L, 201872650L, 564679329L, 532783765L, 558518324L})
    public void myScore(CqMsg cqMsg) {
        SearchParam searchParam = parseSearchKeyword(cqMsg);
        if (searchParam == null) {
            return;
        }
        User user;
        Userinfo userFromAPI;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        userFromAPI = apiManager.getUser(null, user.getUserId());
        if (userFromAPI == null) {
            cqMsg.setMessage("没有获取到QQ：" + cqMsg.getUserId() + "绑定的uid为" + user.getUserId() + "玩家的信息。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        Beatmap beatmap;

        if (searchParam.getBeatmapId() == null) {
            beatmap = webPageManager.searchBeatmap(searchParam);
        } else {
            //如果是纯数字的搜索词，则改为用API直接获取
            beatmap = apiManager.getBeatmap(searchParam.getBeatmapId());
        }
        logger.info("开始处理" + userFromAPI.getUserName() + "进行的谱面搜索，关键词为：" + searchParam);
        if (beatmap == null) {
            cqMsg.setMessage("根据提供的关键词：" + searchParam + "没有找到任何谱面。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        List<Score> scores = apiManager.getScore(beatmap.getBeatmapId(), user.getUserId());
        if (scores.size() > 0) {
            if (searchParam.getMods() != null) {
                for (Score s : scores) {
                    if (s.getEnabledMods().equals(searchParam.getMods())) {
                        String filename = imgUtil.drawResult(userFromAPI, s, beatmap);
                        cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
                        cqManager.sendMsg(cqMsg);
                        return;
                    }
                }
                cqMsg.setMessage("找到的谱面为：https://osu.ppy.sh/b/" + beatmap.getBeatmapId()
                        + "\n" + beatmap.getArtist() + " - " + beatmap.getTitle() + "[" + beatmap.getVersion() + "](" + beatmap.getCreator() + ")。" +
                        "\n你在该谱面没有指定Mod：" + searchParam.getModsString() + "的成绩。");
            } else {
                String filename = imgUtil.drawResult(userFromAPI, scores.get(0), beatmap);
                cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
            }
        } else {
            cqMsg.setMessage("找到的谱面为：https://osu.ppy.sh/b/" + beatmap.getBeatmapId()
                    + "\n" + beatmap.getArtist() + " - " + beatmap.getTitle() + "[" + beatmap.getVersion() + "](" + beatmap.getCreator() + ")" +
                    "，你在该谱面没有成绩。");
        }
        cqManager.sendMsg(cqMsg);
    }


    @GroupRoleControl(banned = {112177148L, 677545541L, 234219559L, 201872650L, 564679329L, 532783765L, 558518324L})
    public void search(CqMsg cqMsg) {
        SearchParam searchParam = parseSearchKeyword(cqMsg);
        if (searchParam == null) {
            return;
        }
        Beatmap beatmap;
        if (searchParam.getBeatmapId() == null) {
            beatmap = webPageManager.searchBeatmap(searchParam);
        } else {
            beatmap = apiManager.getBeatmap(searchParam.getBeatmapId());
        }
        logger.info("开始处理" + cqMsg.getUserId() + "进行的谱面搜索，关键词为：" + searchParam);

        if (beatmap == null) {
            cqMsg.setMessage("根据提供的关键词：" + searchParam + "没有找到任何谱面。");
            cqManager.sendMsg(cqMsg);
            return;
        } else {
            if (searchParam.getMods() == null) {
                //在search中，未指定mod即视为none
                searchParam.setMods(0);
            }
            String filename = imgUtil.drawBeatmap(beatmap, searchParam.getMods());
            cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]" + "\n" + "https://osu.ppy.sh/b/" + beatmap.getBeatmapId() + "\n"
                    + beatmap.getArtist() + " - " + beatmap.getTitle() + "[" + beatmap.getVersion() + "](" + beatmap.getCreator() + ")" + "\n" + "http://bloodcat.com/osu/s/" + beatmap.getBeatmapSetId());
        }
        cqManager.sendMsg(cqMsg);

    }

    public void chartMemberCmd(CqMsg cqMsg) {
        String role;
        Userinfo userFromAPI;
        Long qq;
        User user;
        String filename;
        String newRole;
        Matcher m = PatternConsts.CHART_ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        //面向mp4 5 chart组，相当于!setid+!sudo add
        List<Long> mpChartMember = new ArrayList<>();
        //加入两个chart组群员
        for (QQInfo q : cqManager.getGroupMembers(517183331L).getData()) {
            mpChartMember.add(q.getUserId());
        }
        for (QQInfo q : cqManager.getGroupMembers(635731109L).getData()) {
            mpChartMember.add(q.getUserId());
        }
        if (!mpChartMember.contains(cqMsg.getUserId())) {
            cqMsg.setMessage("[CQ:face,id=14]？");
            cqManager.sendMsg(cqMsg);
            return;
        }

        userFromAPI = apiManager.getUser(m.group(2), null);
        if (userFromAPI == null) {
            cqMsg.setMessage("没有从osu!api获取到用户名为" + m.group(2) + "的玩家信息。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        user = userDAO.getUser(null, userFromAPI.getUserId());
        switch (String.valueOf(cqMsg.getGroupId())) {
            case "564679329":
            case "517183331":
                role = "mp4";
                break;
            case "201872650":
            case "635731109":
                //MP5
                role = "mp5";
                break;
            default:
                cqMsg.setMessage("请不要在mp4/5/chart群之外的地方使用。");
                cqManager.sendMsg(cqMsg);
                return;
        }
        String resp = "";
        switch (m.group(1).toLowerCase(Locale.CHINA)) {
            case "add":
                qq = Long.valueOf(m.group(3));
                if (user == null) {
                    //进行登记，构建user存入，将userinfo加上时间存入
                    user = new User(userFromAPI.getUserId(), "creep", qq, "[]", userFromAPI.getUserName(), false, null, null, 0L, 0L);
                    userDAO.addUser(user);
                    if (LocalTime.now().isAfter(LocalTime.of(4, 0))) {
                        userFromAPI.setQueryDate(LocalDate.now());
                    } else {
                        userFromAPI.setQueryDate(LocalDate.now().minusDays(1));
                    }
                    userInfoDAO.addUserInfo(userFromAPI);
                    int scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
                    filename = imgUtil.drawUserInfo(userFromAPI, null, role, 0, false, scoreRank);
                    resp = "登记成功，用户组已修改为" + role;
                    resp = resp.concat("\n[CQ:image,file=base64://" + filename + "]");
                } else {
                    //拿到原先的user，把role拼上去，塞回去
                    //如果当前的用户组是creep，就直接改成现有的组
                    resp = "\n该用户之前已使用过白菜。原有用户组为：" + user.getRole();
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
                    resp += "，修改后的用户组为：" + newRole;
                    if (user.getQq().equals(0L)) {
                        user.setQq(qq);
                        resp += "\n绑定的QQ已登记为" + qq;
                    } else {
                        resp += "\n该玩家已经绑定了QQ：" + user.getQq() + "，没有做出修改。";
                    }
                    user.setRole(newRole);
                    userDAO.updateUser(user);
                    int scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
                    filename = imgUtil.drawUserInfo(userFromAPI, null, role, 0, false, scoreRank);
                    resp = resp.concat("\n[CQ:image,file=base64://" + filename + "]");
                }
                cqMsg.setMessage(resp);
                cqManager.sendMsg(cqMsg);
                break;
            case "del":
                if (user == null) {
                    cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "没有使用过白菜。");
                    cqManager.sendMsg(cqMsg);
                    return;
                } else {
                    resp = "已将玩家" + userFromAPI.getUserName() + "从" + role + "用户组中移除。";
                    List<String> roles = new ArrayList<>(Arrays.asList(user.getRole().split(",")));
                    //2017-11-27 21:04:36 增强健壮性，只有在含有这个role的时候才进行移除
                    if (roles.contains(role)) {
                        roles.remove(role);
                    }
                    if (roles.size() == 0) {
                        newRole = "creep";
                    } else {
                        //转换为字符串，此处得去除空格（懒得遍历+拼接了）
                        //2017-12-6 14:24:25当时我为啥不用json……看起来好不优雅啊这样
                        newRole = roles.toString().replace(" ", "").
                                substring(1, roles.toString().replace(" ", "").indexOf("]"));

                    }
                    resp += "\n修改后的用户组为：" + newRole;
                    user.setRole(newRole);
                    userDAO.updateUser(user);
                    cqMsg.setMessage(resp);
                    cqManager.sendMsg(cqMsg);
                }
                break;

        }
    }

    public void welcomeNewsPaper(CqMsg cqMsg) {
        logger.info("开始处理" + cqMsg.getUserId() + "在" + cqMsg.getGroupId() + "群的加群请求");
        String resp;
        switch (String.valueOf(cqMsg.getGroupId())) {
            case "201872650":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp5。请修改一下你的群名片(包含完整osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                break;
            case "564679329":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp4。请修改一下你的群名片(包含完整osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                break;
            case "210342787":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp3。请修改一下你的群名片(包含完整osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                break;
            case "537646635":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp乐园主群。请修改一下你的群名片(包含完整osu! id)，以下为mp乐园系列分群介绍：\n" +
                        "osu! MP乐园高rank部 592339532\n" +
                        "OSU! MP乐园2号群 (MP2) *(5500-7000pp):234219559\n" +
                        "OSU! MP乐园3号群 (MP3) *(4700-5800pp):210342787\n" +
                        "OSU! MP乐园4号群 (MP4) *(3600-5100pp):564679329\n" +
                        "OSU! MP乐园5号群 (MP5) *(2500-4000pp，无严格下限):201872650";
                break;
            case "112177148":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "],欢迎来到第一届MP4杯赛群。\n本群作为历届mp4选手聚集地，之后比赛结束后会将赛群合并到本群。";
                break;
            default:
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎加入本群。";
                break;
        }
        cqMsg.setMessageType("group");
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);

    }

    @GroupRoleControl(allBanned = true)
    public void cost(CqMsg cqMsg) {
        User user = null;
        Userinfo userFromAPI;
        Matcher m = PatternConsts.REG_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        String username;
        switch (m.group(1).toLowerCase(Locale.CHINA)) {
            case "costme":
            case "mycost":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage("你没有绑定osu!id。请使用!setid 你的osuid 命令。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (user.isBanned()) {
                    cqMsg.setMessage("玩家" + user.getCurrentUname() + "。。\n这么悲伤的事情，不忍心说啊。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "cost":
                username = m.group(2);
                //处理彩蛋
                if ("白菜".equals(username)) {
                    cqMsg.setMessage("[Crz]Makii  11:00:45\n" +
                            "...\n" +
                            "[Crz]Makii  11:01:01\n" +
                            "思考");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有从osu!api获取到用户名为" + username + "的玩家信息。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (userFromAPI.getUserId() == 3) {
                    cqMsg.setMessage("EASTER_EGG_OF_COST_BANCHO_BOT");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user = userDAO.getUser(null, userFromAPI.getUserId());
                if (user == null) {
                    logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
                    //构造User对象写入数据库
                    user = new User(userFromAPI.getUserId(), "creep", 0L, "[]", userFromAPI.getUserName(), false, null, null, 0L, 0L);
                    userDAO.addUser(user);
                    if (LocalTime.now().isAfter(LocalTime.of(4, 0))) {
                        userFromAPI.setQueryDate(LocalDate.now());
                    } else {
                        userFromAPI.setQueryDate(LocalDate.now().minusDays(1));
                    }
                    //写入一行userinfo
                    userInfoDAO.addUserInfo(userFromAPI);
                }
                break;
            default:
                break;

        }
        Map<String, Integer> map = webPageManager.getPPPlus(user.getUserId());
        if (map != null) {
            double cost = Math.pow((map.get("Jump") / 3000F), 0.8F)
                    * Math.pow((map.get("Flow") / 1500F), 0.6F)
                    + Math.pow((map.get("Speed") / 2000F), 0.8F)
                    * Math.pow((map.get("Stamina") / 2000F), 0.5F)
                    + (map.get("Accuracy") / 2250F);
//        cost=(jump/3000)^0.8*(flow/1500)^0.6+(speed/2000)^0.8*(stamina/2000)^0.5+accuracy/2250
            cqMsg.setMessage(user.getCurrentUname() + "的Jump：" + map.get("Jump")
                    + "\nFlow：" + map.get("Flow")
                    + "\nPrecision：" + map.get("Precision")
                    + "\nSpeed：" + map.get("Speed")
                    + "\nStamina：" + map.get("Stamina")
                    + "\nAccuracy：" + map.get("Accuracy")
                    + "\n在本届点金杯中，该玩家的Cost是：" + new DecimalFormat("#0.00").format(cost) + "。" +
                    "\n后期公式可能会变动，该Cost只对本次比赛有效。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        cqMsg.setMessage("由于网络原因（PP+的网站过于弱智），获取你的Cost失败……");
        cqManager.sendMsg(cqMsg);
        return;
    }

    public void recentPassed(CqMsg cqMsg) {
        Matcher m = PatternConsts.REG_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        Userinfo userFromAPI = null;
        User user;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (user.isBanned()) {
            cqMsg.setMessage("……期待你回来的那一天。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        userFromAPI = apiManager.getUser(null, user.getUserId());
        if (userFromAPI == null) {
            cqMsg.setMessage("没有获取到QQ" + cqMsg.getUserId() + "绑定的uid为" + user.getUserId() + "玩家的信息。");
            cqManager.sendMsg(cqMsg);
            return;
        }

        logger.info("检测到对" + userFromAPI.getUserName() + "的最近游戏记录查询");
        List<Score> scores = apiManager.getRecents(null, userFromAPI.getUserId());
        if (scores.size() == 0) {
            cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "最近没有游戏记录。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        Score score = null;
        for (Score s : scores) {
            if (!"F".equals(s.getRank())) {
                score = s;
                //找到第一个pass的分数
                break;
            }
        }
        if (score == null) {
            cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "最近没有Pass的游戏记录。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        Beatmap beatmap = apiManager.getBeatmap(score.getBeatmapId());
        if (beatmap == null) {
            cqMsg.setMessage("网络错误：没有获取到Bid为" + score.getBeatmapId() + "的谱面信息。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        switch (m.group(1).toLowerCase(Locale.CHINA)) {
            case "prs":
                String resp = scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName());
                cqMsg.setMessage(resp);
                cqManager.sendMsg(cqMsg);
                break;
            case "pr":
                String filename = imgUtil.drawResult(userFromAPI, score, beatmap);
                cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
                cqManager.sendMsg(cqMsg);
                break;
            default:
                break;
        }

    }

    private SearchParam parseSearchKeyword(CqMsg cqMsg) {
        SearchParam searchParam = new SearchParam();
        Matcher getKeyWordAndMod = PatternConsts.OSU_SEARCH_MOD_REGEX.matcher(cqMsg.getMessage());
        Integer modsNum = null;
        String mods = "None";
        String keyword;
        Double ar = null;
        Double od = null;
        Double cs = null;
        Double hp = null;
        if (getKeyWordAndMod.find()) {
            mods = getKeyWordAndMod.group(3);
            modsNum = scoreUtil.reverseConvertMod(mods);
            //如果字符串解析出错，会返回null，因此这里用null值来判断输入格式
            if (modsNum == null) {
                cqMsg.setMessage("请使用MOD的双字母缩写，不需要任何分隔符。" +
                        "\n接受的Mod有：NF EZ TD HD HR SD DT HT NC FL SO PF。");
                cqManager.sendMsg(cqMsg);
                return null;
            }
            searchParam.setMods(modsNum);
            searchParam.setModsString(mods);
            keyword = getKeyWordAndMod.group(2);
        } else {
            //未指定mod的情况下，mods和modnum依然为null
            getKeyWordAndMod = PatternConsts.REG_CMD_REGEX.matcher(cqMsg.getMessage());
            getKeyWordAndMod.find();
            keyword = getKeyWordAndMod.group(2);
        }

        Matcher allNumberKeyword = PatternConsts.ALL_NUMBER_SEARCH_KEYWORD.matcher(keyword);
        if (allNumberKeyword.find()) {
            searchParam.setBeatmapId(Integer.valueOf(allNumberKeyword.group(1)));
            return searchParam;
        }


        //比较菜，手动补齐参数
        if (!(keyword.endsWith("]") || keyword.endsWith(")") || keyword.endsWith("}"))) {
            //如果圆括号 方括号 花括号都没有
            keyword += "[](){}";
        }
        if (keyword.endsWith("]"))
            //如果有方括号
            keyword += "(){}";
        if (keyword.endsWith(")"))
            //如果有圆括号
            keyword += "{}";
        Matcher getArtistTitleEtc = PatternConsts.OSU_SEARCH_KETWORD.matcher(keyword);
        if (!getArtistTitleEtc.find()) {
            cqMsg.setMessage("请使用艺术家-歌曲标题[难度名](麻婆名){AR9.0OD9.0CS9.0HP9.0} +MOD双字母简称 的格式。\n" +
                    "所有参数都可以省略(但横线、方括号和圆括号不能省略)，四维顺序必须按AR OD CS HP排列。");
            cqManager.sendMsg(cqMsg);
            return null;
        } else {
            //没啥办法……手动处理吧，这个正则管不了了，去掉可能存在的空格
            String artist;
            //横杠之前的artist（手动去空格）
            if (getArtistTitleEtc.group(1).endsWith(" ")) {
                artist = getArtistTitleEtc.group(1).substring(0, getArtistTitleEtc.group(1).length() - 1);
            } else {
                artist = getArtistTitleEtc.group(1);
            }
            String title;
            if (getArtistTitleEtc.group(2).startsWith(" ")) {
                title = getArtistTitleEtc.group(2).substring(1);
            } else {
                title = getArtistTitleEtc.group(2);
            }
            searchParam.setArtist(artist);
            searchParam.setTitle(title);
            searchParam.setDiffName(getArtistTitleEtc.group(3));
            searchParam.setMapper(getArtistTitleEtc.group(4));
            //处理四维字符串
            String fourDemensions = getArtistTitleEtc.group(5);
            if (!"".equals(fourDemensions)) {
                Matcher getFourDemens = PatternConsts.OSU_SEARCH_FOUR_DEMENSIONS_REGEX.matcher(fourDemensions);
                getFourDemens.find();
                if (getFourDemens.group(1) != null) {
                    ar = Double.valueOf(getFourDemens.group(1));
                }
                if (getFourDemens.group(2) != null) {
                    od = Double.valueOf(getFourDemens.group(2));
                }
                if (getFourDemens.group(3) != null) {
                    cs = Double.valueOf(getFourDemens.group(3));
                }
                if (getFourDemens.group(4) != null) {
                    hp = Double.valueOf(getFourDemens.group(4));
                }

            }
            searchParam.setAr(ar);
            searchParam.setOd(od);
            searchParam.setCs(cs);
            searchParam.setHp(hp);

            return searchParam;
        }

    }
    @Scheduled(cron = "0 0 4 * * ?")
    public void importUserInfo() {
        //似乎每分钟并发也就600+，不需要加延迟……
        java.util.Date start = Calendar.getInstance().getTime();
        //清掉前一天全部信息
        userInfoDAO.clearTodayInfo(LocalDate.now().minusDays(1));
        logger.info("开始进行每日登记");
        List<Integer> list = userDAO.listUserIdByRole(null, false);
        for (Integer aList : list) {
            User user = userDAO.getUser(null, aList);
            Userinfo userinfo = apiManager.getUser(null, aList);
            if (userinfo != null) {
                //将日期改为一天前写入
                userinfo.setQueryDate(LocalDate.now().minusDays(1));
                userInfoDAO.addUserInfo(userinfo);
                logger.info("将" + userinfo.getUserName() + "的数据录入成功");
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
                //如果用户在mp4组
                List<String> roles = new ArrayList<>(Arrays.asList(user.getRole().split(",")));
                if (roles.contains("mp4")) {
                    CqMsg cqMsg = new CqMsg();
                    cqMsg.setMessageType("group");
                    cqMsg.setGroupId(564679329L);
                    //并且刷超了
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
                    if (userinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp4PP")) + 0.49) {

                        //回溯昨天这时候检查到的pp
                        Userinfo lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(2));
                        //如果昨天这时候的PP存在，并且也超了
                        if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp4PP")) + 0.49) {
                            //继续回溯前天这时候的PP
                            lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(3));
                            //如果前天这时候的PP存在，并且也超了
                            if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp4PP")) + 0.49) {
                                //回溯大前天的PP
                                lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(4));
                                //如果大前天这个时候也超了，就飞了
                                if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp4PP")) + 0.49) {
                                    if (!user.getQq().equals(0L)) {
                                        cqMsg.setUserId(user.getQq());
                                        cqMsg.setMessageType("kick");
                                        cqManager.sendMsg(cqMsg);
                                        cqMsg.setMessageType("private");
                                        cqMsg.setMessage("由于PP超限，已将你移出MP4群。请考虑加入mp3群：234219559。");
                                        cqManager.sendMsg(cqMsg);
                                        //清除用户组，并且踢人
                                        String newRole;
                                        roles.remove("mp4");
                                        if (roles.size() == 0) {
                                            newRole = "creep";
                                        } else {
                                            newRole = roles.toString().replace(" ", "").
                                                    substring(1, roles.toString().replace(" ", "").indexOf("]"));
                                        }
                                        user.setRole(newRole);
                                        userDAO.updateUser(user);
                                    }
                                } else {
                                    //大前天没超
                                    if (!user.getQq().equals(0L)) {
                                        cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在1天后将你移除。请考虑加入mp3群：234219559。");
                                        cqManager.sendMsg(cqMsg);
                                    }
                                }
                            } else {
                                //前天没超
                                if (!user.getQq().equals(0L)) {
                                    cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在2天后将你移除。请考虑加入mp3群：234219559。");
                                    cqManager.sendMsg(cqMsg);
                                }
                                continue;
                            }
                        } else {
                            //昨天没超
                            if (!user.getQq().equals(0L)) {
                                cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在3天后将你移除。请考虑加入mp3群：234219559。");
                                cqManager.sendMsg(cqMsg);
                            }

                        }
                        continue;
                    }

                }

                if (roles.contains("mp5")) {
                    CqMsg cqMsg = new CqMsg();
                    cqMsg.setMessageType("group");
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
                    if (userinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp5PP")) + 0.49) {

                        //回溯昨天这时候检查到的pp
                        Userinfo lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(2));
                        //如果昨天这时候的PP存在，并且也超了
                        if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp5PP")) + 0.49) {
                            //继续回溯前天这时候的PP
                            lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(3));
                            //如果前天这时候的PP存在，并且也超了
                            if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp5PP")) + 0.49) {
                                //回溯大前天的PP
                                lastDayUserinfo = userInfoDAO.getUserInfo(aList, LocalDate.now().minusDays(4));
                                //如果大前天这个时候也超了，就飞了
                                if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString("mp5PP")) + 0.49) {
                                    if (!user.getQq().equals(0L)) {
                                        cqMsg.setUserId(user.getQq());
                                        cqMsg.setMessageType("kick");
                                        cqManager.sendMsg(cqMsg);
                                        cqMsg.setMessageType("private");
                                        cqMsg.setMessage("由于PP超限，已将你移出MP5群。请考虑加入mp4群：564679329。");
                                        cqManager.sendMsg(cqMsg);
                                        String newRole;

                                        roles.remove("mp5");
                                        if (roles.size() == 0) {
                                            newRole = "creep";
                                        } else {
                                            newRole = roles.toString().replace(" ", "").
                                                    substring(1, roles.toString().replace(" ", "").indexOf("]"));
                                        }
                                        user.setRole(newRole);
                                        userDAO.updateUser(user);
                                    }
                                } else {
                                    //大前天没超
                                    if (!user.getQq().equals(0L)) {
                                        cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在1天后将你移除。请考虑加入mp4群：564679329。");
                                        cqManager.sendMsg(cqMsg);
                                    }
                                }
                            } else {
                                //前天没超
                                if (!user.getQq().equals(0L)) {
                                    cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在2天后将你移除。请考虑加入mp4群：564679329。");
                                    cqManager.sendMsg(cqMsg);
                                }
                                continue;
                            }
                        } else {
                            //昨天没超
                            if (!user.getQq().equals(0L)) {
                                cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在3天后将你移除。请考虑加入mp4群：564679329。");
                                cqManager.sendMsg(cqMsg);
                            }

                        }
                        continue;
                    }

                }

                //如果能获取到userinfo，就把banned设置为0
                user.setBanned(false);
                userDAO.updateUser(user);
            } else {
                //将null的用户直接设为banned
                user.setBanned(true);
                logger.info("检测到玩家" + user.getUserId() + "被Ban，已登记");
                userDAO.updateUser(user);
            }
        }
    }

    /**
     * 清理每天生成的临时文件。
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void clearTodayImages() {
        final Path path = Paths.get(OverallConsts.CABBAGE_CONFIG.getString("path") + "/data/image");
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("正在删除" + file.toString());
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };
        final Path path2 = Paths.get(OverallConsts.CABBAGE_CONFIG.getString("path") + "/data/record");
        SimpleFileVisitor<Path> finder2 = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("正在删除" + file.toString());
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };
        try {

            Files.walkFileTree(path, finder);
            Files.walkFileTree(path2, finder2);
        } catch (IOException e) {
            logger.error("清空临时文件时出现异常，" + e.getMessage());
        }

    }

}
