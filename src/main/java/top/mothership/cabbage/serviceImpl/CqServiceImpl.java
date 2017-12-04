package top.mothership.cabbage.serviceImpl;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.consts.PatternConsts;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.CoolQ.CqResponse;
import top.mothership.cabbage.pojo.CoolQ.QQInfo;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.util.Overall;
import top.mothership.cabbage.util.osu.ApiUtil;
import top.mothership.cabbage.util.qq.*;

import javax.annotation.PostConstruct;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Cq service.
 */
@Service
public class CqServiceImpl {


    private final ApiManager apiManager;
    private final CqManager cqManager;
    private final CmdUtil cmdUtil;
    private final MsgUtil msgUtil;
    private final UserInfoDAO userInfoDAO;
    private UserDAO userDAO;
    private Logger logger = LogManager.getLogger(this.getClass());

    /**
     * Instantiates a new Cq service.
     *
     * @param apiManager  the api manager
     * @param cqManager   the cq manager
     * @param msgUtil     the msg util
     * @param userDAO     the user dao
     * @param cmdUtil     the cmd util
     * @param userInfoDAO the user info dao
     */
    @Autowired
    public CqServiceImpl(ApiManager apiManager, CqManager cqManager, MsgUtil msgUtil, UserDAO userDAO, CmdUtil cmdUtil, UserInfoDAO userInfoDAO) {
        this.apiManager = apiManager;
        this.cqManager = cqManager;
        this.msgUtil = msgUtil;
        this.userDAO = userDAO;
        this.cmdUtil = cmdUtil;
        this.userInfoDAO = userInfoDAO;
    }


