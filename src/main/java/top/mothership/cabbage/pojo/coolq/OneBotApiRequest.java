package top.mothership.cabbage.pojo.coolq;

import lombok.Data;

@Data
public class OneBotApiRequest {
    private String echo;
    private String action;
    private CqMsg msg;
}
