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

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RestController
public class CqController {
    @Autowired
    private CqServiceImpl cqService;
    private Logger logger = LogManager.getLogger(this.getClass());
    private static String mainRegex = "[!！]([^ \\u4e00-\\u9fa5]+)([\\u892a\\u88d9\\u9000\\u7fa4\\u767d\\u83dcA-Za-z0-9\\[\\] :#-_]*+)";
    private static String imgRegex = ".*\\[CQ:image,file=(.+)\\].*";
    private static String singleImgRegex = "\\[CQ:image,file=(.+)\\]";
    private Date start;
    private Matcher m;
    @RequestMapping(value = "/cqAPI", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String cqMsgPrase(@RequestBody CqMsg cqMsg){
        //待整理业务逻辑
        switch (cqMsg.getPostType()){
            case "message":
                start = Calendar.getInstance().getTime();
                String msgWithoutImage;
                String msg = cqMsg.getMessage();
                if (msg.matches(imgRegex)) {
                    msgWithoutImage = msg.replaceAll(singleImgRegex, "");
                } else {
                    msgWithoutImage = msg;
                }
                //识别消息类型
                switch (cqMsg.getMessageType()) {
                    case "group":
                        //如果去掉图片之后的消息是一条命令
                        if (msgWithoutImage.matches(mainRegex)) {
                            logger.info("开始处理群" + cqMsg.getGroupId() + "的成员" + cqMsg.getUserId() + "发送的命令");
                        } else {
                            //否则将消息传入禁言处理方法（只有群消息才会进）
                            cqService.praseSmoke(cqMsg);
                        }
                        break;
                    case "discuss":
                        if (msgWithoutImage.matches(mainRegex)) {
                            logger.info("开始处理讨论组" + cqMsg.getDiscussId() + "的成员" + cqMsg.getUserId() + "发送的命令");
                        }
                        break;
                    case "private":
                        //如果是私聊消息，覆盖掉正则表达式（识别汉字）
                        mainRegex = "[!！]([^ \\u4e00-\\u9fa5]+)(.*+)";
                        if (msgWithoutImage.matches(mainRegex)) {
                            logger.info("开始处理" + cqMsg.getUserId() + "发送的命令");
                        }
                        break;
                }
                if (msgWithoutImage.matches(mainRegex)) {
                    //如果检测到命令，直接把消息中的图片去掉
                    cqMsg.setMessage(msgWithoutImage);
                    m = Pattern.compile(mainRegex).matcher(msgWithoutImage);
                    m.find();
                    switch (m.group(1)) {
                        //处理命令
                        case "sudo":
                            cqService.praseAdminCmd(cqMsg);
                            logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - start.getTime()) + "ms。");
                            break;
                        default:
                            cqService.praseCmd(cqMsg);
                            logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - start.getTime()) + "ms。");
                            break;
                    }

                }
                break;
            case "event":
                if(cqMsg.getEvent().equals("group_increase")) {
                    start = Calendar.getInstance().getTime();
                    cqService.praseNewsPaper(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - start.getTime()) + "ms。");
                }
                break;
            case "request":
                if(cqMsg.getRequestType().equals("group")&&cqMsg.getSubType().equals("invite")){
                    start = Calendar.getInstance().getTime();
                    logger.info("已将"+cqMsg.getUserId()+"将白菜邀请入"+cqMsg.getGroupId()+"的请求进行暂存");
                    cqService.stashInviteRequest(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - start.getTime()) + "ms。");
                }
                break;
            default:
                logger.error("传入无法识别的Request，可能是HTTPAPI插件已经更新");

        }
        //先写返回null吧，如果以后有直接返回的逻辑也可以直接return
        return null;
    }

}
