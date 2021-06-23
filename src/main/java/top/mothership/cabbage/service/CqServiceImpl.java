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
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.mapper.RedisDAO;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.elo.Elo;
import top.mothership.cabbage.pojo.elo.EloChange;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.coolq.Argument;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.coolq.QQInfo;
import top.mothership.cabbage.pojo.osu.*;
import top.mothership.cabbage.util.osu.ScoreUtil;
import top.mothership.cabbage.util.osu.UserUtil;
import top.mothership.cabbage.util.qq.ImgUtil;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 普通命令进行业务处理的类
 *
 * @author QHS
 */
@Service
public class CqServiceImpl {
    //
    private final ApiManager apiManager;
    private final CqManager cqManager;
    private final WebPageManager webPageManager;
    private final UserInfoDAO userInfoDAO;
    private final UserDAO userDAO;
    private final ImgUtil imgUtil;
    private final ScoreUtil scoreUtil;
    private final UserUtil userUtil;
    private final ResDAO resDAO;
    private final RedisDAO redisDAO;
    private Logger logger = LogManager.getLogger(this.getClass());

    /**
     * Instantiates a new Cq service.
     *
     * @param
     * @param apiManager     the api manager
     * @param cqManager      the cq manager
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
    public CqServiceImpl(ApiManager apiManager, CqManager cqManager, WebPageManager webPageManager, UserDAO userDAO, UserInfoDAO userInfoDAO, ImgUtil imgUtil, ScoreUtil scoreUtil, UserUtil userUtil, ResDAO resDAO, RedisDAO redisDAO) {
        this.apiManager = apiManager;
        this.cqManager = cqManager;
        this.webPageManager = webPageManager;
        this.userDAO = userDAO;
        this.userInfoDAO = userInfoDAO;
        this.imgUtil = imgUtil;
        this.scoreUtil = scoreUtil;
        this.userUtil = userUtil;
        this.resDAO = resDAO;
        this.redisDAO = redisDAO;
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
                    cqManager.sendMsg(cqMsg);
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
                        cqManager.sendMsg(cqMsg);
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
                        cqManager.sendMsg(cqMsg);
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
                        cqManager.sendMsg(cqMsg);
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
                        cqManager.sendMsg(cqMsg);
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
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                //直接从api根据参数提供的用户名获取
                userFromAPI = apiManager.getUser(0, argument.getUsername());

                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERNAME_GET_FAILED, argument.getUsername()));
                    cqManager.sendMsg(cqMsg);
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
            scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
        }
        String result = imgUtil.drawUserInfo(userFromAPI, userInDB, role, argument.getDay(), approximate, scoreRank, argument.getMode());
        cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
        cqManager.sendMsg(cqMsg);
    }


    public void printBP(CqMsg cqMsg) {

        Argument argument = cqMsg.getArgument();
        if ("白菜".equals(argument.getUsername())) {
            cqMsg.setMessage("大白菜（学名：Brassica rapa pekinensis，异名Brassica campestris pekinensis或Brassica pekinensis）" +
                    "是一种原产于中国的蔬菜，又称“结球白菜”、“包心白菜”、“黄芽白”、“胶菜”等。(via 维基百科)");
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (argument.isText()) {
            cqMsg.setMessage("不（lan）支（de）持（zuo）以文本形式展现今日BP。");
            cqManager.sendMsg(cqMsg);
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
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "bpu":
            case "bpus":
                userFromAPI = apiManager.getUser(0, argument.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERID_GET_FAILED, argument.getUserId()));
                    cqManager.sendMsg(cqMsg);
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
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user.setLastActiveDate(LocalDate.now());
                userDAO.updateUser(user);
                if (user.isBanned()) {
                    cqMsg.setMessage(Tip.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(user.getMode(), user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, user.getQq(), user.getUserId()));
                    cqManager.sendMsg(cqMsg);
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
                    if (bpListMixedMode.get(i).get(j).getDate().toInstant().isAfter(Instant.now().minus(1, ChronoUnit.DAYS))) {
                        bpListMixedMode.get(i).get(j).setBpId(j);
                        //对BP进行遍历，请求API将名称写入
                        Beatmap map = apiManager.getBeatmap(bpListMixedMode.get(i).get(j).getBeatmapId());
                        bpListMixedMode.get(i).get(j).setBeatmapName(map.getArtist() + " - " + map.getTitle() + " [" + map.getVersion() + "]");
                        todayBP.add(bpListMixedMode.get(i).get(j));
                    }
                }
            }
            if (todayBP.size() == 0) {
                cqMsg.setMessage("[CQ:record,file=base64://" + Base64.getEncoder().encodeToString((byte[]) resDAO.getResource("NI_QI_BU_QI.wav")) + "]");
                cqManager.sendMsg(cqMsg);
                cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "今天还。。\n这么悲伤的事情，不忍心说啊。");
                cqManager.sendMsg(cqMsg);
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
                if (bpListSingleMode.get(i).getDate().after(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))) {
                    bpListSingleMode.get(i).setBpId(i);
                    Beatmap map = apiManager.getBeatmap(bpListSingleMode.get(i).getBeatmapId());
                    bpListSingleMode.get(i).setBeatmapName(map.getArtist() + " - " + map.getTitle() + " [" + map.getVersion() + "]");
                    todayBP.add(bpListSingleMode.get(i));
                }
            }
            if (todayBP.size() == 0) {
                cqMsg.setMessage("[CQ:record,file=base64://" + Base64.getEncoder().encodeToString((byte[]) resDAO.getResource("NI_QI_BU_QI.wav")) + "]");
                cqManager.sendMsg(cqMsg);
                cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "今天还。。\n这么悲伤的事情，不忍心说啊。");
                cqManager.sendMsg(cqMsg);
                return;
            }
        }

        //如果是多模式的BP，mixedmode是true，getmode是null
        String result = imgUtil.drawUserBP(userFromAPI, todayBP, argument.getMode(), mixedMode);
        cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
        cqManager.sendMsg(cqMsg);

    }

    //很迷啊，在printBP里传userinfo cqmsg text等参数，aop拦截不到，只能让代码重复了_(:з」∠)_
    @GroupAuthorityControl
    public void printSpecifiedBP(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        if ("白菜".equals(argument.getUsername())) {
            cqMsg.setMessage("大白菜（学名：Brassica rapa pekinensis，异名Brassica campestris pekinensis或Brassica pekinensis）" +
                    "是一种原产于中国的蔬菜，又称“结球白菜”、“包心白菜”、“黄芽白”、“胶菜”等。(via 维基百科)");
            cqManager.sendMsg(cqMsg);
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
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "bpu":
            case "bpus":
                userFromAPI = apiManager.getUser(0, argument.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERID_GET_FAILED, argument.getUserId()));
                    cqManager.sendMsg(cqMsg);
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
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user.setLastActiveDate(LocalDate.now());
                userDAO.updateUser(user);
                if (user.isBanned()) {
                    cqMsg.setMessage(Tip.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(0, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, user.getQq(), user.getUserId()));
                    cqManager.sendMsg(cqMsg);
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

        bpList = apiManager.getBP(argument.getMode(), userFromAPI.getUserId());


        if (argument.getNum() > bpList.size()) {
            cqMsg.setMessage("该玩家没有打出指定的bp……");
            cqManager.sendMsg(cqMsg);
            return;
        } else {
            if (argument.isText()) {
                //list基于0，得-1
                Score score = bpList.get(argument.getNum() - 1);
                logger.info("获得了玩家" + userFromAPI.getUserName() + "在模式：" + argument.getMode() + "的第" + argument.getNum() + "个BP：" + score.getBeatmapId() + "，正在获取歌曲名称");
                Beatmap beatmap = apiManager.getBeatmap(score.getBeatmapId());
                cqMsg.setMessage(scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName(), null));
                cqManager.sendMsg(cqMsg);
            } else {
                //list基于0，得-1
                Score score = bpList.get(argument.getNum() - 1);
                logger.info("获得了玩家" + userFromAPI.getUserName() + "在模式：" + argument.getMode() + "的第" + argument.getNum() + "个BP：" + score.getBeatmapId() + "，正在获取歌曲名称");
                Beatmap map = apiManager.getBeatmap(score.getBeatmapId());
                String result = imgUtil.drawResult(userFromAPI, score, map, argument.getMode());
                cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
                cqManager.sendMsg(cqMsg);
            }
        }
    }

    public void recent(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        Userinfo userFromAPI = null;
        User user;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(Tip.USER_NOT_BIND);
            cqManager.sendMsg(cqMsg);
            return;
        }
        user.setLastActiveDate(LocalDate.now());
        userDAO.updateUser(user);
        if (user.isBanned()) {
            cqMsg.setMessage(Tip.USER_IS_BANNED);
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (argument.getMode() == null) {
            //如果没有指定mode，就改为user的mode
            argument.setMode(user.getMode());
        }
        userFromAPI = apiManager.getUser(argument.getMode(), user.getUserId());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, user.getQq(), user.getUserId()));
            cqManager.sendMsg(cqMsg);
            return;
        }

        logger.info("检测到对" + userFromAPI.getUserName() + "的最近游戏记录查询");
        Score score = apiManager.getRecent(argument.getMode(), userFromAPI.getUserId());
        if (score == null) {
            cqMsg.setMessage(String.format(Tip.NO_RECENT_RECORD, userFromAPI.getUserName(), scoreUtil.convertGameModeToString(argument.getMode())));
            cqManager.sendMsg(cqMsg);
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
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (argument.isText()) {
            String resp = scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName(), count);
            cqMsg.setMessage(resp);
            cqManager.sendMsg(cqMsg);
        } else {
            String filename = imgUtil.drawResult(userFromAPI, score, beatmap, argument.getMode());
            cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
            cqManager.sendMsg(cqMsg);
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
        cqManager.sendMsg(cqMsg);

    }

    public void sleep(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        logger.info(cqMsg.getUserId() + "被自己禁言" + argument.getHour() + "小时。");
        cqMsg.setMessage("[CQ:record,file=base64://" + Base64.getEncoder().encodeToString((byte[]) resDAO.getResource("zou_hao_bu_song.wav")) + "]");
        cqManager.sendMsg(cqMsg);
        cqMsg.setMessageType("smoke");
        cqMsg.setDuration((int) (argument.getHour() * 3600));
        cqManager.sendMsg(cqMsg);
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
            cqManager.sendMsg(cqMsg);
            return;
        }
        user.setLastActiveDate(LocalDate.now());
        userDAO.updateUser(user);
        userFromAPI = apiManager.getUser(0, user.getUserId());
        if (userFromAPI == null) {
            cqMsg.setMessage("没有获取到QQ：" + cqMsg.getUserId() + "绑定的uid为" + user.getUserId() + "玩家的信息。");
            cqManager.sendMsg(cqMsg);
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
            cqManager.sendMsg(cqMsg);
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
            cqManager.sendMsg(cqMsg);
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
                            cqManager.sendMsg(cqMsg);
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
            cqManager.sendMsg(cqMsg);


        } else {
            userFromAPI = apiManager.getUser(0, scores.get(0).getUserId());
            //为了日志+和BP的PP计算兼容，补上get_score的API缺失的部分
            scores.get(0).setBeatmapName(beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]");
            scores.get(0).setBeatmapId(Integer.valueOf(beatmap.getBeatmapId()));
            String filename = imgUtil.drawFirstRank(beatmap, scores.get(0), userFromAPI, scores.get(0).getScore() - scores.get(1).getScore(), argument.getMode());
            cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
            cqManager.sendMsg(cqMsg);
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
        } else {
            beatmap = apiManager.getBeatmap(searchParam.getBeatmapId());
        }
        logger.info("开始处理" + cqMsg.getUserId() + "进行的谱面搜索，关键词为：" + searchParam);

        if (beatmap == null) {
            cqMsg.setMessage("根据提供的关键词：" + searchParam + "没有找到任何谱面。" +
                    "\n请尝试根据解析出的结果，去掉关键词中的特殊符号……");
            cqManager.sendMsg(cqMsg);
            return;
        } else {
            if (!beatmap.getMode().equals(0)) {
                cqMsg.setMessage("根据提供的bid找到了一张" + scoreUtil.convertGameModeToString(beatmap.getMode()) + "模式的专谱。由于oppai不支持其他模式，因此白菜也只有主模式支持!search命令。");
                cqManager.sendMsg(cqMsg);
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
            } else {
                score.setCount100(0);
                score.setCount300(-1);
                OppaiResult oppaiResult = scoreUtil.calcPP(score, beatmap);
                //随意指定300 100,先计算出谱面总物件数
                int objects = oppaiResult.getNumCircles() + oppaiResult.getNumSliders() + oppaiResult.getNumSpinners();
//                100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50())
//                        / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()));
                //解方程：acc = 100*(6*(a-x)+2x)/(6*a)
                score.setCount100((int) (100D - searchParam.getAcc()) * 3 * objects / 200);
                score.setCount300(objects - score.getCount100());
            }

            //这里默认构造FC成绩，所以不需要处理NPE……吧？
            OppaiResult oppaiResult = scoreUtil.calcPP(score, beatmap);
            String filename = imgUtil.drawBeatmap(beatmap, searchParam.getMods(), oppaiResult, argument.getMode());
            cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]" + "\n" + "https://osu.ppy.sh/b/" + beatmap.getBeatmapId() + "\n"
                    + beatmap.getArtist() + " - " + beatmap.getTitle() + "[" + beatmap.getVersion() + "](" + beatmap.getCreator() + ")"
                    + "\n" + "http://bloodcat.com/osu/s/" + beatmap.getBeatmapSetId()
                    + "\n" + "在线试玩：http://osugame.online/search.html?q=" + beatmap.getBeatmapSetId()
                    + "\n" + "预览：https://bloodcat.com/osu/preview.html#" + beatmap.getBeatmapId());
        }
        cqManager.sendMsg(cqMsg);

    }

    public void chartMemberCmd(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        String role;
        Userinfo userFromAPI;
        Long qq;
        User user;
        String filename;
        String newRole;

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

        userFromAPI = apiManager.getUser(0, argument.getUsername());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, argument.getUsername()));
            cqManager.sendMsg(cqMsg);
            return;
        }
        user = userDAO.getUser(null, userFromAPI.getUserId());
        switch (String.valueOf(cqMsg.getGroupId())) {
            case "201872650":
            case "635731109":
                //MP5
                role = "mp5";
                break;
            default:
                cqMsg.setMessage("请不要在mp5/chart群之外的地方使用。");
                cqManager.sendMsg(cqMsg);
                return;
        }
        String resp = "";
        switch (argument.getSubCommandLowCase()) {
            case "add":
                if (user == null) {
                    //2018-3-2 09:40:26修正：add命令需要检测提供的qq是否有绑定用户，不然会出现重复qq的情况
                    user = userDAO.getUser(argument.getQq(), null);
                    if (user == null) {
                        //进行登记，构建user存入，将userinfo加上时间存入
                        //2018-1-24 16:04:55修正：构建user的时候就写入role
                        userUtil.registerUser(userFromAPI.getUserId(), 0, argument.getQq(), role);
                        int scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
                        //2018-3-1 10:23:28 debug 这边传的参数是user.getMode(),但是封装独立方法之后不生成user对象了，所以这边直接传0就可以了
                        filename = imgUtil.drawUserInfo(userFromAPI, null, role, 0, false, scoreRank, 0);
                        resp = "登记成功，用户组已修改为" + role;
                        resp = resp.concat("\n[CQ:image,file=base64://" + filename + "]");
                    } else {
                        resp = "提供的QQ已经被绑定在玩家" + user.getCurrentUname() + "上。操作失败，请检查QQ并重新添加用户组";
                    }
                } else {
                    //拿到原先的user，把role拼上去，塞回去
                    //如果当前的用户组是creep，就直接改成现有的组
                    resp = "\n该用户之前已使用过白菜。原有用户组为：" + user.getRole();
                    user = userUtil.addRole(role, user);
                    resp += "，修改后的用户组为：" + user.getRole();
                    if (user.getQq().equals(0L)) {
                        user.setQq(argument.getQq());
                        resp += "\n绑定的QQ已登记为" + argument.getQq();
                    } else {
                        resp += "\n该玩家已经绑定了QQ：" + user.getQq() + "，没有做出修改。";
                    }
                    userDAO.updateUser(user);
                    int scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
                    filename = imgUtil.drawUserInfo(userFromAPI, null, role, 0, false, scoreRank, user.getMode());
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
                    user = userUtil.delRole(role, user);
                    resp += "\n修改后的用户组为：" + user.getRole();
                    userDAO.updateUser(user);
                    cqMsg.setMessage(resp);
                    cqManager.sendMsg(cqMsg);
                }
                break;

        }
    }

    public void welcomeNewsPaper(CqMsg cqMsg) {
        logger.info("开始处理" + cqMsg.getUserId() + "在" + cqMsg.getGroupId() + "群的加群请求");
        String resp = null;
        switch (String.valueOf(cqMsg.getGroupId())) {
            case "201872650":
                String role = null;
                Long chartGroupId = null;
                switch (String.valueOf(cqMsg.getGroupId())) {
                    case "201872650":
                        role = "mp5";
                        chartGroupId = 635731109L;
                        resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp5。请修改一下你的群名片(包含完整osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                        break;
                }
                User user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    resp += "\n该玩家没有使用过白菜，请使用add命令手动添加。";
                } else {
                    String newRole;
                    //拿到原先的user，把role拼上去，塞回去
                    //如果当前的用户组是creep，就直接改成现有的组
                    resp += "\n该玩家之前已使用过白菜。原有用户组为：" + user.getRole();
                    if (Overall.DEFAULT_ROLE.equals(user.getRole())) {
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
                    user.setRole(newRole);
                    user.setLastActiveDate(LocalDate.now());
                    Userinfo userFromAPI = apiManager.getUser(0, user.getUserId());
                    if (userFromAPI == null) {
                        resp += "\n警告：从API获取绑定的玩家信息失败，已将被ban状态设为True；如果出现错误，请提醒我手动修改！";
                        user.setBanned(true);
                    } else {
                        boolean near = false;
                        Userinfo userInDB = userInfoDAO.getUserInfo(0, userFromAPI.getUserId(), LocalDate.now().minusDays(1));
                        if (userInDB == null) {
                            userInDB = userInfoDAO.getNearestUserInfo(0, userFromAPI.getUserId(), LocalDate.now().minusDays(1));
                            near = true;
                        }
                        int scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
                        String filename = imgUtil.drawUserInfo(userFromAPI, userInDB, role, 1, near, scoreRank, user.getMode());
                        resp = resp.concat("\n[CQ:image,file=base64://" + filename + "]");
                    }
                    userDAO.updateUser(user);
                }

                break;
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
            case "895214831":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "],欢迎来到MP5杯赛群。\n请修改群名片为osu! id，并且仔细阅读群公告，以及群文件中的比赛规程、给选手的建议。\n报名地址：http://www.mpmatch.cn/5/reg.html\n比赛信息： http://www.mpmatch.cn/5/info.html\n选手列表：http://www.mpmatch.cn/5/roster.html";
                break;
            default:
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎加入本群。";
                break;
        }

        cqMsg.setMessageType("group");
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);

    }

    public void seeYouNextTime(CqMsg cqMsg) {
        logger.info("开始处理" + cqMsg.getUserId() + "在" + cqMsg.getGroupId() + "群的褪裙信息");
        String resp = null;
        String role = null;
        String newRole;
        Long chartGroupId = null;
        User user = userDAO.getUser(cqMsg.getUserId(), null);
        //先判断群号
        switch (String.valueOf(cqMsg.getGroupId())) {
            case "201872650":
                role = "mp5";
                chartGroupId = 635731109L;
                break;
            default:
                //只处理mp5的褪裙
                return;
        }
        switch (cqMsg.getSubType()) {
            case "leave":
                resp = "检测到QQ为" + cqMsg.getUserId() + "的玩家退出" + role + "群；";
                break;
            case "kick":
                resp = "检测到QQ为" + cqMsg.getUserId() + "的玩家被" + cqMsg.getOperatorId() + "移出" + role + "群；";
        }
        if (user == null) {
            //褪裙的人没有用过白菜
            resp += "该玩家没有使用过白菜。";
        } else {
            Userinfo userFromAPI = apiManager.getUser(0, user.getUserId());
            user = userUtil.delRole(role, user);
            userDAO.updateUser(user);
            resp += "已自动将玩家" + userFromAPI.getUserName() + "从" + role + "用户组中移除。";
            resp += "\n修改后的用户组为：" + user.getRole();
        }
        cqMsg.setGroupId(chartGroupId);
        cqMsg.setMessageType("group");
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
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
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user.setLastActiveDate(LocalDate.now());
                userDAO.updateUser(user);
                if (user.isBanned()) {
                    cqMsg.setMessage(Tip.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(0, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, user.getQq(), user.getUserId()));
                    cqManager.sendMsg(cqMsg);
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
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(0, username);
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERNAME_GET_FAILED, username));
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (userFromAPI.getUserId() == 3) {
                    cqMsg.setMessage(Tip.QUERY_BANCHO_BOT);
                    cqManager.sendMsg(cqMsg);
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
            double acc=Math.max(map.get("Accuracy"),500F);
            double cost1 = (Math.sqrt(map.get("Jump") / 3000) + Math.sqrt(map.get("Flow") / 1500)) * (Math.sqrt(map.get("Jump") / 3000) + Math.sqrt(map.get("Flow") / 1500)) / 4;
            cost1 = cost1 * (1+ map.get("Precision") /5000)/1.2;
            double cost2 = Math.pow((acc - 500) / 2000, 0.6)*0.8;
            double cost3 = Math.pow(integral(1, 1 + map.get("Speed") / 1000) / 2, 0.8) * Math.pow(integral( 1, 1 + map.get("Stamina") / 1000) / 2, 0.5);
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
            cqManager.sendMsg(cqMsg);
            return;
        }
        cqMsg.setMessage("获取你的Cost失败，根据以往经验，国内凌晨（1:00-7:00）的成功率可能会增加……");
        cqManager.sendMsg(cqMsg);
        return;
    }
    double integral(double min, double max)
    {
        double result = 0;
        double delta = (max - min) / 100000;
        for (int i = 0; i < 100000; i++)
        {
            result += f1(min + (i+0.5) * delta)*delta;
        }
        return result;
    }
    double f1(double x)
    {
        return Math.pow(x, 1 / x - 1);
    }
    public void recentPassed(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();

        Userinfo userFromAPI = null;
        User user;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(Tip.USER_NOT_BIND);
            cqManager.sendMsg(cqMsg);
            return;
        }
        user.setLastActiveDate(LocalDate.now());
        userDAO.updateUser(user);
        if (user.isBanned()) {
            cqMsg.setMessage(Tip.USER_IS_BANNED);
            cqManager.sendMsg(cqMsg);
            return;
        }
        userFromAPI = apiManager.getUser(0, user.getUserId());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, cqMsg.getUserId(), user.getUserId()));
            cqManager.sendMsg(cqMsg);
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
            cqMsg.setMessage(String.format(Tip.NO_RECENT_RECORD_PASSED, userFromAPI.getUserName(), scoreUtil.convertGameModeToString(argument.getMode())));
            cqManager.sendMsg(cqMsg);
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
            cqManager.sendMsg(cqMsg);
            return;
        }
        switch (argument.getSubCommandLowCase()) {
            case "prs":
                String resp = scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName(), count);
                cqMsg.setMessage(resp);
                cqManager.sendMsg(cqMsg);
                break;
            case "pr":
                String filename = imgUtil.drawResult(userFromAPI, score, beatmap, argument.getMode());
                cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
                cqManager.sendMsg(cqMsg);
                break;
            default:
                break;
        }

    }

    @GroupAuthorityControl
    public void getBonusPP(CqMsg cqMsg) {
        //为什么这个方法会被切面拦截两次。。
        //2018-2-28 17:25:09 卧槽 没写break 我是sb
        Userinfo userFromAPI = null;
        User user;
        int num = 0;
        boolean text = true;
        Argument argument = cqMsg.getArgument();
        if (argument.getMode() == null) {
            argument.setMode(0);
        }
        switch (argument.getSubCommandLowCase()) {
            case "bns":
                String username = argument.getUsername();
                //处理彩蛋
                if ("白菜".equals(username)) {
                    cqMsg.setMessage("你以为会有彩蛋吗x");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(argument.getMode(), username);
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERNAME_GET_FAILED, argument.getUsername()));
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (userFromAPI.getUserId() == 3) {
                    cqMsg.setMessage(Tip.QUERY_BANCHO_BOT);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user = userDAO.getUser(null, userFromAPI.getUserId());
                if (user == null) {
                    logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
                    user = userUtil.registerUser(userFromAPI.getUserId(), argument.getMode(), 0L, Overall.DEFAULT_ROLE);
                }
                if (user.isBanned()) {
                    cqMsg.setMessage(Tip.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }

                break;
            case "mybns":
            case "bnsme":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage(Tip.USER_NOT_BIND);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user.setLastActiveDate(LocalDate.now());
                userDAO.updateUser(user);
                if (user.isBanned()) {
                    cqMsg.setMessage(Tip.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(argument.getMode(), user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, cqMsg.getUserId(), user.getUserId()));
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            default:
                break;
        }
        //获取页面：getuser
        List<Score> bps = apiManager.getBP(argument.getMode(), userFromAPI.getUserName());

        double scorepp = calculateScorePP(bps);
        double totalpp = userFromAPI.getPpRaw();
        double bonuspp = totalpp - scorepp;

        int scoreCount = ((int) (Math.log10(-(bonuspp / 416.6667D) + 1.0D) / Math.log10(0.9994D)));
        String scoreCountS = (scoreCount == 0 && bonuspp > 0.0D) ? "25397+" : String.valueOf(scoreCount);
        String resp = "玩家" + userFromAPI.getUserName() + "在模式" + scoreUtil.convertGameModeToString(argument.getMode()) + "的BonusPP为：" + new DecimalFormat("#0.00").format(bonuspp)
                + "\n计算出的ScorePP（所有成绩提供的PP）为：" + new DecimalFormat("#0.00").format(scorepp)
                + "\n总PP为：" + new DecimalFormat("#0.00").format(userFromAPI.getPpRaw())
                + "\n计算出的总成绩数为：" + scoreCountS
                + "\n改造自https://github.com/RoanH/osu-BonusPP项目。";
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
        return;
    }

    @GroupAuthorityControl
    public void getElo(CqMsg cqMsg) {
        Userinfo userFromAPI = null;
        User user;
        int num = 0;
        boolean text = true;
        Argument argument = cqMsg.getArgument();
        if (argument.getMode() == null) {
            argument.setMode(0);
        }
        switch (argument.getSubCommandLowCase()) {
            case "elo":
                String username = argument.getUsername();
                //处理彩蛋
                if ("白菜".equals(username)) {
                    cqMsg.setMessage("你以为会有彩蛋吗x");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(argument.getMode(), username);
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USERNAME_GET_FAILED, argument.getUsername()));
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (userFromAPI.getUserId() == 3) {
                    cqMsg.setMessage(Tip.QUERY_BANCHO_BOT);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user = userDAO.getUser(null, userFromAPI.getUserId());
                if (user == null) {
                    logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
                    user = userUtil.registerUser(userFromAPI.getUserId(), argument.getMode(), 0L, Overall.DEFAULT_ROLE);
                }
                if (user.isBanned()) {
                    cqMsg.setMessage(Tip.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }

                break;
            case "myelo":
            case "elome":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage(Tip.USER_NOT_BIND);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user.setLastActiveDate(LocalDate.now());
                userDAO.updateUser(user);
                if (user.isBanned()) {
                    cqMsg.setMessage(Tip.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(argument.getMode(), user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, cqMsg.getUserId(), user.getUserId()));
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            default:
                return;
        }
        //获取页面：getuser
        Elo elo = webPageManager.getElo(userFromAPI.getUserId());
        EloChange eloChange = webPageManager.getEloChange(userFromAPI.getUserId());
        String resp;
        if (elo == null) {
            resp = "没有找到你的ELO信息。";

        } else {
            resp = "玩家" + userFromAPI.getUserName() + "的ELO为：" + elo.getElo()
                    + "\n由PP计算的初始ELO为：" + elo.getInit_elo();
            if (elo.getRank()!=null){
                resp+= "\n排名为：" + elo.getRank();
            }
            if (Objects.equals(elo.getCode(), 40004)) {
                resp += "\n您的初始ELO仅供参考，请尽快参加比赛获得真实ELO数据！" +
                        "\nELO周赛火热进行中，QQ群：738401694";
            }
        }
        if (eloChange.getElo_change() != null) {
            resp += "\n最近一次ELO更改：" + eloChange.getElo_change();
            resp += "\nMP Link：http://otsu.fun/matches/" + eloChange.getMatch_id();
        } else {
            resp += "\n最近没有ELO变动。";
        }

        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
        return;
    }


    public void roll(CqMsg cqMsg) {
        cqMsg.setMessage(String.valueOf(new Random().nextInt(100)));
        cqManager.sendMsg(cqMsg);
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
        cqManager.sendMsg(cqMsg);
    }
    @GroupAuthorityControl
    public void myRole(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        String username;
        Userinfo userFromAPI = null;
        User user;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(Tip.USER_NOT_BIND);
        } else {
            cqMsg.setMessage("你的当前用户组有：" + user.getRole() + "，主显用户组为：" + user.getMainRole());
        }
        cqManager.sendMsg(cqMsg);
        return;

    }

    public void pretreatmentParameterForBPCommand(CqMsg cqMsg) {

    }

    /**
     * 尝试计算非Bonus PP
     *
     * @param s The list of the player's top 100 scores
     * @return The amount of non-bonus PP this player has
     */
    private double calculateScorePP(List<Score> s) {
        double scorepp = 0.0D;
        for (int i = 0; i < s.size(); i++) {
            scorepp += s.get(i).getPp() * Math.pow(0.95D, i);
        }
        return scorepp + extraPolatePPRemainder(s);
    }

