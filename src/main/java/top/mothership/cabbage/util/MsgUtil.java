package top.mothership.cabbage.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.mapper.BaseMapper;
import top.mothership.cabbage.pojo.CqMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class MsgUtil {
    private static String cmdRegex = "[!！]([^ ]+)([^]*)";
    private static String cmdRegexWithNum = "[!！]([^ ]+)([^#]*) #(.+)";
    private final ApiUtil apiUtil;
    private final CqUtil cqUtil;
    private final ImgUtil imgUtil;
    private final WebPageUtil webPageUtil;
    private BaseMapper baseMapper;
    private Logger logger = LogManager.getLogger(this.getClass());
    private static String singleImgRegex = "\\[CQ:image,file=(.+)\\]";
    private static String[] msgs = new String[100];
    private static int start = 0;
    private static int end = 0;
    private static int len = 0;
    private static List<Long> mp5Admin = Arrays.asList(2643555740L, 290514894L, 2307282906L, 2055805091L, 735862173L,
            1142592265L, 263202941L, 992931505L, 1335734657L, 526942417L, 1012621328L);
    private static List<Long> mp4Admin = Arrays.asList(89217167L,295539897L,290514894L,2307282906L,
            2643555740L,2055805091L,954176984L,879804833L,526942417L);
    private static List<CqMsg> inviteRequests = new ArrayList<>();

    @Autowired
    public MsgUtil(ApiUtil apiUtil, CqUtil cqUtil, ImgUtil imgUtil, WebPageUtil webPageUtil, BaseMapper baseMapper) {
        this.apiUtil = apiUtil;
        this.cqUtil = cqUtil;
        this.imgUtil = imgUtil;
        this.webPageUtil = webPageUtil;
        this.baseMapper = baseMapper;
    }

    public void AdminCmd(CqMsg cqMsg){

    }
    public void CommonCmd(CqMsg cqMsg){

    }
    public void stashInviteRequest(CqMsg cqMsg){
        //等待日后调用
        inviteRequests.add(cqMsg);
    }
    public void Smoke(CqMsg cqMsg){
        //这里拿到的是没有刮去图片的
        int count = 0;
        String msg = cqMsg.getMessage();
        if(msg.matches(singleImgRegex)){
            msg = "Image";
        }
        //刮掉除了中文英文数字之外的东西
        msg = msg.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        //循环数组
        if (cqMsg.getGroupId()==201872650 || cqMsg.getGroupId()==564679329 ||cqMsg.getGroupId()==532783765) {
            len++;
            if (len >= 100) {
                len = 100;
                start++;
            }
            if (end == 100) {
                end = 0;
            }
            if (start == 100) {
                start = 0;
            }
            //把群号拼在字符串上
            msgs[end] = cqMsg.getGroupId() + msg;
            end++;
            if (start < end) {
                //复读不抓三个字以下的和纯图片
                for (int i = 0; i < end; i++) {
                    if ((cqMsg.getGroupId() + msg).equals(msgs[i])&&!msg.equals("Image") && msg.length() >= 3) {
                        count++;
                    }
                }
            } else {
                for (int i = 0; i < start - 1; i++) {
                    if ((cqMsg.getGroupId() + msg).equals(msgs[i])&&!msg.equals("Image")  && msg.length() >= 3) {
                        count++;
                    }
                }
                for (int i = end; i < msgs.length; i++) {
                    if ((cqMsg.getGroupId() + msg).equals(msgs[i])&&!msg.equals("Image") && msg.length() >= 3) {
                        count++;
                    }
                }
            }

        }
        if (count >= 6) {
            String resp;
            if (mp5Admin.contains(cqMsg.getUserId())&&cqMsg.getGroupId()==201872650) {
                logger.info("检测到群管" + cqMsg.getUserId()+ "的复读行为");
                cqMsg.setMessage("[CQ:at,qq=2643555740] 检测到群管" + "[CQ:at,qq=" + cqMsg.getUserId() + "] 复读。");

            } else if(mp4Admin.contains(cqMsg.getUserId())&&cqMsg.getGroupId()==564679329){
                logger.info("检测到群管" + cqMsg.getUserId() + "的复读行为");
                cqMsg.setMessage("[CQ:at,qq=1012621328] 检测到群管" + "[CQ:at,qq=" + cqMsg.getUserId() + "] 复读。");
            } else {
                logger.info("正在尝试禁言" + cqMsg.getUserId());
                cqMsg.setDuration(600);
                cqMsg.setMessageType("smoke");
            }
            cqUtil.sendMsg(cqMsg);
        }
    }

}
