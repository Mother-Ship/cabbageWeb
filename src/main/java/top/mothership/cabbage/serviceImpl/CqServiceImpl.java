package top.mothership.cabbage.serviceImpl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.pojo.CqMsg;
import top.mothership.cabbage.service.CqService;
import top.mothership.cabbage.util.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CqServiceImpl implements CqService {
    private static String mainRegex = "[!！]([^ \\u4e00-\\u9fa5]+)([\\u892a\\u88d9\\u9000\\u7fa4\\u767d\\u83dcA-Za-z0-9\\[\\] :#-_]*+)";
    private static String imgRegex = ".*\\[CQ:image,file=(.+)\\].*";
    private static String singleImgRegex = "\\[CQ:image,file=(.+)\\]";

    private Logger logger = LogManager.getLogger(this.getClass());
    private ResourceBundle rb = ResourceBundle.getBundle("cabbage");
    private Matcher m;
    private Date start;

    private MsgUtil msgUtil;
    private CqUtil cqUtil;
    @Autowired
    public CqServiceImpl(MsgUtil msgUtil, CqUtil cqUtil) {
        this.msgUtil = msgUtil;
        this.cqUtil = cqUtil;
    }


    @Override
    public void praseMsg(CqMsg cqMsg) {
        //先去掉图片
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
                    msgUtil.Smoke(cqMsg);
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
                    msgUtil.AdminCmd(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - start.getTime()) + "ms。");
                    break;
                default:
                    msgUtil.CommonCmd(cqMsg);
                    logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - start.getTime()) + "ms。");
                    break;
            }

        }

    }

    @Override
    public void praseNewsPaper(CqMsg cqMsg) {
        start = Calendar.getInstance().getTime();
        logger.info("开始处理"+cqMsg.getUserId()+"在"+cqMsg.getGroupId()+"群的加群请求");
        String resp;
        switch (String.valueOf(cqMsg.getGroupId())){
            case "201872650":
                resp = "[CQ:at,qq=" +cqMsg.getUserId()+"]，欢迎来到mp5。请修改一下你的群名片(包含osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                break;
            case "564679329":
                resp = "[CQ:at,qq=" +cqMsg.getUserId()+"]，欢迎来到mp4。请修改一下你的群名片(包含osu! id)，并读一下置顶的群规。另外欢迎参加mp群系列活动Chart(详见公告)，成绩高者可以赢取奖励。";
                break;
            case "537646635":
                resp = "[CQ:at,qq=" +cqMsg.getUserId()+"]，欢迎来到mp乐园主群。请修改一下你的群名片(包含osu! id)，以下为mp乐园系列分群介绍：\n" +
                        "osu!mp乐园高rank部 592339532\n" +
                        "osu!mp乐园3号群(四位数7000pp上限):234219559\n" +
                        "OSU! MP乐园4号群 (MP4) *(3600-5100pp):564679329\n" +
                        "OSU! MP乐园5号群 (MP5) *(2500-4000pp，无严格下限):201872650";
                break;
            default:
                resp = "[CQ:at,qq="+cqMsg.getUserId()+"]，欢迎加入本群。";
                break;
        }

        cqMsg.setMessageType("group");
        cqMsg.setMessage(resp);
        cqUtil.sendMsg(cqMsg);
        logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - start.getTime()) + "ms。");
    }

    @Override
    public void stashInviteRequest(CqMsg cqMsg) {
        start = Calendar.getInstance().getTime();
        msgUtil.stashInviteRequest(cqMsg);
        logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - start.getTime()) + "ms。");
    }


}
