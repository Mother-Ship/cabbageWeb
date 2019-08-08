package top.mothership.cabbage.manager;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.constant.Overall;
import top.mothership.cabbage.pojo.coolq.osu.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Component
public class ApiManager {
    private final String getUserURL = "https://osu.ppy.sh/api/get_user";
    private final String getBPURL = "https://osu.ppy.sh/api/get_user_best";
    private final String getMapURL = "https://osu.ppy.sh/api/get_beatmaps";
    private final String getRecentURL = "https://osu.ppy.sh/api/get_user_recent";
    private final String getScoreURL = "https://osu.ppy.sh/api/get_scores";
    private final String getMatchURL = "https://osu.ppy.sh/api/get_match";

    private final String key = Overall.CABBAGE_CONFIG.getString("apikey");
    private Logger logger = LogManager.getLogger(this.getClass());



    public Userinfo getUser(Integer mode, String username) {
        String result = accessAPI("user", username, "string", null, null, null, null, mode);
        Userinfo userFromAPI = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson(result, Userinfo.class);
        if (userFromAPI != null) {
            //请求API时加入mode的标记，并且修复Rank问题
            userFromAPI.setMode(mode);
            fixRank(userFromAPI);
        }
        return userFromAPI;
    }

    public Userinfo getUser(Integer mode, Integer userId) {
        String result = accessAPI("user", String.valueOf(userId), "id", null, null, null, null, mode);
        Userinfo userFromAPI = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson(result, Userinfo.class);
        if (userFromAPI != null) {
            //请求API时加入mode的标记，并且修复Rank问题
            userFromAPI.setMode(mode);
            fixRank(userFromAPI);
        }
        return userFromAPI;
    }

    public Beatmap getBeatmap(Integer bid) {
        String result = accessAPI("beatmap", null, null, String.valueOf(bid), null, null, null, null);
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson(result, Beatmap.class);
    }

    public List<Beatmap> getBeatmaps(Integer sid) {
        String result = accessAPI("beatmaps", null, null, String.valueOf(sid), null, null, null, null);
        result = "[" + result + "]";
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson(result, new TypeToken<List<Beatmap>>() {}.getType());
    }

    public Beatmap getBeatmap(String hash) {
        String result = accessAPI("beatmapHash", null, null, null, String.valueOf(hash), null, null, null);
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson(result, Beatmap.class);
    }

    public List<Score> getBP(Integer mode, String username) {
        String result = accessAPI("bp", username, "string", null, null, null, null, mode);
        //由于这里用到List，手动补上双括号
        result = "[" + result + "]";
        List<Score> list = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create()
                .fromJson(result, new TypeToken<List<Score>>() {}.getType());
        for (Score s : list) {
            //2018-2-28 15:54:11哪怕是返回单模式的bp也设置模式，避免计算acc时候判断是单模式还是多模式
            s.setMode(mode.byteValue());
        }
        return list;
    }

    public List<Score> getBP(Integer mode, Integer userId) {
        String result = accessAPI("bp", String.valueOf(userId), "id", null, null, null, null, mode);
        //由于这里用到List，手动补上双括号
        result = "[" + result + "]";
        List<Score> list = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create()
                .fromJson(result, new TypeToken<List<Score>>() {
                }.getType());
        for (Score s : list) {
            s.setMode(mode.byteValue());
        }
        return list;
    }

