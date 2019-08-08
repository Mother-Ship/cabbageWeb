package top.mothership.cabbage.service.command;

import top.mothership.cabbage.pojo.coolq.CqMsg;

public interface CommandHandler {
    void setNext(CommandHandler next);
    void handle(CqMsg msg);
}
