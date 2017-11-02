package top.mothership.cabbage.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.CoolQ.CqResponse;
import top.mothership.cabbage.pojo.CoolQ.QQInfo;
import top.mothership.cabbage.pojo.CoolQ.RespData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

//将CQ的HTTP API封装为接口，并托管到Spring
@Component
public class CqUtil {
    private final String baseURL = "http://localhost:5700";

    public CqResponse sendMsg(CqMsg cqMsg) {
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
            os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes("UTF-8"));
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
            return new Gson().fromJson(tmp2.toString(), CqResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public CqResponse<List<QQInfo>> getGroupMembers(Long groupId) {
        String URL = baseURL + "/get_group_member_list";
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
            os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes("UTF-8"));
            os.flush();
            os.close();
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream()),"UTF-8"));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            //采用泛型封装，接住变化无穷的data
            return new Gson().fromJson(tmp2.toString(), new TypeToken<CqResponse<List<QQInfo>>>() {}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
    public CqResponse<List<RespData>> getGroups() {
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
            os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes("UTF-8"));
            os.flush();
            os.close();
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream()),"UTF-8"));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            return new Gson().fromJson(tmp2.toString(), new TypeToken<CqResponse<List<RespData>>>() {}.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
    public List<Long> getGroupAdmins(Long groupId){
        List<QQInfo> members = getGroupMembers(groupId).getData();
        List<Long> result = new ArrayList<>();
        for(int i=0;i<members.size();i++){
            if(members.get(i).getRole().equals("admin")){

                result.add(members.get(i).getUserId());
            }
        }
        return result;
    }
    public String getCard(Long QQ,Long groupId){
        List<QQInfo> members = getGroupMembers(groupId).getData();
        for(int i=0;i<members.size();i++){
            if(members.get(i).getUserId().equals(QQ)){
                if("".equals(members.get(i).getCard())){
                    return members.get(i).getNickname();
                }else{
                    return members.get(i).getCard();
                }
            }
        }
        return "";
    }
    public Long getOwner(Long groupId){
        List<QQInfo> members = getGroupMembers(groupId).getData();
        for(int i=0;i<members.size();i++){
            if(members.get(i).getRole().equals("owner")){
                return members.get(i).getUserId();
            }
        }
        return 0L;
    }
}
