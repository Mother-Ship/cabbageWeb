package top.mothership.cabbage.service;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.annotation.UserAuthorityControl;
import top.mothership.cabbage.consts.OverallConsts;
import top.mothership.cabbage.consts.TipConsts;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pattern.RegularPattern;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.coolq.Argument;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.coolq.CqResponse;
import top.mothership.cabbage.pojo.coolq.QQInfo;
import top.mothership.cabbage.pojo.coolq.osu.Beatmap;
import top.mothership.cabbage.pojo.coolq.osu.Score;
import top.mothership.cabbage.pojo.coolq.osu.SearchParam;
import top.mothership.cabbage.pojo.coolq.osu.Userinfo;
import top.mothership.cabbage.util.osu.ScoreUtil;
import top.mothership.cabbage.util.osu.UserUtil;
import top.mothership.cabbage.util.qq.CompressLevelEnum;
import top.mothership.cabbage.util.qq.ImgUtil;
import top.mothership.cabbage.util.qq.MsgQueue;
import top.mothership.cabbage.util.qq.SmokeUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;

/**
 * 管理命令进行业务处理的类
 *
 * @author QHS
 */
@Service
@UserAuthorityControl
public class CqAdminServiceImpl {
    public static Map<CqMsg, String> request = new HashMap<>();
    private static ResDAO resDAO;
    private final CqManager cqManager;
    private final ApiManager apiManager;
    private final UserDAO userDAO;
    private final UserInfoDAO userInfoDAO;
    private final WebPageManager webPageManager;
    private final ImgUtil imgUtil;
    private final ScoreUtil scoreUtil;
    private final UserUtil userUtil;
    private Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    public CqAdminServiceImpl(CqManager cqManager, ApiManager apiManager, UserDAO userDAO, UserInfoDAO userInfoDAO, WebPageManager webPageManager, ImgUtil imgUtil, ResDAO resDAO, ScoreUtil scoreUtil, UserUtil userUtil) {
        this.cqManager = cqManager;
        this.apiManager = apiManager;
        this.userDAO = userDAO;
        this.userInfoDAO = userInfoDAO;
        this.webPageManager = webPageManager;
        this.imgUtil = imgUtil;
        CqAdminServiceImpl.resDAO = resDAO;
        this.scoreUtil = scoreUtil;
        this.userUtil = userUtil;
        loadCache();
    }

