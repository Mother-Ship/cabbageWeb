package top.mothership.cabbage.service;

import top.mothership.cabbage.pojo.CqMsg;

public interface CqService {
    void praseCmd(CqMsg cqMsg);
    void praseAdminCmd(CqMsg cqMsg);
    void praseNewsPaper(CqMsg cqMsg);
    void stashInviteRequest(CqMsg cqMsg);
}
