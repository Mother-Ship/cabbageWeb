package top.mothership.cabbage.serviceImpl;

import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.CoolQ.CqResponse;
import top.mothership.cabbage.pojo.CoolQ.QQInfo;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.util.qq.MsgQueue;
import top.mothership.cabbage.util.qq.SmokeUtil;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CqAdminServiceImpl {
    /**
     * Prase admin cmd.
     *
     * @param cqMsg the cq msg
     * @throws Exception the exception
     */
    public void praseAdminCmd(CqMsg cqMsg) throws Exception {
        java.util.Date s = Calendar.getInstance().getTime();
        String msg = cqMsg.getMessage();
        Matcher m = Pattern.compile(Overall.ADMIN_CMD_REGEX).matcher(msg);
        m.find();
        if (!Overall.ADMIN_LIST.contains(String.valueOf(cqMsg.getUserId()))
                && !(cqMsg.getUserId() == 1427922341 && "bg".equals(m.group(1)))) {
            //如果QQ不包括在ADMIN_LIST，并且不是欧根要求改BG
            cqMsg.setMessage("[CQ:face,id=14]？");
            cqUtil.sendMsg(cqMsg);
            return;

        }

        int day;
        String username;
        String target;
        String URL;
        String[] usernames;
        String role;
        Long QQ;
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
            case "checku":
                Integer userId = Integer.valueOf(m.group(2).substring(1));
                user = userDAO.getUser(null, userId);
                if (user == null) {
                    cqMsg.setMessage("玩家" + userId + "没有使用过白菜。请先用add命令添加。");
                } else {
                    cqMsg.setMessage("玩家" + userId + "的用户组是" + user.getRole() + "，被Ban状态：" + (user.getBanned().equals(1)));
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
                } else {
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
                if (!"private".equals(cqMsg.getMessage())) {
                    cqMsg.setMessage("已经私聊返回结果，请查看，如果没有收到请添加好友。");
                    cqUtil.sendMsg(cqMsg);
                    cqMsg.setMessageType("private");
                }
                if (Overall.inviteRequests.size() > 0) {
                    resp = "以下是白菜本次启动期间收到的加群邀请：";
                    for (CqMsg aList : Overall.inviteRequests.keySet()) {
                        resp = resp.concat("\n" + "Flag：" + aList.getFlag() + "，群号：" + aList.getGroupId()
                                + "，邀请人：" + aList.getUserId() + "，时间：" + new SimpleDateFormat("HH:mm:ss").
                                format(new Date(aList.getTime() * 1000L)) + "已通过：" + Overall.inviteRequests.get(aList));
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
                    CqMsg cqMsg1 = new CqMsg();
                    cqMsg1.setMessage("Flag为：" + flag + "的邀请被" + cqMsg.getUserId() + "通过");
                    cqMsg1.setMessageType("private");
                    cqMsg1.setUserId(1335734657L);
                    cqUtil.sendMsg(cqMsg1);
                    cqMsg.setMessage("已通过Flag为：" + flag + "的邀请");
                    cqUtil.sendMsg(cqMsg);
                } else {
                    cqMsg.setMessage("通过Flag为：" + flag + "的邀请失败，返回信息：" + cqResponse);
                    cqUtil.sendMsg(cqMsg);
                    cqMsg.setMessage("通过Flag为：" + flag + "的邀请失败，消息体：" + cqMsg + "，返回信息：" + cqResponse);
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
                QQ = Long.valueOf(m.group(2).substring(1));
                user = userDAO.getUser(QQ, null);
                if (user == null) {
                    cqMsg.setMessage("该QQ没有绑定用户……");
                    cqUtil.sendMsg(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    return;
                }
                user.setQQ(0L);
                userDAO.updateUser(user);
                cqMsg.setMessage("QQ" + QQ + "的绑定信息已经清除");
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "fp":
                if (!msgUtil.CheckBidParam(m.group(2).substring(1), cqMsg)) {
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
                QQ = Long.valueOf(msg.substring(24, index));
                if ("all".equals(QQ)) {
                    resp = "啥玩意啊 咋回事啊";
                } else {
                    ArrayList<CqMsg> msgs = SmokeUtil.msgQueues.get(cqMsg.getGroupId()).getMsgsByQQ(Long.valueOf(QQ));
                    String card = cqUtil.getGroupMember(cqMsg.getGroupId(), Long.valueOf(QQ)).getData().getCard();
                    if (msgs.size() == 0) {
                        resp = "没有" + QQ + "的最近消息。";
                    } else if (msgs.size() <= 10) {
                        for (int i = 0; i < msgs.size(); i++) {
                            resp += card + "<" + QQ + "> " + new SimpleDateFormat("HH:mm:ss").
                                    format(new Date(msgs.get(i).getTime() * 1000L)) + "\n  " + msgs.get(i).getMessage() + "\n";
                        }
                    } else {
                        for (int i = msgs.size() - 10; i < msgs.size(); i++) {
                            resp += card + "<" + QQ + "> " + new SimpleDateFormat("HH:mm:ss").
                                    format(new Date(msgs.get(i).getTime() * 1000L)) + "\n  " + msgs.get(i).getMessage() + "\n";
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
            case "findPlayer":
                username = m.group(2).substring(1);
                List<User> list = userDAO.listUserIdByUname(username);
                if (list.size() > 0) {
                    resp = "找到以下玩家曾用/现用名是" + username + "：\n";
                    for (User u : list) {
                        resp += "现用名：" + u.getCurrentUname() + "，曾用名：" + u.getLegacyUname() + "，uid：" + u.getUserId();
                        if (u.getQQ() != null) {
                            resp += "，QQ：" + u.getQQ();
                        }
                        resp += "\n";
                    }
                } else {
                    resp = "没有找到现用/曾用" + username + "作为用户名的玩家。";
                }
                cqMsg.setMessage(resp);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "scanCard":
                CqResponse<List<QQInfo>> cqResponse1 = cqUtil.getGroupMembers(cqMsg.getGroupId());
                resp = "找到以下玩家群名片不含完整id：\n";
                for (QQInfo qqInfo : cqResponse1.getData()) {
                    //根据QQ获取user
                    user = userDAO.getUser(qqInfo.getUserId(), null);
                    if (user != null) {
                        userFromAPI = apiUtil.getUser(null, user.getUserId());
                        String card = cqUtil.getGroupMember(qqInfo.getGroupId(), qqInfo.getUserId()).getData().getCard();
                        if (!card.toLowerCase(Locale.CHINA).replace("_", " ")
                                .contains(userFromAPI.getUserName().toLowerCase(Locale.CHINA).replace("_", " "))) {
                            resp += "osu! id：" + userFromAPI.getUserName() + "，QQ：" + qqInfo.getUserId() + "，群名片：" + card + "\n";
                        }
                    }
                }
                cqMsg.setMessage(resp);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "help":
                resp = "!sudo add xxx,xxx:yyy 将xxx,xxx添加到yyy用户组。\n" +
                        "!sudo del xxx:yyy 将xxx的用户组中yyy删除，如果不带:yyy则重置为默认（creep）。\n" +
                        "!sudo check xxx 查询xxx的用户组。\n" +
                        "!sudo 褪裙 xxx 查询xxx用户组中多少人超过PP上线。\n" +
                        "!sudo bg xxx:http://123 将给定连接中的图以xxx.png的文件名写入数据库。\n" +
                        "!sudo recent xxx 查询他人的recent。\n" +
                        "!sudo afk n:xxx 查询xxx用户组中，n天以上没有登录的玩家(以官网为准)。\n" +
                        "!sudo smoke @xxx n 在白菜是管理的群，把被艾特的人禁言n秒。\n" +
                        "（艾特全体成员则遍历群成员并禁言）\n" +
                        "!sudo smokeAll/unsmokeAll 开关全员禁言。\n" +
                        "!sudo listInvite 列举当前的加群邀请（无论在哪里使用都会私聊返回结果）。\n" +
                        "!sudo handleInvite n 通过Flag为n的邀请。\n" +
                        "!sudo clearInvite 清空邀请列表。\n" +
                        "!sudo unbind qq 解绑某个QQ对应的id（找到该QQ对应的uid，并将QQ改为0）。\n" +
                        "!sudo fp bid 打印给定bid的#1。\n" +
                        "!sudo listMsg @xxx 打印被艾特的人最近的10条消息。在对方撤回消息时起作用。\n" +
                        "!sudo PP xxx 查询xxx组中所有成员PP（一般用于比赛计算Cost）。\n" +
                        "!sudo findPlayer xxx 查询曾用/现用xxx用户名的玩家。\n" +
                        "!sudo scanCard 扫描所在群的所有绑定了QQ的群成员，检测群名片是否包含完整id（无视大小写，并且会自动识别横线/空格）。\n" +
                        "!sudo checkBind @xxx 打印被艾特的人的id绑定情况。\n" +
                        "!sudo checkGroupBind xxx 打印该群所有成员是否绑定id，以及绑定id是否在mp4/5组内。特别的，不带参数会将群号设置为当前消息的群号。（只支持mp4/5群）\n"
                ;


                cqMsg.setMessage(resp);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "checkBind":
                QQ = Long.valueOf(msg.substring(19));
                user = userDAO.getUser(QQ, null);
                if (user != null) {
                    resp = "QQ" + QQ + "绑定的osu! uid为：" + user.getUserId() + "，用户组：" + user.getRole() + "，是否被Ban：" + (user.getBanned() == 1);
                } else {
                    resp = "QQ" + QQ + "没有绑定osuid。";
                }
                cqMsg.setMessage(resp);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;

            case "checkGroupBind":
                if ("".equals(m.group(2))) {
                    cqResponse1 = cqUtil.getGroupMembers(cqMsg.getGroupId());
                } else {
                    cqResponse1 = cqUtil.getGroupMembers(Long.valueOf(m.group(2).substring(1)));
                }
                for (QQInfo qqInfo : cqResponse1.getData()) {
                    //根据QQ获取user
                    user = userDAO.getUser(qqInfo.getUserId(), null);
                    String card = cqUtil.getGroupMember(qqInfo.getGroupId(), qqInfo.getUserId()).getData().getCard();
                    if (user != null) {
                        List<String> roles = Arrays.asList(user.getRole().split(","));
                        switch (String.valueOf(cqResponse1.getData().get(0).getGroupId())) {
                            case "201872650":
                                if (!roles.contains("mp5")) {
                                    resp += "QQ： " + qqInfo.getUserId() + " 绑定的id不在mp5用户组，osu! uid：" + user.getUserId() + "，用户组：" + user.getRole() + "。\n";
                                }
                                break;
                            case "564679329":
                                if (!roles.contains("mp4")) {
                                    resp += "QQ： " + qqInfo.getUserId() + " 绑定的id不在mp4用户组，osu! uid：" + user.getUserId() + "，用户组：" + user.getRole() + "。\n";
                                }
                                break;
                        }
                    } else {
                        resp += "QQ： " + qqInfo.getUserId() + " 没有绑定id，群名片是：" + card + "\n";
                    }
                }
                cqMsg.setMessage(resp);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");

                break;
        }

    }
}
