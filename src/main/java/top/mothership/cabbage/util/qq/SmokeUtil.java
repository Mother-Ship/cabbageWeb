package top.mothership.cabbage.util.qq;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.coolq.CqMsg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SmokeUtil {

    public final static Map<Long, MsgQueue> MSG_QUEUE_MAP = new HashMap<>();
    private final static List<String> REPEAT_SMOKE_GROUP = Arrays.asList((
            "201872650," +//MP5
                    "112177148," +//MP4后花园
                    "693299572," +//测试群
                    "136312506," +//MP5赛群136312506
                    "521774765"//ODNL S3
    ).split(","));
    private final static List<String> REPEAT_RECORD_GROUP = Arrays.asList("576214175,532783765".split(","));
    private final CqManager cqManager;
    private final UserDAO userDAO;
    private final ResDAO resDAO;
    private Logger logger = LogManager.getLogger(this.getClass());


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
        if (msgQueue == null) {
            if (!REPEAT_RECORD_GROUP.contains(String.valueOf(cqMsg.getGroupId()))
                    && !REPEAT_SMOKE_GROUP.contains(String.valueOf(cqMsg.getGroupId()))) {
//                MSG_QUEUE_MAP.put(respData.getGroupId(), new MsgQueue());
            } else {
                MSG_QUEUE_MAP.put(cqMsg.getGroupId(), new MsgQueue());
            }
        }

        //进行添加
        //判断非空……提高健壮性
        if (msgQueue != null) {
            msgQueue.addMsg(cqMsg);
            //如果是开启禁言的群,并且该条触发了禁言
            int countRepeat = msgQueue.countRepeat();
            if ((REPEAT_SMOKE_GROUP.contains(String.valueOf(cqMsg.getGroupId())) && countRepeat >= 6)) {
                logger.info("触发复读禁言，正在记录案发现场：" + new Gson().toJson(msgQueue.getRepeatList()));
                // 由于onebot实现改造，尽量减少API对接行为，去掉获取群管
//                if (GROUP_ADMIN_LIST.get(cqMsg.getGroupId()).contains(cqMsg.getUserId())) {
//                    logger.info("检测到群管" + cqMsg.getUserId() + "的复读行为");
//                    cqMsg.setMessage("[CQ:at,qq=" + cqManager.getOwner(cqMsg.getGroupId()) + "] 检测到群管" + "[CQ:at,qq=" + cqMsg.getUserId() + "] 复读。");
//                } else {

                int time = (countRepeat - 5) * 600;
                time = Math.min(time, 8 * 3600);

                if ("136312506".equals(String.valueOf(cqMsg.getGroupId()))
                        || "693299572".equals(String.valueOf(cqMsg.getGroupId()))) {
                    // 1 2 4 8 16 24 +24 h
                    switch ((countRepeat - 5)) {
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            time = (int) (Math.pow(2, (countRepeat - 5 - 1)) * 3600);
                            break;
                        case 6:
                            time = 24 * 3600;
                            break;
                        default:
                            time = (countRepeat - 5 - 5) * 24 * 3600;
                            break;
                    }
                    time = Math.min(time, 30 * 24 * 3600);
                }
                logger.info("检测到最近100条消息中{}发送第{}条复读，正在尝试禁言 {}秒", cqMsg.getUserId(), countRepeat, time);

                cqMsg.setDuration(time);
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
