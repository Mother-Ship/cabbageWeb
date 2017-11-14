package top.mothership.cabbage.serviceImpl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.CoolQ.CqResponse;
import top.mothership.cabbage.pojo.CoolQ.QQInfo;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.util.MsgQueue;
import top.mothership.cabbage.util.Overall;
import top.mothership.cabbage.util.osu.ApiUtil;
import top.mothership.cabbage.util.qq.*;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CqServiceImpl {


    private final ApiUtil apiUtil;
    private final CqUtil cqUtil;
    private final CmdUtil cmdUtil;
    private final MsgUtil msgUtil;
    private UserDAO userDAO;
    private Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    public CqServiceImpl(ApiUtil apiUtil, MsgUtil msgUtil, CqUtil cqUtil, UserDAO userDAO, CmdUtil cmdUtil) {
        this.apiUtil = apiUtil;
        this.msgUtil = msgUtil;
        this.cqUtil = cqUtil;
        this.userDAO = userDAO;
        this.cmdUtil = cmdUtil;

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
        int day;
        int num;
        switch (m.group(1)) {
            case "stat":
                //先分割出一个username，再尝试使用带数字的正则去匹配
                day = 1;
                //屏蔽掉用户没有输入用户名时候的异常
                if("".equals(m.group(2)))
                    return;
                username = m.group(2).substring(1);

                m = Pattern.compile(Overall.CMD_REGEX_NUM).matcher(msg);
                if (m.find()) {
                    //传入检查日期参数的方法，必须要把消息体改为日期，否则那个方法没办法发送消息
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
                cmdUtil.statUserInfo(userFromAPI, day, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "bp":
            case "bps":
                num = 0;
                //屏蔽掉用户没有输入用户名时候的异常
                if("".equals(m.group(2)))
                    return;
                username = m.group(2).substring(1);
                if(username.equals(""))
                    return;
                Matcher m2 = Pattern.compile(Overall.CMD_REGEX_NUM).matcher(msg);
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
                    cmdUtil.printSimpleBP(userFromAPI, num, cqMsg);
                } else {
                    cmdUtil.printBP(userFromAPI, num, cqMsg);
                }
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "setid":
                //屏蔽掉用户没有输入用户名时候的异常
                if("".equals(m.group(2)))
                    return;
                username = m.group(2).substring(1);
                cmdUtil.bindQQAndOsu(username, Long.toString(cqMsg.getUserId()), cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "statme":
                day = 1;
                user = userDAO.getUser(String.valueOf(cqMsg.getUserId()), null);
                if (user == null) {
                    cqMsg.setMessage("你没有绑定默认id。请使用!setid <你的osu!id> 命令。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                m = Pattern.compile(Overall.CMD_REGEX_NUM).matcher(msg);
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
                cmdUtil.statUserInfo(userFromAPI, day, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "bpme":
            case "bpmes":
                num = 0;
                user = userDAO.getUser(String.valueOf(cqMsg.getUserId()), null);
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
                m2 = Pattern.compile(Overall.CMD_REGEX_NUM).matcher(msg);
                if (m2.find()) {
                    cqMsg.setMessage(m2.group(3));
                    if (!msgUtil.CheckBPNumParam(cqMsg)) {
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
                user = userDAO.getUser(String.valueOf(cqMsg.getUserId()), null);
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
                List<Score> scores = apiUtil.getFirstScore(bid, 2);
                if (scores.size() == 0) {
                    cqMsg.setMessage("提供的bid没有找到#1成绩。");
                    cqUtil.sendMsg(cqMsg);
                    return;
                }
                if (scores.get(0).getUserId() != userDAO.getUser(String.valueOf(cqMsg.getUserId()), null).getUserId()) {
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
                cqMsg.setMessage("[CQ:record,file=base64://"+Overall.ZOU_HAO_BU_SONG+"]");
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


    public void praseAdminCmd(CqMsg cqMsg) throws Exception {
        java.util.Date s = Calendar.getInstance().getTime();

        if (!Overall.ADMIN_LIST.contains(String.valueOf(cqMsg.getUserId()))) {
            //如果没有权限
            cqMsg.setMessage("[CQ:face,id=14]？");
            cqUtil.sendMsg(cqMsg);
            return;

        }
        String msg = cqMsg.getMessage();
        Matcher m = Pattern.compile(Overall.ADMIN_CMD_REGEX).matcher(msg);
        m.find();
        int day;
        String username;
        String target;
        String URL;
        String[] usernames;
        String role;
        String QQ;
        String resp = "";
        String flag;
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
                cmdUtil.addUserRole(usernames, role, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "del":
                index = m.group(2).indexOf(":");
                if (index == -1) {
                    //如果拿不到
                    usernames = m.group(2).substring(1).split(",");
                    logger.info("分隔字符串完成，用户：" + Arrays.toString(usernames) + "，用户组：All");
                    cmdUtil.removeUserRole(usernames, "all", cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    break;
                } else {
                    usernames = m.group(2).substring(1, index).split(",");
                    role = m.group(2).substring(index + 1);
                    logger.info("分隔字符串完成，用户：" + Arrays.toString(usernames) + "，用户组：" + role);
                    cmdUtil.removeUserRole(usernames, role, cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    break;
                }
            case "check":
                username = m.group(2).substring(1);
                Userinfo userFromAPI = apiUtil.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("API没有获取到玩家" + username + "的信息。");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                User user = userDAO.getUser(null, userFromAPI.getUserId());
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
                cmdUtil.checkPPOverflow(role, cqMsg);
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
                cmdUtil.downloadBG(URL, target, cqMsg);

                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "recent":
            case "rs":
                username = m.group(2).substring(1);
                userFromAPI = apiUtil.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有获取到玩家" + username + "的信息。");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                if (m.group(1).equals("rs")) {
                    cmdUtil.getSimpleRecent(userFromAPI, cqMsg);
                }else {
                    cmdUtil.getRecent(userFromAPI, cqMsg);
                }
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
                cmdUtil.checkAfkPlayer(role, day, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "smoke":
                cmdUtil.smoke(cqMsg);
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
                if (Overall.inviteRequests.size() > 0) {
                    resp = "以下是白菜本次启动期间收到的加群邀请：";
                    for (CqMsg aList : Overall.inviteRequests.keySet()) {
                        resp = resp.concat("\n" + "Flag：" + aList.getFlag() + "，群号：" + aList.getGroupId() + "，已通过：" + Overall.inviteRequests.get(aList));
                    }
                } else {
                    resp = "本次启动白菜没有收到加群邀请。";
                }
                cqMsg.setMessage(resp);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "handleInvite":
                flag = m.group(2).substring(1);
                logger.info("正在通过对Flag为：" + flag + "的邀请");
                //开启一个新消息用来通过邀请
                CqMsg newMsg = new CqMsg();
                newMsg.setFlag(flag);
                newMsg.setApprove(true);
                newMsg.setType("invite");
                newMsg.setMessageType("handleInvite");
                CqResponse cqResponse = cqUtil.sendMsg(newMsg);
                if (cqResponse.getRetCode() == 0) {
                    for (CqMsg aList : Overall.inviteRequests.keySet()) {
                        if (aList.getFlag().equals(flag)) {
                            Overall.inviteRequests.replace(aList, "是");
                            //通过新群邀请时，向消息队列Map中添加一个消息队列对象
                            SmokeUtil.msgQueues.put(aList.getGroupId(), new MsgQueue());
                        }
                    }
                    cqMsg.setMessage("已通过Flag为：" + flag + "的邀请");
                    cqUtil.sendMsg(cqMsg);
                    cqMsg.setMessage("有其他管理员通过Flag为：" + flag + "的邀请，消息体："+cqMsg);
                    cqMsg.setMessageType("private");
                    cqMsg.setUserId(1335734657L);
                    cqUtil.sendMsg(cqMsg);
                }else{
                    cqMsg.setMessage("通过Flag为：" + flag + "的邀请失败，返回信息："+cqResponse);
                    cqUtil.sendMsg(cqMsg);
                    cqMsg.setMessage("有其他管理员通过Flag为：" + flag + "的邀请失败，消息体："+cqMsg+"，返回信息："+cqResponse);
                    cqMsg.setMessageType("private");
                    cqMsg.setUserId(1335734657L);
                    cqUtil.sendMsg(cqMsg);
                }
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "clearInvite":
                logger.info("正在清理邀请请求");
                Overall.inviteRequests.clear();
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "unbind":
                QQ = m.group(2).substring(1);
                user = userDAO.getUser(QQ, null);
                if (user == null) {
                    cqMsg.setMessage("该QQ没有绑定用户……");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                user.setQQ("0");
                userDAO.updateUser(user);
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
                List<Score> score = apiUtil.getFirstScore(bid, 2);
                if (score.size() == 0) {
                    cqMsg.setMessage("提供的bid没有找到#1成绩。");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                cmdUtil.getFristRank(beatmap, score, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "listMsg":
                //照例使用艾特
                index = msg.indexOf("]");
                QQ = msg.substring(24, index);
                if ("all".equals(QQ)) {
                    resp = "啥玩意啊 咋回事啊";
                } else {
                    ArrayList<CqMsg> msgs = SmokeUtil.msgQueues.get(cqMsg.getGroupId()).getMsgsByQQ(Long.valueOf(QQ));
                    String card = cqUtil.getCard(Long.valueOf(QQ), cqMsg.getGroupId());
                    if (msgs.size() == 0) {
                        resp = "没有" + QQ + "的最近消息。";
                    } else if (msgs.size() <= 10) {
                        for (int i = 0; i < msgs.size(); i++) {
                            resp = resp.concat(card + "<" + QQ + "> " + new SimpleDateFormat("HH:mm:ss").
                                    format(new Date(msgs.get(i).getTime() * 1000L)) + "\n  " + msgs.get(i).getMessage() + "\n");
                        }
                    } else {
                        for (int i = msgs.size() - 10; i < msgs.size(); i++) {
                            resp = resp.concat(card + "<" + QQ + "> " + new SimpleDateFormat("HH:mm:ss").
                                    format(new Date(msgs.get(i).getTime() * 1000L)) + "\n  " + msgs.get(i).getMessage() + "\n");
                        }
                    }
                }
                cqMsg.setMessage(resp);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "pp":
            case "PP":
                role = m.group(2).substring(1);
                cmdUtil.listPP(role, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
        }

    }




}
