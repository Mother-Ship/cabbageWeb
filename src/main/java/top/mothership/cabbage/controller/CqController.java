package top.mothership.cabbage.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.mothership.cabbage.consts.PatternConsts;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.serviceImpl.CqServiceImpl;
import top.mothership.cabbage.util.qq.CmdUtil;
import top.mothership.cabbage.util.qq.SmokeUtil;

import java.util.regex.Matcher;

/**
 * CQ控制器，用于处理CQ消息
 *
 * @author QHS
 */
@RestController
public class CqController {


    private final CqServiceImpl cqService;
    private final SmokeUtil smokeUtil;
    private final CmdUtil cmdUtil;
    private Logger logger = LogManager.getLogger(this.getClass());

    /**
     * Spring构造方法自动注入
     *
     * @param cqService Service层
     * @param smokeUtil 负责禁言的工具类
     * @param cmdUtil   the cmd util
     */
    @Autowired
    public CqController(CqServiceImpl cqService, SmokeUtil smokeUtil, CmdUtil cmdUtil) {
        this.cqService = cqService;
        this.smokeUtil = smokeUtil;
        this.cmdUtil = cmdUtil;
    }

    /**
     * Controller主方法
     *
     * @param cqMsg 传入的QQ消息
     * @throws Exception 抛出异常给AOP检测
     */
    @RequestMapping(value = "/cqAPI", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public void cqMsgParse(@RequestBody CqMsg cqMsg) throws Exception {
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
                Matcher imageMatcher = PatternConsts.IMG_REGEX.matcher(msg);
                if (imageMatcher.find()) {
                    //替换掉消息内所有图片
                    msgWithoutImage = imageMatcher.replaceAll("");
                } else {
                    msgWithoutImage = msg;
                }
                //识别消息类型，根据是否私聊使用不同的表达式
                Matcher cmdMatcher = PatternConsts.MAIN_FILTER_REGEX.matcher(msgWithoutImage);
                switch (cqMsg.getMessageType()) {
                    case "group":
                        smokeUtil.parseSmoke(cqMsg);
                        break;
                    case "private":
                        //如果是私聊消息，覆盖掉正则表达式（识别汉字）
                        //不必考虑线程安全问题，每次进入这个方法，cmdRegex都会被重置为没有汉字的版本
                        cmdMatcher = PatternConsts.MAIN_FILTER_REGEX_CHINESE.matcher(msgWithoutImage);
                        break;
                    default:
                        break;
                }
                if (cmdMatcher.find()) {
                    //如果检测到命令，直接把消息中的图片去掉，避免Service层进行后续处理
                    cqMsg.setMessage(msgWithoutImage);
                    String log = "";
                    switch (cqMsg.getMessageType()) {
                        case "group":
                            log += "群" + cqMsg.getGroupId() + "成员" + cqMsg.getUserId() + "发送了命令" + cqMsg.getMessage();
                            break;
                        case "discuss":
                            log += "讨论组" + cqMsg.getDiscussId() + "成员" + cqMsg.getUserId() + "发送了命令" + cqMsg.getMessage();
                            break;
                        case "private":
                            log += "用户" + cqMsg.getUserId() + "发送了命令" + cqMsg.getMessage();
                            break;
                        default:
                            break;
                    }
                    logger.info(log);
                    switch (cmdMatcher.group(1)) {
                        //处理命令
                        case "sudo":
                            cmdMatcher = PatternConsts.ADMIN_CMD_REGEX.matcher(msg);
                            switch (cmdMatcher.group(1)) {
                                case ""
                                default:
                                    break;

                            }
                            break;
                        default:
                            cmdMatcher = PatternConsts.CMD_REGEX_NUM.matcher(msg);
                            switch (cmdMatcher.group(1)) {
                                case "stat":
                                case "statu":
                                    if ("".equals(cmdMatcher.group(2))) {
                                        return;
                                    }
                                case "statme":
                                    cqService.statUserInfo(cqMsg);
                                break;
                                case "bp":
                                case "bps":
                                case "bpu":
                                case "bpus":
                                case "bpme":
                                case "bpmes":

                                    break;
                                case "setid":
                                    break;
                                case "recent":
                                case "rs":
                                    break;
                                case "help":
                                    break;
                                case "sleep":
                                    break;
                                case "fp":
                                    break;
                                case "roll":
                                    break;
                                case "add":
                                    break;
                                case "del":
                                    break;
                                default:
                                    break;

                            }
                            break;
                    }

                }
                break;
            case "event":
                if ("group_increase".equals(cqMsg.getEvent())) {
                    cmdUtil.praseNewsPaper(cqMsg);
                }
                if ("group_admin".equals(cqMsg.getEvent())) {
                    smokeUtil.loadGroupAdmins();
                }
                break;
            case "request":
                //只有是加群请求的时候才进入
                if ("group".equals(cqMsg.getRequestType()) && "invite".equals(cqMsg.getSubType())) {
                    cmdUtil.stashInviteRequest(cqMsg);
                }
                break;
            default:
                logger.error("传入无法识别的Request，可能是HTTP API插件已经更新");
        }
    }

}
