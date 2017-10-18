package top.mothership.cabbage.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.CqMsg;
import top.mothership.cabbage.pojo.RespData;

import java.util.*;

@Component
public class SmokeUtil {
    //对所有群开启消息记录
    private static ResourceBundle rb = ResourceBundle.getBundle("cabbage");
    private static List<String> smokeGroups = Arrays.asList(rb.getString("smokeGroups").split(","));
    private Logger logger = LogManager.getLogger(this.getClass());
    private final CqUtil cqUtil;
    //对每个开启禁言复读的群，创建一个新的queue
    public static Map<Long, MsgQueue> msgQueues = new HashMap<>();
    private static Map<Long, List<Long>> groupAdmins;


    //而读取管理员则独立成为方法，方便刷新
    //现在的问题是这个静态方法会在cqUtil之前初始化，而且CQUtil不是静态的，所以在这个方法里没法用CQUtil
    //不使用静态方法，调用这个方法放在构造函数里，而Controller里正好由Spring托管了一个
    public void loadGroupAdmins() {
        groupAdmins = new HashMap<>();
        for (String smokeGroup : smokeGroups) {
            groupAdmins.put(Long.valueOf(smokeGroup), cqUtil.getGroupAdmins(Long.valueOf(smokeGroup)));
        }
    }

    @Autowired
    public SmokeUtil(CqUtil cqUtil) {
        this.cqUtil = cqUtil;
        loadGroupAdmins();
        //对所有群开启消息记录
        List<RespData> groups = cqUtil.getGroups().getData();
        for (RespData respData : groups) {
            msgQueues.put(respData.getGroupId(), new MsgQueue());
        }
    }

    public void praseSmoke(CqMsg cqMsg) {
        java.util.Date s = Calendar.getInstance().getTime();
        //ArrayList内部使用.equals比较对象，所以直接传入String
        //如果是开启禁言的群

            int count = 0;
            //获取绑定的那个MsgQueue
            MsgQueue msgQueue = msgQueues.get(cqMsg.getGroupId());
            //进行添加
            msgQueue.addMsg(cqMsg);
        //如果是开启禁言的群,并且该条触发了禁言

            if (smokeGroups.contains(String.valueOf(cqMsg.getGroupId()))&&msgQueue.isRepeat()) {
                if (groupAdmins.get(cqMsg.getGroupId()).contains(cqMsg.getUserId())){
                    logger.info("检测到群管" + cqMsg.getUserId() + "的复读行为");
                    //第一个永远是群主
                    cqMsg.setMessage("[CQ:at,qq="+cqUtil.getOwner(cqMsg.getGroupId())+"] 检测到群管" + "[CQ:at,qq=" + cqMsg.getUserId() + "] 复读。");
                } else{
                    logger.info("正在尝试禁言" + cqMsg.getUserId());
                    cqMsg.setDuration(600);
                    cqMsg.setMessageType("smoke");
                }
                cqUtil.sendMsg(cqMsg);
                logger.info("处理完毕，共耗费" + (Calendar.getInstance().getTimeInMillis() - s.getTime()) + "ms。");
            }


    }
}
