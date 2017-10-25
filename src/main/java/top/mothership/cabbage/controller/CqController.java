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
import top.mothership.cabbage.util.Constant;
import top.mothership.cabbage.util.SmokeUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RestController
public class CqController {


    private final CqServiceImpl cqService;
    private final SmokeUtil smokeUtil;
    private Logger logger = LogManager.getLogger(this.getClass());

    private Matcher m;

    @Autowired
    public CqController(CqServiceImpl cqService, SmokeUtil smokeUtil) {
        this.cqService = cqService;
        this.smokeUtil = smokeUtil;
    }

    @RequestMapping(value = "/cqAPI", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String cqMsgPrase(@RequestBody CqMsg cqMsg){
        String cmdRegex = Constant.MAIN_FILTER_REGEX;
        //待整理业务逻辑
        switch (cqMsg.getPostType()){
            case "message":
                //转义
                String msg  = cqMsg.getMessage();
                msg = msg.replaceAll("&#91;", "[");
                msg = msg.replaceAll("&#93;", "]");
                msg = msg.replaceAll("&#44;", ",");
                cqMsg.setMessage(msg);
                String msgWithoutImage;
                if (msg.matches(Constant.IMG_REGEX)) {
                    msgWithoutImage = msg.replaceAll(Constant.SINGLE_IMG_REGEX, "");
                } else {
                    msgWithoutImage = msg;
                }
                //识别消息类型
                switch (cqMsg.getMessageType()) {
                    case "group":
                            //直接将群消息传入禁言处理
                            //增加一个时间戳（划掉）插件自带了Time
//                            cqMsg.setTime(Calendar.getInstance().getTimeInMillis());
                            smokeUtil.praseSmoke(cqMsg);
                        if (msgWithoutImage.matches(cmdRegex)) {
                            logger.info("开始处理群" + cqMsg.getGroupId() + "的成员" + cqMsg.getUserId() + "发送的命令");
                        }
                        break;

                    case "discuss":
                        if (msgWithoutImage.matches(cmdRegex)) {
                            logger.info("开始处理讨论组" + cqMsg.getDiscussId() + "的成员" + cqMsg.getUserId() + "发送的命令");
                        }
                        break;
                    case "private":
                        //如果是私聊消息，覆盖掉正则表达式（识别汉字）
                        //不必考虑线程安全问题，每次进入这个方法，cmdRegex都会被重置为没有汉字的版本
                        cmdRegex = Constant.MAIN_FILTER_REGEX_CHINESE;
                        if (msgWithoutImage.matches(cmdRegex)) {
                            logger.info("开始处理" + cqMsg.getUserId() + "发送的命令");
                        }
                        break;
                }
                if (msgWithoutImage.matches(cmdRegex)) {
                    //如果检测到命令，直接把消息中的图片去掉

                    cqMsg.setMessage(msgWithoutImage);
                    m = Pattern.compile(cmdRegex).matcher(msgWithoutImage);
                    m.find();
                    switch (m.group(1)) {
                        //处理命令
                        case "sudo":
                            cqService.praseAdminCmd(cqMsg);
                            break;
                        default:
                            cqService.praseCmd(cqMsg);
                            break;
                    }

                }
                break;
            case "event":
                if(cqMsg.getEvent().equals("group_increase")) {
                    cqService.praseNewsPaper(cqMsg);
                }
                if(cqMsg.getEvent().equals("group_admin")){
                    logger.info("检测到群管变动："+cqMsg.getUserId()+"，操作为"+cqMsg.getSubType());
                    smokeUtil.loadGroupAdmins();
                }
                break;
            case "request":
                //只有是加群请求的时候才进入
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