    public List<Score> getBP(String username) {
        List<Score> resultList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String result = result = accessAPI("bp", username, "string", null, null, null, null, i);
            //由于这里用到List，手动补上双括号
            result = "[" + result + "]";
            List<Score> list = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create()
                    .fromJson(result, new TypeToken<List<Score>>() {
                    }.getType());
            for (Score s : list) {
                s.setMode((byte) i);
            }
            resultList.addAll(list);
        }
        return resultList;
    }

    public List<List<Score>> getBP(Integer userId) {
        List<List<Score>> resultList = new ArrayList<>();
        //小技巧，这里i设为byte
        for (int i = 0; i < 4; i++) {
            String result = accessAPI("bp", String.valueOf(userId), "id", null, null, null, null, i);
            //由于这里用到List，手动补上双括号
            result = "[" + result + "]";
            List<Score> list = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create()
                    .fromJson(result, new TypeToken<List<Score>>() {
                    }.getType());
            for (Score s : list) {
                s.setMode((byte) i);
            }
            resultList.add(list);
        }
        return resultList;
    }

    public Score getRecent(Integer mode, String username) {
        String result = accessAPI("recent", username, "string", null, null, null, null, mode);
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson(result, Score.class);
    }

    public Score getRecent(Integer mode, Integer userId) {
        String result = accessAPI("recent", String.valueOf(userId), "id", null, null, null, null, mode);

        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson(result, Score.class);
    }

    // 用于获取所有的recent
    public List<Score> getRecents(Integer mode, String username) {
        String result = result = accessAPI("recents", username, "string", null, null, null, null, mode);
        result = "[" + result + "]";
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson(result, new TypeToken<List<Score>>() {
                }.getType());
    }

    public List<Score> getRecents(Integer mode, Integer userId) {
        String result = accessAPI("recents", String.valueOf(userId), "id", null, null, null, null, mode);

        result = "[" + result + "]";
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson(result, new TypeToken<List<Score>>() {
                }.getType());
    }

    public List<Score> getFirstScore(Integer mode, Integer bid, Integer rank) {
        String result = accessAPI("first", null, null, String.valueOf(bid), null, rank, null, mode);
        result = "[" + result + "]";
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create()
                .fromJson(result, new TypeToken<List<Score>>() {
                }.getType());

    }

    public List<Score> getScore(Integer mode, Integer bid, Integer uid) {
        String result = accessAPI("score", String.valueOf(uid), "id", String.valueOf(bid), null, null, null, mode);
        result = "[" + result + "]";
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create()
                .fromJson(result, new TypeToken<List<Score>>() {
                }.getType());
    }

    public Lobby getMatch(Integer mid) {
        String result = accessAPI("match", null, null, null, null, null, String.valueOf(mid), null);
        result = "{" + result + "}";
        logger.info(result);
        //他妈的ppysb，上面那些获取单个对象的时候给加个中括号，害的我得在下面删掉再在上面看情况加上
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                //这个的API，date可能是null，而Gson2.8.1会有问题，因此升级Gson库
                .setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson(result, Lobby.class);
    }

    private void fixRank(Userinfo userFromAPI) {
        userFromAPI.setCountRankSs(userFromAPI.getCountRankSs() + userFromAPI.getCountRankSsh());
        userFromAPI.setCountRankS(userFromAPI.getCountRankS() + userFromAPI.getCountRankSh());
    }

    private String accessAPI(String apiType, String uid, String uidType, String bid, String hash, Integer rank, String mid, Integer mode) {
        String URL;
        String failLog;
        String output = null;
        HttpURLConnection httpConnection;
        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("u", uid));
        switch (apiType) {
            case "user":
                URL = getUserURL + "?k=" + key + "&type=" + uidType + "&m=" + mode + "&" + URLEncodedUtils.format(params, "utf-8");
                failLog = "玩家" + uid + "请求API：get_user失败五次";
                break;
            case "bp":
                URL = getBPURL + "?k=" + key + "&m=" + mode + "&type=" + uidType + "&limit=100&" + URLEncodedUtils.format(params, "utf-8");
                failLog = "玩家" + uid + "请求API：get_user_best失败五次";
                break;
            case "beatmap":
                URL = getMapURL + "?k=" + key + "&b=" + bid;
                failLog = "谱面" + bid + "请求API：get_beatmaps失败五次";
                break;
                case "beatmaps":
                URL = getMapURL + "?k=" + key + "&s=" + bid;
                failLog = "谱面" + bid + "请求API：get_beatmaps失败五次";
                break;
            case "beatmapHash":
                URL = getMapURL + "?k=" + key + "&h=" + hash;
                failLog = "谱面" + bid + "请求API：get_beatmaps失败五次";
                break;
            case "recent":
                URL = getRecentURL + "?k=" + key + "&m=" + mode + "&type=" + uidType + "&limit=1&" + URLEncodedUtils.format(params, "utf-8");
                failLog = "玩家" + uid + "请求API：get_recent失败五次";
                break;
            case "recents":
                URL = getRecentURL + "?k=" + key + "&m=" + mode + "&type=" + uidType + "&limit=100&" + URLEncodedUtils.format(params, "utf-8");
                failLog = "玩家" + uid + "请求API：get_recent失败五次";
                break;
            case "first":
                URL = getScoreURL + "?k=" + key + "&m=" + mode + "&limit=" + rank + "&b=" + bid;
                failLog = "谱面" + bid + "请求API：get_scores失败五次";
                break;
            case "score":
                URL = getScoreURL + "?k=" + key + "&m=" + mode + "&type=" + uidType + "&u=" + uid + "&b=" + bid;
                failLog = "谱面" + bid + "请求API：get_scores失败五次";
                break;
            case "match":
                URL = getMatchURL + "?k=" + key + "&mp=" + mid;
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
