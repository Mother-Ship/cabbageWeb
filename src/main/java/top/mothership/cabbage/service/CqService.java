package top.mothership.cabbage.service;

import top.mothership.cabbage.pojo.CqMsg;

public interface CqService {
    void praseMsg(CqMsg cqMsg);
    void praseNewsPaper(CqMsg cqMsg);
    void stashInviteRequest(CqMsg cqMsg);
}
