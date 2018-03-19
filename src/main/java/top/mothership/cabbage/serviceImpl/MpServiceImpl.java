package top.mothership.cabbage.serviceImpl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.pattern.CQCodePattern;
import top.mothership.cabbage.pattern.MpCommandPattern;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.LobbyDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Lobby;
import top.mothership.cabbage.pojo.osu.Userinfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Matcher;

/**
 * The type Mp service.
 */
@Service
public class MpServiceImpl {
    private final LobbyDAO lobbyDAO;
    private final ApiManager apiManager;
    private Logger logger = LogManager.getLogger(this.getClass());
    private final UserDAO userDAO;
    private final CqManager cqManager;
    private final UserInfoDAO userInfoDAO;

    /**
     * Instantiates a new Mp service.
     * @param lobbyDAO   the lobby dao
     * @param apiManager the api manager
     * @param userDAO    the user dao
     * @param cqManager  the cq manager
     * @param userInfoDAO
     */
    public MpServiceImpl(LobbyDAO lobbyDAO, ApiManager apiManager, UserDAO userDAO, CqManager cqManager, UserInfoDAO userInfoDAO) {
        this.lobbyDAO = lobbyDAO;
        this.apiManager = apiManager;
        this.userDAO = userDAO;
        this.cqManager = cqManager;
        this.userInfoDAO = userInfoDAO;

    }

