package top.mothership.cabbage.serviceImpl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.mapper.BaseMapper;
import top.mothership.cabbage.pojo.*;
import top.mothership.cabbage.service.CqService;
import top.mothership.cabbage.util.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CqServiceImpl implements CqService {
    private static LinkedHashMap<CqMsg, String> inviteRequests = new LinkedHashMap<>();
    private ApiUtil apiUtil;
    private CqUtil cqUtil;
    private ImgUtil imgUtil;
    private MsgUtil msgUtil;
    private WebPageUtil webPageUtil;
    private ScoreUtil scoreUtil;
    private BaseMapper baseMapper;
    private Logger logger = LogManager.getLogger(this.getClass());
    private static List<String> admin = Arrays.asList(Constant.CABBAGE_CONFIG.getString("admin").split(","));

    @Autowired
    public CqServiceImpl(ApiUtil apiUtil, MsgUtil msgUtil, CqUtil cqUtil, ImgUtil imgUtil, WebPageUtil webPageUtil, ScoreUtil scoreUtil, BaseMapper baseMapper) {
        this.apiUtil = apiUtil;
        this.msgUtil = msgUtil;
        this.cqUtil = cqUtil;
        this.imgUtil = imgUtil;
        this.webPageUtil = webPageUtil;
        this.scoreUtil = scoreUtil;
        this.baseMapper = baseMapper;

    }


    @Override
    public void praseCmd(CqMsg cqMsg) {
        java.util.Date s = Calendar.getInstance().getTime();
        String msg = cqMsg.getMessage();
        Matcher m = Pattern.compile(Constant.CMD_REGEX).matcher(msg);
        m.find();
        String username;
        Userinfo userFromAPI;
        User user;
        Score score;
        Beatmap beatmap;
        int day;
        int num;
        switch (m.group(1)) {
            case "stat":
                //先分割出一个username，再尝试使用带数字的正则去匹配
                day = 1;
                username = m.group(2).substring(1);

                m = Pattern.compile(Constant.CMD_REGEX_NUM).matcher(msg);
                if (m.find()) {
                    cqMsg.setMessage(m.group(3));
                    if (!msgUtil.CheckDayParam(cqMsg)) {
                        return;
                    }
                    day = Integer.valueOf(m.group(3));
                    username = m.group(2).substring(1);
                }
                if ("白菜".equals(username)) {
                    cqMsg.setMessage("唉，没人疼没人爱，我是地里一颗小白菜。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiUtil.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有从osu!api获取到" + username + "的玩家信息。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                statUserInfo(userFromAPI, day, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "bp":
            case "bps":
                num = 0;
                username = m.group(2).substring(1);
                Matcher m2 = Pattern.compile(Constant.CMD_REGEX_NUM).matcher(msg);
                if (m2.find()) {
                    cqMsg.setMessage(m2.group(3));
                    if (!msgUtil.CheckBPNumParam(cqMsg)) {
                        return;
                    }
                    num = Integer.valueOf(m2.group(3));
                    username = m2.group(2).substring(1);
                }
                if ("白菜".equals(username)) {
                    cqMsg.setMessage("大白菜（学名：Brassica rapa pekinensis，异名Brassica campestris pekinensis或Brassica pekinensis）" +
                            "是一种原产于中国的蔬菜，又称“结球白菜”、“包心白菜”、“黄芽白”、“胶菜”等。(via 维基百科)");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiUtil.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有从osu!api获取到" + username + "的玩家信息。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                if (m.group(1).equals("bps") && num > 0) {
                    //文字版不对批量BP做处理
                    printSimpleBP(userFromAPI, num, cqMsg);
                } else {
                    printBP(userFromAPI, num, cqMsg);
                }
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "setid":
                username = m.group(2).substring(1);
                bindQQAndOsu(username, Long.toString(cqMsg.getUserId()), cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "statme":
                day = 1;
                user = baseMapper.getUser(String.valueOf(cqMsg.getUserId()), null);
                if (user == null) {
                    cqMsg.setMessage("你没有绑定默认id。请使用!setid <你的osu!id> 命令。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                m = Pattern.compile(Constant.CMD_REGEX_NUM).matcher(msg);
                if (m.find()) {
                    cqMsg.setMessage(m.group(3));
                    if (!msgUtil.CheckDayParam(cqMsg)) {
                        return;
                    }
                    day = Integer.valueOf(m.group(3));
                }

                userFromAPI = apiUtil.getUser(null, String.valueOf(user.getUserId()));
                if (userFromAPI == null) {
                    cqMsg.setMessage("API没有获取到" + cqMsg.getUserId() + "绑定的uid为" + user.getUserId() + "的玩家信息。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                statUserInfo(userFromAPI, day, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "bpme":
            case "bpmes":
                num = 0;
                user = baseMapper.getUser(String.valueOf(cqMsg.getUserId()), null);
                if (user == null) {
                    cqMsg.setMessage("你没有绑定默认id。请使用!setid <你的osu!id> 命令。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiUtil.getUser(null, String.valueOf(user.getUserId()));
                if (userFromAPI == null) {
                    cqMsg.setMessage("API没有获取到" + cqMsg.getUserId() + "绑定的uid为" + user.getUserId() + "的玩家信息。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                m2 = Pattern.compile(Constant.CMD_REGEX_NUM).matcher(msg);
                if (m2.find()) {
                    cqMsg.setMessage(m2.group(3));
                    if (!msgUtil.CheckBPNumParam(cqMsg)) {
                        return;
                    }
                    num = Integer.valueOf(m2.group(3));
                }

                if (m.group(1).equals("bpmes") && num > 0) {
                    printSimpleBP(userFromAPI, num, cqMsg);
                } else {
                    printBP(userFromAPI, num, cqMsg);
                }
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;


            case "recent":
            case "rs":
                user = baseMapper.getUser(String.valueOf(cqMsg.getUserId()), null);
                if (user == null) {
                    cqMsg.setMessage("你没有绑定默认id。请使用!setid <你的osu!id> 命令。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                userFromAPI = apiUtil.getUser(null, String.valueOf(user.getUserId()));
                if (userFromAPI == null) {
                    cqMsg.setMessage("API没有获取到QQ" + cqMsg.getUserId() + "绑定的" + user.getUserId() + "玩家的信息。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                if (m.group(1).equals("rs")) {
                    getSimpleRecent(userFromAPI, cqMsg);
                } else {
                    getRecent(userFromAPI, cqMsg);
                }
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;

            case "help":
                if ((int) (Math.random() * 10) == 1) {
                    logger.info("QQ" + cqMsg.getUserId() + "抽中了1/10的几率，触发了Trick");
                    cqMsg.setMessage("[CQ:image,file=!helpTrick.png]");
                } else {
                    cqMsg.setMessage("[CQ:image,file=!help.png]");
                }
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "fp":

                cqMsg.setMessage(m.group(2).substring(1));
                if (!msgUtil.CheckBidParam(cqMsg)) {
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
                List<Score> scores = apiUtil.getScore(bid, 2);
                if (scores.size() == 0) {
                    cqMsg.setMessage("提供的bid没有找到#1成绩。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                if (scores.get(0).getUserId() != baseMapper.getUser(String.valueOf(cqMsg.getUserId()), null).getUserId()) {
                    cqMsg.setMessage("不是你打的#1不给看哦。\n如果你确定是你打的，看看是不是没登记osu!id？(使用!setid命令)");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                getFristRank(beatmap, scores, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "sleep":
                Long hour;
                try {
                    hour = Long.valueOf(m.group(2).substring(1));
                } catch (StringIndexOutOfBoundsException e) {
                    hour = 13L;
                }
                if (hour > 13) {
                    hour = 13L;
                }
                logger.info(cqMsg.getUserId() + "被自己禁言" + hour + "小时。");
                cqMsg.setMessage("睡吧。");
                cqUtil.sendMsg(cqMsg);
                cqMsg.setMessageType("smoke");
                cqMsg.setDuration((int) (hour * 3600));
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
        }

    }

    @Override
    public void praseAdminCmd(CqMsg cqMsg) {
        java.util.Date s = Calendar.getInstance().getTime();

        if (!admin.contains(String.valueOf(cqMsg.getUserId()))) {

            //如果没有权限
            cqMsg.setMessage("[CQ:face,id=14]？");
            cqUtil.sendMsg(cqMsg);
            return;

        }
        String msg = cqMsg.getMessage();
        Matcher m = Pattern.compile(Constant.ADMIN_CMD_REGEX).matcher(msg);
        m.find();
        int day;
        int sec;
        String username;
        String target;
        String URL;
        String[] usernames;
        String role;
        String QQ;
        String resp = "";
        int index;
        switch (m.group(1)) {
            case "add":
                index = m.group(2).indexOf(":");
                if (index == -1) {
                    //如果拿不到
                    cqMsg.setMessage("没有指定用户组。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                usernames = m.group(2).substring(1, index).split(",");
                role = m.group(2).substring(index + 1);
                logger.info("分隔字符串完成，用户：" + Arrays.toString(usernames) + "，用户组：" + role);
                modifyUserRole(usernames, role, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "del":
                usernames = m.group(2).substring(1).split(",");
                role = "creep";
                logger.info("分隔字符串完成，用户：" + Arrays.toString(usernames) + "，用户组：" + role);
                modifyUserRole(usernames, role, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "check":
                username = m.group(2).substring(1);
                Userinfo userFromAPI = apiUtil.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("API没有获取到玩家" + username + "的信息。");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                User user = baseMapper.getUser(null, userFromAPI.getUserId());
                if (user == null) {
                    cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "没有使用过白菜。请先用add命令添加。");
                } else {
                    cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "的用户组是" + user.getRole() + "。");
                }
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;

            case "褪裙":
            case "退群":
                role = m.group(2).substring(1);
                checkPPOverflow(role, cqMsg);
                break;
            case "bg":
                try {
                    URL = m.group(2).substring(m.group(2).indexOf("http"));
                    target = m.group(2).substring(1, m.group(2).indexOf("http") - 1);
                } catch (IndexOutOfBoundsException e) {
                    cqMsg.setMessage("字符串处理异常。");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                downloadBG(URL, target, cqMsg);
                //手动调用重载缓存
                ImgUtil.loadCache();
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "recent":
                username = m.group(2).substring(1);
                userFromAPI = apiUtil.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有获取到玩家" + username + "的信息。");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                getRecent(userFromAPI, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "afk":
                try {
                    index = m.group(2).substring(1).indexOf(":");
                    if (index == -1) {
                        role = "mp5";
                        day = Integer.valueOf(m.group(2).substring(1));
                    } else {
                        //这里因为字符串最前面有个空格，所以得是index+2
                        role = m.group(2).substring(index + 2);
                        day = Integer.valueOf(m.group(2).substring(1).substring(0, index));
                    }
                } catch (IndexOutOfBoundsException e) {
                    cqMsg.setMessage("字符串处理异常。");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                logger.info("检测到管理员对" + role + "用户组" + day + "天前的AFK玩家查询");
                checkAfkPlayer(role, day, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "smoke":
                try {
                    //改为!sudo smoke @xx 600这样的格式
                    index = msg.indexOf("]");
                    sec = Integer.valueOf(msg.substring(index + 2));
                    //!sudo smoke [CQ:at,qq=1012621328] 600
                    QQ = msg.substring(22, index);

                } catch (NumberFormatException e) {
                    try {
                        index = msg.indexOf("]");
                        QQ = msg.substring(22, index);
                        sec = 600;
                    } catch (IndexOutOfBoundsException e1) {
                        cqMsg.setMessage("字符串处理异常。");
                        cqUtil.sendMsg(cqMsg);
                        logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                        return;
                    }
                }
                if ("all".equals(QQ)) {

                    List<QQInfo> memberList = cqUtil.getGroupMembers(cqMsg.getGroupId()).getData();
                    cqMsg.setMessageType("smoke");
                    cqMsg.setDuration(sec);
                    String operator = cqMsg.getUserId().toString();
                    for (QQInfo aList : memberList) {
                        cqMsg.setUserId(aList.getUserId());
                        cqUtil.sendMsg(cqMsg);
                        logger.info(aList.getUserId() + "被" + operator + "禁言" + sec + "秒。");
                    }
                    cqMsg.setMessage("[CQ:image,file=!smokeAll.jpg]");
                    cqMsg.setMessageType("group");
                    cqUtil.sendMsg(cqMsg);
                } else {
                    logger.info(QQ + "被" + cqMsg.getUserId() + "禁言" + sec + "秒。");
                    cqMsg.setMessage("They're all dead!!");
                    cqUtil.sendMsg(cqMsg);
                    cqMsg.setMessageType("smoke");
                    cqMsg.setDuration(sec);
                    cqMsg.setUserId(Long.valueOf(QQ));
                    cqUtil.sendMsg(cqMsg);

                }
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "smokeAll":
                cqMsg.setMessageType("smokeAll");
                cqMsg.setEnable(true);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "unsmokeAll":
                cqMsg.setMessageType("smokeAll");
                cqMsg.setEnable(false);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "listInvite":

                if (inviteRequests.size() > 0) {
                    resp = "以下是白菜本次启动期间收到的加群邀请：";
                    for (CqMsg aList : inviteRequests.keySet()) {
                        resp = resp.concat("\n" + "Flag：" + aList.getFlag() + "，群号：" + aList.getGroupId() + "，已通过：" + inviteRequests.get(aList));
                    }
                } else {
                    resp = "本次启动白菜没有收到加群邀请。";
                }
                cqMsg.setMessage(resp);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "handleInvite":
                String flag = m.group(2).substring(1);
                logger.info("正在通过对Flag为：" + flag + "的邀请");
                cqMsg.setFlag(flag);
                cqMsg.setApprove(true);
                cqMsg.setType("invite");
                cqMsg.setMessageType("handleInvite");
                CqResponse cqResponse = cqUtil.sendMsg(cqMsg);
                if (cqResponse.getRetCode() == 0) {
                    for (CqMsg aList : inviteRequests.keySet()) {
                        if (aList.getFlag().equals(flag)) {
                            inviteRequests.replace(aList, "是");
                        }
                    }
                    cqMsg.setMessageType("group");
                    cqMsg.setMessage("已通过Flag为：" + flag + "的邀请");
                    cqUtil.sendMsg(cqMsg);
                }
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "clearInvite":
                logger.info("正在清理邀请请求");
                inviteRequests.clear();
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "unbind":
                QQ = m.group(2).substring(1);
                user = baseMapper.getUser(QQ, null);
                if (user == null) {
                    cqMsg.setMessage("该QQ没有绑定用户……");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                user.setQQ("0");

                baseMapper.updateUser(user);
                cqMsg.setMessage("QQ" + QQ + "的绑定信息已经清除");
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "fp":
                cqMsg.setMessage(m.group(2).substring(1));
                if (!msgUtil.CheckBidParam(cqMsg)) {
                    return;
                }
                Integer bid = Integer.valueOf(m.group(2).substring(1));
                Beatmap beatmap = apiUtil.getBeatmap(bid);
                if (beatmap == null) {
                    cqMsg.setMessage("提供的bid没有找到谱面信息。");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                //一次性取2个
                List<Score> score = apiUtil.getScore(bid, 2);
                if (score.size() == 0) {
                    cqMsg.setMessage("提供的bid没有找到#1成绩。");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                getFristRank(beatmap, score, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "listMsg":
                //照例使用艾特
                index = msg.indexOf("]");
                QQ = msg.substring(24, index);
                if ("all".equals(QQ)) {
                    resp = "啥玩意啊 咋回事啊";
                }else {
                    ArrayList<CqMsg> msgs = SmokeUtil.msgQueues.get(cqMsg.getGroupId()).getMsgsByQQ(Long.valueOf(QQ));
                    String card = cqUtil.getCard(Long.valueOf(QQ), cqMsg.getGroupId());
                    if (msgs.size() == 0) {
                        resp = "没有" + QQ + "的最近消息。";
                    } else if(msgs.size()<=10){

                        for (int i=0;i<msgs.size();i++) {
                            resp = resp.concat(  card + "<" + QQ + "> " + new SimpleDateFormat("HH:mm:ss").
                                    format(new Date(msgs.get(i).getTime() * 1000L)) + "\n  " + msgs.get(i).getMessage()+"\n");
                        }
                    }else{
                        for (int i=msgs.size()-10;i<msgs.size();i++) {
                            resp = resp.concat( card + "<" + QQ + "> " + new SimpleDateFormat("HH:mm:ss").
                                    format(new Date(msgs.get(i).getTime() * 1000L)) + "\n  " + msgs.get(i).getMessage()+"\n");
                        }
                    }
                }
                cqMsg.setMessage(resp);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;

        }

    }


    @Override
    public void praseNewsPaper(CqMsg cqMsg) {
        java.util.Date s = Calendar.getInstance().getTime();
        logger.info("开始处理" + cqMsg.getUserId() + "在" + cqMsg.getGroupId() + "群的加群请求");
        String resp;
        switch (String.valueOf(cqMsg.getGroupId())) {
            case "201872650":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp5。请修改一下你的群名片(包含osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                break;
            case "564679329":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp4。请修改一下你的群名片(包含osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                break;
            case "537646635":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎来到mp乐园主群。请修改一下你的群名片(包含osu! id)，以下为mp乐园系列分群介绍：\n" +
                        "osu!mp乐园高rank部 592339532\n" +
                        "osu!mp乐园3号群(四位数7000pp上限):234219559\n" +
                        "OSU! MP乐园4号群 (MP4) *(3600-5100pp):564679329\n" +
                        "OSU! MP乐园5号群 (MP5) *(2500-4000pp，无严格下限):201872650";
                break;
            case "677545541":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "],欢迎来到第四届MP5杯赛群。\n报名比赛需要加入本群并填写此表（http://t.cn/RO9bS4D），详见置顶公告。祝大家在比赛中有好的发挥！";
                break;
            default:
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "]，欢迎加入本群。";
                break;
        }
        cqMsg.setMessageType("group");
        cqMsg.setMessage(resp);
        cqUtil.sendMsg(cqMsg);
        logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
    }

    @Override
    public void stashInviteRequest(CqMsg cqMsg) {
        java.util.Date s = Calendar.getInstance().getTime();
        inviteRequests.put(cqMsg, "否");
        logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
    }

    private void statUserInfo(Userinfo userFromAPI, int day, CqMsg cqMsg) {
        //将day转换为Date
        Calendar cl = Calendar.getInstance();
        if (cl.get(Calendar.HOUR_OF_DAY) < 4) {
            cl.add(Calendar.DAY_OF_MONTH, -1);
        }
        cl.add(Calendar.DATE, -day);
        Date date = new Date(cl.getTimeInMillis());
        //预定义变量
        boolean near = false;
        Userinfo userInDB = null;
        String role;
        int scoreRank;
        //彩蛋


        if (userFromAPI == null) {
            cqMsg.setMessage("没有在官网查到这个玩家。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        if (userFromAPI.getUserId() == 3) {
            cqMsg.setMessage("你们总是想查BanchoBot。\n可是BanchoBot已经很累了，她不想被查。\n她想念自己的小ppy，而不是被逼着查PP。\n你有考虑过这些吗？没有！你只考虑过你自己。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        //如果day=0则不检验也不获取数据库中的userinfo（不写入数据库，避免预先查过之后add不到）
        logger.info("开始调用API查询" + userFromAPI.getUserName() + "的信息");
        if (day > 0) {
            regIfFirstUse(userFromAPI);
            userInDB = baseMapper.getUserInfo(userFromAPI.getUserId(), date);
            if (userInDB == null) {
                userInDB = baseMapper.getNearestUserInfo(userFromAPI.getUserId(), date);
                near = true;
            }
        }
        role = baseMapper.getUser(null, userFromAPI.getUserId()).getRole();
        if (role == null) {
            role = "creep";
        }
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
        //调用绘图类绘图
        imgUtil.drawUserInfo(userFromAPI, userInDB, role, day, near, scoreRank);
        //构造消息并发送
        cqMsg.setMessage("[CQ:image,file=" + userFromAPI.getUserId() + "stat.png]");
        cqUtil.sendMsg(cqMsg);

    }

    private void regIfFirstUse(Userinfo userFromAPI) {
        if (baseMapper.getUser(null, userFromAPI.getUserId()) == null) {
            logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
            //构造User对象写入数据库
            User user = new User();
            user.setUserId(userFromAPI.getUserId());
            user.setRole("creep");
            user.setQQ("0");
            baseMapper.addUser(user);
            //构造日历对象和List
            Calendar c = Calendar.getInstance();
            if (c.get(Calendar.HOUR_OF_DAY) < 4) {
                c.add(Calendar.DAY_OF_MONTH, -1);
            }
            userFromAPI.setQueryDate(new java.sql.Date(c.getTime().getTime()));

            //写入一行userinfo
            baseMapper.addUserInfo(userFromAPI);
        }
    }

    private void printBP(Userinfo userinfo, int num, CqMsg cqMsg) {
        //构造一个上一个四点的日历对象
        Calendar c = Calendar.getInstance();
//        凌晨四点之前，将日期减一
        if (c.get(Calendar.HOUR_OF_DAY) < 4) {
            c.add(Calendar.DATE, -1);
        }
        c.set(Calendar.HOUR_OF_DAY, 4);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        //BP功能就不登记了吧
        logger.info("开始获取玩家" + userinfo.getUserName() + "的BP");
        List<Score> list = apiUtil.getBP(userinfo.getUserName(), null);
        LinkedHashMap<Score, Integer> result = new LinkedHashMap<>();

        for (int i = 0; i < list.size(); i++) {
            //对BP进行遍历，如果产生时间晚于当天凌晨4点
            if (list.get(i).getDate().after(c.getTime())) {
                result.put(list.get(i), i);
            }
        }

        if (num == 0) {
            logger.info("筛选今日BP成功");
            if (result.size() == 0) {
                cqMsg.setMessage("玩家" + userinfo.getUserName() + "今天还。。\n这么悲伤的事情，不忍心说啊。");
                cqUtil.sendMsg(cqMsg);

                logger.info("没有查到该玩家今天更新的BP");
                return;
            }

            for (Score aList : result.keySet()) {
                //对BP进行遍历，请求API将名称写入
                logger.info("正在获取Beatmap id为" + aList.getBeatmapId() + "的谱面的名称");
                Beatmap map = apiUtil.getBeatmap(aList.getBeatmapId());
                aList.setBeatmapName(map.getArtist() + " - " + map.getTitle() + " [" + map.getVersion() + "]");
            }
            imgUtil.drawUserBP(userinfo, result);
            cqMsg.setMessage("[CQ:image,file=" + userinfo.getUserId() + "BP.png]");
            cqUtil.sendMsg(cqMsg);
        } else {
            if (num > list.size()) {
                cqMsg.setMessage("该玩家没有打出指定的bp……");
                logger.info("请求的bp数比玩家bp总数量大");
                cqUtil.sendMsg(cqMsg);
            } else {
                //list基于0，得-1
                Score score = list.get(num - 1);
                logger.info("获得了玩家" + userinfo.getUserName() + "的第" + num + "个BP：" + score.getBeatmapId() + "，正在获取歌曲名称");
                Beatmap map = apiUtil.getBeatmap(score.getBeatmapId());

                imgUtil.drawResult(userinfo, score, map);
                cqMsg.setMessage("[CQ:image,file=" + score.getBeatmapId() + "_" + new SimpleDateFormat("yy-MM-dd").format(score.getDate()) + ".png]");
                cqUtil.sendMsg(cqMsg);
            }
        }

    }

    private void bindQQAndOsu(String username, String fromQQ, CqMsg cqMsg) {
        Userinfo userinfo = apiUtil.getUser(username, null);

        //这彩蛋没意思，去了

        if (userinfo == null) {
            cqMsg.setMessage("没有在官网找到该玩家。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        username = userinfo.getUserName();
        logger.info("尝试将" + username + "绑定到" + fromQQ + "上");
        regIfFirstUse(userinfo);
        int userId = userinfo.getUserId();


        //只有这个QQ对应的id是0
        User userFromDB = baseMapper.getUser(fromQQ, null);
        if (userFromDB == null) {
            //只有这个id对应的QQ是null
            userFromDB = baseMapper.getUser(null, userId);
            if (userFromDB.getQQ().equals("0")) {
                //由于reg方法中已经进行过登记了,所以这用的应该是update操作
                User user = new User();
                user.setUserId(userId);
                user.setQQ(fromQQ);
                baseMapper.updateUser(user);
                cqMsg.setMessage("将" + username + "绑定到" + fromQQ + "成功。");
            } else {
                cqMsg.setMessage("你的osu!账号已经绑定了" + userFromDB.getQQ() + "，如果发生错误请联系妈妈船。");
            }
        } else {
            userinfo = apiUtil.getUser(null, String.valueOf(userFromDB.getUserId()));
            cqMsg.setMessage("你的QQ已经绑定了" + userinfo.getUserName() + "，如果发生错误请联系妈妈船。");
        }
        cqUtil.sendMsg(cqMsg);
    }

    private void modifyUserRole(String[] usernames, String role, CqMsg cqMsg) {
        List<String> nullList = new ArrayList<>();
        List<String> doneList = new ArrayList<>();
        List<String> addList = new ArrayList<>();
        Userinfo userFromAPI = null;
        for (String username : usernames) {
            logger.info("开始从API获取" + username + "的信息");
            userFromAPI = apiUtil.getUser(username, null);
            //如果user不是空的(官网存在记录)
            if (userFromAPI != null) {
                //查找userRole数据库

                if (baseMapper.getUser(null, userFromAPI.getUserId()) == null) {
                    //如果userRole库中没有这个用户
                    //构造User对象写入数据库
                    logger.info("开始将用户" + userFromAPI.getUserName() + "添加到数据库。");
                    User user = new User();
                    user.setUserId(userFromAPI.getUserId());
                    user.setRole("creep");
                    user.setQQ("0");
                    baseMapper.addUser(user);

                    Calendar c = Calendar.getInstance();
                    if (c.get(Calendar.HOUR_OF_DAY) < 4) {
                        c.add(Calendar.DAY_OF_MONTH, -1);
                    }
                    userFromAPI.setQueryDate(new Date(c.getTime().getTime()));

                    baseMapper.addUserInfo(userFromAPI);

                    if (usernames.length == 1) {
                        logger.info("新增单个用户，绘制名片");
                        int scoreRank = webPageUtil.getRank(userFromAPI.getRankedScore(), 1, 2000);
                        imgUtil.drawUserInfo(userFromAPI, null, role, 0, false, scoreRank);
                    }
                    addList.add(userFromAPI.getUserName());
                } else {
                    //进行Role更新
                    User user = new User();
                    user.setUserId(userFromAPI.getUserId());
                    user.setRole(role);
                    baseMapper.updateUser(user);
                    doneList.add(userFromAPI.getUserName());
                }


            } else {
                nullList.add(username);
            }

        }
        String resp;
        resp = "用户组修改完成。";

        if (doneList.size() > 0) {
            resp = resp.concat("\n修改成功：" + doneList.toString());
        }

        if (addList.size() > 0) {
            resp = resp.concat("\n新增成功：" + addList.toString());
        }
        if (nullList.size() > 0) {
            resp = resp.concat("\n不存在的：" + nullList.toString());
        }
        if (usernames.length == 0) {
            resp = "没有做出改动。";
        }
        //最后的条件可以不用写，不过为了干掉这个报错还是谢了
        if (addList.size() == 1 && usernames.length == 1 && userFromAPI != null) {
            //这时候是只有单个用户，并且没有在nulllist里
            resp = resp.concat("\n[CQ:image,file=" + userFromAPI.getUserId() + "stat.png]");
        }
        cqMsg.setMessage(resp);
        cqUtil.sendMsg(cqMsg);
    }

    private void checkPPOverflow(String role, CqMsg cqMsg) {
        logger.info("开始检查" + role + "用户组中超限的玩家。");
        String resp;
        List<Integer> list = baseMapper.listUserIdByRole(role);
        List<String> overflowList = new ArrayList<>();
        for (Integer aList : list) {
            //拿到用户最接近今天的数据（因为是最接近所以也不用打补丁了）
            Userinfo userinfo = baseMapper.getNearestUserInfo(aList, new Date(Calendar.getInstance().getTimeInMillis()));
            //如果PP超过了警戒线，请求API拿到最新PP
            if (userinfo.getPpRaw() > Integer.valueOf(Constant.CABBAGE_CONFIG.getString(role + "RiskPP"))) {
                logger.info("开始从API获取" + aList + "的信息");
                userinfo = apiUtil.getUser(null, String.valueOf(aList));
                if (userinfo.getPpRaw() > Integer.valueOf(Constant.CABBAGE_CONFIG.getString(role + "PP")) + 0.49) {
                    logger.info("玩家" + aList + "超限，已记录");
                    overflowList.add(userinfo.getUserName());
                } else {
                    logger.info("玩家" + aList + "没有超限");
                }
            }
        }
        resp = "查询PP溢出玩家完成。";
        if (overflowList.size() > 0) {
            resp = resp.concat("\n查询到" + role + "用户组中，以下玩家：" + overflowList.toString() + "PP超出了设定的限制。");
        } else {
            resp = resp.concat("\n没有检测" + role + "用户组中PP溢出的玩家。");
        }
        cqMsg.setMessage(resp);
        cqUtil.sendMsg(cqMsg);
    }

    private void downloadBG(String URL, String target, CqMsg cqMsg) {
        BufferedImage bg;
        try {
            logger.info("开始根据URL下载新背景。");
            bg = ImageIO.read(new URL(URL));
        } catch (IOException e) {
            logger.error("根据URL下载背景图失败，" + e.getMessage());
            cqMsg.setMessage("根据URL下载背景图失败。错误信息是：" + e.getMessage());
            cqUtil.sendMsg(cqMsg);
            return;
        }
        //并不需要删除旧图片
        try {
            logger.info("开始将新背景写入硬盘");
            ImageIO.write(bg, "png", new File(Constant.CABBAGE_CONFIG.getString("path") + "\\data\\image\\resource\\img\\stat\\" + target + ".png"));
        } catch (IOException e) {
            logger.error("将新背景写入硬盘失败，" + e.getMessage());
            cqMsg.setMessage("将新背景写入硬盘失败。错误信息是：" + e.getMessage());
            cqUtil.sendMsg(cqMsg);
            return;
        }
        cqMsg.setMessage("修改用户/用户组" + target + "的背景图成功。");
        cqUtil.sendMsg(cqMsg);
    }

    private void getRecent(Userinfo userFromAPI, CqMsg cqMsg) {
        logger.info("检测到对" + userFromAPI.getUserName() + "的最近游戏记录查询");
        Score score = apiUtil.getRecent(null, userFromAPI.getUserName());
        if (score == null) {
            cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "最近没有游戏记录。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        Beatmap beatmap = apiUtil.getBeatmap(score.getBeatmapId());
        if (beatmap == null) {
            cqMsg.setMessage("网络错误：没有获取到Bid为" + score.getBeatmapId() + "的谱面信息。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        imgUtil.drawResult(userFromAPI, score, beatmap);
        cqMsg.setMessage("[CQ:image,file=" + score.getBeatmapId() + "_" + new SimpleDateFormat("yy-MM-dd").format(score.getDate()) + ".png]");
        cqUtil.sendMsg(cqMsg);
    }

    private void checkAfkPlayer(String role, int day, CqMsg cqMsg) {
        String resp;
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DATE, -day);
        java.sql.Date date = new Date(cl.getTimeInMillis());

        List<Integer> list = baseMapper.listUserIdByRole(role);
        List<String> afkList = new ArrayList<>();
        logger.info("开始查询" + role + "用户组中" + day + "天前的AFK玩家");
        for (Integer aList : list) {
            if (webPageUtil.getLastActive(aList).before(date)) {
                afkList.add(apiUtil.getUser(null, String.valueOf(aList)).getUserName());
            }
        }
        resp = "查询" + role + "用户组中，最后登录时间早于" + day + "天前的AFK玩家完成。";
        if (afkList.size() > 0) {
            resp = resp.concat("\n以下玩家：" + afkList.toString() + "最后登录时间在" + day + "天前。");
        } else {
            resp = resp.concat("\n没有检测" + role + "用户组中最后登录时间在" + day + "天前。的玩家。");
        }
        cqMsg.setMessage(resp);
        cqUtil.sendMsg(cqMsg);
    }

    private void getFristRank(Beatmap beatmap, List<Score> score, CqMsg cqMsg) {

        Userinfo userFromAPI = apiUtil.getUser(null, String.valueOf(score.get(0).getUserId()));
        //为了日志+和BP的PP计算兼容
        score.get(0).setBeatmapName(beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]");
        score.get(0).setBeatmapId(Integer.valueOf(beatmap.getBeatmapId()));
        imgUtil.drawFirstRank(beatmap, score.get(0), userFromAPI, score.get(0).getScore() - score.get(1).getScore());
        cqMsg.setMessage("[CQ:image,file=" + score.get(0).getBeatmapId() + "_" + new SimpleDateFormat("yy-MM-dd").format(score.get(0).getDate()) + "fp.png]");
        logger.info("开始调用函数发送" + score.get(0).getBeatmapId() + "_" + new SimpleDateFormat("yy-MM-dd").format(score.get(0).getDate()) + "fp.png");
        cqUtil.sendMsg(cqMsg);
    }


    private void printSimpleBP(Userinfo userinfo, int num, CqMsg cqMsg) {

        logger.info("开始获取玩家" + userinfo.getUserName() + "的BP");
        List<Score> list = apiUtil.getBP(userinfo.getUserName(), null);


        if (num > list.size()) {
            cqMsg.setMessage("该玩家没有打出指定的bp……");
            logger.info("请求的bp数比玩家bp总数量大");
            cqUtil.sendMsg(cqMsg);
        } else {
            //list基于0，得-1
            Score score = list.get(num - 1);
            logger.info("获得了玩家" + userinfo.getUserName() + "的第" + num + "个BP：" + score.getBeatmapId() + "，正在获取歌曲名称");
            Beatmap beatmap = apiUtil.getBeatmap(score.getBeatmapId());


            cqMsg.setMessage("https://osu.ppy.sh/b/" + score.getBeatmapId() + "\n"
                    + beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]"
                    + " , " + scoreUtil.convertMOD(score.getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "")
                    + " (" + new DecimalFormat("###.00").format(
                    100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50())
                            / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()))) + "%)\n"
                    + "Played by " + userinfo.getUserName() + ", " + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(score.getDate()) + ", "
                    + String.valueOf(Math.round(scoreUtil.calcPP(score, beatmap).getPp())) + "PP");
            cqUtil.sendMsg(cqMsg);
        }


    }

    private void getSimpleRecent(Userinfo userFromAPI, CqMsg cqMsg) {
        logger.info("检测到对" + userFromAPI.getUserName() + "的最近游戏记录查询");
        Score score = apiUtil.getRecent(null, userFromAPI.getUserName());
        if (score == null) {
            cqMsg.setMessage("玩家" + userFromAPI.getUserName() + "最近没有游戏记录。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        Beatmap beatmap = apiUtil.getBeatmap(score.getBeatmapId());
        if (beatmap == null) {
            cqMsg.setMessage("网络错误：没有获取到Bid为" + score.getBeatmapId() + "的谱面信息。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        cqMsg.setMessage("https://osu.ppy.sh/b/" + score.getBeatmapId() + "\n"
                + beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]"
                + " , " + scoreUtil.convertMOD(score.getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "")
                + " (" + new DecimalFormat("###.00").format(
                100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50())
                        / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()))) + "%)\n"
                + "Played by " + userFromAPI.getUserName() + ", " + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(score.getDate()) + ", "
                + String.valueOf(Math.round(scoreUtil.calcPP(score, beatmap).getPp())) + "PP");
        cqUtil.sendMsg(cqMsg);
    }

}
