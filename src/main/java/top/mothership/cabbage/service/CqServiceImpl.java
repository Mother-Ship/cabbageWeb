package top.mothership.cabbage.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.annotation.GroupAuthorityControl;
import top.mothership.cabbage.consts.OverallConsts;
import top.mothership.cabbage.consts.TipConsts;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.mapper.RedisDAO;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.coolq.Argument;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.coolq.CqResponse;
import top.mothership.cabbage.pojo.coolq.QQInfo;
import top.mothership.cabbage.pojo.coolq.osu.*;
import top.mothership.cabbage.util.osu.ScoreUtil;
import top.mothership.cabbage.util.osu.UserUtil;
import top.mothership.cabbage.util.qq.CompressLevelEnum;
import top.mothership.cabbage.util.qq.ImgUtil;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.time.*;
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
     * @param paramVerifyUtil
     * @param apiManager      the api manager
     * @param cqManager       the cq manager
     * @param webPageManager  网页相关抓取工具
     * @param userDAO         the user dao
     * @param userInfoDAO     the user info dao
     * @param imgUtil         the img util
     * @param scoreUtil       the score util
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
                    cqMsg.setMessage(TipConsts.USER_NOT_BIND);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
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
                        cqMsg.setMessage(TipConsts.USER_IS_BANNED);
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
                        cqMsg.setMessage(String.format(TipConsts.USER_GET_FAILED, user.getQq(), user.getUserId()));
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
                        cqMsg.setMessage(String.format(TipConsts.USER_ID_GET_FAILED_AND_NOT_USED, argument.getUserId()));
                        cqManager.sendMsg(cqMsg);
                        return;
                    } else {
                        //构造User对象和4条Userinfo写入数据库，如果指定了mode就使用指定mode
                        if (argument.getMode() == null) {
                            argument.setMode(0);
                        }
                        userUtil.registerUser(userFromAPI.getUserId(), argument.getMode(), 0L, OverallConsts.DEFAULT_ROLE);
                        userInDB = userFromAPI;
                        //初次使用，数据库肯定没有指定天数的数据
                        approximate = true;
                    }
                    role = OverallConsts.DEFAULT_ROLE;
                } else if (user.isBanned()) {
                    //只有在确定user不是null的时候，如果参数没有提供mode，用user预设的覆盖
                    if (argument.getMode() == null) {
                        argument.setMode(user.getMode());
                    }
                    //当数据库查到该玩家，并且被ban时，从数据库里取出最新的一份userinfo，作为要展现的数据传给绘图类
                    userFromAPI = userInfoDAO.getNearestUserInfo(argument.getMode(), user.getUserId(), LocalDate.now());
                    if (userFromAPI == null) {
                        //如果数据库中该玩家该模式没有历史记录……
                        cqMsg.setMessage(TipConsts.USER_IS_BANNED);
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
                    cqMsg.setMessage(String.format(TipConsts.USERNAME_GET_FAILED, argument.getUsername()));
                    cqManager.sendMsg(cqMsg);
                    return;
                }


                user = userDAO.getUser(null, userFromAPI.getUserId());
                if (user == null) {
                    //未指定mode的时候改为0
                    if (argument.getMode() == null) {
                        argument.setMode(0);
                    }
                    userUtil.registerUser(userFromAPI.getUserId(), argument.getMode(), 0L, OverallConsts.DEFAULT_ROLE);
                    userInDB = userFromAPI;
                    role = OverallConsts.DEFAULT_ROLE;
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
                    role = user.getRole();
                    if (argument.getDay() > 0) {
                        if (argument.getDay().equals(1)) {
                            //加一个从redis取数据的设定
                            //TODO 开debug测试一下这个特性
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
        roles = userUtil.sortRoles(role);

        if (argument.getMode().equals(0)) {
            //主模式才获取score rank
            //gust？
            if (userFromAPI.getUserId() == 1244312
                    //怕他
                    || userFromAPI.getUserId() == 6149313
                    //小飞菜
                    || userFromAPI.getUserId() == 3995056
                    //苏娜小苏娜
                    || userFromAPI.getUserId() == 3213720
                    //MFA
                    || userFromAPI.getUserId() == 6854920) {
                scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 10000);
            } else {
                scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
            }
            //调用绘图类绘图(2017-10-19 14:09:04 roles改为List，排好序后直接取第一个)
        }
        String result = imgUtil.drawUserInfo(userFromAPI, userInDB, roles.get(0), argument.getDay(), approximate, scoreRank, argument.getMode());
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
                    cqMsg.setMessage(String.format(TipConsts.USERNAME_GET_FAILED, argument.getUsername()));
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "bpu":
            case "bpus":
                userFromAPI = apiManager.getUser(0, argument.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(TipConsts.USERID_GET_FAILED, argument.getUserId()));
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
                    cqMsg.setMessage(TipConsts.USER_NOT_BIND);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (user.isBanned()) {
                    cqMsg.setMessage(TipConsts.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(user.getMode(), user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(TipConsts.USER_GET_FAILED, user.getQq(), user.getUserId()));
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
                for (int j = 0; j < bpListMixedMode.get(i).size(); j++)
                    if (bpListMixedMode.get(i).get(j).getDate().after(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))) {
                        bpListMixedMode.get(i).get(j).setBpId(j);
                        //对BP进行遍历，请求API将名称写入
                        Beatmap map = apiManager.getBeatmap(bpListMixedMode.get(i).get(j).getBeatmapId());
                        bpListMixedMode.get(i).get(j).setBeatmapName(map.getArtist() + " - " + map.getTitle() + " [" + map.getVersion() + "]");
                        todayBP.add(bpListMixedMode.get(i).get(j));
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
                    cqMsg.setMessage(String.format(TipConsts.USERNAME_GET_FAILED, argument.getUsername()));
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            case "bpu":
            case "bpus":
                userFromAPI = apiManager.getUser(0, argument.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(TipConsts.USERID_GET_FAILED, argument.getUserId()));
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
                    cqMsg.setMessage(TipConsts.USER_NOT_BIND);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (user.isBanned()) {
                    cqMsg.setMessage(TipConsts.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(0, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(TipConsts.USER_GET_FAILED, user.getQq(), user.getUserId()));
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
                cqMsg.setMessage(scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName(),null));
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


    public void setId(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        String username;
        Userinfo userFromAPI = null;
        User user;

        userFromAPI = apiManager.getUser(0, argument.getUsername());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(TipConsts.USERNAME_GET_FAILED, argument.getUsername()));
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
                userUtil.registerUser(userFromAPI.getUserId(), argument.getMode(), cqMsg.getUserId(), OverallConsts.DEFAULT_ROLE);

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
            if(user.isBanned()){
                cqMsg.setMessage("你的QQ已经绑定了玩家：" + user.getCurrentUname() + "，并且该账号已经被ban；如果发生错误请联系妈妈船。");
            }else {
                userFromAPI = apiManager.getUser(0, user.getUserId());
                cqMsg.setMessage("你的QQ已经绑定了玩家：" + userFromAPI.getUserName() + "，如果发生错误请联系妈妈船。");
            }
        }
        cqManager.sendMsg(cqMsg);

    }

    public void recent(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        Userinfo userFromAPI = null;
        User user;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(TipConsts.USER_NOT_BIND);
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (user.isBanned()) {
            cqMsg.setMessage(TipConsts.USER_IS_BANNED);
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (argument.getMode() == null) {
            //如果没有指定mode，就改为user的mode
            argument.setMode(user.getMode());
        }
        userFromAPI = apiManager.getUser(argument.getMode(), user.getUserId());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(TipConsts.USER_GET_FAILED, user.getQq(), user.getUserId()));
            cqManager.sendMsg(cqMsg);
            return;
        }

        logger.info("检测到对" + userFromAPI.getUserName() + "的最近游戏记录查询");
        Score score = apiManager.getRecent(argument.getMode(), userFromAPI.getUserId());
        if (score == null) {
            cqMsg.setMessage(String.format(TipConsts.NO_RECENT_RECORD, userFromAPI.getUserName(), scoreUtil.convertGameModeToString(argument.getMode())));
            cqManager.sendMsg(cqMsg);
            return;
        }
        List<Score> scores = apiManager.getRecents(argument.getMode(),userFromAPI.getUserId());
        Integer count = 0;
        for (Score score1 : scores) {
            if(score.getBeatmapId().equals(score1.getBeatmapId())){
                count++;
            }
        }

        Beatmap beatmap = apiManager.getBeatmap(score.getBeatmapId());
        if (beatmap == null) {
            cqMsg.setMessage(String.format(TipConsts.BEATMAP_GET_FAILED, score.getBeatmapId()));
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (argument.isText()) {
            String resp = scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName(),count);
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
            img = imgUtil.drawImage(ImgUtil.images.get("helpTrick.png"),CompressLevelEnum.不压缩);
            cqMsg.setMessage("[CQ:image,file=base64://" + img + "]");
        } else {
            img = imgUtil.drawImage(ImgUtil.images.get("help.png"), CompressLevelEnum.不压缩);
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
            cqMsg.setMessage(String.format(TipConsts.BEATMAP_NO_SCORE, beatmap.getBeatmapId(), scoreUtil.convertGameModeToString(argument.getMode())));
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
                    + "\n" + "http://inso.link/yukiho/?m=" + beatmap.getBeatmapSetId()
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
            cqMsg.setMessage(String.format(TipConsts.USER_GET_FAILED, argument.getUsername()));
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
            case "564679329":
                String role = null;
                Long chartGroupId = null;
                switch (String.valueOf(cqMsg.getGroupId())) {
                    case "201872650":
                        role = "mp5";
                        chartGroupId = 635731109L;
                        resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp5。请修改一下你的群名片(包含完整osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                        break;
                    case "564679329":
                        role = "mp4";
                        chartGroupId = 517183331L;
                        resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp4。请修改一下你的群名片(包含完整osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
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
                    if (OverallConsts.DEFAULT_ROLE.equals(user.getRole())) {
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
                        "OSU! MP乐园4号群 (MP4) *(3600-5100pp):564679329\n" +
                        "OSU! MP乐园5号群 (MP5) *(2500-4000pp，无严格下限):201872650";
                break;
            case "112177148":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "],欢迎来到第一届MP4杯赛群。\n本群作为历届mp4选手聚集地，之后比赛结束后会将赛群合并到本群。";
                break;
            case "772918786":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "],欢迎来到第七届MP5杯赛群。\n请修改群名片为osu! id，并且仔细阅读群公告。\n报名地址：https://www.wenjuan.com/s/uEvquqW/";
                break;
            case "807757470":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "],欢迎来到第三届MP4杯赛群。\n请修改群名片为osu! id，并且仔细阅读群公告。";
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
            case "564679329":
                role = "mp4";
                chartGroupId = 517183331L;
                break;
            default:
                //只处理mp4 5的褪裙
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
                    cqMsg.setMessage(TipConsts.USER_NOT_BIND);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (user.isBanned()) {
                    cqMsg.setMessage(TipConsts.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(0, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(TipConsts.USER_GET_FAILED, user.getQq(), user.getUserId()));
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
                    cqMsg.setMessage(String.format(TipConsts.USERNAME_GET_FAILED, username));
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (userFromAPI.getUserId() == 3) {
                    cqMsg.setMessage(TipConsts.QUERY_BANCHO_BOT);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user = userDAO.getUser(null, userFromAPI.getUserId());
                if (user == null) {
                    logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
                    user = userUtil.registerUser(userFromAPI.getUserId(), 0, 0L, OverallConsts.DEFAULT_ROLE);
                }
                break;
            default:
                break;

        }
        Map<String, Double> map = webPageManager.getPPPlus(user.getUserId());
        Map<String,Integer> map2 = webPageManager.getOsuChanBestBpmAndLength(user.getUserId());
        //2018-3-29 09:52:16加入Map容量判断
        if (map != null && map.size() == 6 &&map2!=null&&map2.size()==2) {
            double drugsS4Cost = Math.pow((map.get("Jump") / 3000F), 0.9F)
                    * Math.pow((map.get("Flow") / 1500F), 0.5F)
                    + Math.pow((map.get("Speed") / 2000F), 1.25F)
                    + (map.get("Accuracy") / 2700F);
            double mp4S2Cost = Math.pow(
                    ((0.02D * (10D * Math.sqrt((Math.atan((2 * map.get("Jump") - (2400D + 2135D)) / (2400D - 2135D)) + Math.PI / 2D + 8D)
                            * (Math.atan((2D * map.get("Flow") - (720D + 418D)) / (720D - 418D)) + Math.PI / 2D + 3D))
                            + 7D * (Math.atan((2D * map.get("Speed") - (1600D + 1324D)) / (1600D - 1324D)) + Math.PI / 2D)
                            + 3D * (Math.atan((2D * map.get("Stamina") - (1300D + 930D)) / (1300D - 930D)) + Math.PI / 2D)
                            + 1D * (Math.atan((2D * map.get("Accuracy") - (1300D + 1000D)) / (1300D - 1000D)) + Math.PI / 2D)
                            + 5D * (Math.atan((2D * map.get("Precision") - (700D + 450D)) / (700D - 450D)) + Math.PI / 2D))) - 1D)
                    , 2.5D);
            double oclbS10Cost = Math.pow((map.get("Jump") / 3000F), 0.8F)
                    * Math.pow((map.get("Flow") / 1500F), 0.6F)
                    + Math.pow((map.get("Speed") / 2000F), 0.8F)
                    * Math.pow((map.get("Stamina") / 2000F), 0.5F)
                    + (map.get("Accuracy") / 2700F);
            double oclrCost = Math.pow((map.get("Jump") / 3000F), 0.8F)
                    * Math.pow((map.get("Flow") / 1500F), 0.5F)
                    + Math.pow((map.get("Speed") / 2000F), 0.8F)
                    * Math.pow((map.get("Stamina") / 2000F), 0.5F)
                    + (map.get("Accuracy") / 2700F);
            double yuTangCost = (Math.pow((map.get("Jump") / 3000F), 0.8F)
                    * Math.pow((map.get("Flow") / 1500F), 0.6F)
                    + Math.pow((map.get("Speed") / 2000F), 0.8F)
                    * Math.pow((map.get("Stamina") / 2000F), 0.5F)
                    + (map.get("Accuracy") / 3000F))
                    *Math.min(1,Math.pow((map2.get("BPM")/190D),2))
                    *Math.min(1,Math.pow((map2.get("Length")*(map2.get("BPM")/(190D*150D))),0.2D));
            String filename = imgUtil.drawRadarImage(map, userFromAPI);
            cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]\n"
                    + "在**OCL系列比赛(A/B/C)**中，该玩家的Cost是：" + new DecimalFormat("#0.00").format(oclbS10Cost)
                    + "。\n在**OCL系列比赛(新秀组)**中，该玩家的Cost是：" + new DecimalFormat("#0.00").format(oclrCost)
                    + "。\n在**第五届某个不能提起名字的比赛**中，该玩家的Cost是：" + new DecimalFormat("#0.00").format(drugsS4Cost)
//                    + "。\n在**第三届MP4**中，该玩家的Cost是：" + new DecimalFormat("#0.00").format(mp4S2Cost)
//                    + "。\n在**第三届鱼塘杯**中，该玩家的Cost是：" + new DecimalFormat("#0.00").format(yuTangCost)
                    + "。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        cqMsg.setMessage("由于网络原因（PP+的网站过于弱智），获取你的Cost失败……");
        cqManager.sendMsg(cqMsg);
        return;
    }

    public void recentPassed(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();

        Userinfo userFromAPI = null;
        User user;
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(TipConsts.USER_NOT_BIND);
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (user.isBanned()) {
            cqMsg.setMessage(TipConsts.USER_IS_BANNED);
            cqManager.sendMsg(cqMsg);
            return;
        }
        userFromAPI = apiManager.getUser(0, user.getUserId());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(TipConsts.USER_GET_FAILED, cqMsg.getUserId(), user.getUserId()));
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
            cqMsg.setMessage(String.format(TipConsts.NO_RECENT_RECORD, userFromAPI.getUserName(), scoreUtil.convertGameModeToString(argument.getMode())));
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
            cqMsg.setMessage(String.format(TipConsts.NO_RECENT_RECORD_PASSED, userFromAPI.getUserName(), scoreUtil.convertGameModeToString(argument.getMode())));
            cqManager.sendMsg(cqMsg);
            return;
        }
        Integer count = 0;
        for (Score score1 : scores) {
            if(score.getBeatmapId().equals(score1.getBeatmapId())){
                count++;
            }
        }
        Beatmap beatmap = apiManager.getBeatmap(score.getBeatmapId());
        if (beatmap == null) {
            cqMsg.setMessage(String.format(TipConsts.BEATMAP_GET_FAILED, score.getBeatmapId()));
            cqManager.sendMsg(cqMsg);
            return;
        }
        switch (argument.getSubCommandLowCase()) {
            case "prs":
                String resp = scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName(),count);
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
                userFromAPI = apiManager.getUser(0, username);
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(TipConsts.USERNAME_GET_FAILED, argument.getUsername()));
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (userFromAPI.getUserId() == 3) {
                    cqMsg.setMessage(TipConsts.QUERY_BANCHO_BOT);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user = userDAO.getUser(null, userFromAPI.getUserId());
                if (user == null) {
                    logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
                    user = userUtil.registerUser(userFromAPI.getUserId(), argument.getMode(), 0L, OverallConsts.DEFAULT_ROLE);
                }
                if (user.isBanned()) {
                    cqMsg.setMessage(TipConsts.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }

                break;
            case "mybns":
            case "bnsme":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage(TipConsts.USER_NOT_BIND);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (user.isBanned()) {
                    cqMsg.setMessage(TipConsts.USER_IS_BANNED);
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiManager.getUser(0, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage(String.format(TipConsts.USER_GET_FAILED, cqMsg.getUserId(), user.getUserId()));
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

    public void setMode(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        String username;
        Userinfo userFromAPI = null;
        User user;
        //只有这个QQ对应的id是null
        user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage(TipConsts.USER_NOT_BIND);
            cqManager.sendMsg(cqMsg);
            return;
        } else {
            userFromAPI = apiManager.getUser(0, user.getUserId());
            if (userFromAPI == null) {
                cqMsg.setMessage(String.format(TipConsts.USER_GET_FAILED, cqMsg.getUserId(), user.getUserId()));
                cqManager.sendMsg(cqMsg);
                return;
            }
            logger.info("尝试将" + userFromAPI.getUserName() + "的模式修改为" + argument.getMode());

            user.setMode(argument.getMode());
            userDAO.updateUser(user);
            cqMsg.setMessage("更新成功：你的游戏模式已修改为" + scoreUtil.convertGameModeToString(argument.getMode()));
        }

        cqManager.sendMsg(cqMsg);

    }

    @GroupAuthorityControl
    public void roll(CqMsg cqMsg) {
        cqMsg.setMessage(String.valueOf(new Random().nextInt(100)));
        cqManager.sendMsg(cqMsg);
    }

    @GroupAuthorityControl(allowed = {308419061,793260840})
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


    @Scheduled(cron = "0 0 4 * * ?")
    public void importUserInfo() {
        //似乎每分钟并发也就600+，不需要加延迟……
        java.util.Date start = Calendar.getInstance().getTime();
        //清掉前一天全部信息
        redisDAO.flushDb();
        userInfoDAO.clearTodayInfo(LocalDate.now().minusDays(1));
        logger.info("开始进行每日登记");
        List<String> bannedList = new ArrayList<>();
        Integer successCount = 0;
        List<Integer> list = userDAO.listUserIdByRole(null, false);
        for (Integer aList : list) {
            User user = userDAO.getUser(null, aList);
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
        cqMsg.setMessage("录入完成，共录入条目数：" + successCount + "，以下玩家本次被标明已封禁：" + bannedList);
        cqManager.sendMsg(cqMsg);
    }

    @Scheduled(cron = "0 0 * * * ?")
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

    private void handlePPOverflow(User user, Userinfo userinfo) {
        //如果用户在mp4组
        List<String> roles = new ArrayList<>(Arrays.asList(user.getRole().split(",")));
        if (roles.contains("mp4")) {
            CqMsg cqMsg = new CqMsg();
            cqMsg.setSelfId(1020640876L);
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
            if (userinfo.getPpRaw() > 5100 + 0.49) {
                //回溯昨天这时候检查到的pp
                Userinfo lastDayUserinfo = userInfoDAO.getUserInfo(0, userinfo.getUserId(), LocalDate.now().minusDays(2));
                //如果昨天这时候的PP存在，并且也超了
                if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > 5100 + 0.49) {
                    //继续回溯前天这时候的PP
                    lastDayUserinfo = userInfoDAO.getUserInfo(0, userinfo.getUserId(), LocalDate.now().minusDays(3));
                    //如果前天这时候的PP存在，并且也超了
                    if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > 5100 + 0.49) {
                        //回溯大前天的PP
                        lastDayUserinfo = userInfoDAO.getUserInfo(0, userinfo.getUserId(), LocalDate.now().minusDays(4));
                        //如果大前天这个时候也超了，就飞了
                        if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > 5100 + 0.49) {
                            if (!user.getQq().equals(0L)) {
                                //2018-3-16 13:13:53似乎现在白菜踢人不会自动删组？在这里补上试试
                                user = userUtil.delRole("mp4", user);
                                userDAO.updateUser(user);
                                cqMsg.setUserId(user.getQq());
                                cqMsg.setMessageType("kick");
                                cqManager.sendMsg(cqMsg);
                                cqMsg.setMessageType("private");
                                cqMsg.setMessage("由于PP超限，已将你移出MP4群。请考虑加入mp3群：210342787。");
                                cqManager.sendMsg(cqMsg);
                                //2018-1-29 12:01:06 现在飞的时候会自动清理用户组
                            }
                        } else {
                            //大前天没超
                            if (!user.getQq().equals(0L)) {
                                cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在1天后将你移除。请考虑加入mp3群：210342787。");
                                cqManager.sendMsg(cqMsg);
                            }
                        }
                    } else {
                        //前天没超
                        if (!user.getQq().equals(0L)) {
                            cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在2天后将你移除。请考虑加入mp3群：210342787。");
                            cqManager.sendMsg(cqMsg);
                        }
                    }
                } else {
                    //昨天没超
                    if (!user.getQq().equals(0L)) {
                        cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在3天后将你移除。请考虑加入mp3群：210342787。");
                        cqManager.sendMsg(cqMsg);
                    }

                }
            }

        }

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
            if (userinfo.getPpRaw() > 4000 + 0.49) {

                //回溯昨天这时候检查到的pp
                Userinfo lastDayUserinfo = userInfoDAO.getUserInfo(0, userinfo.getUserId(), LocalDate.now().minusDays(2));
                //如果昨天这时候的PP存在，并且也超了
                if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > 4000 + 0.49) {
                    //继续回溯前天这时候的PP
                    lastDayUserinfo = userInfoDAO.getUserInfo(0, userinfo.getUserId(), LocalDate.now().minusDays(3));
                    //如果前天这时候的PP存在，并且也超了
                    if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > 4000 + 0.49) {
                        //回溯大前天的PP
                        lastDayUserinfo = userInfoDAO.getUserInfo(0, userinfo.getUserId(), LocalDate.now().minusDays(4));
                        //如果大前天这个时候也超了，就飞了
                        if (lastDayUserinfo != null && lastDayUserinfo.getPpRaw() > 4000 + 0.49) {
                            if (!user.getQq().equals(0L)) {
                                //2018-3-16 13:13:53似乎现在白菜踢人不会自动删组？在这里补上试试
                                user = userUtil.delRole("mp5", user);
                                userDAO.updateUser(user);
                                cqMsg.setUserId(user.getQq());
                                cqMsg.setMessageType("kick");
                                cqManager.sendMsg(cqMsg);
                                cqMsg.setMessageType("private");
                                cqMsg.setMessage("由于PP超限，已将你移出MP5群。请考虑加入mp4群：564679329。");
                                cqManager.sendMsg(cqMsg);
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
                    }
                } else {
                    //昨天没超
                    if (!user.getQq().equals(0L)) {
                        cqMsg.setMessage("[CQ:at,qq=" + user.getQq() + "] 检测到你的PP超限。将会在3天后将你移除。请考虑加入mp4群：564679329。");
                        cqManager.sendMsg(cqMsg);
                    }

                }
            }

        }
    }

    /**
     * 清理每天生成的临时文件。
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void clearTodayImages() {
        final Path path = Paths.get("/root/coolq/data/image");
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("正在删除" + file.toString());
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };
        final Path path2 = Paths.get("/root/coolq/data/record");
        SimpleFileVisitor<Path> finder2 = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("正在删除" + file.toString());
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };
        final Path path3 = Paths.get("/root/coolq2/data/image");
        SimpleFileVisitor<Path> finder3 = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("正在删除" + file.toString());
                Files.delete(file);
                return super.visitFile(file, attrs);
            }
        };
        final Path path4 = Paths.get("/root/coolq2/data/record");
        SimpleFileVisitor<Path> finder4 = new SimpleFileVisitor<Path>() {
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
            Files.walkFileTree(path3, finder3);
            Files.walkFileTree(path4, finder4);
        } catch (IOException e) {
            logger.error("清空临时文件时出现异常，" + e.getMessage());
        }

    }

    public void pretreatmentParameterForBPCommand(CqMsg cqMsg) {

    }


}
