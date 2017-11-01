package top.mothership.cabbage.serviceImpl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.aspect.CqServiceAspect;
import top.mothership.cabbage.mapper.BaseMapper;
import top.mothership.cabbage.pojo.*;
import top.mothership.cabbage.service.CqService;
import top.mothership.cabbage.util.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.Exception;
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
    private static List<String> admin = Arrays.asList(Constant.CABBAGE_CONFIG.getString("admin").split(","));
    private ApiUtil apiUtil;
    private CqUtil cqUtil;
    private ImgUtil imgUtil;
    private MsgUtil msgUtil;
    private WebPageUtil webPageUtil;
    private ScoreUtil scoreUtil;
    private BaseMapper baseMapper;
    private Logger logger = LogManager.getLogger(this.getClass());

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
    public void praseCmd(CqMsg cqMsg) throws Exception{
        java.util.Date s = Calendar.getInstance().getTime();
        String msg = cqMsg.getMessage();
        Matcher m = Pattern.compile(Constant.CMD_REGEX).matcher(msg);
        m.find();
        String username;
        String pwd;
        String target;
        Userinfo userFromAPI;
        User user;
        Beatmap beatmap;
        List<Cookie> list;
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
                cqMsg.setMessage("[CQ:record,file=zou_hao_bu_song.wav]");
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

    @Override
    public void praseAdminCmd(CqMsg cqMsg) throws Exception {
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
                addUserRole(usernames, role, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "del":
                index = m.group(2).indexOf(":");


                if (index == -1) {
                    //如果拿不到
                    usernames = m.group(2).substring(1).split(",");
                    logger.info("分隔字符串完成，用户：" + Arrays.toString(usernames) + "，用户组：All");
                    removeUserRole(usernames, "all", cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                    break;
                } else {
                    usernames = m.group(2).substring(1, index).split(",");
                    role = m.group(2).substring(index + 1);
                    logger.info("分隔字符串完成，用户：" + Arrays.toString(usernames) + "，用户组：" + role);
                    removeUserRole(usernames, role, cqMsg);
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
                    cqMsg.setMessage("[CQ:record,file=all_dead.mp3]");
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
                flag = m.group(2).substring(1);
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
                            //通过新群邀请时，向消息队列Map中添加一个消息队列对象
                            SmokeUtil.msgQueues.put(aList.getGroupId(), new MsgQueue());
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
                listPP(role, cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "debug":
                logger.info("正在获取白菜本次启动发生的异常");
                if (CqServiceAspect.Exceptions.size() == 0) {
                    resp = "白菜本次启动没有发生异常。";
                } else {
                    resp = "以下是本次白菜启动期间发生的异常：";
                    for (Map.Entry<Integer, top.mothership.cabbage.pojo.Exception> entry : CqServiceAspect.Exceptions.entrySet()) {
                        resp = resp.concat("\n" + "Flag：" + entry.getKey() + "，日期：" + new SimpleDateFormat("yy-MM-dd HH:mm:ss").
                                format(entry.getValue().getTime()) + "\n" + entry.getValue().getE());
                    }
                }
                cqMsg.setMessage(resp);
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
                break;
            case "getStackTrace":
                flag = m.group(2).substring(1);
                logger.info("正在获取Flag为：" + flag + "的堆栈信息");
                top.mothership.cabbage.pojo.Exception ex = CqServiceAspect.Exceptions.get(Integer.valueOf(flag));
                resp = ex.getE().toString() + "，方法：" + ex.getPjp().getTarget().getClass() + "." + ex.getPjp().getSignature().getName() + "()\n方法入参：";
                Object[] args = ex.getPjp().getArgs();
                for (Object arg : args) {
                    resp = resp.concat("\n" + arg);
                }
                StackTraceElement[] a = ex.getE().getStackTrace();
                for (int i=0;i<a.length;i++) {
                    if(a[i].toString().contains("top.mothership")
                            &&!a[i].toString().contains("<generated>")
                            &&!a[i].toString().contains("Aspect"))
                        resp = resp.concat("\n    at " + a[i]);
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
            case "112177148":
                resp = "[CQ:at,qq=" + cqMsg.getUserId() + "],欢迎来到第一届MP4杯赛群。\n报名比赛需要加入本群并填写此表（https://yiqixie.com/e/kabu/v/home/fcABM_9zSnKLHAM2A8bfTCoDM），详见置顶公告。祝大家在比赛中有好的发挥！";
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

    private void statUserInfo(Userinfo userFromAPI, int day, CqMsg cqMsg) throws Exception {
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
        List<String> roles;
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
        roles = Arrays.asList(role.split(","));
        //此处自定义实现排序方法
        //dev>分群>主群>比赛
        roles.sort((o1, o2) -> {
            //mp5s得在mp5及其他分部前面
            if (o1.contains("mp5s") && (o2.equals("mp5") || o2.equals("mp5mc") || o2.equals("mp5chart"))) {
                return -1;
            }
            //mp4s得在mp4前面
            if (o1.contains("mp4s") && o2.equals("mp4")) {
                return -1;
            }
            //dev得在最后面
            if (o1.equals("dev")) {
                return 1;
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
        String filename = imgUtil.drawUserInfo(userFromAPI, userInDB, roles.get(0), day, near, scoreRank);
        //构造消息并发送
        logger.info("开始调用函数发送" + filename );
        cqMsg.setMessage("[CQ:image,file=" + filename + "]");
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

    private void printBP(Userinfo userinfo, int num, CqMsg cqMsg) throws Exception{
        if (num > 0 && ("112177148".equals(String.valueOf(cqMsg.getGroupId()))
                || "677545541".equals(String.valueOf(cqMsg.getGroupId()))
                || "234219559".equals(String.valueOf(cqMsg.getGroupId()))
                || "201872650".equals(String.valueOf(cqMsg.getGroupId()))
                || "564679329".equals(String.valueOf(cqMsg.getGroupId())))) {
            logger.info(cqMsg.getUserId() + "触发了赛群/4 5群禁用!bpme #n命令。");
            return;

        }
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
                cqMsg.setMessage("[CQ:record,file=wan_bu_liao_la.wav]");
                cqUtil.sendMsg(cqMsg);
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
            logger.info("开始调用函数发送" + userinfo.getUserId() + "BP.png");
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
                logger.info("开始调用函数发送" + score.getBeatmapId() + "_" + new SimpleDateFormat("yy-MM-dd").format(score.getDate()) + ".png");
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

    private void addUserRole(String[] usernames, String role, CqMsg cqMsg) throws Exception{
        List<String> nullList = new ArrayList<>();
        List<String> doneList = new ArrayList<>();
        List<String> addList = new ArrayList<>();
        Userinfo userFromAPI = null;
        String filename = null;
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
                    //2017-10-19 15:28:37新增加用户时候直接将用户组改为指定用户组
                    user.setRole(role);
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
                        filename = imgUtil.drawUserInfo(userFromAPI, null, role, 0, false, scoreRank);
                    }
                    addList.add(userFromAPI.getUserName());
                } else {
                    //进行Role更新

                    User user = baseMapper.getUser(null, userFromAPI.getUserId());
                    //拿到原先的user，把role拼上去，塞回去
                    String newRole;
                    //如果当前的用户组是creep，就直接改成现有的组
                    if ("creep".equals(user.getRole())) {
                        newRole = role;
                    } else {
                        newRole = user.getRole() + "," + role;
                    }
                    user.setRole(newRole);
                    baseMapper.updateUser(user);
                    doneList.add(userFromAPI.getUserName());
                }


            } else {
                nullList.add(username);
            }

        }
        String resp;
        resp = "用户组添加完成。";

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
            logger.info("开始调用函数发送" + filename );
            resp = resp.concat("\n[CQ:image,file=" + filename + "]");
        }
        cqMsg.setMessage(resp);
        cqUtil.sendMsg(cqMsg);
    }

    private void removeUserRole(String[] usernames, String role, CqMsg cqMsg) {
        List<String> nullList = new ArrayList<>();
        List<String> doneList = new ArrayList<>();
        List<String> notUsedList = new ArrayList<>();
        Userinfo userFromAPI;
        String lastUserOldRole = "";
        for (String username : usernames) {
            logger.info("开始从API获取" + username + "的信息");
            userFromAPI = apiUtil.getUser(username, null);
            if (userFromAPI != null) {
                //查找userRole数据库

                //进行Role更新
                User user = baseMapper.getUser(null, userFromAPI.getUserId());
                if (user == null) {
                    notUsedList.add(userFromAPI.getUserName());
                    //直接忽略掉下面的，进行下一次循环
                    continue;
                }
                //拿到原先的user，把role去掉
                String newRole;
                //这里如果不把Arrays.asList传入构造函数，而是直接使用会有个Unsupported异常
                //因为Arrays.asList做出的List是不可变的
                List<String> roles = new ArrayList<>(Arrays.asList(user.getRole().split(",")));
                roles.remove(role);
                if ("all".equals(role) || roles.size() == 0) {
                    newRole = "creep";
                } else {

                    //转换为字符串，此处得去除空格（懒得遍历+拼接了）
                    newRole = roles.toString().replace(" ", "").
                            substring(1, roles.toString().replace(" ", "").indexOf("]"));
                }

                lastUserOldRole = user.getRole();
                user.setRole(newRole);
                baseMapper.updateUser(user);
                doneList.add(userFromAPI.getUserName());
            } else {
                nullList.add(username);
            }

        }

        String resp;
        resp = "用户组移除完成。";

        if (doneList.size() > 0) {
            resp = resp.concat("\n修改成功：" + doneList.toString());
        }
        if (nullList.size() > 0) {
            resp = resp.concat("\n不存在的：" + nullList.toString());
        }
        if (notUsedList.size() > 0) {
            resp = resp.concat("\n没用过的：" + notUsedList.toString());
        }
        if (usernames.length == 0) {
            resp = "没有做出改动。";
        }
        if (doneList.size() == 1 && "all".equals(role)) {
            resp = resp.concat("\n该用户之前的用户组是：" + lastUserOldRole);
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
            //这里没有做多用户组。2017-10-25 17:39:10修正
//            if (Arrays.asList(aList.getRole().split(",")).contains(role)) {
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
//            }
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

    private void downloadBG(String URL, String target, CqMsg cqMsg) throws IOException {
        BufferedImage bg;
        logger.info("开始根据URL下载新背景。");
        bg = ImageIO.read(new URL(URL));

        //并不需要删除旧图片

        logger.info("开始将新背景写入硬盘");
        ImageIO.write(bg, "png", new File(Constant.CABBAGE_CONFIG.getString("path") + "\\data\\image\\resource\\img\\stat\\" + target + ".png"));

        cqMsg.setMessage("修改用户/用户组" + target + "的背景图成功。");
        cqUtil.sendMsg(cqMsg);
    }

    private void getRecent(Userinfo userFromAPI, CqMsg cqMsg) throws Exception{
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
        logger.info("开始调用函数发送" + score.getBeatmapId() + "_" + new SimpleDateFormat("yy-MM-dd").format(score.getDate()) + ".png");
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
//            if (Arrays.asList(aList.getRole().split(",")).contains(role)
//                    && webPageUtil.getLastActive(aList.getUserId()).before(date)) {
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

    private void getFristRank(Beatmap beatmap, List<Score> score, CqMsg cqMsg) throws Exception{

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
        if (num > 0 && ("112177148".equals(String.valueOf(cqMsg.getGroupId()))
                || "677545541".equals(String.valueOf(cqMsg.getGroupId())))
                || "201872650".equals(String.valueOf(cqMsg.getGroupId()))
                || "564679329".equals(String.valueOf(cqMsg.getGroupId()))) {
            logger.info(cqMsg.getUserId() + "触发了赛群/4 5群禁用!bpme #n命令。");
            return;

        }
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

    private void login(User user, String pwd, CqMsg cqMsg) throws IOException {
        Userinfo userFromAPI = apiUtil.getUser(null, String.valueOf(user.getUserId()));
        if (userFromAPI == null) {
            cqMsg.setMessage("API没有获取到QQ" + cqMsg.getUserId() + "绑定的" + user.getUserId() + "玩家的信息。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        logger.info("检测到对" + userFromAPI.getUserName() + "的模拟登陆请求");
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("https://osu.ppy.sh/forum/ucp.php?mode=login");
        //添加请求头
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("autologin", "on"));
        urlParameters.add(new BasicNameValuePair("login", "login"));
        urlParameters.add(new BasicNameValuePair("username", userFromAPI.getUserName()));
        urlParameters.add(new BasicNameValuePair("password", pwd));
        HttpResponse response;


        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        response = client.execute(post);


        List<Cookie> cookies = client.getCookieStore().getCookies();
        String CookieNames = "";
        for (Cookie c : cookies) {
            CookieNames = CookieNames.concat(c.getName());
        }
        if (CookieNames.contains("phpbb3_2cjk5_sid")) {
            String cookie = new Gson().toJson(cookies);
            user.setCookie(cookie);
            baseMapper.updateUser(user);
            cqMsg.setMessage("登陆成功，Cookie到期时间为：" +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cookies.get(2).getExpiryDate()));
            cqUtil.sendMsg(cqMsg);
        } else if (CookieNames.contains("PHPSESSID")) {
            cqMsg.setMessage("登录失败，可能触发了长时间未登录强行要求重设密码。");
            cqUtil.sendMsg(cqMsg);

        } else {
            cqMsg.setMessage("登陆失败，请检查输入。");
            cqUtil.sendMsg(cqMsg);
        }

    }

    private void mutual(User user, String target, CqMsg cqMsg)throws Exception {
        String msg = cqMsg.getMessage();
        int uid = 0;
        if (target.contains("[CQ:at,qq=")) {
            int index = msg.indexOf("]");
            User targetUser = baseMapper.getUser(msg.substring(14, index), null);
            if (targetUser == null) {
                cqMsg.setMessage("对方没有绑定osu!id。请提醒他使用!setid <你的osu!id> 命令。");
                cqUtil.sendMsg(cqMsg);
                return;
            }
            uid = targetUser.getUserId();

        } else {
            Userinfo userFromAPI = apiUtil.getUser(target, null);
            if (userFromAPI == null) {
                cqMsg.setMessage("没有从osu!API获取到玩家" + target + "的信息。");
                cqUtil.sendMsg(cqMsg);
                return;
            }
            uid = userFromAPI.getUserId();
            target = userFromAPI.getUserName();
        }
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("https://osu.ppy.sh/u/" + uid);
        HttpParams params = client.getParams();
        //禁用GET请求自动重定向
        params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        List<Cookie> list = new Gson().fromJson(user.getCookie(), new TypeToken<List<BasicClientCookie>>() {
        }.getType());
        CookieStore cookieStore = new BasicCookieStore();
        for (Cookie c : list) {
            cookieStore.addCookie(c);
        }
        client.setCookieStore(cookieStore);
        HttpResponse response = null;
        HttpEntity entity;

        response = client.execute(httpGet);
        entity = response.getEntity();
        String html = EntityUtils.toString(entity, "GBK");
        httpGet.releaseConnection();
        Matcher m = Pattern.compile("<div class='centrep'>\\n<a href='([^']*)").matcher(html);
        m.find();
        String addLink = m.group(1);
        if (addLink.contains("remove")) {
            cqMsg.setMessage("你和" + target + "已经是好友了，请不要重复添加。");
            cqUtil.sendMsg(cqMsg);
            return;
        }
        httpGet = new HttpGet("https://osu.ppy.sh" + m.group(1));
        response = client.execute(httpGet);
        httpGet.releaseConnection();
        if (response.getStatusLine().getStatusCode() != 200) {
            //这里是跳转了，获取当前Cookie存入数据库，然后使用verify命令
            //Cookie似乎没有变化，暂时先不update Cookie
//                httpGet = new HttpGet("https://osu.ppy.sh/p/verify?r="+addLink);
//                List<Cookie> cookies = client.getCookieStore().getCookies();
//                String CookieNames = "";
//                for (Cookie c : cookies) {
//                    CookieNames = CookieNames.concat(c.getName());
//                }
//                String cookie = new Gson().toJson(cookies);
//                user.setCookie(cookie);
//                baseMapper.updateUser(user);
            cqMsg.setMessage("触发验证，请登录osu!并尝试使用!verify命令。");
            cqUtil.sendMsg(cqMsg);
        } else {
            cqMsg.setMessage("添加成功。");
            cqUtil.sendMsg(cqMsg);
        }


    }

    private void verify(User user, CqMsg cqMsg) throws Exception{
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("https://osu.ppy.sh/p/verify");
        List<Cookie> list = new Gson().fromJson(user.getCookie(), new TypeToken<List<BasicClientCookie>>() {
        }.getType());
        CookieStore cookieStore = new BasicCookieStore();
        for (Cookie c : list) {
            cookieStore.addCookie(c);
        }
        client.setCookieStore(cookieStore);
        HttpResponse response;
        HttpEntity entity;

        response = client.execute(httpGet);
        entity = response.getEntity();
        String html = EntityUtils.toString(entity, "GBK");
        httpGet.releaseConnection();
        //验证未通过时也有这个……
        if (html.contains("<h1>Success!</h1>")) {
            cqMsg.setMessage("验证通过。");
        } else {
            cqMsg.setMessage("验证未通过，请检查游戏是否登录。");
        }
        cqUtil.sendMsg(cqMsg);

    }

    private void listPP(String role, CqMsg cqMsg) {
        logger.info("开始检查" + role + "用户组中所有人的PP");
        List<Integer> list = baseMapper.listUserIdByRole(role);
        String resp;
        if (list.size() > 0) {
            resp = role + "用户组中所有人的PP：";

            for (Integer aList : list) {
//            if (Arrays.asList(aList.getRole().split(",")).contains(role)) {
                Userinfo userinfo = apiUtil.getUser(null, String.valueOf(aList));
                logger.info(userinfo.getUserName() + "的PP是" + userinfo.getPpRaw());
                resp = resp.concat("\n" + userinfo.getUserName() + "\t" + userinfo.getPpRaw());
//            }
            }
        } else {
            resp = role + "用户组没有成员。";
        }

        cqMsg.setMessage(resp);
        cqUtil.sendMsg(cqMsg);
    }
}
