package top.mothership.cabbage.serviceImpl;

import org.springframework.stereotype.Service;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.mapper.LobbyDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;

@Service
public class MpServiceImpl {
    private final LobbyDAO lobbyDAO;
    private final ApiManager apiManager;
    public MpServiceImpl(LobbyDAO lobbyDAO, ApiManager apiManager) {
        this.lobbyDAO = lobbyDAO;
        this.apiManager = apiManager;
    }

    public void reserveLobby(CqMsg cqMsg){

    }

    public void createLobby(CqMsg cqMsg){

    }
    public void invitePlayer(CqMsg cqMsg){

    }
    public void listLobby(CqMsg cqMsg){

    }

    public void abortReserve(CqMsg cqMsg){

    }
    public void joinLobby(CqMsg cqMsg){

    }
    public void addMap(CqMsg cqMsg){

    }
    public void delMap(CqMsg cqMsg){

    }
    public void listMap(CqMsg cqMsg){

    }
}