    /**
     * 计算BP外的PP，Top玩家可能这个值非常大，如果BP数目不到100返回0
     *
     * @param s The list of the player's top scores
     * @return The amount of PP the player has from non-top-100 scores
     */
    private double extraPolatePPRemainder(List<Score> s) {
        if (s.size() < 100) {
            return 0D;
        }
        double[] b = calculateLinearRegression(s);
        double n = s.size() + 1;
        double pp = 0D;
        while (true) {
            double val = (b[0] + b[1] * n) * Math.pow(0.95D, n);
            if (val < 0D) {
                break;
            }
            pp += val;
            n++;
        }
        return pp;
    }

    /**
     * 用线性回归等式推断BP外的成绩
     * <pre>
     * The following formulas are used:
     * B1 = Ox,y / Ox^2
     * B0 = Uy - B1 * Ux
     * Ox,y = (1/N) * 'sigma(N,i=1)'((Xi - Ux)(Yi - Uy))
     * Ox^2 = (1/N) * 'sigma(N,i=1)'((Xi - U)^2)
     * </pre>
     *
     * @param s 前100BP
     * @return 线性回归方程的两个参数： y = b0 + b1 * x
     */
    private double[] calculateLinearRegression(List<Score> s) {
        double sumOxy = 0.0D;
        double sumOx2 = 0.0D;
        double avgX = 0.0D;
        double avgY = 0.0D;
        for (Score score : s) {
            avgX++;
            avgY += score.getPp();
        }
        avgX = avgX / s.size();
        avgY = avgY / s.size();
        double n = 0;
        for (Score sc : s) {
            sumOxy += (n - avgX) * (sc.getPp() - avgY);
            sumOx2 += Math.pow(n - avgX, 2.0D);
            n++;
        }
        double Oxy = sumOxy / s.size();
        double Ox2 = sumOx2 / s.size();
        return new double[]{avgY - (Oxy / Ox2) * avgX, Oxy / Ox2};
    }

