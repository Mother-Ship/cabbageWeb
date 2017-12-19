package top.mothership.cabbage.serviceImpl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.consts.PatternConsts;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.LobbyDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Lobby;

import java.time.LocalDateTime;
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

    /**
     * Instantiates a new Mp service.
     *
     * @param lobbyDAO   the lobby dao
     * @param apiManager the api manager
     * @param userDAO
     * @param cqManager
     */
    public MpServiceImpl(LobbyDAO lobbyDAO, ApiManager apiManager, UserDAO userDAO, CqManager cqManager) {
        this.lobbyDAO = lobbyDAO;
        this.apiManager = apiManager;
        this.userDAO = userDAO;
        this.cqManager = cqManager;
    }

    /**
     * Reserve lobby.
     *
     * @param cqMsg the cq msg
     */
    public void reserveLobby(CqMsg cqMsg) {
        Matcher cmdMatcher = PatternConsts.MP_CMD_REGEX.matcher(cqMsg.getMessage());
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
            cqMsg.setMessage("请指定用户组。");
            cqManager.sendMsg(cqMsg);
            return;
        } else {
            group = cmdMatcher.group(3);
        }
        Integer time = 0;
        switch (cmdMatcher.group(1)) {
            case "rs":
                time = Integer.valueOf(cmdMatcher.group(2));
                break;
            case "make":
                break;
            default:
                break;
        }
        Lobby lobby = new Lobby();
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

    }

    /**
     * List lobby.
     *
     * @param cqMsg the cq msg
     */
    public void listLobby(CqMsg cqMsg) {

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

    public void help(CqMsg cqMsg) {
        String resp = "!mp rs 180:xxx 预约一次180分钟后的mp。xxx为指定水平组。\n" +
                "!mp abort 取消自己之前预约的mp。\n" +
                "!mp make xxx 立即发起一次指定水平组的mp。\n" +
                "!mp invite @xxx /xxx 向对方发送邀请，对方如果在线会在游戏内收到私信。\n" +
                "!mp list 列出所有未关闭的mp房。\n" +
                "!mp join xxx 加入某个已开始的mp房间（会收到邀请。）\n" +
                "以下命令只有选图组可以使用：\n" +
                "!mp add n+EZDT:xxx 将bid为n、MOD为EZDT的谱面加入xxx水平组。\n" +
                "!mp del n:xxx 将某个谱面从某水平组删除。\n" +
                "!mp listMap xxx 显示每个水平组所有谱面。\n";
        cqMsg.setMessage(resp);
        cqManager.sendMsg(cqMsg);
    }
//    @Scheduled(cron = "0 * * * * ? ")
//    public void scanLobby(){
//
//    }

}
