package top.mothership.cabbage.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.serviceImpl.CqServiceImpl;
import top.mothership.cabbage.util.Overall;
import top.mothership.cabbage.util.qq.CmdUtil;
import top.mothership.cabbage.util.qq.SmokeUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RestController
public class CqController {


    private final CqServiceImpl cqService;
    private final SmokeUtil smokeUtil;
    private final CmdUtil cmdUtil;
    private Logger logger = LogManager.getLogger(this.getClass());

    private Matcher m;

    @Autowired
    public CqController(CqServiceImpl cqService, SmokeUtil smokeUtil, CmdUtil cmdUtil) {
        this.cqService = cqService;
        this.smokeUtil = smokeUtil;
        this.cmdUtil = cmdUtil;
    }

    @RequestMapping(value = "/cqAPI", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String cqMsgPrase(@RequestBody CqMsg cqMsg) throws Exception {
        String cmdRegex = Overall.MAIN_FILTER_REGEX;
        //待整理业务逻辑
        switch (cqMsg.getPostType()) {
            case "message":
                //转义
                String msg = cqMsg.getMessage();
                msg = msg.replaceAll("&#91;", "[");
                msg = msg.replaceAll("&#93;", "]");
                msg = msg.replaceAll("&#44;", ",");
                cqMsg.setMessage(msg);
                String msgWithoutImage;
                if (msg.matches(Overall.IMG_REGEX)) {
                    msgWithoutImage = msg.replaceAll(Overall.SINGLE_IMG_REGEX, "");
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
                        break;
                    case "discuss":
                        break;
                    case "private":
                        //如果是私聊消息，覆盖掉正则表达式（识别汉字）
                        //不必考虑线程安全问题，每次进入这个方法，cmdRegex都会被重置为没有汉字的版本
                        cmdRegex = Overall.MAIN_FILTER_REGEX_CHINESE;
                        break;
                }
                if (msgWithoutImage.matches(cmdRegex)) {
                    //如果检测到命令，直接把消息中的图片去掉
                    String log = "";
                    switch (cqMsg.getMessageType()) {
                        case "group":
                            log+="群"+cqMsg.getGroupId()+"成员"+cqMsg.getUserId()+"发送了命令"+cqMsg.getMessage();
                            break;
                        case "discuss":
                            log+="讨论组"+cqMsg.getDiscussId()+"成员"+cqMsg.getUserId()+"发送了命令"+cqMsg.getMessage();
                            break;
                        case "private":
                            log+="用户"+cqMsg.getUserId()+"发送了命令"+cqMsg.getMessage();
                            break;
                    }
                    logger.info(log);
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
                if (cqMsg.getEvent().equals("group_increase")) {
                    cmdUtil.praseNewsPaper(cqMsg);
                }
                if (cqMsg.getEvent().equals("group_admin")) {
                    smokeUtil.loadGroupAdmins();
                }
                break;
            case "request":
                //只有是加群请求的时候才进入
                if (cqMsg.getRequestType().equals("group") && cqMsg.getSubType().equals("invite")) {
                    cmdUtil.stashInviteRequest(cqMsg);
                }
                break;
            default:
                logger.error("传入无法识别的Request，可能是HTTPAPI插件已经更新");

        }
        //先写返回null吧，如果以后有直接返回的逻辑也可以直接return
        return null;
    }

}