    /**
     * 处理statu/stat/statme的方法。
     *
     * @param cqMsg QQ消息体
     */
    public void statUserInfo(CqMsg cqMsg) {
        //明天好好用文字整理下逻辑吧……
        String msg = cqMsg.getMessage();
        Matcher cmdRegex = PatternConsts.CMD_REGEX_NUM.matcher(msg);
        String username = null;
        //预定义变量
        int day;
        User user = null;
        Userinfo userFromAPI;
        boolean near = false;
        Userinfo userInDB = null;
        String role;
        int scoreRank;
        List<String> roles;
        //首先尝试解析数字，对各种数字异常情况进行处理并返回
        if (!"".equals(cmdRegex.group(3))) {
            try {
                day = Integer.valueOf(cmdRegex.group(3));
                if (day < 0) {
                    cqMsg.setMessage("白菜不会预知未来。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (LocalDate.now().isAfter(LocalDate.of(2007, 9, 16).plusDays(day))) {
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


        //对statme和statu，取出对应的user信息
        switch (cmdRegex.group(1)) {
            case "statme":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if(user==null){
                    cqMsg.setMessage("你没有绑定osu!id。请使用!setid 你的osuid 命令。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                if (user.getBanned() == 1) {
                    //当数据库查到该玩家，并且被ban时，从数据库里取出最新的一份userinfo伪造
                    userFromAPI = userInfoDAO.getNearestUserInfo(user.getUserId(), new Date(Instant.now().toEpochMilli()));
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
                }
                break;
            case "statu":
                username = cmdRegex.group(2);
                user = userDAO.getUser(null, Integer.valueOf(username));
                if (user != null && user.getBanned() == 1) {
                    //当数据库查到该玩家，并且被ban时，从数据库里取出最新的一份userinfo伪造
                    userFromAPI = userInfoDAO.getNearestUserInfo(user.getUserId(), new Date(Instant.now().toEpochMilli()));
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
                    userFromAPI = apiManager.getUser(null, Integer.valueOf(username));
                }
                break;
            case "stat":
                username = cmdRegex.group(2);
                userFromAPI = apiManager.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有从osu!api获取到" + username + "的玩家信息。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user = userDAO.getUser(null, userFromAPI.getUserId());
                //处理彩蛋
                if ("白菜".equals(username)) {
                    cqMsg.setMessage("唉，没人疼没人爱，我是地里一颗小白菜。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            default:
                break;
        }

//如果day=0则不检验也不获取数据库中的userinfo（不写入数据库，避免预先查过之后add不到）
        if (day > 0) {
            regIfFirstUse(userFromAPI);
            userInDB = userInfoDAO.getUserInfo(userFromAPI.getUserId(), date);
            if (userInDB == null) {
                userInDB = userInfoDAO.getNearestUserInfo(userFromAPI.getUserId(), date);
                near = true;
            }
        }


        //生成数天前的java.sql.date
        Date date = new Date(cl.getTimeInMillis());


        if (userFromAPI.getUserId() == 3) {
            cqMsg.setMessage("你们总是想查BanchoBot。\n可是BanchoBot已经很累了，她不想被查。\n她想念自己的小ppy，而不是被逼着查PP。\n你有考虑过这些吗？没有！你只考虑过你自己。");
            cqUtil.sendMsg(cqMsg);
            return;
        }

        //之前有一个逻辑错误：如果day=0而且该用户没有用过白菜，userDAO.getUser是null。
        User
        if (user == null) {
            role = "creep";
        } else {
            role = user.getRole();
        }
        roles = Arrays.asList(role.split(","));
        //此处自定义实现排序方法
        //dev>分群>主群>比赛
        roles.sort((o1, o2) -> {
            //mp5s优先级得低于mp5
//            if (o1.contains("mp5s") && (o2.equals("mp5") || o2.equals("mp5mc") || o2.equals("mp5chart"))) {
//                return -1;
//            }
            //比赛期间mp5s优先级比mp5高，只比mc和chart低
            if (o1.contains("mp5s") && (o2.equals("mp5mc") || o2.equals("mp5chart"))) {
                return -1;
            }
            //mp4s<mp4
            if (o1.contains("mp4s") && o2.equals("mp4")) {
                return -1;
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

        //获取score rank
        //gust？
        if (userFromAPI.getUserId() == 1244312
                || userFromAPI.getUserId() == 6149313
                || userFromAPI.getUserId() == 3213720
                //MFA
                || userFromAPI.getUserId() == 6854920) {
            scoreRank = webPageUtil.getRank(userFromAPI.getRankedScore(), 1, 10000);
        } else {
            scoreRank = webPageUtil.getRank(userFromAPI.getRankedScore(), 1, 2000);
        }
        //调用绘图类绘图(2017-10-19 14:09:04 roles改为List，排好序后直接取第一个)
        String result = imgUtil.drawUserInfo(userFromAPI, userInDB, roles.get(0), day, near, scoreRank);
        cqMsg.setMessage("[CQ:image,file=base64://" + result + "]");
        cqManager.sendMsg(cqMsg);
    }


    public void praseCmd(CqMsg cqMsg) throws Exception {
        java.util.Date s = Calendar.getInstance().getTime();
        String msg = cqMsg.getMessage();
        Matcher m = Pattern.compile(Overall.CMD_REGEX).matcher(msg);
        m.find();
        String username;
        Userinfo userFromAPI;
        User user;
        Beatmap beatmap;
        int num;
        switch (m.group(1)) {
            case "bp":
            case "bps":
            case "bpu":
            case "bpus":
                num = 0;
                //屏蔽掉用户没有输入用户名时候的异常
                if ("".equals(m.group(2)))
                    return;
                username = m.group(2).substring(1);
                m2 = Pattern.compile(Overall.CMD_REGEX_NUM).matcher(msg);
                if (m2.find()) {
                    if (!msgUtil.CheckBPNumParam(m2.group(3), cqMsg)) {
                        return;
                    }
                    num = Integer.valueOf(m2.group(3));
                    //屏蔽掉用户没有输入用户名时候的异常
                    if ("".equals(m2.group(2)))
                        return;
                    username = m2.group(2).substring(1);
                }
                if ("白菜".equals(username)) {
                    cqMsg.setMessage("大白菜（学名：Brassica rapa pekinensis，异名Brassica campestris pekinensis或Brassica pekinensis）" +
                            "是一种原产于中国的蔬菜，又称“结球白菜”、“包心白菜”、“黄芽白”、“胶菜”等。(via 维基百科)");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                if (m.group(1).equals("bpu") || m.group(1).equals("bpus")) {
                    userFromAPI = apiUtil.getUser(null, Integer.valueOf(username));
                } else {
                    userFromAPI = apiUtil.getUser(username, null);
                }
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有从osu!api获取到" + username + "的玩家信息。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                if (m.group(1).equals("bps") && num > 0) {
                    //文字版不对批量BP做处理
                    cmdUtil.printSimpleBP(userFromAPI, num, cqMsg);
                } else {
                    cmdUtil.printBP(userFromAPI, num, cqMsg);
                }
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "setid":
                //屏蔽掉用户没有输入用户名时候的异常
                if ("".equals(m.group(2)))
                    return;
                username = m.group(2).substring(1);
                cmdUtil.bindQQAndOsu(username, cqMsg.getUserId(), cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;

            case "bpme":
            case "bpmes":
                num = 0;
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiUtil.getUser(null, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage("API没有获取到" + cqMsg.getUserId() + "绑定的uid为" + user.getUserId() + "的玩家信息。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                m2 = Pattern.compile(Overall.CMD_REGEX_NUM).matcher(msg);
                if (m2.find()) {
                    if (!msgUtil.CheckBPNumParam(m2.group(3), cqMsg)) {
                        return;
                    }
                    num = Integer.valueOf(m2.group(3));
                }

                if (m.group(1).equals("bpmes") && num > 0) {
                    cmdUtil.printSimpleBP(userFromAPI, num, cqMsg);
                } else {
                    cmdUtil.printBP(userFromAPI, num, cqMsg);
                }
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "recent":
            case "rs":
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiUtil.getUser(null, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage("API没有获取到QQ" + cqMsg.getUserId() + "绑定的" + user.getUserId() + "玩家的信息。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                if (m.group(1).equals("rs")) {
                    cmdUtil.getSimpleRecent(userFromAPI, cqMsg);
                } else {
                    cmdUtil.getRecent(userFromAPI, cqMsg);
                }
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;

            case "help":
                cmdUtil.sendHelp(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "fp":
                if (!msgUtil.CheckBidParam(m.group(2).substring(1), cqMsg)) {
                    return;
                }
                Integer bid = Integer.valueOf(m.group(2).substring(1));
                beatmap = apiUtil.getBeatmap(bid);
                if (beatmap == null) {
                    cqMsg.setMessage("提供的bid没有找到谱面信息。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                //一次性取2个
                List<Score> scores = apiUtil.getFirstScore(bid, 2);
                if (scores.size() == 0) {
                    cqMsg.setMessage("提供的bid没有找到#1成绩。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                if (scores.get(0).getUserId() != userDAO.getUser(cqMsg.getUserId(), null).getUserId()) {
                    cqMsg.setMessage("不是你打的#1不给看哦。\n如果你确定是你打的，看看是不是没登记osu!id？(使用!setid命令)");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                cmdUtil.getFristRank(beatmap, scores, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "sleep":
                Long hour;
                try {
                    hour = Long.valueOf(m.group(2).substring(1));
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
                cqMsg.setMessage("[CQ:record,file=base64://" + Overall.ZOU_HAO_BU_SONG + "]");
                cqUtil.sendMsg(cqMsg);
                cqMsg.setMessageType("smoke");
                cqMsg.setDuration((int) (hour * 3600));
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "roll":
                if (cqMsg.getGroupId() == 677545541 || cqMsg.getGroupId() == 112177148) {
                    cqMsg.setMessage(String.valueOf(new Random().nextInt(100)));
                    cqUtil.sendMsg(cqMsg);
                }
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "me":
                String keyword = m.group(2).substring(1);
                user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user == null) {
                    cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiUtil.getUser(null, user.getUserId());
                if (userFromAPI == null) {
                    cqMsg.setMessage("API没有获取到QQ" + cqMsg.getUserId() + "绑定的" + user.getUserId() + "玩家的信息。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                cmdUtil.getMyScore(keyword, user, userFromAPI, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "search":
                keyword = m.group(2).substring(1);
                cmdUtil.searchBeatmap(keyword, cqMsg);
                break;
            case "add":
                //面向mp4 5 chart组，相当于!setid+!sudo add
                List<Long> mpChartMember = new ArrayList<>();
                //加入两个chart组群员
                for (QQInfo q : cqUtil.getGroupMembers(517183331L).getData()) {
                    mpChartMember.add(q.getUserId());
                }
                for (QQInfo q : cqUtil.getGroupMembers(635731109L).getData()) {
                    mpChartMember.add(q.getUserId());
                }
                if (!mpChartMember.contains(cqMsg.getUserId())) {
                    cqMsg.setMessage("[CQ:face,id=14]？");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                switch (String.valueOf(cqMsg.getGroupId())) {
                    case "564679329":
                    case "517183331":
                        //MP4
                        msg = m.group(2).substring(1);
                        username = msg.substring(0, msg.indexOf(":"));
                        msg = msg.substring(msg.indexOf(":") + 1);
                        Long QQ = Long.valueOf(msg);
                        cmdUtil.addUserRole(new String[]{username}, "mp4", cqMsg);
                        cmdUtil.bindQQAndOsu(username, QQ, cqMsg);
                        break;
                    case "201872650":
                    case "635731109":
                        //MP5
                        msg = m.group(2).substring(1);
                        username = msg.substring(0, msg.indexOf(":"));
                        msg = msg.substring(msg.indexOf(":") + 1);
                        QQ = Long.valueOf(msg);
                        cmdUtil.addUserRole(new String[]{username}, "mp5", cqMsg);
                        cmdUtil.bindQQAndOsu(username, QQ, cqMsg);
                        break;
                    default:
                        cqMsg.setMessage("请不要在mp4/5/chart群之外的地方使用。");
                        cqUtil.sendMsg(cqMsg);
                        return;
                }
                break;
            case "del":
                mpChartMember = new ArrayList<>();
                //加入两个chart组群员
                for (QQInfo q : cqUtil.getGroupMembers(517183331L).getData()) {
                    mpChartMember.add(q.getUserId());
                }
                for (QQInfo q : cqUtil.getGroupMembers(635731109L).getData()) {
                    mpChartMember.add(q.getUserId());
                }
                if (!mpChartMember.contains(cqMsg.getUserId())) {
                    cqMsg.setMessage("[CQ:face,id=14]？");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                switch (String.valueOf(cqMsg.getGroupId())) {
                    case "564679329":
                    case "517183331":
                        username = m.group(2).substring(1);
                        cmdUtil.removeUserRole(new String[]{username}, "mp4", cqMsg);
                        break;
                    case "201872650":
                    case "635731109":
                        username = m.group(2).substring(1);
                        cmdUtil.removeUserRole(new String[]{username}, "mp5", cqMsg);
                        break;
                    default:
                        cqMsg.setMessage("请不要在mp4/5/chart群之外的地方使用。");
                        cqUtil.sendMsg(cqMsg);
                        return;
                }
                break;

//邮箱验证码没什么样本……还是不搞了，这块注释掉吧
//            case "login":
//                pwd = m.group(2).substring(1);
//                user = baseMapper.getUser(String.valueOf(cqMsg.getUserId()), null);
//                if (user == null) {
//                    cqMsg.setMessage("你没有绑定默认id。请使用!setid <你的osu!id> 命令。");
//                    cqUtil.sendMsg(cqMsg);
//                    return;
//                }
//
//                login(user, pwd, cqMsg);
//                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
//                break;
//            case "mu":
//                target = m.group(2).substring(1);
//                user = baseMapper.getUser(String.valueOf(cqMsg.getUserId()), null);
//                if (user == null) {
//                    cqMsg.setMessage("你没有绑定默认id。请使用!setid <你的osu!id> 命令。");
//                    cqUtil.sendMsg(cqMsg);
//                    return;
//                }
//                if (user.getCookie() == null) {
//                    cqMsg.setMessage("你没有登陆。请使用!login <你的osu!密码> 命令。");
//                    cqUtil.sendMsg(cqMsg);
//                    return;
//                }
//                list = new Gson().fromJson(user.getCookie(), new TypeToken<List<BasicClientCookie>>() {
//                }.getType());
//                if (Calendar.getInstance().getTime().after(list.get(2).getExpiryDate())) {
//                    cqMsg.setMessage("你的Cookie已过期，请私聊使用!login <你的osu!密码> 重新登录。");
//                    cqUtil.sendMsg(cqMsg);
//                    return;
//                }
//                mutual(user, target, cqMsg);
//                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
//                break;
//            case "verify":
//                user = baseMapper.getUser(String.valueOf(cqMsg.getUserId()), null);
//                if (user == null) {
//                    cqMsg.setMessage("你没有绑定默认id。请使用!setid <你的osu!id> 命令。");
//                    cqUtil.sendMsg(cqMsg);
//                    return;
//                }
//                if (user.getCookie() == null) {
//                    cqMsg.setMessage("你没有登陆。请使用!login <你的osu!密码> 命令。");
//                    cqUtil.sendMsg(cqMsg);
//                    return;
//                }
//                verify(user, cqMsg);
//                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
//                break;
        }

    }




    @PostConstruct
    private void notifyInitComplete() {
        CqMsg cqMsg = new CqMsg();
        cqMsg.setMessage("初始化完成，欢迎使用");
        cqMsg.setMessageType("private");
        cqMsg.setUserId(1335734657L);
        cqUtil.sendMsg(cqMsg);
    }


}
