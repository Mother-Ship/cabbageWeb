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
    public void cqMsgPrase(@RequestBody CqMsg cqMsg){
        //待整理业务逻辑
        switch (cqMsg.getPostType()){
            case "message":
                cqService.praseMsg(cqMsg);
                break;
            case "event":
                cqService.praseNewsPaper(cqMsg);
                break;
            case "request":
                cqService.praseGroupInvite(cqMsg);
                break;
            default:
                logger.error("传入无法识别的Request，可能是HTTPAPI插件已经更新");

        }
    }

}
