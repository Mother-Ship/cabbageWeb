package top.mothership.cabbage.serviceImpl;

import org.springframework.stereotype.Service;
import top.mothership.cabbage.mapper.LobbyDAO;

@Service
public class MpServiceImpl {
    private final LobbyDAO lobbyDAO;

    public MpServiceImpl(LobbyDAO lobbyDAO) {
        this.lobbyDAO = lobbyDAO;
    }


}
