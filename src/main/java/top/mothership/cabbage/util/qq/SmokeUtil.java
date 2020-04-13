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
import top.mothership.cabbage.pojo.coolq.RespData;

import java.util.*;

@Component
public class SmokeUtil {

    private final static List<String> REPEAT_SMOKE_GROUP = Arrays.asList((
            "201872650," +//MP5
            "112177148," +//MP4后花园
            "532783765," +//测试群
            "915580939," +//MP5赛群
            "521774765"//ODNL S3
    ).split(","));
    private final static List<String> REPEAT_RECORD_GROUP = Arrays.asList("576214175,532783765".split(","));
    private Logger logger = LogManager.getLogger(this.getClass());
    private final CqManager cqManager;
    private final UserDAO userDAO;
    private final ResDAO resDAO;
    public final static Map<Long, MsgQueue> MSG_QUEUE_MAP = new HashMap<>();
    private  static Map<Long, List<Long>> GROUP_ADMIN_LIST;


    /*
    而读取管理员则独立成为方法，方便刷新
    现在的问题是这个静态方法会在cqManager之前初始化，而且cqManager不是静态的，所以在这个方法里没法用cqManager
    不使用静态方法，调用这个方法放在构造函数里，而Controller里正好由Spring托管了一个
    */

    public void loadGroupAdmins() {
        GROUP_ADMIN_LIST = new HashMap<>(16);
        //仅仅记录复读的群不需要群管
        for (String smokeGroup : REPEAT_SMOKE_GROUP) {
            GROUP_ADMIN_LIST.put(Long.valueOf(smokeGroup), cqManager.getGroupAdmins(Long.valueOf(smokeGroup)));
        }
        logger.info("读取群管理员完成");
    }

    @Autowired
    public SmokeUtil(CqManager cqManager, UserDAO userDAO, ResDAO resDAO) {
        this.cqManager = cqManager;
        this.userDAO = userDAO;
        this.resDAO = resDAO;
        loadGroupAdmins();
        //对所有群开启消息记录
        List<RespData> groups = cqManager.getGroups(1335734629L).getData();
        List<RespData> groups2 = cqManager.getGroups(1020640876L).getData();
        groups.addAll(groups2);
        //懒得去重了 反正Map会自动去
        for (RespData respData : groups) {
            if(!REPEAT_RECORD_GROUP.contains(String.valueOf(respData.getGroupId()))
                    &&!REPEAT_SMOKE_GROUP.contains(String.valueOf(respData.getGroupId()))) {
                MSG_QUEUE_MAP.put(respData.getGroupId(), new MsgQueue());
            }else{
                MSG_QUEUE_MAP.put(respData.getGroupId(), new MsgQueue());
            }
        }
    }

    /**
     * Parse smoke.
     *
     * @param cqMsg the cq msg
     */
    public void parseSmoke(CqMsg cqMsg) {
        //ArrayList内部使用.equals比较对象，所以直接传入String
        //如果是开启禁言的群
        //获取绑定的那个MsgQueue
        MsgQueue msgQueue = MSG_QUEUE_MAP.get(cqMsg.getGroupId());
        //进行添加
        //判断非空……提高健壮性
        if (msgQueue != null) {
            msgQueue.addMsg(cqMsg);
            //如果是开启禁言的群,并且该条触发了禁言

            if ((REPEAT_SMOKE_GROUP.contains(String.valueOf(cqMsg.getGroupId())) && msgQueue.countRepeat() >= 6)||(cqMsg.getGroupId().equals(521774765L) && msgQueue.countRepeat() >=2)) {
                logger.info("触发复读禁言，正在记录案发现场：" + new Gson().toJson(msgQueue.getRepeatList()));
                if (GROUP_ADMIN_LIST.get(cqMsg.getGroupId()).contains(cqMsg.getUserId())) {
                    logger.info("检测到群管" + cqMsg.getUserId() + "的复读行为");
                    cqMsg.setMessage("[CQ:at,qq=" + cqManager.getOwner(cqMsg.getGroupId()) + "] 检测到群管" + "[CQ:at,qq=" + cqMsg.getUserId() + "] 复读。");
                } else {
                    logger.info("正在尝试禁言" + cqMsg.getUserId());
                    cqMsg.setDuration(600);
                    cqMsg.setMessageType("smoke");
                }
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
