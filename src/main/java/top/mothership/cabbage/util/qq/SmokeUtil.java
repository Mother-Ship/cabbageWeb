package top.mothership.cabbage.util.qq;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.coolq.RespData;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class SmokeUtil {

    private final static List<String> REPEAT_SMOKE_GROUP = Arrays.asList((
            "201872650," +//MP5
            "112177148," +//MP4后花园
            "532783765," +//测试群
            "136312506," +//MP5赛群136312506
            "521774765"//ODNL S3
    ).split(","));
    private final static List<String> REPEAT_RECORD_GROUP = Arrays.asList("576214175,532783765".split(","));
    private Logger logger = LogManager.getLogger(this.getClass());
    private final CqManager cqManager;
    private final UserDAO userDAO;
    private final ResDAO resDAO;
    public final static Map<Long, MsgQueue> MSG_QUEUE_MAP = new HashMap<>();



    @Autowired
    public SmokeUtil(CqManager cqManager, UserDAO userDAO, ResDAO resDAO) {
        this.cqManager = cqManager;
        this.userDAO = userDAO;
        this.resDAO = resDAO;
    }


    /**
     * Parse smoke.
     *
     * @param cqMsg the cq msg
     */
    public void parseSmoke(CqMsg cqMsg) {

        MsgQueue msgQueue = MSG_QUEUE_MAP.get(cqMsg.getGroupId());
        //如果获取队列失败，而且是开启禁言的群号，直接在这里初始化队列，懒加载避免获取群列表
        if (msgQueue == null){
            if(!REPEAT_RECORD_GROUP.contains(String.valueOf(cqMsg.getGroupId()))
                    &&!REPEAT_SMOKE_GROUP.contains(String.valueOf(cqMsg.getGroupId()))) {
//                MSG_QUEUE_MAP.put(respData.getGroupId(), new MsgQueue());
            }else{
                MSG_QUEUE_MAP.put(cqMsg.getGroupId(), new MsgQueue());
            }
        }

        //进行添加
        //判断非空……提高健壮性
        if (msgQueue != null) {
            msgQueue.addMsg(cqMsg);
            //如果是开启禁言的群,并且该条触发了禁言

            if ((REPEAT_SMOKE_GROUP.contains(String.valueOf(cqMsg.getGroupId())) && msgQueue.countRepeat() >= 6)) {
                logger.info("触发复读禁言，正在记录案发现场：" + new Gson().toJson(msgQueue.getRepeatList()));
                // 由于onebot实现改造，尽量减少API对接行为，去掉获取群管
//                if (GROUP_ADMIN_LIST.get(cqMsg.getGroupId()).contains(cqMsg.getUserId())) {
//                    logger.info("检测到群管" + cqMsg.getUserId() + "的复读行为");
//                    cqMsg.setMessage("[CQ:at,qq=" + cqManager.getOwner(cqMsg.getGroupId()) + "] 检测到群管" + "[CQ:at,qq=" + cqMsg.getUserId() + "] 复读。");
//                } else {
                    logger.info("正在尝试禁言" + cqMsg.getUserId());
                    cqMsg.setDuration(600);
                    cqMsg.setMessageType("smoke");
//                }
                cqManager.sendMsg(cqMsg);

            }
            if (REPEAT_RECORD_GROUP.contains(String.valueOf(cqMsg.getGroupId()))) {
                User user = userDAO.getUser(cqMsg.getUserId(), null);
                if (user != null) {
                    if (msgQueue.countRepeat() >= 2) {
                        Long count = user.getRepeatCount();
                        user.setRepeatCount(++count);
                    }
                    Long count = user.getSpeakingCount();
                    user.setSpeakingCount(++count);
                    userDAO.updateUser(user);
                }
            }
        }
    }
}
