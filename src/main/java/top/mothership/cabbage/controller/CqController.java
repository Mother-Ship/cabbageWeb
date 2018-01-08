package top.mothership.cabbage.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.mothership.cabbage.consts.PatternConsts;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.serviceImpl.AnalyzeServiceImpl;
import top.mothership.cabbage.serviceImpl.CqAdminServiceImpl;
import top.mothership.cabbage.serviceImpl.CqServiceImpl;
import top.mothership.cabbage.serviceImpl.MpServiceImpl;
import top.mothership.cabbage.util.qq.SmokeUtil;

import javax.annotation.PostConstruct;
import java.util.Locale;
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
    private final CqAdminServiceImpl cqAdminService;
    private final MpServiceImpl mpService;
    private final CqManager cqManager;
    private final AnalyzeServiceImpl analyzeService;
    private Logger logger = LogManager.getLogger(this.getClass());

    /**
     * Spring构造方法自动注入
     * @param cqService      Service层
     * @param smokeUtil      负责禁言的工具类
     * @param cqAdminService
     * @param mpService
     * @param cqManager
     * @param analyzeService
     */
    @Autowired
    public CqController(CqServiceImpl cqService, SmokeUtil smokeUtil, CqAdminServiceImpl cqAdminService, MpServiceImpl mpService, CqManager cqManager, AnalyzeServiceImpl analyzeService) {
        this.cqService = cqService;
        this.smokeUtil = smokeUtil;
        this.cqAdminService = cqAdminService;
        this.mpService = mpService;
        this.cqManager = cqManager;
        this.analyzeService = analyzeService;
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
                //识别消息类型，根据是否是群聊，加入禁言消息队列
                Matcher cmdMatcher = PatternConsts.REG_CMD_REGEX.matcher(msgWithoutImage);
                switch (cqMsg.getMessageType()) {
                    case "group":
                        smokeUtil.parseSmoke(cqMsg);
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
                    switch (cmdMatcher.group(1).toLowerCase(Locale.CHINA)) {
                        //处理命令
                        case "sudo":
                            cmdMatcher = PatternConsts.ADMIN_CMD_REGEX.matcher(msg);
                            cmdMatcher.find();
                            //无视命令大小写
                            switch (cmdMatcher.group(1).toLowerCase(Locale.CHINA)) {
                                case "add":
                                    if ("".equals(cmdMatcher.group(2)) || "".equals(cmdMatcher.group(3))) {
                                        cqMsg.setMessage("参数错误。");
                                        cqManager.sendMsg(cqMsg);
                                        return;
                                    }
                                    cqAdminService.addUserRole(cqMsg);
                                    break;
                                case "del":
                                    if ("".equals(cmdMatcher.group(2))) {
                                        cqMsg.setMessage("参数错误。");
                                        cqManager.sendMsg(cqMsg);
                                        return;
                                    }
                                    cqAdminService.delUserRole(cqMsg);
                                    break;
                                case "check":
                                case "checku":
                                case "checkq":
                                    if ("".equals(cmdMatcher.group(2))) {
                                        cqMsg.setMessage("参数错误。");
                                        cqManager.sendMsg(cqMsg);
                                        return;
                                    }
                                    cqAdminService.getUserRole(cqMsg);
                                    break;
                                case "褪裙":
                                case "退群":
                                    if ("".equals(cmdMatcher.group(2))) {
                                        cqMsg.setMessage("参数错误。");
                                        cqManager.sendMsg(cqMsg);
                                        return;
                                    }
                                    cqAdminService.listPPOverflow(cqMsg);
                                    break;
                                case "bg":
                                    if ("".equals(cmdMatcher.group(2)) || "".equals(cmdMatcher.group(3))) {
                                        cqMsg.setMessage("参数错误。");
                                        cqManager.sendMsg(cqMsg);
                                        return;
                                    }
                                    cqAdminService.addComponent(cqMsg);
                                    break;
                                case "recent":
                                case "rs":
                                    if ("".equals(cmdMatcher.group(2))) {
                                        cqMsg.setMessage("参数错误。");
                                        cqManager.sendMsg(cqMsg);
                                        return;
                                    }
                                    cqAdminService.recent(cqMsg);
                                    break;
                                case "afk":
                                    cqAdminService.checkAfkPlayer(cqMsg);
                                    break;
                                case "smoke":
                                    if ("".equals(cmdMatcher.group(2))) {
                                        cqMsg.setMessage("参数错误。");
                                        cqManager.sendMsg(cqMsg);
                                        return;
                                    }
                                    cqAdminService.smoke(cqMsg);
                                    break;
                                case "listinvite":
                                    cqAdminService.listInvite(cqMsg);
                                    break;
                                case "handleinvite":
                                    cqAdminService.handleInvite(cqMsg);
                                    break;
                                case "clearinvite":
                                    CqAdminServiceImpl.request.clear();
                                    cqMsg.setMessage("清除列表成功");
                                    cqManager.sendMsg(cqMsg);
                                    return;
                                case "unbind":
                                    cqAdminService.unbind(cqMsg);
                                    break;
                                case "fp":
                                    if ("".equals(cmdMatcher.group(2))) {
                                        cqMsg.setMessage("参数错误。");
                                        cqManager.sendMsg(cqMsg);
                                        return;
                                    }
                                    cqAdminService.firstPlace(cqMsg);
                                    break;
                                case "listmsg":
                                    if ("".equals(cmdMatcher.group(2))) {
                                        cqMsg.setMessage("参数错误。");
                                        cqManager.sendMsg(cqMsg);
                                        return;
                                    }
                                    cqAdminService.listMsg(cqMsg);
                                    break;
                                case "pp":
                                    cqAdminService.listUserPP(cqMsg);
                                    break;
                                case "findplayer":
                                    cqAdminService.findPlayer(cqMsg);
                                    break;
                                case "scancard":
                                    cqAdminService.scanCard(cqMsg);
                                    break;
                                case "checkgroupbind":
                                    cqAdminService.checkGroupBind(cqMsg);
                                    break;
                                case "help":
                                    cqAdminService.help(cqMsg);
                                    break;
                                case "repeatstar":
                                    cqAdminService.getRepeatStar(cqMsg);
                                    break;
                                case "checkroleban":
                                    cqAdminService.checkRoleBan(cqMsg);
                                default:
                                    break;
                            }
                            break;
                        case "mp":
                            cmdMatcher = PatternConsts.MP_CMD_REGEX.matcher(msg);
                            cmdMatcher.find();
                            switch (cmdMatcher.group(1).toLowerCase(Locale.CHINA)) {
                                case "rs":
                                    //rs命令必须指定开始时间
                                    if ("".equals(cmdMatcher.group(2))) {
                                        return;
                                    }
                                case "make":
                                    mpService.reserveLobby(cqMsg);
                                    break;
                                case "invite":
                                    if ("".equals(cmdMatcher.group(2))) {
                                        return;
                                    }
                                    mpService.invitePlayer(cqMsg);
                                    break;
                                case "list":
                                    mpService.listLobby(cqMsg);
                                    break;
                                case "abort":
                                    mpService.abortReserve(cqMsg);
                                    break;
                                case "join":
                                    mpService.joinLobby(cqMsg);
                                    break;
                                case "add":
                                    mpService.addMap(cqMsg);
                                    break;
                                case "del":
                                    mpService.delMap(cqMsg);
                                    break;
                                case "listmap":
                                    mpService.listMap(cqMsg);
                                    break;
                                case "help":
                                    mpService.help(cqMsg);
                                default:
                                    break;
                            }
                            break;
                        default:
                            Matcher recentQianeseMatcher = PatternConsts.QIANESE_RECENT.matcher(cmdMatcher.group(1).toLowerCase(Locale.CHINA));
                            if(recentQianeseMatcher.find()){
                                cqService.recent(cqMsg);
                            }
                            switch (cmdMatcher.group(1).toLowerCase(Locale.CHINA)) {
                                case "stat":
                                case "statu":
                                case "statme":
                                    cqService.statUserInfo(cqMsg);
                                    break;
                                case "bp":
                                case "bpu":
                                case "bps":
                                case "bpus":
                                case "bpme":
                                case "mybp":
                                case "mybps":
                                case "bpmes":
                                    Matcher cmdMatcherWithNum = PatternConsts.REG_CMD_REGEX_NUM_PARAM.matcher(msg);
                                    if (cmdMatcherWithNum.find()) {
                                        //如果有指定#n
                                        cqService.printSpecifiedBP(cqMsg);
                                    } else {
                                        cqService.printBP(cqMsg);
                                    }
                                    break;
                                case "setid":
                                    cqService.setId(cqMsg);
                                    break;
                                case "rs":
                                    cqService.recent(cqMsg);
                                    break;
                                case "help":
                                    cqService.help(cqMsg);
                                    break;
                                case "sleep":
                                    cqService.sleep(cqMsg);
                                    break;
                                case "fp":
                                    cqService.firstPlace(cqMsg);
                                    break;
                                case "add":
                                case "del":
                                    cqService.chartMemberCmd(cqMsg);
                                    break;
                                case "me":
                                    cqService.myScore(cqMsg);
                                    break;
                                case "search":
                                    cqService.search(cqMsg);
                                    break;
                                case "costme":
                                case "mycost":
                                case "cost":
                                    cqService.cost(cqMsg);
                                    break;
                                case "pr":
                                case "prs":
                                    cqService.recentPassed(cqMsg);
                                    break;
                                case "addmap":
                                    analyzeService.addTargetMap(cqMsg);
                                    break;
                                case "delmap":
                                    analyzeService.delTargetMap(cqMsg);
                                    break;
                                case "delallmap":
                                    analyzeService.delAllTargetMap(cqMsg);
                                    break;
                                case "listmap":
                                    analyzeService.listTargetMap(cqMsg);
                                    break;
                                case "adduser":
                                    analyzeService.addTargetUser(cqMsg);
                                    break;
                                case "deluser":
                                    analyzeService.delTargetUser(cqMsg);
                                    break;
                                case "delalluser":
                                    analyzeService.delAllTargetUser(cqMsg);
                                    break;
                                case "listuser":
                                    analyzeService.listTargetUser(cqMsg);
                                default:
                                    break;

                            }
                            break;
                    }

                }
                break;
            case "event":
                if ("group_increase".equals(cqMsg.getEvent())) {
                    //新增人口
                    cqService.welcomeNewsPaper(cqMsg);
                }
                if ("group_admin".equals(cqMsg.getEvent())) {
                    //群管变动
                    smokeUtil.loadGroupAdmins();
                }
                break;
            case "request":
                //只有是加群请求的时候才进入
                if ("group".equals(cqMsg.getRequestType()) && "invite".equals(cqMsg.getSubType())) {
                    cqAdminService.stashInviteRequest(cqMsg);
                }
                break;
            default:
                logger.error("传入无法识别的Request，可能是HTTP API插件已经更新");
        }
    }

    @PostConstruct
    private void notifyInitComplete() {
        CqMsg cqMsg = new CqMsg();
        cqMsg.setMessageType("private");
        cqMsg.setUserId(1335734657L);
        cqMsg.setMessage("初始化完成，欢迎使用");
        cqManager.sendMsg(cqMsg);
    }
}