    /**
     * 创建一个mp房间预约。
     *
     * @param cqMsg the cq msg
     */
    public void reserveLobby(CqMsg cqMsg) {
        Matcher cmdMatcher = MpCommandPattern.MP_CMD_REGEX.matcher(cqMsg.getMessage());
        cmdMatcher.find();
        User user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        //判断是否指定了水平组
        String group;
        if ("".equals(cmdMatcher.group(3))) {
            cqMsg.setMessage("请指定你想创建mp房间的水平组。");
            cqManager.sendMsg(cqMsg);
            return;
        } else {
            group = cmdMatcher.group(3);
        }
        Integer time = 0;
        switch (cmdMatcher.group(1)) {
            case "rs":
                time = Integer.valueOf(cmdMatcher.group(2));
                if (lobbyDAO.getLobbyNotEnded().size() >= 4) {
                    cqMsg.setMessage("警告：当前已经有4个房间正在游戏中，达到了Bancho允许的上限。如果在预约的时间，游戏中的房间依然是四个，房间将不会创建。");
                    cqManager.sendMsg(cqMsg);
                }
                break;
            case "make":
                if (lobbyDAO.getLobbyNotEnded().size() >= 4) {
                    cqMsg.setMessage("当前已经有4个房间正在游戏中，达到了Bancho允许的上限，请稍后再试。");
                    cqManager.sendMsg(cqMsg);
                    return;
                }
                break;
            default:
                break;
        }
        Lobby lobby = lobbyDAO.getStartedLobbyByCreator(user.getUserId());
        if (lobby != null) {
            cqMsg.setMessage("你有一个创建的mp房间尚未结束。请等待结束再发起下一次预约。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        lobby = lobbyDAO.getReservedLobbyByCreator(user.getUserId());
        if (lobby != null) {
            cqMsg.setMessage("你有一个预约了但还未开始的mp房间。如果要取消预约，请使用abort命令。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        lobby = new Lobby();
        lobby.setCreator(user.getUserId());
        lobby.setGroup(group);
        lobby.setReservedStartTime(LocalDateTime.now().minusMinutes(time));
        lobbyDAO.addReserveLobby(lobby);

    }


    /**
     * Invite player.
     *
     * @param cqMsg the cq msg
     */
    public void invitePlayer(CqMsg cqMsg) {
        Matcher cmdMatcher = MpCommandPattern.MP_CMD_REGEX.matcher(cqMsg.getMessage());
        cmdMatcher.find();
        User user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        //判断此人是否已经开设了可以加入的房间
        //以后考虑加一列，让已经预约没开始的房间也能邀请……这块也没想好……

        Lobby lobby = lobbyDAO.getStartedLobbyByCreator(user.getUserId());
        if (lobby == null) {
            cqMsg.setMessage("你没有开设可以加入的房间。请先使用!mp rs或!mp make命令。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        String target = cmdMatcher.group(2);
        Matcher atMatcher = CQCodePattern.AT.matcher(target);
        User targetUser;
        Userinfo userFromAPI;
        if (atMatcher.find()) {
            String QQ = atMatcher.group(1);
            targetUser = userDAO.getUser(Long.valueOf(QQ), null);
            if (targetUser == null) {
                cqMsg.setMessage("你邀请的人没有绑定osu!id。请提醒他使用!setid 他的osuid 功能。");
                cqManager.sendMsg(cqMsg);
                return;
            }
            if (targetUser.isBanned()) {
                cqMsg.setMessage("你邀请的人。。\n这么悲伤的事情，不忍心说啊。");
                cqManager.sendMsg(cqMsg);
                return;
            }
            userFromAPI = apiManager.getUser(0, targetUser.getUserId());
        } else {
            userFromAPI = apiManager.getUser(0, target);
            if (userFromAPI == null) {
                cqMsg.setMessage("没有获取到id是" + target + "的玩家。");
                cqManager.sendMsg(cqMsg);
                return;
            } else {
                targetUser = userDAO.getUser(null, userFromAPI.getUserId());
                //录入
                if (targetUser == null) {
                    user = new User(userFromAPI.getUserId(), "creep", 0L, "[]", userFromAPI.getUserName(), false, 0, null, null, 0L, 0L);
                    userDAO.addUser(user);
                    if (LocalTime.now().isAfter(LocalTime.of(4, 0))) {
                        userFromAPI.setQueryDate(LocalDate.now());
                    } else {
                        userFromAPI.setQueryDate(LocalDate.now().minusDays(1));
                    }
                    //写入一行userinfo
                    userInfoDAO.addUserInfo(userFromAPI);
                }
            }
        }
//        ircClient.sendMessage("#mp_" + lobby.getMatch().getMatchId(), "!mp invite " + userFromAPI.getUserName());
    }

    /**
     * List lobby.
     *
     * @param cqMsg the cq msg
     */
    public void listLobby(CqMsg cqMsg) {
        User user = userDAO.getUser(cqMsg.getUserId(), null);
        if (user == null) {
            cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        List<Lobby> notStarted = lobbyDAO.getLobbyNotStarted();
        List<Lobby> notEnded = lobbyDAO.getLobbyNotEnded();
        String resp = "";
        for (Lobby l : notStarted) {
            resp += l.getCreator();
        }

    }

    /**
     * Abort reserve.
     *
     * @param cqMsg the cq msg
     */
    public void abortReserve(CqMsg cqMsg) {

    }

    /**
     * Join lobby.
     *
     * @param cqMsg the cq msg
     */
    public void joinLobby(CqMsg cqMsg) {

    }


    /**
     * Help.
     *
     * @param cqMsg the cq msg
     */
    public void help(CqMsg cqMsg) {
        String resp = "!mp rs 180:xxx 预约一次180分钟后的mp。xxx为指定水平组。\n" +
                "!mp abort 取消自己之前预约的mp。\n" +
                "!mp make xxx 立即发起一次指定水平组的mp。\n" +
                "!mp invite @xxx /xxx 在预约的mp开启后，向对方发送邀请，对方如果在线会在游戏内收到私信。\n" +
                "!mp list 列出所有未关闭的mp房。\n" +
                "!mp join xxx 加入某个已开始的mp房间（会收到邀请。）\n" +
                "以下命令只有选图组可以使用：\n" +
                "!mp add n+EZDT:xxx 将bid为n、MOD为EZDT的谱面加入xxx水平组。\n" +
                "!mp del n:xxx 将某个谱面从某水平组删除。\n" +
                "!mp listMap xxx 显示每个水平组所有谱面。\n";
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }

    /**
     * 扫描所有的需要开启的房间 并且开启
     */
//    @Scheduled(cron = "0 * * * * ? ")
    public void scanLobby() {

    }


    public void reconnectAllLobby() {
        //把所有已经开启的房间重连一次
    }

    public void shutdownLobby(Integer mpId) {
        //登记某个房间的结束时间
    }

    /**
     * Add map.
     *
     * @param cqMsg the cq msg
     */
    public void addMap(CqMsg cqMsg) {

    }

    /**
     * Del map.
     *
     * @param cqMsg the cq msg
     */
    public void delMap(CqMsg cqMsg) {

    }

    /**
     * List map.
     *
     * @param cqMsg the cq msg
     */
    public void listMap(CqMsg cqMsg) {

    }
}
