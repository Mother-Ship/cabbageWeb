package top.mothership.cabbage.util.osu;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.pojo.osu.Userinfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
@Component
public class ApiUtil {
    private final String getUserURL = "https://osu.ppy.sh/api/get_user";
    private final String getBPURL = "https://osu.ppy.sh/api/get_user_best";
    private final String getMapURL = "https://osu.ppy.sh/api/get_beatmaps";
    private final String getRecentURL = "https://osu.ppy.sh/api/get_user_recent";
    private final String getScoreURL = "https://osu.ppy.sh/api/get_scores";

    private final String key = "25559acca3eea3e2c730cd65ee8a6b2da55b52c0";
    private Logger logger = LogManager.getLogger(this.getClass());

    public Userinfo getUser(String username, String userId) {
        String result = praseUid("user", username, userId);
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("YYYY-MM-dd HH:mm:ss").create().fromJson(result, Userinfo.class);
    }

    public Beatmap getBeatmap(Integer bid) {
        String result = accessAPI("beatmap", null, null, String.valueOf(bid),null,null);
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("YYYY-MM-dd HH:mm:ss").create().fromJson(result, Beatmap.class);
    }
    public Beatmap getBeatmap(String hash) {
        String result = accessAPI("beatmapHash", null, null, null,String.valueOf(hash),null);
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("YYYY-MM-dd HH:mm:ss").create().fromJson(result, Beatmap.class);
    }
    public List<Score> getBP(String username, String userId) {
        String result = praseUid("bp", username, userId);
        //由于这里用到List，手动补上双括号
        result = "["+result+"]";
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("YYYY-MM-dd HH:mm:ss").create()
                .fromJson(result, new TypeToken<List<Score>>() {
        }.getType());
    }

    public Score getRecent(String username, String userId) {
        String result = praseUid("recent", username, userId);
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("YYYY-MM-dd HH:mm:ss").create().fromJson(result, Score.class);
    }

    public List<Score> getFirstScore(Integer bid,Integer rank){
        String result = accessAPI("first", null, null, String.valueOf(bid),null,rank);
        result = "["+result+"]";
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("YYYY-MM-dd HH:mm:ss").create()
                .fromJson(result, new TypeToken<List<Score>>(){}.getType());

    }
    public List<Score> getScore(Integer bid,String uid){
        String result = accessAPI("score", uid, "string", String.valueOf(bid),null,null);
        result = "["+result+"]";
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("YYYY-MM-dd HH:mm:ss").create()
                .fromJson(result, new TypeToken<List<Score>>(){}.getType());

    }
    private String praseUid(String apiType, String username, String userId) {
        String result;
        if (username != null && userId == null) {
            result = accessAPI(apiType, username, "string", null,null,null);
            return result;
        } else if (username == null && userId != null) {
            result = accessAPI(apiType, String.valueOf(userId), "id", null,null,null);
            return result;
        } else {
            logger.error("不可同时指定用户名和用户id。");
            return null;
        }
    }

    private String accessAPI(String apiType, String uid, String uidType, String bid,String hash,Integer rank) {
        String URL;
        String failLog;
        String output = null;
        HttpURLConnection httpConnection;
        switch (apiType) {
            case "user":
                URL = getUserURL + "?k=" + key + "&type=" + uidType + "&u=" + uid;
                failLog = "玩家" + uid + "请求API：get_user失败五次";
                break;
            case "bp":
                URL = getBPURL + "?k=" + key + "&type=" + uidType + "&limit=100&u=" + uid;
                failLog = "玩家" + uid + "请求API：get_user_best失败五次";
                break;
            case "beatmap":
                URL = getMapURL + "?k=" + key + "&b=" + bid;
                failLog = "谱面" + bid + "请求API：get_beatmaps失败五次";
                break;
            case "beatmapHash":
                URL = getMapURL + "?k=" + key + "&h=" + hash;
                failLog = "谱面" + bid + "请求API：get_beatmaps失败五次";
                break;
            case "recent":
                URL = getRecentURL + "?k=" + key + "&type=" + uidType + "&limit=1&u=" + uid;
                failLog = "玩家" + uid + "请求API：get_recent失败五次";
                break;
            case "first":
                URL = getScoreURL + "?k=" + key + "&limit="+rank+"&b=" + bid;
                failLog = "谱面" + bid + "请求API：get_scores失败五次";
                break;
            case "score":
                URL = getScoreURL + "?k=" + key + "&type=" + uidType + "&u=" + uid+"&b=" + bid;
                failLog = "谱面" + bid + "请求API：get_scores失败五次";
                break;
            default:
                logger.info("apiType错误");
                return null;

        }

        int retry = 0;
        while (retry < 5) {
            try {
                httpConnection =
                        (HttpURLConnection) new URL(URL).openConnection();
                //设置请求头
                httpConnection.setRequestMethod("GET");
                httpConnection.setRequestProperty("Accept", "application/json");
                httpConnection.setConnectTimeout((int) Math.pow(2, retry + 1) * 1000);
                httpConnection.setReadTimeout((int) Math.pow(2, retry + 1) * 1000);
                if (httpConnection.getResponseCode() != 200) {
                    logger.info("HTTP GET请求失败: " + httpConnection.getResponseCode() + "，正在重试第" + (retry + 1) + "次");
                    retry++;
                    continue;
                }
                //读取返回结果
                BufferedReader responseBuffer =
                        new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));
                StringBuilder tmp2 = new StringBuilder();
                String tmp;
                while ((tmp = responseBuffer.readLine()) != null) {
                    tmp2.append(tmp);
                }
                //去掉两侧的中括号
                output = tmp2.toString().substring(1, tmp2.toString().length() - 1);
                //手动关闭流
                httpConnection.disconnect();
                responseBuffer.close();
                break;
            } catch (IOException e) {
                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                retry++;
            }
        }
        if (retry == 5) {
            logger.error(failLog);
            return null;
        }
        return output;
    }
}
