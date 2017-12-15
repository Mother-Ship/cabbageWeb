package top.mothership.cabbage.serviceImpl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.consts.PatternConsts;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.LobbyDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.User;

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
        User user = userDAO.getUser(cqMsg.getUserId(),null);
        if (user == null) {
            cqMsg.setMessage("你没有绑定默认id。请使用!setid 你的osu!id 命令。");
            cqManager.sendMsg(cqMsg);
            return;
        }
        switch (cmdMatcher.group(1)){
            case "rs":
               Integer time = Integer.valueOf(cmdMatcher.group(2));
                break;
            case "create":

                break;
            default:
                break;

        }
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

    @Scheduled(cron = "0 * * * * ? ")
    private void scanLobby(){

    }
    public void handleMsg(String msg){

        //对消息内容进行判断，001是登录，ping和pong应该是心跳包，其他的就是正常消息
        if (!msg.contains("cho@ppy.sh QUIT")) {
            if (msg.startsWith("PONG")) {
                logger.info("收到pong消息。");
            } else {
                //处理消息
                Matcher serverMsg = PatternConsts.IRC_SERVER_MSG.matcher(msg);
                Matcher regularMsg = PatternConsts.IRC_PRIVATE_MSG.matcher(msg);
                if (serverMsg.find()) {
                    switch (serverMsg.group(1)) {
                        case "001":
                            logger.info("开始登录……");
                            break;
                        case "376":
                            logger.info("登录成功！");
                            break;
                        case "401":
                            // :cho.ppy.sh 401 AutoHost #mp_32349656 :No such nick
                            logger.info("指定的房间已关闭：" + msg);
                            break;
                        default:
                            break;
                    }

                } else if (regularMsg.find()) {
                    logger.info("收到私聊消息：" + msg);
                    // RECV(Mon Oct 23 16:14:35 ART 2017): :BanchoBot!cho@ppy.sh PRIVMSG AutoHost :
                    // You cannot create any more tournament matches. Please close any previous
                    // tournament matches you have open.

                    // :HyPeX!cho@ppy.sh JOIN :#mp_29904363、
                    //看起来是提示有人加入，实际上的业务逻辑是刷新……？还是说这是新建房间之后的必要步骤

//                    if (matcher.matches()) {
//                        if (matcher.group(1).equalsIgnoreCase(m_client.getUser())) {
//                            String lobbyChannel = matcher.group(2);
//                            newLobby(lobbyChannel);
//                            System.out.println("New lobby: " + lobbyChannel);
//                        }
//                    }

                    // :AutoHost!cho@ppy.sh PART :#mp_35457515
//                    group1:登录用户名 group2：房间频道名
//                    if (partM.matches()) {
//                        if (partM.group(1).equalsIgnoreCase(m_client.getUser())) {
                    //如果是bancho发给登录号的消息
//                            if (m_lobbies.containsKey("#mp_" + partM.group(2))) {
                    //如果房间数据库里有消息体的房间
//                                Lobby lobby = m_lobbies.get("#mp_" + partM.group(2));
//                                if (lobby.channel.equalsIgnoreCase("#mp_" + partM.group(2))) {
                    //停止计时器 并且移除它
//                                    lobby.timer.stopTimer();
//                                    removeLobby(lobby);
//                                }
//                            }
                    //如果是一个常住房间，重新开启它

//                            if (m_permanentLobbies.containsKey("#mp_" + partM.group(2))) {
//                                Lobby lobby = m_permanentLobbies.get("#mp_" + partM.group(2)).lobby;
//                                m_permanentLobbies.get("#mp_" + partM.group(2)).stopped = true;
//                                m_permanentLobbies.remove("#mp_" + partM.group(2));
//                                createNewLobby(lobby.name, lobby.minDifficulty, lobby.maxDifficulty, lobby.creatorName,
//                                        lobby.OPLobby, true);
//                            }
//                        }
//                    }
                } else {
                    logger.info("出现了不能理解的消息：" + msg);
                }
            }
        }


//        Pattern channel = Pattern.compile(":(.+)!cho@ppy.sh PRIVMSG (.+) :(.+)");
//        Matcher channelmatch = channel.matcher(line);
//        if (channelmatch.find()) {
//            // :AutoHost!cho@ppy.sh PRIVMSG #lobby :asd
//            String user = channelmatch.group(1);
//            String target = channelmatch.group(2);
//            String message = channelmatch.group(3);
//            if (target.startsWith("#")) {
//                new ChannelMessageHandler(this).handle(target, user, message);
//            } else {
//                if (LOCK_NAME != null && !user.equalsIgnoreCase(LOCK_NAME) && !user.equalsIgnoreCase("BanchoBot")) {
//                    m_client.sendMessage(user, "hypex is currently testing / fixing AutoHost. "
//                            + "He'll announce in the [https://discord.gg/UDabf2y AutoHost Discord] when he's done");
//                } else {
//                    new PrivateMessageHandler(this).handle(user, message);
//                }
//            }
//        }
//
    }
}
