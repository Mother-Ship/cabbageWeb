package top.mothership.cabbage.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.CqMsg;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@Component
public class SmokeUtil {
    private static String singleImgRegex = "\\[CQ:image,file=(.+)\\]";
    private static List<Long> mp5Admin = Arrays.asList(2643555740L, 290514894L, 2307282906L, 2055805091L, 735862173L,
            1142592265L, 263202941L, 992931505L, 1335734657L, 526942417L, 1012621328L);
    private static List<Long> mp4Admin = Arrays.asList(89217167L, 295539897L, 290514894L, 2307282906L,
            2643555740L, 2055805091L, 954176984L, 879804833L, 526942417L);
    private static List<Long> mp5S4Admin = Arrays.asList(2643555740L, 2307282906L,  1335734657L,89217167L, 1594504329L,
            290514894L,372427060L, 1012621328L,992931505L,1142592265L);
    private Logger logger = LogManager.getLogger(this.getClass());
    private final CqUtil cqUtil;
    //对每个开启禁言复读的群，创建一个新的queue
    private static MsgQueue mp5Queue = new MsgQueue();
    private static MsgQueue mp4Queue = new MsgQueue();
    private static MsgQueue mp5S4Queue = new MsgQueue();
    private static MsgQueue testQueue = new MsgQueue();
    @Autowired
    public SmokeUtil(CqUtil cqUtil) {
        this.cqUtil = cqUtil;
    }

    public void praseSmoke(CqMsg cqMsg) {
        java.util.Date s = Calendar.getInstance().getTime();
//这里拿到的是没有刮去图片的
        int count = 0;
        String msg = cqMsg.getMessage();
        if (msg.matches(singleImgRegex)) {
            msg = "Image";
        }
        //刮掉除了中文英文数字之外的东西
        msg = msg.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");

        if (cqMsg.getGroupId() == 201872650 ) {
            mp5Queue.addMsg(msg);
            if (mp5Queue.isRepeat()) {
                if (mp5Admin.contains(cqMsg.getUserId())) {
                    logger.info("检测到群管" + cqMsg.getUserId() + "的复读行为");
                    cqMsg.setMessage("[CQ:at,qq=2643555740] 检测到群管" + "[CQ:at,qq=" + cqMsg.getUserId() + "] 复读。");
                }else {
                    logger.info("正在尝试禁言" + cqMsg.getUserId());
                    cqMsg.setDuration(600);
                    cqMsg.setMessageType("smoke");
                }
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
            }
        }
        if(cqMsg.getGroupId() == 564679329){
            mp4Queue.addMsg(msg);
            if(mp4Queue.isRepeat()){
                if (mp4Admin.contains(cqMsg.getUserId())) {
                    logger.info("检测到群管" + cqMsg.getUserId() + "的复读行为");
                    cqMsg.setMessage("[CQ:at,qq=1012621328] 检测到群管" + "[CQ:at,qq=" + cqMsg.getUserId() + "] 复读。");
                }else{
                    logger.info("正在尝试禁言" + cqMsg.getUserId());
                    cqMsg.setDuration(600);
                    cqMsg.setMessageType("smoke");
                }
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
            }
        }
        if(cqMsg.getGroupId() == 677545541){
            mp5S4Queue.addMsg(msg);
            if(mp5S4Queue.isRepeat()){
                if (mp5S4Admin.contains(cqMsg.getUserId())) {
                    logger.info("检测到群管" + cqMsg.getUserId() + "的复读行为");
                    cqMsg.setMessage("[CQ:at,qq=2643555740] 检测到群管" + "[CQ:at,qq=" + cqMsg.getUserId() + "] 复读。");
                }else{
                    logger.info("正在尝试禁言" + cqMsg.getUserId());
                    cqMsg.setDuration(600);
                    cqMsg.setMessageType("smoke");
                }
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
            }
        }
        if(cqMsg.getGroupId() == 532783765){
            testQueue.addMsg(msg);
            if(testQueue.isRepeat()){
                logger.info("正在尝试禁言" + cqMsg.getUserId());
                cqMsg.setDuration(600);
                cqMsg.setMessageType("smoke");
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
            }
        }
    }
}
