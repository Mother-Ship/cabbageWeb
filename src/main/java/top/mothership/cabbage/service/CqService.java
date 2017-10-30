package top.mothership.cabbage.service;

import top.mothership.cabbage.pojo.CqMsg;

public interface CqService {
    void praseCmd(CqMsg cqMsg) throws Exception;
    void praseAdminCmd (CqMsg cqMsg) throws Exception;
    void praseNewsPaper(CqMsg cqMsg);
    void stashInviteRequest(CqMsg cqMsg);
}
