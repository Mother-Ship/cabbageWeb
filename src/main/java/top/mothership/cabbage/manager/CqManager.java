package top.mothership.cabbage.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.coolq.CqResponse;
import top.mothership.cabbage.pojo.coolq.QQInfo;
import top.mothership.cabbage.pojo.coolq.RespData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

//将CQ的HTTP API封装为接口，并托管到Spring
@Component
public class CqManager {
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
                baseURL = "http://cq.mothership.top:5701";
                break;
            case "1335734629":
                baseURL = "http://cq.mothership.top:5700";
                break;
            case "2758858579":
                baseURL = "http://cq.mothership.top:5702";
                break;
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
                warn("图片发送失败，"+cqMsg +response);
            }
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public CqResponse<List<QQInfo>> getGroupMembers(Long groupId) {
        String URL = "http://cq.mothership.top:5700/get_group_member_list";
        HttpURLConnection httpConnection;
        try {
            CqMsg cqMsg = new CqMsg();
            cqMsg.setGroupId(groupId);
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
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream()), StandardCharsets.UTF_8));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            //采用泛型封装，接住变化无穷的data
            CqResponse<List<QQInfo>> response = new Gson().fromJson(tmp2.toString(), new TypeToken<CqResponse<List<QQInfo>>>() {
            }.getType());
            if (response.getRetCode() != 0) {
                URL = "http://cq.mothership.top:5701/get_group_member_list";
                httpConnection =
                        (HttpURLConnection) new URL(URL).openConnection();
                httpConnection.setRequestMethod("POST");
                httpConnection.setRequestProperty("Accept", "application/json");
                httpConnection.setRequestProperty("Content-Type", "application/json");
                httpConnection.setDoOutput(true);

                os = httpConnection.getOutputStream();
                os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
                responseBuffer = new BufferedReader(new InputStreamReader((httpConnection.getInputStream()), StandardCharsets.UTF_8));
                tmp2 = new StringBuilder();
                String tmp3;
                while ((tmp3 = responseBuffer.readLine()) != null) {
                    tmp2.append(tmp3);
                }
                return new Gson().fromJson(tmp2.toString(), new TypeToken<CqResponse<List<QQInfo>>>() {
                }.getType());
            }
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    //在SmokeUtil里请求两次，将两个QQ的所有群合并
    public CqResponse<List<RespData>> getGroups(Long selfId) {
        String baseURL = null;
        switch (selfId.toString()) {
            case "1020640876":
                baseURL = "http://cq.mothership.top:5701";
                break;
            case "1335734629":
                baseURL = "http://cq.mothership.top:5700";
                break;
        }
        String URL = baseURL + "/get_group_list";
        HttpURLConnection httpConnection;
        try {
            CqMsg cqMsg = new CqMsg();
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
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream()), StandardCharsets.UTF_8));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            return new Gson().fromJson(tmp2.toString(), new TypeToken<CqResponse<List<RespData>>>() {
            }.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public List<Long> getGroupAdmins(Long groupId) {
        List<QQInfo> members = getGroupMembers(groupId).getData();
        List<Long> result = new ArrayList<>();
        if (members != null) {
            for (int i = 0; i < members.size(); i++) {
                if (members.get(i).getRole().equals("admin")) {
                    result.add(members.get(i).getUserId());
                }
            }
        }
        return result;
    }

    public Long getOwner(Long groupId) {
        List<QQInfo> members = getGroupMembers(groupId).getData();
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getRole().equals("owner")) {
                return members.get(i).getUserId();
            }
        }
        return 0L;
    }

    public CqResponse<QQInfo> getGroupMember(Long groupId, Long userId) {
        //内部重试两个QQ的API
        String URL = "http://cq.mothership.top:5700/get_group_member_info";
        HttpURLConnection httpConnection;
        try {
            CqMsg cqMsg = new CqMsg();
            cqMsg.setGroupId(groupId);
            cqMsg.setUserId(userId);
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
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream()), StandardCharsets.UTF_8));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            //采用泛型封装，接住变化无穷的data
            CqResponse<QQInfo> response = new Gson().fromJson(tmp2.toString(), new TypeToken<CqResponse<QQInfo>>() {
            }.getType());
            if (response.getRetCode() != 0) {
                URL = "http://cq.mothership.top:5701/get_group_member_info";
                httpConnection =
                        (HttpURLConnection) new URL(URL).openConnection();
                httpConnection.setRequestMethod("POST");
                httpConnection.setRequestProperty("Accept", "application/json");
                httpConnection.setRequestProperty("Content-Type", "application/json");
                httpConnection.setDoOutput(true);

                os = httpConnection.getOutputStream();
                os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
                responseBuffer = new BufferedReader(new InputStreamReader((httpConnection.getInputStream()), StandardCharsets.UTF_8));
                tmp2 = new StringBuilder();
                String tmp3;
                while ((tmp3 = responseBuffer.readLine()) != null) {
                    tmp2.append(tmp3);
                }
                return new Gson().fromJson(tmp2.toString(), new TypeToken<CqResponse<QQInfo>>() {
                }.getType());
            }
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
