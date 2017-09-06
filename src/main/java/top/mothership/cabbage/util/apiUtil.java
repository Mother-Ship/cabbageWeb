package top.mothership.cabbage.util;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.mothership.cabbage.pojo.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class apiUtil {
    private final String getUserURL = "https://osu.ppy.sh/api/get_user";
    private final String getBPURL = "https://osu.ppy.sh/api/get_user_best";
    private final String getMapURL = "https://osu.ppy.sh/api/get_beatmaps";
    private final String getRecentURL = "https://osu.ppy.sh/api/get_user_recent";
    private final String key = "25559acca3eea3e2c730cd65ee8a6b2da55b52c0";
    private Logger logger = LogManager.getLogger(this.getClass());

    public User getUser(String username, int userId) {
        String result;
        result = praseUid("user",username,userId);
        return new Gson().fromJson(result, User.class);
    }

    private String praseUid(String apiType,String username,int userId){
        String result;
        if (username != null && userId == 0) {
            result = accessAPI(apiType, username,false, null);
            return result;
        } else if (username == null && userId != 0) {
            result = accessAPI(apiType, String.valueOf(userId), true,null);
            return result;
        } else {
            logger.error("不可同时指定用户名和用户id。");
            return null;
        }
    }

    private String accessAPI(String apiType, String uid, boolean isUid,String bid) {
        String URL;
        String failLog;
        switch (apiType) {
            case "user":
                if(isUid) {
                    URL = getUserURL + "?k=" + key +
                }else{
                    URL =
                }
                break;
            case "bp":
                break;
            case "map":
                break;
            case "recent":
                break;

        }
        String output = null;
        HttpURLConnection httpConnection;
        int retry = 0;
        while (retry < 8) {
            try {
                httpConnection =
                        (HttpURLConnection) new URL(URL).openConnection();
                //设置请求头
                httpConnection.setRequestMethod("GET");
                httpConnection.setRequestProperty("Accept", "application/json");
                httpConnection.setConnectTimeout((int) Math.pow(2, retry) * 1000);
                httpConnection.setReadTimeout((int) Math.pow(2, retry) * 1000);
                if (httpConnection.getResponseCode() != 200) {
                    logger.info("HTTP GET请求失败: " + httpConnection.getResponseCode() + "，正在重试第" + (retry + 1) + "次");
                    retry++;
                    continue;
                }
                //读取返回结果
                BufferedReader responseBuffer =
                        new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));
                //BP的返回结果有时候会有换行，必须手动拼接
                output = responseBuffer.readLine();
                //去掉两侧的中括号
                output = output.substring(1, output.length() - 1);
                //手动关闭流
                httpConnection.disconnect();
                responseBuffer.close();
                break;
            } catch (IOException e) {
                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                retry++;
            }
        }
        if (retry == 8) {
            logger.error("玩家" + username + "请求API获取最近游戏记录，失败五次");
            return null;
        }

    }
}