    /**
     * 从数据库一口气读取所有图片素材文件的方法。
     * 其实好像没必要做成静态的了……？但是静态的似乎也没啥影响，先留着吧
     */
    private static void loadCache() {
        //调用NIO遍历那些可以加载一次的文件
        //在方法体内初始化，重新初始化的时候就可以去除之前缓存的文件
        ImgUtil.images = new HashMap<>();
        //逻辑改为从数据库加载
        List<Map<String, Object>> list = resDAO.getImages();
        for (Map<String, Object> aList : list) {
            String name = (String) aList.get("name");
            Matcher m = RegularPattern.OSU_USER_ID.matcher(name);
            //节约内存，只缓存不是用户自定义的BG
            if (!m.find()) {
                byte[] data = (byte[]) aList.get("data");
                try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
                    ImgUtil.images.put(name, ImageIO.read(in));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void addUserRole(CqMsg cqMsg) {

        Argument argument = cqMsg.getArgument();
        List<String> usernames = argument.getUsernames();
        String role = argument.getRole();
        logger.info("分隔字符串完成，用户：" + usernames + "，用户组：" + role);
        List<String> nullList = new ArrayList<>();
        List<String> doneList = new ArrayList<>();
        List<String> addList = new ArrayList<>();
        Userinfo userFromAPI;
        for (String username : usernames) {
            logger.info("开始从API获取" + username + "的信息");
            //这里只是为了获取uid，直接指定mode为0
            userFromAPI = apiManager.getUser(0, username);
            //如果user不是空的(官网存在记录)
            if (userFromAPI != null) {
                //查找userRole数据库

                if (userDAO.getUser(null, userFromAPI.getUserId()) == null) {
                    //如果userRole库中没有这个用户
                    //构造User对象写入数据库
                    logger.info("开始将用户" + userFromAPI.getUserName() + "添加到数据库。");
                    User user = new User(userFromAPI.getUserId(), "creep", 0L, "[]", userFromAPI.getUserName(), false, 0, null, null, 0L, 0L);
                    userDAO.addUser(user);

                    if (LocalTime.now().isAfter(LocalTime.of(4, 0))) {
                        userFromAPI.setQueryDate(LocalDate.now());
                    } else {
                        userFromAPI.setQueryDate(LocalDate.now().minusDays(1));
                    }
                    userInfoDAO.addUserInfo(userFromAPI);

                    addList.add(userFromAPI.getUserName());
                } else {
                    //进行Role更新
                    User user = userDAO.getUser(null, userFromAPI.getUserId());
                    //拿到原先的user，把role拼上去，塞回去
                    user = userUtil.addRole(role, user);
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
        if (usernames.size() == 0) {
            resp = "没有做出改动。";
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    public void delUserRole(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        List<String> usernames = argument.getUsernames();
        String role = argument.getRole();
        if (role == null) {
            role = "All";
        }
        logger.info("分隔字符串完成，用户：" + usernames + "，用户组：" + role);

        List<String> nullList = new ArrayList<>();
        List<String> doneList = new ArrayList<>();
        List<String> notUsedList = new ArrayList<>();
        Userinfo userFromAPI;
        String lastUserOldRole = "";
        for (String username : usernames) {
            logger.info("开始从API获取" + username + "的信息");
            //同理，只是为了获取id，直接设为0
            userFromAPI = apiManager.getUser(0, username);
            if (userFromAPI != null) {
                //查找userRole数据库
                //进行Role更新
                User user = userDAO.getUser(null, userFromAPI.getUserId());
                if (user == null) {
                    notUsedList.add(userFromAPI.getUserName());
                    //直接忽略掉下面的，进行下一次循环
                    continue;
                }
                lastUserOldRole = user.getRole();
                user = userUtil.delRole(role, user);
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
        if (usernames.size() == 0) {
            resp = "没有做出改动。";
        }
        if (doneList.size() == 1) {
            resp = resp.concat("\n该用户之前的用户组是：" + lastUserOldRole);
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }


    @UserAuthorityControl({1427922341})
    public void addComponent(CqMsg cqMsg) throws IOException {
        Argument argument = cqMsg.getArgument();
        //实验性功能
        //2018-3-9 11:54:14验收通过
        if (argument.getFileName().contains(".")) {
            HttpURLConnection httpConnection;
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                httpConnection =
                        (HttpURLConnection) new URL(argument.getUrl()).openConnection();
                httpConnection.setRequestMethod("GET");
                httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.40 Safari/537.36");
                InputStream is = httpConnection.getInputStream();
                byte[] b = new byte[1024];
                int n;
                while ((n = is.read(b)) != -1) {
                    bos.write(b, 0, n);
                }
                byte[] bytes = bos.toByteArray();
                resDAO.addResource(argument.getFileName(), bytes);
                is.close();
            } catch (IOException ignore) {
                cqMsg.setMessage("修改组件" + argument.getFileName() + "失败，错误信息：" + ignore.getMessage());
                cqManager.sendMsg(cqMsg);
                return;
            }
            cqMsg.setMessage("修改组件" + argument.getFileName() + "成功。");
            cqManager.sendMsg(cqMsg);
        } else {
            BufferedImage tmp = ImageIO.read(new URL(argument.getUrl()));
            //这个方法从QQ直接发送图片+程序下载，改为采用URL写入到硬盘，到现在改为存入数据库+打破目录限制，只不过命令依然叫!sudo bg……
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(tmp, "png", out);
                tmp.flush();
                byte[] imgBytes = out.toByteArray();
                resDAO.addImage(argument.getFileName() + ".png", imgBytes);
            } catch (IOException ignore) {

            }
            //手动调用重载缓存
            loadCache();
            cqMsg.setMessage("修改组件" + argument.getFileName() + ".png成功。");
            cqManager.sendMsg(cqMsg);

        }
    }

    public void recent(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();

        if (argument.getMode() == null) {
            argument.setMode(0);
        }
        Userinfo userFromAPI = apiManager.getUser(0, argument.getUsername());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(TipConsts.USERNAME_GET_FAILED, argument.getUsername()));
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
        Beatmap beatmap = apiManager.getBeatmap(score.getBeatmapId());
        if (beatmap == null) {
            cqMsg.setMessage(String.format(TipConsts.BEATMAP_GET_FAILED, score.getBeatmapId()));
            cqManager.sendMsg(cqMsg);
            return;
        }
        switch (argument.getSubCommandLowCase()) {
            case "rs":
                String resp = scoreUtil.genScoreString(score, beatmap, userFromAPI.getUserName(),null);
                cqMsg.setMessage(resp);
                cqManager.sendMsg(cqMsg);
                break;
            case "recent":
                String filename = imgUtil.drawResult(userFromAPI, score, beatmap, argument.getMode());
                cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
                cqManager.sendMsg(cqMsg);
                break;
            default:
                break;
        }
    }

    public void checkAfkPlayer(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        if (argument.getRole() == null) {
            argument.setRole("mp5");
        }
        logger.info("检测到管理员对" + argument.getRole() + "用户组" + argument.getDay() + "天前的AFK玩家查询");
        String resp;
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DATE, -argument.getDay());
        java.sql.Date date = new Date(cl.getTimeInMillis());

        List<Integer> list = userDAO.listUserIdByRole(argument.getRole(), true);
        List<String> afkList = new ArrayList<>();
        for (Integer aList : list) {
            java.util.Date afkDate = webPageManager.getLastActive(aList);
            if (afkDate != null && date.after(afkDate)) {
                afkList.add(apiManager.getUser(0, aList).getUserName());
            }
        }
        resp = "查询" + argument.getRole() + "用户组中，最后登录时间早于" + argument.getDay() + "天前的AFK玩家完成。";
        if (afkList.size() > 0) {
            resp = resp.concat("\n以下玩家：" + afkList.toString() + "最后登录时间在" + argument.getDay() + "天前。");
        } else {
            resp = resp.concat("\n没有检测" + argument.getRole() + "用户组中最后登录时间在" + argument.getDay() + "天前的玩家。");
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    @UserAuthorityControl({372427060})
    public void smoke(CqMsg cqMsg) {
        //开放给树姐
        Argument argument = cqMsg.getArgument();
        if (argument.getQq().equals(-1L)) {
            List<QQInfo> memberList = cqManager.getGroupMembers(cqMsg.getGroupId()).getData();
            cqMsg.setMessageType("smoke");
            cqMsg.setDuration(argument.getSecond());
            String operator = cqMsg.getUserId().toString();
            for (QQInfo aList : memberList) {
                cqMsg.setUserId(aList.getUserId());
                cqManager.sendMsg(cqMsg);
                logger.info(aList.getUserId() + "被" + operator + "禁言" + argument.getSecond() + "秒。");
            }
            String img = imgUtil.drawImage(ImgUtil.images.get("smokeAll.png"), CompressLevelEnum.不压缩);
            cqMsg.setMessage("[CQ:image,file=base64://" + img + "]");
            cqMsg.setMessageType("group");
            cqManager.sendMsg(cqMsg);
        } else {
            logger.info(argument.getQq() + "被" + cqMsg.getUserId() + "禁言" + argument.getSecond() + "秒。");
            if (argument.getSecond() > 0) {
                cqMsg.setMessage("[CQ:record,file=base64://" + Base64.getEncoder().encodeToString((byte[]) resDAO.getResource("all_dead.wav")) + "]");
                cqManager.sendMsg(cqMsg);
            }
            cqMsg.setMessageType("smoke");
            cqMsg.setDuration(argument.getSecond());
            cqMsg.setUserId(argument.getQq());
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
        Argument argument = cqMsg.getArgument();
        logger.info("正在通过对Flag为：" + argument.getFlag() + "的邀请");
        //开启一个新消息用来通过邀请
        CqMsg newMsg = new CqMsg();
        newMsg.setFlag(argument.getFlag());
        newMsg.setApprove(true);
        newMsg.setType("invite");
        newMsg.setMessageType("handleInvite");
        newMsg.setSelfId(cqMsg.getSelfId());
        //邀请谁的只能在谁那边回消息通过
        CqResponse cqResponse = cqManager.sendMsg(newMsg);
        if (cqResponse.getRetCode() == 0) {
            for (CqMsg aList : request.keySet()) {
                if (aList.getFlag().equals(argument.getFlag())) {
                    request.replace(aList, "是");
                    //通过新群邀请时，向消息队列Map中添加一个消息队列对象
                    SmokeUtil.msgQueues.put(aList.getGroupId(), new MsgQueue());
                }
            }
            CqMsg cqMsg1 = new CqMsg();
            cqMsg1.setMessage("Flag为：" + argument.getFlag() + "的邀请被" + cqMsg.getUserId() + "通过");
            cqMsg1.setMessageType("private");
            cqMsg1.setSelfId(cqMsg.getSelfId());
            for (long l : OverallConsts.ADMIN_LIST) {
                cqMsg1.setUserId(l);
                cqManager.sendMsg(cqMsg1);
            }
        } else {
            cqMsg.setMessage("通过Flag为：" + argument.getFlag() + "的邀请失败，返回信息：" + cqResponse);
            cqManager.sendMsg(cqMsg);
            cqMsg.setMessage("通过Flag为：" + argument.getFlag() + "的邀请失败，操作人：" + cqMsg.getUserId() + "，返回信息：" + cqResponse);
            cqMsg.setMessageType("private");
            cqMsg.setUserId(1335734657L);
            cqManager.sendMsg(cqMsg);
        }
    }

    public void appoint(CqMsg cqMsg) {

        Argument argument = cqMsg.getArgument();
        String resp;
        Userinfo userFromAPI = apiManager.getUser(0, argument.getUsername());
        if (userFromAPI == null) {
            cqMsg.setMessage(String.format(TipConsts.USERNAME_GET_FAILED, argument.getUsername()));
            cqManager.sendMsg(cqMsg);
            return;
        }
        User user = userDAO.getUser(null, userFromAPI.getUserId());
        if (user == null) {
            resp = "玩家" + userFromAPI.getUserName() + "没有使用过白菜，已完成注册";
            userUtil.registerUser(userFromAPI.getUserId(), 0, argument.getQq(), argument.getRole());
        } else {
           User user2 = userDAO.getUser(argument.getQq(), null);
            if (user2 == null) {
                //这个QQ没有绑定过玩家
                resp = "更新前的QQ：" + user.getQq() + "，更新前的用户组：" + user.getRole();
                user.setQq(argument.getQq());
                user.setRole(argument.getRole());
                if (argument.getQq() != null) {
                    resp += "\n更新后的QQ：" + argument.getQq();
                }
                if (argument.getRole() != null) {
                    resp += "\n更新后的用户组：" + argument.getRole();
                }
                userDAO.updateUser(user);
            } else {
                resp = "你钦点的QQ已经绑定了" + user.getCurrentUname() + ",请先尝试解绑！";
            }

        }

        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    public void firstPlace(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        SearchParam searchParam = argument.getSearchParam();

        Beatmap beatmap;

        if (searchParam.getBeatmapId() == null) {
            beatmap = webPageManager.searchBeatmap(searchParam, argument.getMode());
        } else {
            //如果是纯数字的搜索词，则改为用API直接获取
            beatmap = apiManager.getBeatmap(searchParam.getBeatmapId());
        }
        if (beatmap == null) {
            cqMsg.setMessage("根据提供的关键词：" + searchParam + "没有找到任何谱面。" +
                    "\n请尝试根据解析出的结果，去掉关键词中的特殊符号……");
            cqManager.sendMsg(cqMsg);
            return;
        }
        //一次性取2个
        List<Score> scores = apiManager.getFirstScore(argument.getMode(), beatmap.getBeatmapId(), 2);
        if (scores.size() == 0) {
            cqMsg.setMessage("提供的bid没有找到#1成绩。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        Userinfo userFromAPI = apiManager.getUser(0, scores.get(0).getUserId());
        //为了日志+和BP的PP计算兼容，补上get_score的API缺失的部分
        scores.get(0).setBeatmapName(beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]");
        scores.get(0).setBeatmapId(beatmap.getBeatmapId());
        String filename = imgUtil.drawFirstRank(beatmap, scores.get(0), userFromAPI, scores.get(0).getScore() - scores.get(1).getScore(), argument.getMode());
        cqMsg.setMessage("[CQ:image,file=base64://" + filename + "]");
        cqManager.sendMsg(cqMsg);
    }

    public void listMsg(CqMsg cqMsg) {
        Long QQ = cqMsg.getArgument().getQq();
        StringBuilder resp = new StringBuilder();
        if ("All".equals(QQ)) {
            resp = new StringBuilder("啥玩意啊 咋回事啊");
        } else {
            MsgQueue msgQueue = SmokeUtil.msgQueues.get(cqMsg.getGroupId());
            if (msgQueue == null) {
                resp = new StringBuilder("获取群" + cqMsg.getGroupId() + "的消息列表失败；请重启Tomcat");
                cqMsg.setMessage(resp.toString());
                cqManager.sendMsg(cqMsg);
                return;
            }
            ArrayList<CqMsg> msgs = msgQueue.getMsgsByQQ(QQ);
            logger.info("记录ListMsg：" + msgs);
            CqResponse<QQInfo> cqResponse = cqManager.getGroupMember(cqMsg.getGroupId(), QQ);
            if (cqResponse.getData() == null) {
                resp = new StringBuilder("获取" + QQ + "的详细信息失败。请尝试再次使用命令。");
                cqMsg.setMessage(resp.toString());
                cqManager.sendMsg(cqMsg);
                return;
            }
            QQInfo data = cqResponse.getData();
            if (msgs.size() == 0) {
                resp = new StringBuilder("没有" + QQ + "的最近消息。");
            } else if (msgs.size() <= 10) {
                for (int i = 0; i < msgs.size(); i++) {
                    if ("".equals(data.getCard())) {
                        resp.append(data.getNickname());
                    } else {
                        resp.append(data.getCard());
                    }

                    resp.append("(").append(QQ).append(") ").append(new SimpleDateFormat("HH:mm:ss").
                            format(new Date(msgs.get(i).getTime() * 1000L))).append("\n  ").append(msgs.get(i).getMessage()).append("\n");
                }
            } else {
                for (int i = msgs.size() - 10; i < msgs.size(); i++) {
                    if ("".equals(data.getCard())) {
                        resp.append(data.getNickname());
                    } else {
                        resp.append(data.getCard());
                    }

                    resp.append("(").append(QQ).append(") ").append(new SimpleDateFormat("HH:mm:ss").
                            format(new Date(msgs.get(i).getTime() * 1000L))).append("\n  ").append(msgs.get(i).getMessage()).append("\n");

                }
            }
        }
        cqMsg.setMessage(resp.toString());
        cqManager.sendMsg(cqMsg);
    }

    @UserAuthorityControl({1427922341, 526942417})
    public void searchPlayer(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        String resp;
        List<User> list = userDAO.searchUser(argument.getUsername());
        if (list.size() > 0) {
            resp = "根据给定的关键字" + argument.getUsername() + "搜索到以下玩家：\n";
            for (User user : list) {
                resp += "搜索结果：\n"
                        + "用户组：" + user.getRole()
                        + "\nosu!id：" + user.getCurrentUname()
                        + "\nosu!uid：" + user.getUserId();
                List<String> legacyUname = new GsonBuilder().create().fromJson(user.getLegacyUname(), new TypeToken<List<String>>() {
                }.getType());
                if (legacyUname.size() > 0) {
                    resp += "\n曾用名：" + user.getLegacyUname();
                }
                resp += "\n被Ban状态：" + user.isBanned()
                        + "\nQQ：" + user.getQq()
                        + "\n主玩模式：" + scoreUtil.convertGameModeToString(user.getMode());
                if (list.size() > 1) {
                    resp += "\n";
                }
            }
        } else {
            resp = "根据提供的关键词" + argument.getUsername() + "没有找到任何玩家。";
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }


    public void groupInfo(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        CqResponse<List<QQInfo>> cqResponse;
        User user;
        String resp = "";
        //根据是否带群号，取出相应的群成员
        if (argument.getGroupId() == null) {
            cqResponse = cqManager.getGroupMembers(cqMsg.getGroupId());
        } else {
            cqResponse = cqManager.getGroupMembers(argument.getGroupId());
        }
        for (QQInfo qqInfo : cqResponse.getData()) {
            //根据QQ获取user和群名片
            user = userDAO.getUser(qqInfo.getUserId(), null);
            String card = cqManager.getGroupMember(qqInfo.getGroupId(), qqInfo.getUserId()).getData().getCard();
            if (user != null) {
                //被ban的玩家也有待在用户组里的权利……
                List<String> roles = Arrays.asList(user.getRole().split(","));
                switch (String.valueOf(cqResponse.getData().get(0).getGroupId())) {
                    //如果群号是mp5但是又不在mp5群，做出相应提示
                    case "201872650":
                        if (!roles.contains("mp5") && !roles.contains("mp5chart")) {
                            resp += "QQ： " + qqInfo.getUserId() + " 绑定的id不在mp5用户组，osu! id：" + user.getCurrentUname() + "，用户组：" + user.getRole() + "。\n";
                        }
                        break;
                    case "564679329":
                        if (!roles.contains("mp4") && !roles.contains("mp4chart")) {
                            resp += "QQ： " + qqInfo.getUserId() + " 绑定的id不在mp4用户组，osu! id：" + user.getCurrentUname() + "，用户组：" + user.getRole() + "。\n";
                        }
                        break;
                    default:
                        break;
                }
                Userinfo userFromAPI = apiManager.getUser(0, user.getUserId());
                if (userFromAPI == null) {
                    user.setBanned(true);
                    userDAO.updateUser(user);
                    if (!card.toLowerCase(Locale.CHINA).replace("_", " ")
                            .contains(user.getCurrentUname().toLowerCase(Locale.CHINA).replace("_", " "))) {
                        resp += "QQ：" + qqInfo.getUserId() + "的id和名片不一致，osu! id：" + user.getCurrentUname() + "，群名片：" + card + "(该玩家本次获取失败，已记录为被ban)\n";
                    }
                } else {
                    user.setBanned(false);
                    if (!userFromAPI.getUserName().equals(user.getCurrentUname())) {
                        user = userUtil.renameUser(user, userFromAPI.getUserName());
                    }
                    if (!card.toLowerCase(Locale.CHINA).replace("_", " ")
                            .contains(userFromAPI.getUserName().toLowerCase(Locale.CHINA).replace("_", " "))) {
                        resp += "QQ：" + qqInfo.getUserId() + "的id和名片不一致，osu! id：" + userFromAPI.getUserName() + "，群名片：" + card + "\n";
                    }
                    userDAO.updateUser(user);
                }
            } else {
                resp += "QQ： " + qqInfo.getUserId() + " 没有绑定id，群名片是：" + card + "\n";
            }

        }
        cqMsg.setMessage(resp);
        logger.info(resp);
        cqManager.sendMsg(cqMsg);
    }

    public void unbind(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        Long qq = argument.getQq();
        User user = userDAO.getUser(qq, null);
        if (user == null) {
            cqMsg.setMessage("该QQ没有绑定用户……");
            cqManager.sendMsg(cqMsg);
            return;
        }
        user.setQq(0L);
        userDAO.updateUser(user);
        cqMsg.setMessage("QQ " + qq + " 的绑定信息已经清除。");
        cqManager.sendMsg(cqMsg);
    }

    public void help(CqMsg cqMsg) {
        String resp = "用户组管理系列：\n" +
                "!sudo add xxx,xxx:yyy 将xxx,xxx添加到yyy用户组。\n" +
                "!sudo del xxx:yyy 将xxx的用户组中yyy删除，如果不带:yyy则重置为默认（creep）。\n" +
                "!sudo roleInfo xxx 返回某个用户组所有成员的信息。\n" +
                "!sudo afk n:xxx 查询xxx用户组中，n天以上没有登录的玩家(以官网为准，如果不提供用户组，默认为mp5)。\n" +
                "!sudo PP xxx 查询xxx组中所有成员PP。\n" +

                "群管理系列：\n" +
                "!sudo smoke @xxx n 在白菜是管理的群，把被艾特的人禁言n秒。\n" +
                "（艾特全体成员则遍历群成员并禁言，慎用）\n" +
                "!sudo listMsg @xxx 打印被艾特的人在该群最近50条消息内 最近的10条消息。在对方撤回消息时起作用。\n" +
                "!sudo repeatStar 输出OCLC赛群内，复读发言/所有发言 比值最高的人。\n" +
                "!sudo groupInfo xxx 检查该群所有成员是否绑定id，以及绑定id是否在mp4/5组内、群名片是否包含完整id。" +
                "不带参数会将群号设置为当前消息的群号。（在mp4/5群外使用，不会检测用户组，只检测是否绑定。）\n" +

                "群邀请管理系列：\n" +
                "!sudo listInvite 列举当前的加群邀请（无论在哪里使用都会私聊返回结果）。\n" +
                "!sudo handleInvite n 通过Flag为n的邀请。\n" +
                "!sudo clearInvite 清空邀请列表。\n" +

                "特权命令系列：\n" +
                "!sudo bg xxx:http://123 将给定连接中的图以xxx.png的文件名写入数据库。\n" +
                "!sudo unbind qq 简单的将某个QQ对应的玩家解绑。\n" +
                "!sudo 钦点 xxx:role#qq 强行修改完整的用户信息。\n" +
                "!sudo recent xxx:mode 查询他人在指定模式的recent。\n" +
                "!sudo fp xxx 打印给定bid的#1。\n" +
                "!sudo score xxx#yyy 打印xxx玩家在yyy谱面的分数。\n" +

                "玩家信息管理系列：\n" +
                "!sudo searchPlayer xxx 查询曾用/现用用户名中包含xxx，或者QQ/uid全文匹配xxx的玩家。\n";

        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }


    public void stashInviteRequest(CqMsg cqMsg) {
        //这里是存引用……所以后面返回是null
        request.put(cqMsg, "否");
        CqMsg cqMsg1 = new CqMsg();
        User user = userDAO.getUser(cqMsg.getUserId(), null);
        cqMsg1.setMessage("有新的拉群邀请，请注意查收：" + "Flag：" + cqMsg.getFlag() + "，群号：" + cqMsg.getGroupId()
                + "，邀请人：" + cqMsg.getUserId() + "，根据邀请人QQ在白菜数据库中的查询结果：" + user);
        cqMsg1.setMessageType("private");
        cqMsg1.setSelfId(cqMsg.getSelfId());
        for (long i : OverallConsts.ADMIN_LIST) {
            //debug 这里设置的user id 应该是cqMsg1的，之前漏了个1
            cqMsg1.setUserId(i);
            cqManager.sendMsg(cqMsg1);
        }

    }

    @UserAuthorityControl({526942417})
    public void getRepeatStar(CqMsg cqMsg) {
        User user = userDAO.getRepeatStar();
        if (!user.getRepeatCount().equals(0L) && !user.getSpeakingCount().equals(0L)) {
            cqMsg.setMessage("在OCLC赛群中，当前的复读之星为：" + user.getQq() + "，总发言数：" + user.getSpeakingCount() + "，复读次数：" + user.getRepeatCount());
            cqManager.sendMsg(cqMsg);
        } else {
            cqMsg.setMessage("暂时还没有复读之星。");
            cqManager.sendMsg(cqMsg);
        }
    }
    @UserAuthorityControl({496802290})
    public void roleInfo(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();

        if (argument.getRole() == null) {
            argument.setRole("mp5");
        }
        String resp;
        List<Integer> list = userDAO.listUserIdByRole(argument.getRole(), false);
        User user;
        if (list.size() == 0) {
            resp = "该用户组没有成员。";
            cqMsg.setMessage(resp);
            cqManager.sendMsg(cqMsg);
            return;
        }
        if (argument.getMode() == null) {
            argument.setMode(0);
        }
        resp = argument.getRole() + "用户组中所有人的信息：";
        for (Integer i : list) {
            user = userDAO.getUser(null, i);
            Userinfo userinfo = apiManager.getUser(argument.getMode(), i);
            Userinfo userinfo1 = userInfoDAO.getUserInfo(argument.getMode(),i,LocalDate.now().minusDays(30));
            Userinfo userinfo2 = userInfoDAO.getUserInfo(argument.getMode(),i,LocalDate.now().minusDays(90));
            if (userinfo != null) {
                resp += "\nuid：" + user.getUserId()
                        + "，现用名：" + user.getCurrentUname()
                        + "，曾用名：" + user.getLegacyUname()
                        + "，绑定的QQ：" + user.getQq()
                        + "，PP：" + userinfo.getPpRaw();
                if(userinfo1!=null && userinfo2!=null){
                    int day30 = userinfo.getPlayCount()-userinfo1.getPlayCount();
                    int day90 = userinfo.getPlayCount()-userinfo2.getPlayCount();
                    if(day90<3*day30){
                        resp += "，90天前PC差："+day90+"和30天内PC差"+day30+"相差过大，有小号嫌疑！";
                    }
                }
            } else {
                if (user.isBanned()) {
                    resp += "\nuid：" + user.getUserId()
                            + "，现用名：" + user.getCurrentUname()
                            + "，曾用名：" + user.getLegacyUname()
                            + "，绑定的QQ：" + user.getQq()
                            + "\n该玩家在今天凌晨录入数据时获取失败，已标记为被ban，如果出错在下一个整点会自动解除";
                } else {
                    resp += "\nuid：" + user.getUserId()
                            + "，现用名：" + user.getCurrentUname()
                            + "，曾用名：" + user.getLegacyUname()
                            + "，绑定的QQ：" + user.getQq()
                            + "\n从osu!api获取该玩家信息失败。";
                }
            }
        }
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);


    }

    public void score(CqMsg cqMsg) {
        Argument argument = cqMsg.getArgument();
        if (argument.getMode() == null) {
            argument.setMode(0);
        }

        Userinfo userinfo = apiManager.getUser(argument.getMode(), argument.getUsername());
        if (userinfo == null) {
            cqMsg.setMessage(String.format(TipConsts.USERNAME_GET_FAILED, argument.getUsername()));
            cqManager.sendMsg(cqMsg);
            return;
        }
        Beatmap beatmap = apiManager.getBeatmap(argument.getBeatmapId());
        if (beatmap == null) {
            cqMsg.setMessage(String.format(TipConsts.BEATMAP_GET_FAILED, argument.getBeatmapId()));
            cqManager.sendMsg(cqMsg);
            return;
        }
        List<Score> scores = apiManager.getScore(argument.getMode(), beatmap.getBeatmapId(), userinfo.getUserId());
        if (scores.size() == 0) {
            cqMsg.setMessage(String.format(TipConsts.THIS_USER_BEATMAP_NO_SCORE, argument.getUsername(), beatmap.getBeatmapId(), scoreUtil.convertGameModeToString(argument.getMode())));
            cqManager.sendMsg(cqMsg);
            return;
        }
        String image = imgUtil.drawResult(userinfo, scores.get(0), beatmap, argument.getMode());
        cqMsg.setMessage("[CQ:image,file=base64://" + image + "]");
        cqManager.sendMsg(cqMsg);

    }
    @UserAuthorityControl({1427922341})
    public void ping(CqMsg cqMsg) {
        Long msgSendTime = cqMsg.getTime() * 1000L;
        Long msgReceivedTime = System.currentTimeMillis();
        Userinfo userinfo = null;
        for (int i = 0; i < 10; i++) {
            userinfo = apiManager.getUser(0, "Mother Ship");
        }
        Long afterQueryOsuApiFor10Times = System.currentTimeMillis();
        imgUtil.drawUserInfo(userinfo, userinfo, "creep", 1, false, 1, 0);
        Long afterDrawStat = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            webPageManager.getPPPlus(userinfo.getUserId());
        }
        Long afterQueryPPPlusFor10Times = System.currentTimeMillis();
        cqMsg.setMessage("自检结束。" +
                "\n从消息发送，到白菜收到消息，花费：" + (msgReceivedTime - msgSendTime) +
                "ms。\n请求10次osu!接口，平均每次花费：" + (afterQueryOsuApiFor10Times - msgReceivedTime) / 10 +
                "ms。\n绘制一次Stat图片，花费：" + (afterDrawStat - afterQueryOsuApiFor10Times) +
                "ms。\n请求10次PP+数据，平均每次花费：" + (afterQueryPPPlusFor10Times - afterDrawStat) / 10 +
                "ms。\n消息离开白菜的时间：" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.CHINA)
                .withZone(ZoneId.systemDefault()).format(Instant.now()) + "。\n接下来会发送一张图片，以测试图片发送是否出错：");
        cqManager.sendMsg(cqMsg);
        cqMsg.setMessage("[CQ:image,file=base64://" + imgUtil.drawImage(ImgUtil.images.get("test.png"), CompressLevelEnum.不压缩) + "]");
        cqManager.sendMsg(cqMsg);
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void watch(){
        CqMsg cqMsg = new CqMsg();
        cqMsg.setMessageType("group");
        cqMsg.setGroupId(693299572L);
        cqMsg.setSelfId(1335734629L);
        String msg = "";
        long time= System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            apiManager.getUser(0, "Mother Ship");
        }
        Long afterQueryOsuApiFor10Times = System.currentTimeMillis();
        if((afterQueryOsuApiFor10Times - time) / 10>1000){
            msg += "osu! API访问缓慢，此时10次平均访问时间为："+(afterQueryOsuApiFor10Times - time) / 10;
        }
        Long afterDrawStat = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            webPageManager.getPPPlus(2545898);
        }
        Long afterQueryPPPlusFor10Times = System.currentTimeMillis();
        if((afterQueryPPPlusFor10Times - afterDrawStat) / 10>5000){
            msg += "\nPP+访问缓慢，此时10次平均访问时间为："+(afterQueryOsuApiFor10Times - time) / 10;
        }
        msg += "[CQ:image,file=base64://" + imgUtil.drawImage(ImgUtil.images.get("test.png"), CompressLevelEnum.不压缩) + "]";
        cqMsg.setMessage(msg);
        cqManager.sendMsg(cqMsg);

        cqMsg.setSelfId(1020640876L);
        msg = "[CQ:image,file=base64://" + imgUtil.drawImage(ImgUtil.images.get("test.png"), CompressLevelEnum.不压缩) + "]";
        cqMsg.setMessage(msg);
        cqManager.sendMsg(cqMsg);
    }
}
