package top.mothership.cabbage.serviceImpl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.annotation.UserRoleControl;
import top.mothership.cabbage.consts.Base64Consts;
import top.mothership.cabbage.consts.OverallConsts;
import top.mothership.cabbage.consts.PatternConsts;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.CoolQ.CqResponse;
import top.mothership.cabbage.pojo.CoolQ.QQInfo;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.util.osu.ScoreUtil;
import top.mothership.cabbage.util.qq.ImgUtil;
import top.mothership.cabbage.util.qq.MsgQueue;
import top.mothership.cabbage.util.qq.SmokeUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;

/**
 * 管理命令进行业务处理的类
 *
 * @author QHS
 */
@Service
@UserRoleControl
public class CqAdminServiceImpl {
    public static Map<CqMsg, String> request = new HashMap<>();
    private final CqManager cqManager;
    private final ApiManager apiManager;
    private final UserDAO userDAO;
    private final UserInfoDAO userInfoDAO;
    private final WebPageManager webPageManager;
    private final ImgUtil imgUtil;
    private static ResDAO resDAO;
    private final ScoreUtil scoreUtil;
    private Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    public CqAdminServiceImpl(CqManager cqManager, ApiManager apiManager, UserDAO userDAO, UserInfoDAO userInfoDAO, WebPageManager webPageManager, ImgUtil imgUtil, ResDAO resDAO, ScoreUtil scoreUtil) {
        this.cqManager = cqManager;
        this.apiManager = apiManager;
        this.userDAO = userDAO;
        this.userInfoDAO = userInfoDAO;
        this.webPageManager = webPageManager;
        this.imgUtil = imgUtil;
        CqAdminServiceImpl.resDAO = resDAO;
        this.scoreUtil = scoreUtil;
        loadCache();
    }

