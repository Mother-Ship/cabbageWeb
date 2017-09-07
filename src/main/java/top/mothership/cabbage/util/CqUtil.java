package top.mothership.cabbage.util;

import com.google.gson.Gson;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.CqResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

//将CQ的HTTP API封装为接口，并托管到Spring
@Component
public class CqUtil {
    private final String baseURL = "htto://localhost:5700";

    public CqResponse sendMsg(String msgType, String msg, String groupId, String qq, String duration) {
        String URL;
        switch (msgType) {
            case "group":
                URL = baseURL + "/send_group_msg?group_id=" + groupId + "&message=" + msg;
                break;
            case "private":
                URL = baseURL + "/send_private_msg?user_id=" + qq + "&message=" + msg;
                break;
            case "smoke":
                URL = baseURL + "/set_group_ban?group_id=" + groupId + "&user_id=" + qq + "&duration=" + duration;
                break;
            default:
                return null;
        }
        HttpURLConnection httpConnection;
        try {
            httpConnection =
                    (HttpURLConnection) new URL(URL).openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Accept", "application/json");
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            return new Gson().fromJson(tmp2.toString(), CqResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

}
