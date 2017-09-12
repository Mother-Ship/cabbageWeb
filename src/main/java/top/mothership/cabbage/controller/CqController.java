package top.mothership.cabbage.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.mothership.cabbage.pojo.CqMsg;
import top.mothership.cabbage.serviceImpl.CqServiceImpl;


@RestController
public class CqController {
    @Autowired
    private CqServiceImpl cqService;
    private Logger logger = LogManager.getLogger(this.getClass());
    @RequestMapping(value = "/cqAPI", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String cqMsgPrase(@RequestBody CqMsg cqMsg){
        //待整理业务逻辑
        switch (cqMsg.getPostType()){
            case "message":
                cqService.praseMsg(cqMsg);
                break;
            case "event":
                if(cqMsg.getEvent().equals("group_increase")) {
                    cqService.praseNewsPaper(cqMsg);
                }
                break;
            case "request":
                if(cqMsg.getRequestType().equals("group")&&cqMsg.getSubType().equals("invite")){
                    logger.info("已将"+cqMsg.getUserId()+"将白菜邀请入"+cqMsg.getGroupId()+"的请求进行暂存");
                    cqService.stashInviteRequest(cqMsg);
                }
                break;
            default:
                logger.error("传入无法识别的Request，可能是HTTPAPI插件已经更新");

        }
        //先写返回null吧，如果以后有直接返回的逻辑也可以直接return
        return null;
    }

}