    private static void loadCache() {
        //调用NIO遍历那些可以加载一次的文件
        //在方法体内初始化，重新初始化的时候就可以去除之前缓存的文件
        ImgUtil.images = new HashMap<>();
        //逻辑改为从数据库加载
        List<Map<String, Object>> list = resDAO.getResource();
        for (Map<String, Object> aList : list) {
            String name = (String) aList.get("name");
            byte[] data = (byte[]) aList.get("data");
            try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
                ImgUtil.images.put(name, ImageIO.read(in));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addUserRole(CqMsg cqMsg) {

        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        String[] usernames = m.group(2).split(",");
        String role = m.group(3);
        logger.info("分隔字符串完成，用户：" + Arrays.toString(usernames) + "，用户组：" + role);
        List<String> nullList = new ArrayList<>();
        List<String> doneList = new ArrayList<>();
        List<String> addList = new ArrayList<>();
        Userinfo userFromAPI = null;
        String filename = null;
        for (String username : usernames) {
            logger.info("开始从API获取" + username + "的信息");
            userFromAPI = apiManager.getUser(username, null);
            //如果user不是空的(官网存在记录)
            if (userFromAPI != null) {
                //查找userRole数据库

                if (userDAO.getUser(null, userFromAPI.getUserId()) == null) {
                    //如果userRole库中没有这个用户
                    //构造User对象写入数据库
                    logger.info("开始将用户" + userFromAPI.getUserName() + "添加到数据库。");
                    User user = new User(userFromAPI.getUserId(), "creep", 0L, "[]", userFromAPI.getUserName(), false, null, null, 0L, 0L);
                    userDAO.addUser(user);

                    if (LocalTime.now().isAfter(LocalTime.of(4, 0))) {
                        userFromAPI.setQueryDate(LocalDate.now());
                    } else {
                        userFromAPI.setQueryDate(LocalDate.now().minusDays(1));
                    }
                    userInfoDAO.addUserInfo(userFromAPI);

                    if (usernames.length == 1) {
                        logger.info("新增单个用户，绘制名片");
                        int scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
                        filename = imgUtil.drawUserInfo(userFromAPI, null, role, 0, false, scoreRank);
                    }
                    addList.add(userFromAPI.getUserName());
                } else {
                    //进行Role更新
                    User user = userDAO.getUser(null, userFromAPI.getUserId());
                    //拿到原先的user，把role拼上去，塞回去
                    String newRole;
                    //如果当前的用户组是creep，就直接改成现有的组
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
                    user.setRole(newRole);
                    userDAO.updateUser(user);
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
            logger.info("开始调用函数发送" + filename);
            resp = resp.concat("\n[CQ:image,file=base64://" + filename + "]");
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    public void delUserRole(CqMsg cqMsg) {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        String role;
        String[] usernames;
        if ("".equals(m.group(3))) {
            //如果没有指定用户组
            usernames = m.group(2).split(",");
            logger.info("分隔字符串完成，用户：" + Arrays.toString(usernames) + "，用户组：All");
            role = "all";
        } else {
            usernames = m.group(2).split(",");
            role = m.group(3);
            logger.info("分隔字符串完成，用户：" + Arrays.toString(usernames) + "，用户组：" + role);
        }
        List<String> nullList = new ArrayList<>();
        List<String> doneList = new ArrayList<>();
        List<String> notUsedList = new ArrayList<>();
        Userinfo userFromAPI;
        String lastUserOldRole = "";
        for (String username : usernames) {
            logger.info("开始从API获取" + username + "的信息");
            userFromAPI = apiManager.getUser(username, null);
            if (userFromAPI != null) {
                //查找userRole数据库
                //进行Role更新
                User user = userDAO.getUser(null, userFromAPI.getUserId());
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
                //2017-11-27 21:04:36 增强健壮性，只有在含有这个role的时候才进行移除
                if (roles.contains(role)) {
                    roles.remove(role);
                }
                if ("all".equals(role) || roles.size() == 0) {
                    newRole = "creep";
                } else {
                    //转换为字符串，此处得去除空格（懒得遍历+拼接了）
                    newRole = roles.toString().replace(" ", "").
                            substring(1, roles.toString().replace(" ", "").indexOf("]"));
                }
                lastUserOldRole = user.getRole();
                user.setRole(newRole);
                userDAO.updateUser(user);
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
        if (doneList.size() == 1) {
            resp = resp.concat("\n该用户之前的用户组是：" + lastUserOldRole);
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    public void getUserRole(CqMsg cqMsg) {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        User user = null;
        String username = null;
        switch (m.group(1)) {
            case "check":
                username = m.group(2);
                Userinfo userFromAPI = apiManager.getUser(username, null);
                if (userFromAPI == null) {
                    cqMsg.setMessage("没有获取到玩家" + username + "的信息。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                user = userDAO.getUser(null, userFromAPI.getUserId());
                break;
            case "checku":
                username = m.group(2);
                user = userDAO.getUser(null, Integer.valueOf(username));
                break;
            case "checkq":
                username = m.group(2);
                user = userDAO.getUser(Long.valueOf(username), null);
                break;
            default:
                break;
        }

        if (user == null) {
            cqMsg.setMessage("玩家" + username + "没有使用过白菜。请先用add命令添加。");
        } else {
            Userinfo userFromAPI = apiManager.getUser(null, user.getUserId());
            if (userFromAPI == null) {
                user.setBanned(true);
            } else if (!userFromAPI.getUserName().equals(user.getCurrentUname())) {
                //如果检测到用户改名，取出数据库中的现用名加入到曾用名，并且更新现用名和曾用名
                List<String> legacyUname = new GsonBuilder().create().fromJson(user.getLegacyUname(), new TypeToken<List<String>>() {
                }.getType());
                if (user.getCurrentUname() != null) {
                    legacyUname.add(user.getCurrentUname());
                }
                user.setLegacyUname(new Gson().toJson(legacyUname));
                user.setCurrentUname(userFromAPI.getUserName());
                logger.info("检测到玩家" + userFromAPI.getUserName() + "改名，已登记");
            }
            userDAO.updateUser(user);
            cqMsg.setMessage("搜索结果：\n"
                    + "该玩家的用户组是" + user.getRole()
                    + "\nosu!id：" + user.getCurrentUname()
                    + "\n被Ban状态：" + user.isBanned()
                    + "\nQQ：" + user.getQq()
                    + "\nosu!uid：" + user.getUserId()
                    + "\n在开启复读计数的群中："
                    + "总复读次数：" + user.getRepeatCount()
                    + "，总发言次数：" + user.getSpeakingCount());
        }
        cqManager.sendMsg(cqMsg);

    }

    public void listPPOverflow(CqMsg cqMsg) {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        String role = m.group(2);
        String resp;
        List<Integer> list = userDAO.listUserIdByRole(role);
        List<String> overflowList = new ArrayList<>();
        for (Integer aList : list) {
            //这里没有做多用户组。2017-10-25 17:39:10修正
            //拿到用户最接近今天的数据（因为是最接近所以也不用打补丁了）
            Userinfo userinfo = userInfoDAO.getNearestUserInfo(aList, LocalDate.now());
            //如果PP超过了警戒线，请求API拿到最新PP
            if (userinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString(role + "RiskPP"))) {
                logger.info("开始从API获取" + aList + "的信息");
                userinfo = apiManager.getUser(null, aList);
                if (userinfo.getPpRaw() > Integer.valueOf(OverallConsts.CABBAGE_CONFIG.getString(role + "PP")) + 0.49) {
                    logger.info("玩家" + aList + "超限，已记录");
                    overflowList.add(userinfo.getUserName());
                } else {
                    logger.info("玩家" + aList + "没有超限");
                }
            }
        }
        resp = "查询PP溢出玩家完成。";
        if (overflowList.size() > 0) {
            resp = resp.concat("\n查询到" + role + "用户组中，以下玩家：" + overflowList.toString().replace(", ", ",") + "PP超出了设定的限制。");
        } else {
            resp = resp.concat("\n没有检测" + role + "用户组中PP溢出的玩家。");
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    @UserRoleControl({1427922341})
    public void addComponent(CqMsg cqMsg) throws IOException {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        String url = m.group(3);
        String target = m.group(2);
        //实验性功能
        if (target.contains(".")) {
            File file = new File(url);
            try (FileInputStream fis = new FileInputStream(file);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] b = new byte[1024];
                int n;
                while ((n = fis.read(b)) != -1) {
                    bos.write(b, 0, n);
                }
                byte[] bytes = bos.toByteArray();
                resDAO.addResource(target, bytes);
            } catch (IOException ignore) {

            }
        } else {
            BufferedImage tmp = ImageIO.read(new URL(url));
            //这个方法从QQ直接发送图片+程序下载，改为采用URL写入到硬盘，到现在改为存入数据库+打破目录限制，只不过命令依然叫!sudo bg……
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(tmp, "png", out);
                tmp.flush();
                byte[] imgBytes = out.toByteArray();
                resDAO.addResource(target + ".png", imgBytes);
            } catch (IOException ignore) {

            }
            cqMsg.setMessage("修改组件" + target + ".png成功。");
            cqManager.sendMsg(cqMsg);
            //手动调用重载缓存
            loadCache();
        }
    }

    public void recent(CqMsg cqMsg) {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        String username = m.group(2);
        Userinfo userFromAPI = apiManager.getUser(username, null);
        if (userFromAPI == null) {
            cqMsg.setMessage("没有获取到玩家" + username + "的信息。");
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
            case "recent":
                String filename = imgUtil.drawResult(userFromAPI, score, beatmap);
                cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
                cqManager.sendMsg(cqMsg);
                break;
            default:
                break;
        }
    }

    public void checkAfkPlayer(CqMsg cqMsg) {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        String role;
        int day = Integer.valueOf(m.group(2));
        if ("".equals(m.group(3))) {
            role = "mp5";
        } else {
            role = m.group(3);
        }
        logger.info("检测到管理员对" + role + "用户组" + day + "天前的AFK玩家查询");
        String resp;
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DATE, -day);
        java.sql.Date date = new Date(cl.getTimeInMillis());

        List<Integer> list = userDAO.listUserIdByRole(role);
        List<String> afkList = new ArrayList<>();
        for (Integer aList : list) {
//            if (Arrays.asList(aList.getRole().split(",")).contains(role)
//                    && webPageUtil.getLastActive(aList.getUserId()).before(date)) {
            if (webPageManager.getLastActive(aList).before(date)) {
                afkList.add(apiManager.getUser(null, aList).getUserName());
            }
        }
        resp = "查询" + role + "用户组中，最后登录时间早于" + day + "天前的AFK玩家完成。";
        if (afkList.size() > 0) {
            resp = resp.concat("\n以下玩家：" + afkList.toString() + "最后登录时间在" + day + "天前。");
        } else {
            resp = resp.concat("\n没有检测" + role + "用户组中最后登录时间在" + day + "天前。的玩家。");
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    public void smoke(CqMsg cqMsg) {
        String msg = cqMsg.getMessage();
        int index = msg.indexOf("]");
        int sec;
        String QQ;

        if (!"".equals(msg.substring(index + 2))) {
            sec = Integer.valueOf(msg.substring(index + 2));
        } else {
            sec = 600;
        }
        QQ = msg.substring(22, index);
        if ("all".equals(QQ)) {
            List<QQInfo> memberList = cqManager.getGroupMembers(cqMsg.getGroupId()).getData();
            cqMsg.setMessageType("smoke");
            cqMsg.setDuration(sec);
            String operator = cqMsg.getUserId().toString();
            for (QQInfo aList : memberList) {
                cqMsg.setUserId(aList.getUserId());
                cqManager.sendMsg(cqMsg);
                logger.info(aList.getUserId() + "被" + operator + "禁言" + sec + "秒。");
            }
            String img = imgUtil.drawImage(ImgUtil.images.get("smokeAll.png"));
            cqMsg.setMessage("[CQ:image,file=base64://" + img + "]");
            cqMsg.setMessageType("group");
            cqManager.sendMsg(cqMsg);
        } else {
            logger.info(QQ + "被" + cqMsg.getUserId() + "禁言" + sec + "秒。");
            if (sec > 0) {
                cqMsg.setMessage("[CQ:record,file=base64://" + Base64Consts.ALL_DEAD + "]");
                cqManager.sendMsg(cqMsg);
            }
            cqMsg.setMessageType("smoke");
            cqMsg.setDuration(sec);
            cqMsg.setUserId(Long.valueOf(QQ));
            cqManager.sendMsg(cqMsg);
        }
    }

    public void listInvite(CqMsg cqMsg) {
        String resp;
        if (!"private".equals(cqMsg.getMessageType())) {
            cqMsg.setMessage("已经私聊返回结果，请查看，如果没有收到请添加好友。");
            cqManager.sendMsg(cqMsg);
            cqMsg.setMessageType("private");
        }
        if (request.size() > 0) {
            resp = "以下是白菜本次启动期间收到的加群邀请：";
            for (CqMsg aList : request.keySet()) {
                resp = resp.concat("\n" + "Flag：" + aList.getFlag() + "，群号：" + aList.getGroupId()
                        + "，邀请人：" + aList.getUserId() + "，时间：" + new SimpleDateFormat("HH:mm:ss").
                        format(new Date(aList.getTime() * 1000L)) + "已通过：" + request.get(aList));
            }
        } else {
            resp = "本次启动白菜没有收到加群邀请。";
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    public void handleInvite(CqMsg cqMsg) {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        String flag = m.group(2);
        logger.info("正在通过对Flag为：" + flag + "的邀请");
        //开启一个新消息用来通过邀请
        CqMsg newMsg = new CqMsg();
        newMsg.setFlag(flag);
        newMsg.setApprove(true);
        newMsg.setType("invite");
        newMsg.setMessageType("handleInvite");
        CqResponse cqResponse = cqManager.sendMsg(newMsg);
        if (cqResponse.getRetCode() == 0) {
            for (CqMsg aList : request.keySet()) {
                if (aList.getFlag().equals(flag)) {
                    request.replace(aList, "是");
                    //通过新群邀请时，向消息队列Map中添加一个消息队列对象
                    SmokeUtil.msgQueues.put(aList.getGroupId(), new MsgQueue());
                }
            }
            CqMsg cqMsg1 = new CqMsg();
            cqMsg1.setMessage("Flag为：" + flag + "的邀请被" + cqMsg.getUserId() + "通过");
            cqMsg1.setMessageType("private");
            for (long l : OverallConsts.ADMIN_LIST) {
                cqMsg1.setUserId(l);
                cqManager.sendMsg(cqMsg1);
            }
        } else {
            cqMsg.setMessage("通过Flag为：" + flag + "的邀请失败，返回信息：" + cqResponse);
            cqManager.sendMsg(cqMsg);
            cqMsg.setMessage("通过Flag为：" + flag + "的邀请失败，消息体：" + cqMsg + "，返回信息：" + cqResponse);
            cqMsg.setMessageType("private");
            cqMsg.setUserId(1335734657L);
            cqManager.sendMsg(cqMsg);
        }
    }

    public void unbind(CqMsg cqMsg) {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        Long qq = Long.valueOf(m.group(2));
        User user = userDAO.getUser(qq, null);
        if (user == null) {
            cqMsg.setMessage("该QQ没有绑定用户……");
            cqManager.sendMsg(cqMsg);
            return;
        }
        user.setQq(0L);
        userDAO.updateUser(user);
        cqMsg.setMessage("QQ" + qq + "的绑定信息已经清除");
        cqManager.sendMsg(cqMsg);
    }

    public void firstPlace(CqMsg cqMsg) {
        Integer bid;
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        try {
            bid = Integer.valueOf(m.group(2));
        } catch (java.lang.NumberFormatException e) {
            cqMsg.setMessage("It's a disastah!!");
            cqManager.sendMsg(cqMsg);
            return;
        }

        Beatmap beatmap = apiManager.getBeatmap(bid);
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
        Userinfo userFromAPI = apiManager.getUser(null, scores.get(0).getUserId());
        //为了日志+和BP的PP计算兼容，补上get_score的API缺失的部分
        scores.get(0).setBeatmapName(beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]");
        scores.get(0).setBeatmapId(beatmap.getBeatmapId());
        String filename = imgUtil.drawFirstRank(beatmap, scores.get(0), userFromAPI, scores.get(0).getScore() - scores.get(1).getScore());
        cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
        cqManager.sendMsg(cqMsg);
    }

    public void listMsg(CqMsg cqMsg) {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        String msg = cqMsg.getMessage();
        int index = msg.indexOf("]");
        String resp = "";
        String QQ = msg.substring(24, index);
        if ("all".equals(QQ)) {
            resp = "啥玩意啊 咋回事啊";
        } else {
            ArrayList<CqMsg> msgs = SmokeUtil.msgQueues.get(cqMsg.getGroupId()).getMsgsByQQ(Long.valueOf(QQ));
            QQInfo data = cqManager.getGroupMember(cqMsg.getGroupId(), Long.valueOf(QQ)).getData();
            if (msgs.size() == 0) {
                resp = "没有" + QQ + "的最近消息。";
            } else if (msgs.size() <= 10) {
                for (int i = 0; i < msgs.size(); i++) {
                    if ("".equals(data.getCard())) {
                        resp += data.getNickname();
                    } else {
                        resp += data.getCard();
                    }

                    resp += "<" + QQ + "> " + new SimpleDateFormat("HH:mm:ss").
                            format(new Date(msgs.get(i).getTime() * 1000L)) + "\n  " + msgs.get(i).getMessage() + "\n";
                }
            } else {
                for (int i = msgs.size() - 10; i < msgs.size(); i++) {
                    if ("".equals(data.getCard())) {
                        resp += data.getNickname();
                    } else {
                        resp += data.getCard();
                    }

                    resp += "<" + QQ + "> " + new SimpleDateFormat("HH:mm:ss").
                            format(new Date(msgs.get(i).getTime() * 1000L)) + "\n  " + msgs.get(i).getMessage() + "\n";

                }
            }
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    public void listUserPP(CqMsg cqMsg) {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        String role;
        if ("".equals(m.group(2))) {
            role = "mp5";
        } else {
            role = m.group(2);
        }


        logger.info("开始检查" + role + "用户组中所有人的PP");
        List<Integer> list = userDAO.listUserIdByRole(role);
        String resp;
        if (list.size() > 0) {
            resp = role + "用户组中所有人的PP：";

            for (Integer aList : list) {
                Userinfo userinfo = apiManager.getUser(null, aList);
                if (userinfo != null) {
                    logger.info(userinfo.getUserName() + "的PP是" + userinfo.getPpRaw());
                    resp = resp.concat("\n" + userinfo.getUserName() + "\t" + userinfo.getPpRaw());
                }
            }
        } else {
            resp = role + "用户组没有成员。";
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }


    public void findPlayer(CqMsg cqMsg) {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        String resp;
        String username = m.group(2);
        List<User> list = userDAO.listUserIdByUname(username);
        if (list.size() > 0) {
            resp = "找到以下玩家曾用/现用名是" + username + "：\n";
            for (User u : list) {
                resp += "现用名：" + u.getCurrentUname() + "，曾用名：" + u.getLegacyUname() + "，uid：" + u.getUserId();
                if (!u.getQq().equals(0L)) {
                    resp += "，QQ：" + u.getQq();
                }
                resp += "\n";
            }
        } else {
            resp = "没有找到现用/曾用" + username + "作为用户名的玩家。";
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    public void scanCard(CqMsg cqMsg) {
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        CqResponse<List<QQInfo>> cqResponse1 = cqManager.getGroupMembers(Long.valueOf(m.group(2)));
        String resp;
        User user;
        Userinfo userFromAPI;
        resp = "找到以下玩家群名片不含完整id：\n";
        for (QQInfo qqInfo : cqResponse1.getData()) {
            //根据QQ获取user
            user = userDAO.getUser(qqInfo.getUserId(), null);
            if (user != null && !user.isBanned()) {
                userFromAPI = apiManager.getUser(null, user.getUserId());
                if (userFromAPI == null) {
                    user.setBanned(true);
                    userDAO.updateUser(user);
                    String card = cqManager.getGroupMember(qqInfo.getGroupId(), qqInfo.getUserId()).getData().getCard();
                    if (!card.toLowerCase(Locale.CHINA).replace("_", " ")
                            .contains(user.getCurrentUname().toLowerCase(Locale.CHINA).replace("_", " "))) {
                        resp += "osu! id：" + user.getCurrentUname() + "，QQ：" + qqInfo.getUserId() + "，群名片：" + card + "(该玩家于今日被ban，已记录)\n";
                    }
                } else {
                    String card = cqManager.getGroupMember(qqInfo.getGroupId(), qqInfo.getUserId()).getData().getCard();
                    if (!card.toLowerCase(Locale.CHINA).replace("_", " ")
                            .contains(userFromAPI.getUserName().toLowerCase(Locale.CHINA).replace("_", " "))) {
                        resp += "osu! id：" + userFromAPI.getUserName() + "，QQ：" + qqInfo.getUserId() + "，群名片：" + card + "\n";
                    }
                }
            }
        }
        if (resp.endsWith("\n")) {
            resp = resp.substring(0, resp.length() - 1);
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    public void checkGroupBind(CqMsg cqMsg) {
        CqResponse<List<QQInfo>> cqResponse;
        User user;
        String resp = "";
        Matcher m = PatternConsts.ADMIN_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        //根据是否带群号，取出相应的群成员
        if ("".equals(m.group(2))) {
            cqResponse = cqManager.getGroupMembers(cqMsg.getGroupId());
        } else {
            cqResponse = cqManager.getGroupMembers(Long.valueOf(m.group(2)));
        }
        for (QQInfo qqInfo : cqResponse.getData()) {
            //根据QQ获取user
            user = userDAO.getUser(qqInfo.getUserId(), null);
            String card = cqManager.getGroupMember(qqInfo.getGroupId(), qqInfo.getUserId()).getData().getCard();
            if (user != null) {
                Userinfo userFromAPI = apiManager.getUser(null, user.getUserId());
                List<String> roles = Arrays.asList(user.getRole().split(","));
                switch (String.valueOf(cqResponse.getData().get(0).getGroupId())) {
                    case "201872650":
                        if (!roles.contains("mp5")) {
                            resp += "QQ： " + qqInfo.getUserId() + " 绑定的id不在mp5用户组，osu! id：" + user.getCurrentUname() + "，用户组：" + user.getRole() + "。\n";
                        }
                        break;
                    case "564679329":
                        if (!roles.contains("mp4")) {
                            resp += "QQ： " + qqInfo.getUserId() + " 绑定的id不在mp4用户组，osu! id：" + user.getCurrentUname() + "，用户组：" + user.getRole() + "。\n";
                        }
                        break;
                    default:
                        break;
                }
            } else {
                resp += "QQ： " + qqInfo.getUserId() + " 没有绑定id，群名片是：" + card + "\n";
            }
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);

    }

    public void help(CqMsg cqMsg) {
        String resp = "!sudo add xxx,xxx:yyy 将xxx,xxx添加到yyy用户组。\n" +
                "!sudo del xxx:yyy 将xxx的用户组中yyy删除，如果不带:yyy则重置为默认（creep）。\n" +
                "!sudo check xxx 根据osu!id查找用户，包括绑定、用户组信息。\n" +
                "!sudo 褪裙 xxx 查询xxx用户组中多少人超过PP上线。\n" +
                "!sudo bg xxx:http://123 将给定连接中的图以xxx.png的文件名写入数据库。\n" +
                "!sudo recent xxx 查询他人的recent。\n" +
                "!sudo afk n:xxx 查询xxx用户组中，n天以上没有登录的玩家(以官网为准，如果不提供用户组，默认为mp5)。\n" +
                "!sudo smoke @xxx n 在白菜是管理的群，把被艾特的人禁言n秒。\n" +
                "（艾特全体成员则遍历群成员并禁言，慎用）\n" +
                "!sudo listInvite 列举当前的加群邀请（无论在哪里使用都会私聊返回结果）。\n" +
                "!sudo handleInvite n 通过Flag为n的邀请。\n" +
                "!sudo clearInvite 清空邀请列表。\n" +
                "!sudo unbind xxx 解绑某个QQ对应的id（找到该QQ对应的uid，并将QQ改为0）。\n" +
                "!sudo fp xxx 打印给定bid的#1。\n" +
                "!sudo listMsg @xxx 打印被艾特的人最近的10条消息。在对方撤回消息时起作用。\n" +
                "!sudo PP xxx 查询xxx组中所有成员PP（一般用于比赛计算Cost）。\n" +
                "!sudo findPlayer xxx 查询曾用/现用xxx用户名的玩家。\n" +
                "!sudo scanCard xxx扫描所在群的所有绑定了QQ的群成员，检测群名片是否包含完整id（无视大小写，并且会自动识别横线/空格）。\n" +
                "!sudo checku xxx 根据uid查找用户。\n" +
                "!sudo checkq xxx 根据QQ查找用户。\n" +
                "!sudo checkGroupBind xxx 打印该群所有成员是否绑定id，以及绑定id是否在mp4/5组内。" +
                "特别的，不带参数会将群号设置为当前消息的群号。（只支持mp4/5群）\n" +
                "!sudo repeatStar 打印所有开启复读计数群内，复读发言/所有发言 比值最高的人。";
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }


    public void stashInviteRequest(CqMsg cqMsg) {
        //这里是存引用……所以后面返回是null
        request.put(cqMsg, "否");
        CqMsg cqMsg1 = new CqMsg();
        cqMsg1.setMessage("有新的拉群邀请，请注意查收：" + "Flag：" + cqMsg.getFlag() + "，群号：" + cqMsg.getGroupId()
                + "，邀请人：" + cqMsg.getUserId() + "，时间：" + new SimpleDateFormat("HH:mm:ss").
                format(new Date(cqMsg.getTime() * 1000L)));
        cqMsg1.setMessageType("private");
        for (long i : OverallConsts.ADMIN_LIST) {
            //debug 这里设置的userid 应该是cqmsg1的，之前漏了个1
            cqMsg1.setUserId(i);
            cqManager.sendMsg(cqMsg1);
        }

    }

    @UserRoleControl({526942417})
    public void getRepeatStar(CqMsg cqMsg) {
        User user = userDAO.getRepeatStar();
        if (!user.getRepeatCount().equals(0L) && !user.getSpeakingCount().equals(0L)) {
            cqMsg.setMessage("在所有开启复读计数的群中，当前的复读之星为：" + user.getQq() + "，总发言数：" + user.getSpeakingCount() + "，复读次数：" + user.getRepeatCount());
            cqManager.sendMsg(cqMsg);
        } else {
            cqMsg.setMessage("暂时还没有复读之星。");
            cqManager.sendMsg(cqMsg);
        }
    }
}
