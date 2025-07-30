package top.mothership.cabbage.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.coolq.CqResponse;
import top.mothership.cabbage.pojo.coolq.OneBotApiRequest;
import top.mothership.cabbage.pojo.coolq.QQInfo;
import top.mothership.cabbage.websocket.OneBotMessageHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

//将OneBot的WS API封装为接口，并托管到Spring
@Component
public class OneBotManager {
    @Autowired
    private OneBotMessageHandler handler;


    public void warn(String msg) {
        CqMsg cqMsg = new CqMsg();
        cqMsg.setMessageType("private");
        cqMsg.setUserId(1335734657L);
        cqMsg.setSelfId(1335734629L);
        cqMsg.setMessage(msg);
        sendMsg(cqMsg);
    }

    public void warn(String msg, Exception e) {
        CqMsg cqMsg = new CqMsg();
        cqMsg.setMessageType("private");
        cqMsg.setUserId(1335734657L);
        cqMsg.setSelfId(1335734629L);
        cqMsg.setMessage(msg + " " + e.getMessage());
        sendMsg(cqMsg);
    }

    public void sendMsg(CqMsg cqMsg) {
        OneBotMessageHandler.sendMessage(cqMsg);
    }

    /**
     * 获取单个群成员列表，仅针对原有2个QQ所在的群才有用
     * 目前用处是全员循环禁言（?什么傻逼功能），超管group info命令，2个chart群给MP系列主群加人用
     * 后续如果自由接入的话获取不到其他接入QQ的群信息（懒得写，得在消息入口把发送人QQ一直保持到调用时候）
     *
     * @param groupId 目标群
     * @return
     */

    public CqResponse<List<QQInfo>> getGroupMembers(Long groupId) {

        CqMsg cqMsg = new CqMsg();
        cqMsg.setGroupId(groupId);
        cqMsg.setSelfId(1335734629L);

        OneBotApiRequest request = new OneBotApiRequest();
        request.setAction("get_group_member_list");
        request.setParams(cqMsg);
        request.setEcho(getId());
        String response = OneBotMessageHandler.callApi(request);
        CqResponse<List<QQInfo>> data = new Gson().fromJson(response, new TypeToken<CqResponse<List<QQInfo>>>() {
        }.getType());

        // 如果报错找不到
        if (data == null || data.getRetCode() != 0) {
            cqMsg.setSelfId(1020640876L);
            response = OneBotMessageHandler.callApi(request);
            data = new Gson().fromJson(response, new TypeToken<CqResponse<List<QQInfo>>>() {
            }.getType());
        }
        return data;

    }


    /**
     * 获取单个群成员信息，仅针对原有2个QQ所在的群才有用
     * 后续如果自由接入的话获取不到其他接入QQ的群信息（懒得写，得在消息入口把发送人QQ一直保持到调用时候）
     * 目前调用方：超管group info命令，list msg命令，还有每天循环查2个主群名片是否包含osu ID
     *
     * @param groupId 目标群
     * @param userId  目标人
     * @return
     */
    public CqResponse<QQInfo> getGroupMember(Long groupId, Long userId) {


        CqMsg cqMsg = new CqMsg();
        cqMsg.setGroupId(groupId);
        cqMsg.setUserId(userId);
        cqMsg.setSelfId(1335734629L);

        OneBotApiRequest request = new OneBotApiRequest();
        request.setAction("get_group_member_list");
        request.setParams(cqMsg);
        request.setEcho(getId());
        String response = OneBotMessageHandler.callApi(request);
        CqResponse<QQInfo> data = new Gson().fromJson(response, new TypeToken<CqResponse<QQInfo>>() {
        }.getType());

        if (data.getRetCode() != 0) {
            cqMsg.setSelfId(1020640876L);
            response = OneBotMessageHandler.callApi(request);
            data = new Gson().fromJson(response, new TypeToken<CqResponse<QQInfo>>() {
            }.getType());
        }
        return data;

    }

    private String getId() {
        // 创建一个新的Random对象
        Random random = new Random();

        // 生成10位随机数
        long randomNumber = random.nextLong() % 10000000000L;

        // 确保随机数是10位的
        randomNumber = Math.abs(randomNumber);
        if (randomNumber < 1000000000L) {
            randomNumber += 1000000000L;
        }
        return System.currentTimeMillis() + "" + randomNumber;
    }
}
