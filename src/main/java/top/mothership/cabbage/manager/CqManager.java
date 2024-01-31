package top.mothership.cabbage.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.coolq.*;
import top.mothership.cabbage.websocket.OneBotMessageHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//将CQ的HTTP API封装为接口，并托管到Spring
@Component
public class CqManager {
    @Autowired
    private OneBotMessageHandler handler;


    public CqResponse warn(String msg) {
        CqMsg cqMsg = new CqMsg();
        cqMsg.setMessageType("private");
        cqMsg.setUserId(1335734657L);
        cqMsg.setSelfId(1335734629L);
        cqMsg.setMessage(msg);
        return sendMsg(cqMsg);
    }

    public CqResponse warn(String msg, Exception e) {
        CqMsg cqMsg = new CqMsg();
        cqMsg.setMessageType("private");
        cqMsg.setUserId(1335734657L);
        cqMsg.setSelfId(1335734629L);
        cqMsg.setMessage(msg + " " + e.getMessage());
        return sendMsg(cqMsg);
    }

    public CqResponse sendMsg(CqMsg cqMsg) {
        String baseURL = null;
        switch (cqMsg.getSelfId().toString()) {
            case "1020640876":
                baseURL = "http://k3.mothership.top:5700";
                break;
            case "1335734629":
                OneBotMessageHandler.sendMessage(cqMsg);
                return null;
            case "2758858579":
                baseURL = "http://cq.mothership.top:5702";
                break;
            default:
                OneBotMessageHandler.sendMessage(cqMsg);
                return null;
        }
        String URL;
        switch (cqMsg.getMessageType()) {
            case "group":
                URL = baseURL + "/send_group_msg";
                break;
            case "discuss":
                URL = baseURL + "/send_discuss_msg";
                break;
            case "private":
                URL = baseURL + "/send_private_msg";
                break;
            case "smoke":
                URL = baseURL + "/set_group_ban";
                break;
            case "smokeAll":
                URL = baseURL + "/set_group_whole_ban";
                break;
            case "handleInvite":
                URL = baseURL + "/set_group_add_request";
                break;
            case "kick":
                URL = baseURL + "/set_group_kick";
                break;
            default:
                return null;
        }
        HttpURLConnection httpConnection;
        try {
            httpConnection =
                    (HttpURLConnection) new URL(URL).openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setDoOutput(true);
            OutputStream os = httpConnection.getOutputStream();
            //防止转义
            //折腾了半天最后是少了UTF-8………………我tm想给自己一巴掌
            os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            //这里不用用到下划线转驼峰
            CqResponse response = new Gson().fromJson(tmp2.toString(), CqResponse.class);
            if(response.getRetCode()!=0 && cqMsg.getMessage().contains("base64")){
                warn("图片发送失败，出现的QQ：" +cqMsg.getSelfId());
            }
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 获取单个群成员列表，仅针对原有2个QQ所在的群才有用
     * 目前用处是全员循环禁言（?什么傻逼功能），超管group info命令，2个chart群给MP系列主群加人用
     * 后续如果自由接入的话获取不到其他接入QQ的群信息（懒得写，得在消息入口把发送人QQ一直保持到调用时候）
     * @param groupId 目标群
     * @return
     */

    public CqResponse<List<QQInfo>> getGroupMembers(Long groupId) {

        HttpURLConnection httpConnection;
        try {
            CqMsg cqMsg = new CqMsg();
            cqMsg.setGroupId(groupId);
            cqMsg.setSelfId(1335734629L);

            OneBotApiRequest request = new OneBotApiRequest();
            request.setAction("get_group_member_list");
            request.setMsg(cqMsg);
            request.setEcho(getId());
            String response = OneBotMessageHandler.callApi(request);
            CqResponse<List<QQInfo>> data = new Gson().fromJson(response, new TypeToken<CqResponse<List<QQInfo>>>() {
            }.getType());

            // 如果报错找不到
            if (data.getRetCode() != 0) {
                String URL = "http://k3.mothership.top:5700/get_group_member_list";
                httpConnection =
                        (HttpURLConnection) new URL(URL).openConnection();
                httpConnection.setRequestMethod("POST");
                httpConnection.setRequestProperty("Accept", "application/json");
                httpConnection.setRequestProperty("Content-Type", "application/json");
                httpConnection.setDoOutput(true);

                OutputStream os = httpConnection.getOutputStream();
                os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
                BufferedReader responseBuffer = new BufferedReader(new InputStreamReader((httpConnection.getInputStream()), StandardCharsets.UTF_8));
                StringBuilder tmp2 = new StringBuilder();
                String tmp3;
                while ((tmp3 = responseBuffer.readLine()) != null) {
                    tmp2.append(tmp3);
                }
                return new Gson().fromJson(tmp2.toString(), new TypeToken<CqResponse<List<QQInfo>>>() {
                }.getType());
            }
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }


    /**
     * 获取单个群成员信息，仅针对原有2个QQ所在的群才有用
     * 后续如果自由接入的话获取不到其他接入QQ的群信息（懒得写，得在消息入口把发送人QQ一直保持到调用时候）
     * 目前调用方：超管group info命令，list msg命令，还有每天循环查2个主群名片是否包含osu ID
     * @param groupId 目标群
     * @param userId 目标人
     * @return
     */
    public CqResponse<QQInfo> getGroupMember(Long groupId, Long userId) {

        HttpURLConnection httpConnection;
        try {
            CqMsg cqMsg = new CqMsg();
            cqMsg.setGroupId(groupId);
            cqMsg.setUserId(userId);
            cqMsg.setSelfId(1335734629L);

            OneBotApiRequest request = new OneBotApiRequest();
            request.setAction("get_group_member_list");
            request.setMsg(cqMsg);
            request.setEcho(getId());
            String response = OneBotMessageHandler.callApi(request);
            CqResponse<QQInfo> data = new Gson().fromJson(response, new TypeToken<CqResponse<QQInfo>>() {
            }.getType());

            if (data.getRetCode() != 0) {
                String URL = "http://k3.mothership.top:5700/get_group_member_info";
                httpConnection =
                        (HttpURLConnection) new URL(URL).openConnection();
                httpConnection.setRequestMethod("POST");
                httpConnection.setRequestProperty("Accept", "application/json");
                httpConnection.setRequestProperty("Content-Type", "application/json");
                httpConnection.setDoOutput(true);

                OutputStream os = httpConnection.getOutputStream();
                os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
                BufferedReader responseBuffer = new BufferedReader(new InputStreamReader((httpConnection.getInputStream()), StandardCharsets.UTF_8));
                StringBuilder tmp2 = new StringBuilder();
                String tmp3;
                while ((tmp3 = responseBuffer.readLine()) != null) {
                    tmp2.append(tmp3);
                }
                return new Gson().fromJson(tmp2.toString(), new TypeToken<CqResponse<QQInfo>>() {
                }.getType());
            }
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    private String getId(){
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
