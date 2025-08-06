package top.mothership.cabbage.service;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.annotation.GroupAuthorityControl;
import top.mothership.cabbage.constant.Overall;
import top.mothership.cabbage.constant.Tip;
import top.mothership.cabbage.enums.CompressLevelEnum;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.OneBotManager;
import top.mothership.cabbage.manager.OsuApiV2Manager;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.mapper.RedisDAO;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.coolq.Argument;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.coolq.QQInfo;
import top.mothership.cabbage.pojo.osu.*;
import top.mothership.cabbage.pojo.osu.apiv2.request.UserScoresRequest;
import top.mothership.cabbage.pojo.osu.apiv2.response.ApiV2Score;
import top.mothership.cabbage.util.osu.ScoreUtil;
import top.mothership.cabbage.util.osu.UserUtil;
import top.mothership.cabbage.util.qq.ImgUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

/**
 * 普通命令进行业务处理的类
 *
 * @author QHS
 */
@Service
public class CqServiceImpl {
    //
    private final ApiManager apiManager;
    private final OneBotManager oneBotManager;
    private final WebPageManager webPageManager;
    private final UserInfoDAO userInfoDAO;
    private final UserDAO userDAO;
    private final ImgUtil imgUtil;
    private final ScoreUtil scoreUtil;
    private final UserUtil userUtil;
    private final ResDAO resDAO;
    private final RedisDAO redisDAO;
    private final OsuApiV2Manager osuApiV2Manager;
    private Logger logger = LogManager.getLogger(this.getClass());

    /**
     * Instantiates a new Cq service.
     *
     * @param
     * @param apiManager     the api manager
     * @param oneBotManager  the cq manager
     * @param webPageManager 网页相关抓取工具
     * @param userDAO        the user dao
     * @param userInfoDAO    the user info dao
     * @param imgUtil        the img util
     * @param scoreUtil      the score util
     * @param userUtil
     * @param resDAO
     * @param redisDAO
     */
    @Autowired
    public CqServiceImpl(ApiManager apiManager, OneBotManager oneBotManager, WebPageManager webPageManager, UserDAO userDAO, UserInfoDAO userInfoDAO, ImgUtil imgUtil, ScoreUtil scoreUtil, UserUtil userUtil, ResDAO resDAO, RedisDAO redisDAO, OsuApiV2Manager osuApiV2Manager) {
        this.apiManager = apiManager;
        this.oneBotManager = oneBotManager;
        this.webPageManager = webPageManager;
        this.userDAO = userDAO;
        this.userInfoDAO = userInfoDAO;
        this.imgUtil = imgUtil;
        this.scoreUtil = scoreUtil;
        this.userUtil = userUtil;
        this.resDAO = resDAO;
        this.redisDAO = redisDAO;
        this.osuApiV2Manager = osuApiV2Manager;
    }


    /**
     * 处理statu/stat/statme的方法。
     *
     * @param cqMsg QQ消息体
     */
    public void statUserInfo(CqMsg cqMsg) {

        //参数校验部分单独提取
        Argument argument = cqMsg.getArgument();

        User user = null;
        Userinfo userFromAPI = null;
        //指定日期没有数据
        boolean approximate = false;
        Userinfo userInDB = null;
        String role = null;
        int scoreRank = 0;
        List<String> roles;

        switch (argument.getSubCommandLowCase()) {
            case "statme":
                //由于statme是对本人的查询，先尝试取出绑定的user，如果没有绑定过给出相应提示
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage(Tip.USER_NOT_BIND);
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                user.setLastActiveDate(LocalDate.now());
                userDAO.updateUser(user);
                if (argument.getMode() == null) {
                    //如果查询没有指定mode，用用户预设的mode覆盖
                    argument.setMode(user.getMode());
                }
                //根据绑定的信息从ppy获取一份玩家信息
                userFromAPI = apiManager.getUser(argument.getMode(), user.getUserId());
                role = user.getRole();

                if (user.isBanned()) {
                    //当数据库查到该玩家，并且被ban时，从数据库里取出最新的一份userinfo伪造
                    userFromAPI = userInfoDAO.getNearestUserInfo(argument.getMode(), user.getUserId(), LocalDate.now());
                    if (userFromAPI == null) {
                        //如果数据库中该玩家该模式没有历史记录……
                        cqMsg.setMessage(Tip.USER_IS_BANNED);
                        oneBotManager.sendMsg(cqMsg);
                        return;
                    }
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
                    //玩家被ban就把日期改成0，因为没有数据进行对比
                    argument.setDay(0);
                } else {
                    if (userFromAPI == null) {
                        cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, user.getQq(), user.getUserId()));
                        oneBotManager.sendMsg(cqMsg);
                        return;
                    }
                    if (argument.getDay() > 0) {
                        if (argument.getDay().equals(1)) {
                            //加一个从redis取数据的设定
                            userInDB = redisDAO.get(userFromAPI.getUserId(), argument.getMode());
                        }
                        if (userInDB == null) {
                            userInDB = userInfoDAO.getUserInfo(argument.getMode(), userFromAPI.getUserId(), LocalDate.now().minusDays(argument.getDay()));
                            if (userInDB == null) {
                                userInDB = userInfoDAO.getNearestUserInfo(argument.getMode(), userFromAPI.getUserId(), LocalDate.now().minusDays(argument.getDay()));
                                approximate = true;
                            }
                        }
                    }
                }
                break;

            case "statu":
                //先尝试根据提供的uid从数据库取出数据
                user = userDAO.getUser(null, argument.getUserId());
                userFromAPI = apiManager.getUser(0, argument.getUserId());

