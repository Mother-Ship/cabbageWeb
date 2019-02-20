import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.junit.Test;
import top.mothership.cabbage.consts.OverallConsts;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.coolq.osu.Beatmap;
import top.mothership.cabbage.task.UpdateOsuClientTasker;
import top.mothership.cabbage.util.StringSimilarityUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleTest {
    @Test
    public void Test() throws IOException {
//        System.out.println(new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson("{\"beatmapset_id\":\"796766\",\"beatmap_id\":\"1673919\",\"approved\":\"-2\",\"total_length\":\"128\",\"hit_length\":\"124\",\"version\":\"Dored's Hard\",\"file_md5\":\"8af6f7b0c47603fd3121b1e3f3d8a385\",\"diff_size\":\"3.5\",\"diff_overall\":\"6\",\"diff_approach\":\"7.5\",\"diff_drain\":\"5\",\"mode\":\"0\",\"approved_date\":null,\"last_update\":\"2018-09-13 04:48:46\",\"artist\":\"himmel\",\"title\":\"Empyrean\",\"creator\":\"Imouto koko\",\"creator_id\":\"7679162\",\"bpm\":\"175\",\"source\":\"Zyon \\u8f09\\u97f3\",\"tags\":\"heavenly trinity dynamix 84461810 papapa213 dored ametrin fushimi rio firika hanazawa kana vert suzuki_1112\",\"genre_id\":\"1\",\"language_id\":\"1\",\"favourite_count\":\"3\",\"playcount\":\"0\",\"passcount\":\"0\",\"max_combo\":\"683\",\"difficultyrating\":\"3.491027355194092\"}", Beatmap.class));
//        System.out.println(new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson("{\"beatmapset_id\":\"13223\",\"beatmap_id\":\"53554\",\"approved\":\"2\",\"total_length\":\"428\",\"hit_length\":\"340\",\"version\":\"Extra Stage\",\"file_md5\":\"2f6b9a08fb595128073a7a8935572a6c\",\"diff_size\":\"4\",\"diff_overall\":\"9\",\"diff_approach\":\"9\",\"diff_drain\":\"8\",\"mode\":\"0\",\"approved_date\":\"2010-05-13 19:26:19\",\"last_update\":\"2010-05-13 19:14:44\",\"artist\":\"Demetori\",\"title\":\"Emotional Skyscraper ~ World's End\",\"creator\":\"happy30\",\"creator_id\":\"27767\",\"bpm\":\"178\",\"source\":\"Touhou\",\"tags\":\"hijiri byakuren touhou 12 cosmic mind nada upasana pundarika mekadon95 ignorethis 2012\",\"genre_id\":\"2\",\"language_id\":\"5\",\"favourite_count\":\"1355\",\"playcount\":\"3035630\",\"passcount\":\"251323\",\"max_combo\":\"2012\",\"difficultyrating\":\"4.99941873550415\"}", Beatmap.class));
//
//


        System.out.println(new ApiManager().getBeatmap(1673919));
//        UpdateOsuClientTasker tasker = new UpdateOsuClientTasker();
//        tasker.setWebPageManager(new WebPageManager());
//        tasker.updateOsuClient();
//        System.out.println(StringSimilarityUtil.calc("Delis' Insane","insane"));
//        System.out.println(StringSimilarityUtil.calc("Insane","insane"));
//        DefaultHttpClient client = new DefaultHttpClient();
//        HttpPost post = new HttpPost("https://osu.ppy.sh/forum/ucp.php?mode=login");
//        //添加请求头
//        java.util.List<NameValuePair> urlParameters = new ArrayList<>();
//        urlParameters.add(new BasicNameValuePair("autologin", "on"));
//        urlParameters.add(new BasicNameValuePair("login", "login"));
//        urlParameters.add(new BasicNameValuePair("username", OverallConsts.CABBAGE_CONFIG.getString("accountForDL")));
//        urlParameters.add(new BasicNameValuePair("password", OverallConsts.CABBAGE_CONFIG.getString("accountForDLPwd")));
//        try {
//            post.setEntity(new UrlEncodedFormEntity(urlParameters));
//            client.execute(post);
//        } catch (Exception ignored) { }
//        List<Cookie> cookies = client.getCookieStore().getCookies();
//        String cookie = "";
//        for (Cookie c : cookies) {
//            cookie = cookie.concat(c.getName()).concat("\n");
//        }
//        System.out.println(cookie);
//        System.out.println("——————————————————");
//        OkHttpClient CLIENT = new OkHttpClient().newBuilder()
//                .followRedirects(false)
//                .followSslRedirects(false)
//                .build();
//        RequestBody formBody = new FormBody.Builder()
//                .add("autologin", "on")
//                .add("login", "login")
//                .add("username", OverallConsts.CABBAGE_CONFIG.getString("accountForDL"))
//                .add("password", OverallConsts.CABBAGE_CONFIG.getString("accountForDLPwd"))
//                .build();
//        Request request = new Request.Builder()
//                .url("https://osu.ppy.sh/forum/ucp.php?mode=login")
//                .post(formBody)
//                .build();
//        StringBuilder cookie2 = new StringBuilder();
//        try (Response response = CLIENT.newCall(request).execute()) {
//            List<okhttp3.Cookie> cookies2 = okhttp3.Cookie.parseAll(request.url(), response.headers());
//            for (okhttp3.Cookie c : cookies2) {
//                cookie2.append(c.name()+"\n");
//            }
//        } catch (Exception ignored) { }
//        System.out.println(cookie2.toString());


//        String a = "123asd";
//        Instant s = Instant.now();
//        pattern ALL_NUMBER_SEARCH_KEYWORD = pattern.compile("^(\\d{1,7})$");
//        for (int i = 0; i < 100000; i++) {
//            try {
//                Integer in = Integer.valueOf(a);
//            } catch (Exception ignore) {
////239
//            }
////            ALL_NUMBER_SEARCH_KEYWORD.matcher(a).find();
////48
//        }
//        System.out.println(Duration.between(s, Instant.now()).toMillis());
//        User a = new User();
//        a.setBanned(false);
//        swap(a);
//        System.out.println(a.isBanned());

//        org.jsoup.nodes.Document doc = Jsoup.connect("https://syrin.me/pp+/api/user/2545898").timeout(10000).get();
//        System.out.println(doc);
    }

//    void swap(User a) {
//        a.setBanned(true);
//    }
}
