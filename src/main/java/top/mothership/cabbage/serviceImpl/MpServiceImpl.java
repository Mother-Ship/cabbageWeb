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

//    @Scheduled(cron = "0 * * * * ? ")
//    public void scanLobby(){
//
//    }

}