                if (user == null) {
                    if (userFromAPI == null) {
                        cqMsg.setMessage(String.format(Tip.USER_ID_GET_FAILED_AND_NOT_USED, argument.getUserId()));
                        oneBotManager.sendMsg(cqMsg);
                        return;
                    } else {
                        //构造User对象和4条Userinfo写入数据库，如果指定了mode就使用指定mode
                        if (argument.getMode() == null) {
                            argument.setMode(0);
                        }
                        userUtil.registerUser(userFromAPI.getUserId(), argument.getMode(), 0L, Overall.DEFAULT_ROLE);
                        userInDB = userFromAPI;
                        //初次使用，数据库肯定没有指定天数的数据
                        approximate = true;
                    }
                    role = Overall.DEFAULT_ROLE;
                } else if (user.isBanned()) {
                    //只有在确定user不是null的时候，如果参数没有提供mode，用user预设的覆盖
                    if (argument.getMode() == null) {
                        argument.setMode(user.getMode());
                    }
                    //当数据库查到该玩家，并且被ban时，从数据库里取出最新的一份userinfo，作为要展现的数据传给绘图类
                    userFromAPI = userInfoDAO.getNearestUserInfo(argument.getMode(), user.getUserId(), LocalDate.now());
                    if (userFromAPI == null) {
                        //如果数据库中该玩家该模式没有历史记录……
                        cqMsg.setMessage(Tip.USER_IS_BANNED);
                        oneBotManager.sendMsg(cqMsg);
                        return;
                    }
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
                    argument.setDay(0);
                    role = user.getRole();
                } else {
                    if (argument.getMode() == null) {
                        argument.setMode(user.getMode());
                    }
                    role = user.getRole();
                    if (argument.getDay() > 0) {
                        if (argument.getDay().equals(1)) {
                            //加一个从redis取数据的设定
                            userInDB = redisDAO.get(userFromAPI.getUserId(), argument.getMode());
                        }
                        if (userInDB == null) {
                            userInDB = userInfoDAO.getUserInfo(argument.getMode(), userFromAPI.getUserId(), LocalDate.now().minusDays(argument.getDay()));
                            if (userInDB == null) {
                                userInDB = userInfoDAO.getNearestUserInfo(argument.getMode(), userFromAPI.getUserId(), LocalDate.now().minusDays(argument.getDay()));
                                approximate = true;
                            }
                        }
                    }
                }
                break;
            case "stat":
                if ("白菜".equals(argument.getUsername())) {
                    cqMsg.setMessage("没人疼，没人爱，我是地里一颗小白菜。");
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                //直接从api根据参数提供的用户名获取
                userFromAPI = apiManager.getUser(0, argument.getUsername());

                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERNAME_GET_FAILED, argument.getUsername()));
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }


                user = userDAO.getUser(null, userFromAPI.getUserId());
                if (user == null) {
                    //未指定mode的时候改为0
                    if (argument.getMode() == null) {
                        argument.setMode(0);
                    }
                    user = userUtil.registerUser(userFromAPI.getUserId(), argument.getMode(), 0L, Overall.DEFAULT_ROLE);
                    userInDB = userFromAPI;
                    role = Overall.DEFAULT_ROLE;
                    //初次使用，数据库肯定没有指定天数的数据，直接标为近似数据
                    approximate = true;
                } else {
                    //未指定mode的时候改为玩家预设的模式
                    if (argument.getMode() == null) {
                        argument.setMode(user.getMode());
                    }
                    if (!argument.getMode().equals(0)) {
                        //2018-1-22 12:59:06如果这个玩家的模式不是主模式，则取出相应模式
                        userFromAPI = apiManager.getUser(argument.getMode(), user.getUserId());
                    }
                    if (argument.getDay() > 0) {
                        if (argument.getDay().equals(1)) {
                            //加一个从redis取数据的设定
                            userInDB = redisDAO.get(userFromAPI.getUserId(), argument.getMode());
                        }
                        if (userInDB == null) {
                            userInDB = userInfoDAO.getUserInfo(argument.getMode(), userFromAPI.getUserId(), LocalDate.now().minusDays(argument.getDay()));
                            if (userInDB == null) {
                                userInDB = userInfoDAO.getNearestUserInfo(argument.getMode(), userFromAPI.getUserId(), LocalDate.now().minusDays(argument.getDay()));
                                approximate = true;
                            }
                        }
                    }
                }
                break;
            default:
                break;

        }
        role = user.getMainRole();
        if (argument.getMode().equals(0)) {
            //主模式才获取score rank
            //2019-7-18 看样子四个人是全够2k名了，没必要特殊处理了
            try {
                scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
            } catch (Exception e) {
                logger.warn("获取BP排名失败");
            }
        }
        String result = imgUtil.drawUserInfo(userFromAPI, userInDB, role, argument.getDay(), approximate, scoreRank, argument.getMode());
        cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
        oneBotManager.sendMsg(cqMsg);
    }


    public void printBP(CqMsg cqMsg) {

        Argument argument = cqMsg.getArgument();
        if ("白菜".equals(argument.getUsername())) {
            cqMsg.setMessage("大白菜（学名：Brassica rapa pekinensis，异名Brassica campestris pekinensis或Brassica pekinensis）" +
                    "是一种原产于中国的蔬菜，又称“结球白菜”、“包心白菜”、“黄芽白”、“胶菜”等。(via 维基百科)");
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        if (argument.isText()) {
            cqMsg.setMessage("不（lan）支（de）持（zuo）以文本形式展现今日BP。");
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        ArrayList<Score> todayBP = new ArrayList<>();
        List<List<Score>> bpListMixedMode;
        Userinfo userFromAPI = null;
        User user = null;
        boolean mixedMode = false;
        switch (argument.getSubCommandLowCase()) {
            case "bp":
            case "bps":
                userFromAPI = apiManager.getUser(0, argument.getUsername());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERNAME_GET_FAILED, argument.getUsername()));
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "bpu":
            case "bpus":
                userFromAPI = apiManager.getUser(0, argument.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERID_GET_FAILED, argument.getUserId()));
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                break;

            case "bpme":
            case "mybp":
            case "bpmes":
            case "mybps":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage(Tip.USER_NOT_BIND);
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                user.setLastActiveDate(LocalDate.now());
                userDAO.updateUser(user);
                if (user.isBanned()) {
                    cqMsg.setMessage(Tip.USER_IS_BANNED);
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(user.getMode(), user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, user.getQq(), user.getUserId()));
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                break;
            default:
                break;
        }

        //如果不是mybp，并且没有指定mode
        if (argument.getMode() == null && user == null) {
            //取出四个模式所有BP
            bpListMixedMode = apiManager.getBP(userFromAPI.getUserId());
            for (int i = 0; i < bpListMixedMode.size(); i++) {
                //双重for
                for (int j = 0; j < bpListMixedMode.get(i).size(); j++) {
                    if (scoreUtil.toInstant(bpListMixedMode.get(i).get(j).getDate())
                            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS))) {
                        bpListMixedMode.get(i).get(j).setBpId(j);
                        //对BP进行遍历，请求API将名称写入
                        Beatmap map = apiManager.getBeatmap(bpListMixedMode.get(i).get(j).getBeatmapId());
                        bpListMixedMode.get(i).get(j).setBeatmapName(
                                map.getArtist() + " - " + map.getTitle() + " [" + map.getVersion() + "]");
                        todayBP.add(bpListMixedMode.get(i).get(j));
                    }
                }
            }
            if (todayBP.size() == 0) {
                cqMsg.setMessage("[CQ:record,file=base64://" + Base64.getEncoder().encodeToString((byte[]) resDAO.getResource("NI_QI_BU_QI.wav")) + "]");
                oneBotManager.sendMsg(cqMsg);
                cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "今天还。。\n这么悲伤的事情，不忍心说啊。");
                oneBotManager.sendMsg(cqMsg);
                return;
            }
            if (todayBP.size() == 1) {
                argument.setMode((int) todayBP.get(0).getMode());
            }
            byte lastTodayBpMode = todayBP.get(0).getMode();
            for (Score aList : todayBP) {
                //如果今日bp里出现了不同的模式，就设allmode为true
                if (!aList.getMode().equals(lastTodayBpMode)) {
                    mixedMode = true;
                }
            }
        } else {
            if (argument.getMode() == null && user != null) {
                //如果是mybp并且没有指定mode
                argument.setMode(user.getMode());
            }
            //如果不是mybp，并且指定了mode，就按指定的mode 获取
            List<Score> bpListSingleMode = apiManager.getBP(argument.getMode(), userFromAPI.getUserId());
            for (int i = 0; i < bpListSingleMode.size(); i++) {
                //对BP进行遍历，如果产生时间在24小时内，就加入今日bp豪华午餐，并且加上bp所在的编号
                if (scoreUtil.toInstant(bpListSingleMode.get(i).getDate())
                        .isAfter(Instant.now().minus(1, ChronoUnit.DAYS))){
                    bpListSingleMode.get(i).setBpId(i);
                    Beatmap map = apiManager.getBeatmap(bpListSingleMode.get(i).getBeatmapId());
                    bpListSingleMode.get(i).setBeatmapName(map.getArtist() + " - " + map.getTitle() + " [" + map.getVersion() + "]");
                    todayBP.add(bpListSingleMode.get(i));
                }
            }
            if (todayBP.size() == 0) {
                cqMsg.setMessage("[CQ:record,file=base64://" + Base64.getEncoder().encodeToString((byte[]) resDAO.getResource("NI_QI_BU_QI.wav")) + "]");
                oneBotManager.sendMsg(cqMsg);
                cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "今天还。。\n这么悲伤的事情，不忍心说啊。");
                oneBotManager.sendMsg(cqMsg);
                return;
            }
        }

        //如果是多模式的BP，mixedmode是true，getmode是null
        String result = imgUtil.drawUserBP(userFromAPI, todayBP, argument.getMode(), mixedMode);
        cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
        oneBotManager.sendMsg(cqMsg);

    }

    //很迷啊，在printBP里传userinfo cqmsg text等参数，aop拦截不到，只能让代码重复了_(:з」∠)_
    @GroupAuthorityControl
    public void printSpecifiedBP(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        if ("白菜".equals(argument.getUsername())) {
            cqMsg.setMessage("大白菜（学名：Brassica rapa pekinensis，异名Brassica campestris pekinensis或Brassica pekinensis）" +
                    "是一种原产于中国的蔬菜，又称“结球白菜”、“包心白菜”、“黄芽白”、“胶菜”等。(via 维基百科)");
            oneBotManager.sendMsg(cqMsg);
            return;
        }

        Userinfo userFromAPI = null;
        User user = null;

        switch (argument.getSubCommandLowCase()) {
            case "bp":
            case "bps":
                userFromAPI = apiManager.getUser(0, argument.getUsername());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERNAME_GET_FAILED, argument.getUsername()));
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "bpu":
            case "bpus":
                userFromAPI = apiManager.getUser(0, argument.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERID_GET_FAILED, argument.getUserId()));
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "bpme":
            case "mybp":
            case "bpmes":
            case "mybps":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage(Tip.USER_NOT_BIND);
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                user.setLastActiveDate(LocalDate.now());
                userDAO.updateUser(user);
                if (user.isBanned()) {
                    cqMsg.setMessage(Tip.USER_IS_BANNED);
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(0, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, user.getQq(), user.getUserId()));
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                break;
            default:
                break;
        }

        List<Score> bpList = null;
        //如果不是bpme（没有取出user），并且没有指定mode
        if (argument.getMode() == null && user == null) {
            //默认为主模式
            argument.setMode(0);
        }
        if (argument.getMode() == null && user != null) {
            //如果是mybp并且没有指定mode
            argument.setMode(user.getMode());
        }
        if (argument.getNum() > 100) {
            handleApiV2BP(cqMsg, argument.isText(), argument.getNum(), argument.getMode(), userFromAPI);
            return;
        }

        bpList = apiManager.getBP(argument.getMode(), userFromAPI.getUserId());


        if (argument.getNum() > bpList.size()) {
            cqMsg.setMessage("该玩家没有打出指定的bp……");
            oneBotManager.sendMsg(cqMsg);
            return;
        } else {
            if (argument.isText()) {
                //list基于0，得-1
                Score score = bpList.get(argument.getNum() - 1);
                logger.info("获得了玩家" + userFromAPI.getUserName() + "在模式：" + argument.getMode() + "的第" + argument.getNum() + "个BP：" + score.getBeatmapId() + "，正在获取歌曲名称");
                Beatmap beatmap = apiManager.getBeatmap(score.getBeatmapId());
                cqMsg.setMessage(scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName(), null));
                oneBotManager.sendMsg(cqMsg);
            } else {
                //list基于0，得-1
                Score score = bpList.get(argument.getNum() - 1);
                logger.info("获得了玩家" + userFromAPI.getUserName() + "在模式：" + argument.getMode() + "的第" + argument.getNum() + "个BP：" + score.getBeatmapId() + "，正在获取歌曲名称");
                Beatmap map = apiManager.getBeatmap(score.getBeatmapId());
                String result = imgUtil.drawResult(userFromAPI, score, map, argument.getMode());
                cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
                oneBotManager.sendMsg(cqMsg);
            }
        }
    }

    private void handleApiV2BP(CqMsg cqMsg, boolean isText, Integer num, Integer mode, Userinfo userFromAPI) {
        String v2Mode = scoreUtil.convertGameModeToV2String(mode);
        List<ApiV2Score.ScoreLazer> list = osuApiV2Manager.getUserBestScores(
                new UserScoresRequest(String.valueOf(userFromAPI.getUserId()),
                        "best", 100, 100,
                        true, true, v2Mode));
        if (isText) {
            //list基于0，得-1
            ApiV2Score.ScoreLazer score = list.get(num - 100 - 1);
            Score scoreV1 = scoreUtil.convertV2ToV1(score);
            logger.info("获得了玩家" + userFromAPI.getUserName() + "在模式：" + mode + "的第" + num + "个BP：" + score.getBeatmapId() + "，正在获取歌曲名称");
            Beatmap beatmap = apiManager.getBeatmap(scoreV1.getBeatmapId());
            cqMsg.setMessage(scoreUtil.genScoreString(scoreV1, beatmap, userFromAPI.getUserName(), null));
            oneBotManager.sendMsg(cqMsg);
        } else {
            //list基于0，得-1
            ApiV2Score.ScoreLazer score = list.get(num - 100 - 1);
            Score scoreV1 = scoreUtil.convertV2ToV1(score);
            logger.info("获得了玩家" + userFromAPI.getUserName() + "在模式：" + mode + "的第" + num + "个BP：" + scoreV1);
            Beatmap map = apiManager.getBeatmap(scoreV1.getBeatmapId());
            String result = imgUtil.drawResult(userFromAPI, scoreV1, map, mode);
            cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
            oneBotManager.sendMsg(cqMsg);
        }


    }

    public void recent(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        Userinfo userFromAPI = null;
        User user;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(Tip.USER_NOT_BIND);
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        user.setLastActiveDate(LocalDate.now());
        userDAO.updateUser(user);
        if (user.isBanned()) {
            cqMsg.setMessage(Tip.USER_IS_BANNED);
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        if (argument.getMode() == null) {
            //如果没有指定mode，就改为user的mode
            argument.setMode(user.getMode());
        }
        userFromAPI = apiManager.getUser(argument.getMode(), user.getUserId());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, user.getQq(), user.getUserId()));
            oneBotManager.sendMsg(cqMsg);
            return;
        }

        logger.info("检测到对" + userFromAPI.getUserName() + "的最近游戏记录查询");
        Score score = apiManager.getRecent(argument.getMode(), userFromAPI.getUserId());
        if (score == null) {
            cqMsg.setMessage(String.format(Tip.NO_RECENT_RECORD, userFromAPI.getUserName(), scoreUtil.convertGameModeToString(argument.getMode())));
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        List<Score> scores = apiManager.getRecents(argument.getMode(), userFromAPI.getUserId());
        Integer count = 0;
        for (Score score1 : scores) {
            if (score.getBeatmapId().equals(score1.getBeatmapId())) {
                count++;
            }
        }

        Beatmap beatmap = apiManager.getBeatmap(score.getBeatmapId());
        if (beatmap == null) {
            cqMsg.setMessage(String.format(Tip.BEATMAP_GET_FAILED, score.getBeatmapId()));
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        if (argument.isText()) {
            String resp = scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName(), count);
            cqMsg.setMessage(resp);
            oneBotManager.sendMsg(cqMsg);
        } else {
            String filename = imgUtil.drawResult(userFromAPI, score, beatmap, argument.getMode());
            cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
            oneBotManager.sendMsg(cqMsg);
        }
    }

    public void help(CqMsg cqMsg) {
        String img;
        if ((int) (Math.random() * 20) == 1) {
            img = imgUtil.drawImage(imgUtil.get("helpTrick.png"), CompressLevelEnum.不压缩);
            cqMsg.setMessage("[CQ:image,file=base64://" + img + "]");
        } else {
            img = imgUtil.drawImage(imgUtil.get("help.png"), CompressLevelEnum.不压缩);
        }
        cqMsg.setMessage("[CQ:image,file=base64://" + img + "]");
        oneBotManager.sendMsg(cqMsg);

    }

    public void sleep(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        logger.info(cqMsg.getUserId() + "被自己禁言" + argument.getHour() + "小时。");
        cqMsg.setMessage("[CQ:record,file=base64://" + Base64.getEncoder().encodeToString((byte[]) resDAO.getResource("zou_hao_bu_song.wav")) + "]");
        oneBotManager.sendMsg(cqMsg);
        cqMsg.setMessageType("smoke");
        cqMsg.setDuration((int) (argument.getHour() * 3600));
        oneBotManager.sendMsg(cqMsg);
    }

    @GroupAuthorityControl
    public void myScore(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        SearchParam searchParam = argument.getSearchParam();

        User user;
        Userinfo userFromAPI;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        user.setLastActiveDate(LocalDate.now());
        userDAO.updateUser(user);
        userFromAPI = apiManager.getUser(0, user.getUserId());
        if (userFromAPI == null) {
            cqMsg.setMessage("没有获取到QQ：" + cqMsg.getUserId() + "绑定的uid为" + user.getUserId() + "玩家的信息。");
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        Beatmap beatmap;

        if (searchParam.getBeatmapId() == null) {
            beatmap = webPageManager.searchBeatmap(searchParam, argument.getMode());
        } else {
            //如果是纯数字的搜索词，则改为用API直接获取
            beatmap = apiManager.getBeatmap(searchParam.getBeatmapId());
        }
        logger.info("开始处理" + userFromAPI.getUserName() + "进行的本人成绩搜索");
        if (beatmap == null) {
            cqMsg.setMessage("根据提供的关键词：" + searchParam + "没有找到任何谱面。" +
                    "\n请尝试根据解析出的结果，去掉关键词中的特殊符号……");
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        if (argument.getMode() == null) {
            //2018-2-28 16:05:51 !me命令未指定模式的时候应该改为用户预设模式
            argument.setMode(user.getMode());
        }
        //先取到前两个分数
        List<Score> scores = apiManager.getFirstScore(argument.getMode(), beatmap.getBeatmapId(), 2);
        if (scores.size() == 0) {
            cqMsg.setMessage(String.format(Tip.BEATMAP_NO_SCORE, beatmap.getBeatmapId(), scoreUtil.convertGameModeToString(argument.getMode())));
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        //如果不是#1
        if (!scores.get(0).getUserId().equals(userDAO.getUser(cqMsg.getUserId(), null).getUserId())) {
            scores = apiManager.getScore(argument.getMode(), beatmap.getBeatmapId(), user.getUserId());
            if (scores.size() > 0) {
                if (searchParam.getMods() != null) {
                    for (Score s : scores) {
                        if (s.getEnabledMods().equals(searchParam.getMods())) {
                            String filename = imgUtil.drawResult(userFromAPI, s, beatmap, argument.getMode());
                            cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
                            oneBotManager.sendMsg(cqMsg);
                            return;
                        }
                    }
                    cqMsg.setMessage("找到的谱面为：https://osu.ppy.sh/b/" + beatmap.getBeatmapId()
                            + "\n" + beatmap.getArtist() + " - " + beatmap.getTitle() + "[" + beatmap.getVersion() + "](" + beatmap.getCreator() + ")。" +
                            "\n你在该谱面没有指定Mod：" + searchParam.getModsString() + "，模式：" + scoreUtil.convertGameModeToString(argument.getMode()) + "的成绩。");
                } else {
                    //如果没有指定mod
                    String filename = imgUtil.drawResult(userFromAPI, scores.get(0), beatmap, argument.getMode());
                    cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
                }
            } else {
                cqMsg.setMessage("找到的谱面为：https://osu.ppy.sh/b/" + beatmap.getBeatmapId()
                        + "\n" + beatmap.getArtist() + " - " + beatmap.getTitle() + "[" + beatmap.getVersion() + "](" + beatmap.getCreator() + ")" +
                        "，你在该谱面没有模式：" + scoreUtil.convertGameModeToString(argument.getMode()) + "的成绩。");
            }
            oneBotManager.sendMsg(cqMsg);


        } else {
            userFromAPI = apiManager.getUser(0, scores.get(0).getUserId());
            //为了日志+和BP的PP计算兼容，补上get_score的API缺失的部分
            scores.get(0).setBeatmapName(beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]");
            scores.get(0).setBeatmapId(Integer.valueOf(beatmap.getBeatmapId()));
            String filename = imgUtil.drawFirstRank(beatmap, scores.get(0), userFromAPI, scores.get(0).getScore() - scores.get(1).getScore(), argument.getMode());
            cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
            oneBotManager.sendMsg(cqMsg);
        }


    }

    @GroupAuthorityControl
    public void search(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        SearchParam searchParam = argument.getSearchParam();
        if (searchParam == null) {
            return;
        }
        if (argument.getMode() == null) {
            argument.setMode(0);
        }
        Beatmap beatmap;
        if (searchParam.getBeatmapId() == null) {
            beatmap = webPageManager.searchBeatmap(searchParam, argument.getMode());
            if (beatmap == null) {
                cqMsg.setMessage("根据提供的关键词：" + searchParam + "没有找到任何谱面。" +
                        "\n请尝试根据解析出的结果，去掉关键词中的特殊符号……");
                oneBotManager.sendMsg(cqMsg);
                return;
            }
            beatmap = apiManager.getBeatmap(beatmap.getBeatmapId());
        } else {
            beatmap = apiManager.getBeatmap(searchParam.getBeatmapId());
            if (beatmap == null) {
                cqMsg.setMessage("根据提供的谱面ID：" + searchParam.getBeatmapId() + "没有找到任何谱面。" +
                        "\n请尝试根据解析出的结果，去掉关键词中的特殊符号……");
                oneBotManager.sendMsg(cqMsg);
                return;
            }
        }
        logger.info("开始处理" + cqMsg.getUserId() + "进行的谱面搜索，关键词为：" + searchParam);


        if (!beatmap.getMode().equals(0)) {
            cqMsg.setMessage("根据提供的bid找到了一张" + scoreUtil.convertGameModeToString(beatmap.getMode()) + "模式的专谱。由于oppai不支持其他模式，因此白菜也只有主模式支持!search命令。");
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        if (searchParam.getMods() == null) {
            //在search中，未指定mod即视为none
            searchParam.setMods(0);
        }

        Score score = new Score();

        //逆计算stdacc
        score.setEnabledMods(searchParam.getMods());
        score.setCountMiss(searchParam.getCountMiss());
        score.setMaxCombo(searchParam.getMaxCombo());
        score.setCount50(searchParam.getCount50());
        if (searchParam.getAcc() == null) {
            score.setCount100(searchParam.getCount100());
            score.setCount300(-1);
            OppaiResult oppaiResult = scoreUtil.calcPP(score, beatmap);
            int objects = oppaiResult.getNumCircles() + oppaiResult.getNumSliders() + oppaiResult.getNumSpinners();
            score.setCount300(objects - (score.getCount100() == null ? 0 : score.getCount100()));
            score.setMaxCombo(score.getMaxCombo() == -1 ? objects : score.getMaxCombo());
        } else {
            OppaiResult oppaiResult = scoreUtil.calcPP(score, beatmap);
            //随意指定300 100,先计算出谱面总物件数
            int objects = oppaiResult.getNumCircles() + oppaiResult.getNumSliders() + oppaiResult.getNumSpinners();
            score.setCount100((int) (100D - searchParam.getAcc()) * 3 * objects / 200);
            score.setCount300(objects - score.getCount100());
            score.setMaxCombo(score.getMaxCombo() == -1 ? objects : score.getMaxCombo());
        }
        System.out.println(score);
        //这里默认构造FC成绩，所以不需要处理NPE……吧？
        OppaiResult oppaiResult = scoreUtil.calcPP(score, beatmap);
        String filename = imgUtil.drawBeatmap(beatmap, searchParam.getMods(), oppaiResult, argument.getMode());
        cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]" + "\n" + "https://osu.ppy.sh/b/" + beatmap.getBeatmapId() + "\n"
                + beatmap.getArtist() + " - " + beatmap.getTitle() + "[" + beatmap.getVersion() + "](" + beatmap.getCreator() + ")"
                + "\n" + "http://bloodcat.com/osu/s/" + beatmap.getBeatmapSetId()
                + "\n" + "在线试玩：http://osugame.online/search.html?q=" + beatmap.getBeatmapSetId()
                + "\n" + "预览：https://bloodcat.com/osu/preview.html#" + beatmap.getBeatmapId());

        oneBotManager.sendMsg(cqMsg);

    }


    public void welcomeNewsPaper(CqMsg cqMsg) {
        logger.info("开始处理" + cqMsg.getUserId() + "在" + cqMsg.getGroupId() + "群的加群请求");
        String resp = null;
        switch (String.valueOf(cqMsg.getGroupId())) {
            case "210342787":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp3。请修改一下你的群名片(包含完整osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                break;
            case "537646635":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp乐园主群。请修改一下你的群名片(包含完整osu! id)，以下为mp乐园系列分群介绍：\n" +
                        "OSU! MP乐园2号群 (MP2) *(5500-7000pp):234219559\n" +
                        "OSU! MP乐园3号群 (MP3) *(4700-5800pp):210342787\n" +
                        "OSU! MP乐园4号群 (MP4) *(4000-6000pp):564679329\n" +
                        "OSU! MP乐园5号群 (MP5) *(2500-4500pp，无严格下限):201872650";
                break;
            case "112177148":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "],欢迎来到第一届MP4杯赛群。\n本群作为历届mp4选手聚集地，之后比赛结束后会将赛群合并到本群。";
                break;
            case "772918786":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "],欢迎来到MP5杯新后花园。\n本群作为第七届及以后的历届mp5选手聚集地，之后比赛结束后会将赛群合并到本群。";
                break;
            case "807757470":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "],欢迎来到第三届MP4杯赛群。\n请修改群名片为osu! id，并且仔细阅读群公告。";
                break;
            case "136312506":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "],欢迎来到MP5杯赛群。\n请修改群名片为osu! id，并使用!setid 你的osuid来绑定到你的QQ。赛事动态等信息请阅读群公告";
                break;
            case "693299572":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，特殊进群提醒测试";
                break;
            default:
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎加入本群。";
                break;
        }

        cqMsg.setMessageType("group");
        cqMsg.setMessage(resp);
        oneBotManager.sendMsg(cqMsg);

    }

    @GroupAuthorityControl
    public void cost(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        User user = null;
        Userinfo userFromAPI = null;

        switch (argument.getSubCommandLowCase()) {
            case "costme":

            case "mycost":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage(Tip.USER_NOT_BIND);
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                user.setLastActiveDate(LocalDate.now());
                userDAO.updateUser(user);
                if (user.isBanned()) {
                    cqMsg.setMessage(Tip.USER_IS_BANNED);
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(0, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, user.getQq(), user.getUserId()));
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "cost":
                String username = argument.getUsername();
                //处理彩蛋
                if ("白菜".equals(username)) {
                    cqMsg.setMessage("[Crz]Makii  11:00:45\n" +
                            "...\n" +
                            "[Crz]Makii  11:01:01\n" +
                            "思考");
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(0, username);
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERNAME_GET_FAILED, username));
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                if (userFromAPI.getUserId() == 3) {
                    cqMsg.setMessage(Tip.QUERY_BANCHO_BOT);
                    oneBotManager.sendMsg(cqMsg);
                    return;
                }
                user = userDAO.getUser(null, userFromAPI.getUserId());
                if (user == null) {
                    logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
                    user = userUtil.registerUser(userFromAPI.getUserId(), 0, 0L, Overall.DEFAULT_ROLE);
                }
                break;
            default:
                break;

        }
        Map<String, Double> map = webPageManager.getPPPlus(user.getUserId());
        Map<String, Integer> map2 = webPageManager.getOsuChanBestBpmAndLength(user.getUserId());
        //2018-3-29 09:52:16加入Map容量判断
        if (map != null && map.size() == 6 && map2 != null && map2.size() == 2) {
            double drugsS6Cost = Math.pow((map.get("Jump") / 3000F), 0.85F)
                    * Math.pow((map.get("Flow") / 1500F), 0.45F)
                    + Math.atan((map.get("Speed") / 2000F)) * 1.3F
                    + (map.get("Accuracy") / 4000F);
            double mp4S5Cost = Math.pow(
                    ((0.02D * (10D * Math.sqrt((Math.atan((2 * map.get("Jump") - (2648D + 2191D)) / (2648D - 2191D)) + Math.PI / 2D + 8D)
                            * (Math.atan((2D * map.get("Flow") - (715D + 496D)) / (715D - 496D)) + Math.PI / 2D + 3D))
                            + 7D * (Math.atan((2D * map.get("Speed") - (1626D + 1356D)) / (1626D - 1356D)) + Math.PI / 2D)
                            + 3D * (Math.atan((2D * map.get("Stamina") - (1271D + 1020D)) / (1271D - 1020D)) + Math.PI / 2D)
                            + 5D * (Math.atan((2D * map.get("Accuracy") - (1425D + 1101D)) / (1425D - 1101D)) + Math.PI / 2D)
                            + 5D * (Math.atan((2D * map.get("Precision") - (597D + 466D)) / (597D - 466D)) + Math.PI / 2D))) - 1D)
                    , 2.5D);
            double acc = Math.max(map.get("Accuracy"), 500F);
            double cost1 = (Math.sqrt(map.get("Jump") / 3000) + Math.sqrt(map.get("Flow") / 1500)) * (Math.sqrt(map.get("Jump") / 3000) + Math.sqrt(map.get("Flow") / 1500)) / 4;
            cost1 = cost1 * (1 + map.get("Precision") / 5000) / 1.2;
            double cost2 = Math.pow((acc - 500) / 2000, 0.6) * 0.8;
            double cost3 = Math.pow(integral(1, 1 + map.get("Speed") / 1000) / 2, 0.8) * Math.pow(integral(1, 1 + map.get("Stamina") / 1000) / 2, 0.5);
            double oclbS10Cost = cost1 + cost2 + cost3;
            double yuTangCost = (Math.pow((map.get("Jump") / 3000F), 0.8F)
                    * Math.pow((map.get("Flow") / 1500F), 0.6F)
                    + Math.pow((map.get("Speed") / 2000F), 0.8F)
                    * Math.pow((map.get("Stamina") / 2000F), 0.5F)
                    + (map.get("Accuracy") / 3000F))
                    * Math.min(1, Math.pow((map2.get("BPM") / 190D), 2))
                    * Math.min(1, Math.pow((map2.get("Length") * (map2.get("BPM") / (190D * 150D))), 0.2D));
            String filename = imgUtil.drawRadarImage(map, userFromAPI);
            cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]\n"
                    + "在**OCL系列比赛**中，该玩家的Cost是：" + new DecimalFormat("#0.000").format(oclbS10Cost)
                    + "。\n在**第六届某个不能提起名字的比赛**中，该玩家的Cost是：" + new DecimalFormat("#0.00").format(drugsS6Cost)
                    + "。\n在**第五届MP4**中，该玩家的Cost是：" + new DecimalFormat("#0.00").format(mp4S5Cost)
//                    + "。\n在**第三届鱼塘杯**中，该玩家的Cost是：" + new DecimalFormat("#0.00").format(yuTangCost)
                    + "。");
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        cqMsg.setMessage("获取你的Cost失败，根据以往经验，国内凌晨（1:00-7:00）的成功率可能会增加……");
        oneBotManager.sendMsg(cqMsg);
        return;
    }

    double integral(double min, double max) {
        double result = 0;
        double delta = (max - min) / 100000;
        for (int i = 0; i < 100000; i++) {
            result += f1(min + (i + 0.5) * delta) * delta;
        }
        return result;
    }

    double f1(double x) {
        return Math.pow(x, 1 / x - 1);
    }

    public void recentPassed(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();

        Userinfo userFromAPI = null;
        User user;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(Tip.USER_NOT_BIND);
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        user.setLastActiveDate(LocalDate.now());
        userDAO.updateUser(user);
        if (user.isBanned()) {
            cqMsg.setMessage(Tip.USER_IS_BANNED);
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        userFromAPI = apiManager.getUser(0, user.getUserId());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, cqMsg.getUserId(), user.getUserId()));
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        if (argument.getMode() == null) {
            //2018-3-5 09:54:18修正pr命令默认模式为主模式的问题
            argument.setMode(user.getMode());
        }
        logger.info("检测到对" + userFromAPI.getUserName() + "的最近Passed游戏记录查询");
        //2018-3-16 11:51:38这里没修正……
        List<Score> scores = apiManager.getRecents(argument.getMode(), userFromAPI.getUserId());
        if (scores.size() == 0) {
            cqMsg.setMessage(String.format(Tip.NO_RECENT_RECORD, userFromAPI.getUserName(), scoreUtil.convertGameModeToString(argument.getMode())));
            oneBotManager.sendMsg(cqMsg);
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
            cqMsg.setMessage(String.format(Tip.NO_RECENT_RECORD_PASSED, userFromAPI.getUserName(), scoreUtil.convertGameModeToString(argument.getMode())));
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        Integer count = 0;
        for (Score score1 : scores) {
            if (score.getBeatmapId().equals(score1.getBeatmapId())) {
                count++;
            }
        }
        Beatmap beatmap = apiManager.getBeatmap(score.getBeatmapId());
        if (beatmap == null) {
            cqMsg.setMessage(String.format(Tip.BEATMAP_GET_FAILED, score.getBeatmapId()));
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        switch (argument.getSubCommandLowCase()) {
            case "prs":
                String resp = scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName(), count);
                cqMsg.setMessage(resp);
                oneBotManager.sendMsg(cqMsg);
                break;
            case "pr":
                String filename = imgUtil.drawResult(userFromAPI, score, beatmap, argument.getMode());
                cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
                oneBotManager.sendMsg(cqMsg);
                break;
            default:
                break;
        }

    }


    public void roll(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();

        if (argument.getBound() != null && !argument.getBound().isEmpty()) {
            try {
                Integer.parseInt(argument.getBound());
            } catch (Exception e) {
                argument.setBound("100");
            }
            if (Integer.parseInt(argument.getBound()) <= 0) {
                argument.setBound("100");
            }
            cqMsg.setMessage(String.valueOf(new Random().nextInt(Integer.parseInt(argument.getBound())) + 1));
        } else {
            cqMsg.setMessage(String.valueOf(new Random().nextInt(100) + 1));
        }
        if (cqMsg.getGroupId() != null) {
            cqMsg.setMessage("[CQ:at,qq=" + cqMsg.getUserId() + "] " + cqMsg.getMessage());
        }
        oneBotManager.sendMsg(cqMsg);
    }

    @GroupAuthorityControl(allowed = {308419061, 793260840})
    public void time(CqMsg cqMsg) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime NY = LocalDateTime.now(ZoneId.of("America/New_York"));
        LocalDateTime UTC = LocalDateTime.now(ZoneId.of("UTC"));

        cqMsg.setMessage("当前美国东部时间（America/NewYork）为：\n"
                + formatter.format(NY)
                + "\n当前UTC时间为："
                + formatter.format(UTC));
        oneBotManager.sendMsg(cqMsg);
    }

    @GroupAuthorityControl
    public void myRole(CqMsg cqMsg) {

        User user;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(Tip.USER_NOT_BIND);
        } else {
            cqMsg.setMessage("你的当前用户组有：" + user.getRole() + "，主显用户组为：" + user.getMainRole());
        }
        oneBotManager.sendMsg(cqMsg);

    }

    public void pretreatmentParameterForBPCommand(CqMsg cqMsg) {

    }

    public void setId(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        String username;
        Userinfo userFromAPI = null;
        User user;

        userFromAPI = apiManager.getUser(0, argument.getUsername());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(Tip.USERNAME_GET_FAILED, argument.getUsername()));
            oneBotManager.sendMsg(cqMsg);
            return;
        }
        logger.info("尝试将" + userFromAPI.getUserName() + "绑定到QQ：" + cqMsg.getUserId() + "上，指定的模式是" + argument.getMode());

        //只有这个QQ对应的id是null
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            //只有这个id对应的QQ是null
            user = userDAO.getUser(null, userFromAPI.getUserId());
            if (user == null) {
                if (argument.getMode() == null) {
                    argument.setMode(0);
                }
                userUtil.registerUser(userFromAPI.getUserId(), argument.getMode(), cqMsg.getUserId(), Overall.DEFAULT_ROLE);

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
            //TODO 小号机制
            if (user.isBanned()) {
                cqMsg.setMessage("你的QQ已经绑定了玩家：" + user.getCurrentUname() + "，并且该账号已经被ban；如果发生错误请联系妈妈船。");
            } else {
                userFromAPI = apiManager.getUser(0, user.getUserId());
                cqMsg.setMessage("你的QQ已经绑定了玩家：" + userFromAPI.getUserName() + "，如果发生错误请联系妈妈船。");
            }
        }
        oneBotManager.sendMsg(cqMsg);

    }

    public void setMode(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        String username;
        Userinfo userFromAPI = null;
        User user;
        //只有这个QQ对应的id是null
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(Tip.USER_NOT_BIND);
            oneBotManager.sendMsg(cqMsg);
            return;
        } else {
            userFromAPI = apiManager.getUser(0, user.getUserId());
            if (userFromAPI == null) {
                cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, cqMsg.getUserId(), user.getUserId()));
                oneBotManager.sendMsg(cqMsg);
                return;
            }
            logger.info("尝试将" + userFromAPI.getUserName() + "的模式修改为" + argument.getMode());

            user.setMode(argument.getMode());
            user.setLastActiveDate(LocalDate.now());
            userDAO.updateUser(user);
            cqMsg.setMessage("更新成功：你的游戏模式已修改为" + scoreUtil.convertGameModeToString(argument.getMode()));
        }

        oneBotManager.sendMsg(cqMsg);

    }

    @GroupAuthorityControl
    public void setRole(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        String username;
        Userinfo userFromAPI = null;
        User user;
        //只有这个QQ对应的id是null
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(Tip.USER_NOT_BIND);
        } else {
            List<String> roles = new ArrayList<>(Arrays.asList(user.getRole().split(",")));
            if (roles.contains(argument.getRole()) || Objects.equals("creep", argument.getRole())) {
                user.setMainRole(argument.getRole());
                userDAO.updateUser(user);
                cqMsg.setMessage("更新成功：你的主显用户组已修改为" + argument.getRole());
            } else {
                cqMsg.setMessage("你当前不在请求的用户组" + argument.getRole() + "中。当前所在用户组为：" + user.getRole());
            }

        }
        oneBotManager.sendMsg(cqMsg);
    }

    public void drawAvatar(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        List<String> usernames = argument.getUsernames();
        if (usernames == null) {
            User user = userDAO.getUser(cqMsg.getUserId(), null);
            usernames = Collections.singletonList(user.getCurrentUname());
        }

        for (String username : usernames) {
            Userinfo userinfo = apiManager.getUser(0, username);

            BufferedImage ava = webPageManager.getAvatar(userinfo.getUserId(), 280);
            BufferedImage flag = webPageManager.getCountryFlag(userinfo.getCountry());

            BufferedImage image = new BufferedImage(400, 450, BufferedImage.TYPE_INT_RGB);

            // 获取Graphics2D对象用于绘制
            Graphics2D g2d = image.createGraphics();

            // 设置抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 填充背景色
            g2d.setColor(Color.decode("#efecfb"));
            g2d.fillRect(0, 0, 400, 450);

            // 设置阴影参数 (对应 0 1px 2px rgba(0, 0, 0, 0.15))

            int shadowBlur = 4;       // 模糊半径
            Color shadowColor = new Color(0, 0, 0, (int) (0.05 * 255)); // rgba(0,0,0,0.15)

            // 绘制阴影
            g2d.setColor(shadowColor);
            g2d.fillRoundRect(
                    54 - shadowBlur,
                    28 - shadowBlur,
                    286 + shadowBlur * 2,
                    286 + shadowBlur * 2,
                    5, 5
            );
            // 绘制方框
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(
                    54,
                    28,
                    286,
                    286,
                    5, 5
            );

            //绘制头像
            g2d.drawImage(ava,
                    57 + (280 - ava.getWidth()) / 2,
                    31 + (280 - ava.getHeight()) / 2,
                    ava.getWidth(), ava.getHeight(), null);
            g2d.drawImage(flag,
                    180, 404,
                    32, 22, null);

            //指定颜色
            g2d.setPaint(Color.BLACK);
            Font font = new Font("Aller", Font.PLAIN, 48);
            //指定字体
            g2d.setFont(font);
            //指定坐标
            FontMetrics fm = g2d.getFontMetrics(font);
            int width = fm.stringWidth(userinfo.getUserName());

            logger.info("绘制ID 宽度" + width);
            g2d.drawString(userinfo.getUserName(), 200 - (width / 2), 376);

            g2d.dispose();

            String result = imgUtil.drawImage(image, CompressLevelEnum.不压缩);

            cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
            oneBotManager.sendMsg(cqMsg);
        }

    }

}
