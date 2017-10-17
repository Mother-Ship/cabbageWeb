package top.mothership.cabbage.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.CqMsg;
import top.mothership.cabbage.pojo.CqResponse;
import top.mothership.cabbage.pojo.QQInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

    public List<QQInfo> getGroupMembers(Long groupId) {
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
            return new Gson().fromJson(tmp2.toString(), CqResponse.class).getData();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