    public void setId(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        String username;
        Userinfo userFromAPI = null;
        User user;

        userFromAPI = apiManager.getUser(0, argument.getUsername());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(Tip.USERNAME_GET_FAILED, argument.getUsername()));
            cqManager.sendMsg(cqMsg);
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
        cqManager.sendMsg(cqMsg);

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
            cqManager.sendMsg(cqMsg);
            return;
        } else {
            userFromAPI = apiManager.getUser(0, user.getUserId());
            if (userFromAPI == null) {
                cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, cqMsg.getUserId(), user.getUserId()));
                cqManager.sendMsg(cqMsg);
                return;
            }
            logger.info("尝试将" + userFromAPI.getUserName() + "的模式修改为" + argument.getMode());

            user.setMode(argument.getMode());
            user.setLastActiveDate(LocalDate.now());
            userDAO.updateUser(user);
            cqMsg.setMessage("更新成功：你的游戏模式已修改为" + scoreUtil.convertGameModeToString(argument.getMode()));
        }

        cqManager.sendMsg(cqMsg);

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
            if (roles.contains(argument.getRole()) || Objects.equals("creep",argument.getRole())) {
                user.setMainRole(argument.getRole());
                userDAO.updateUser(user);
                cqMsg.setMessage("更新成功：你的主显用户组已修改为" + argument.getRole());
            } else {
                cqMsg.setMessage("你当前不在请求的用户组" + argument.getRole() + "中。当前所在用户组为：" + user.getRole());
            }

        }
        cqManager.sendMsg(cqMsg);
    }
    @GroupAuthorityControl
    public void switchBorder(CqMsg cqMsg) {
        User user;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(Tip.USER_NOT_BIND);
            cqManager.sendMsg(cqMsg);
            return;
        }
        user.setLastActiveDate(LocalDate.now());
        userDAO.updateUser(user);
        if (user.isBanned()) {
            cqMsg.setMessage(Tip.USER_IS_BANNED);
            cqManager.sendMsg(cqMsg);
            return;
        }

        user.setUseEloBorder(!user.getUseEloBorder());
        userDAO.updateUser(user);
        cqMsg.setMessage("更新成功：你已修改为" + (user.getUseEloBorder() ? "" : "不") + "使用ELO边框");
        cqManager.sendMsg(cqMsg);
    }
}
