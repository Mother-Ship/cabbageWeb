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
import top.mothership.cabbage.pojo.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleTest {
    @Test
    public void Test() throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("https://osu.ppy.sh/forum/ucp.php?mode=login");
        //添加请求头
        java.util.List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("autologin", "on"));
        urlParameters.add(new BasicNameValuePair("login", "login"));
        urlParameters.add(new BasicNameValuePair("username", OverallConsts.CABBAGE_CONFIG.getString("accountForDL")));
        urlParameters.add(new BasicNameValuePair("password", OverallConsts.CABBAGE_CONFIG.getString("accountForDLPwd")));
        try {
            post.setEntity(new UrlEncodedFormEntity(urlParameters));
            client.execute(post);
        } catch (Exception ignored) { }
        List<Cookie> cookies = client.getCookieStore().getCookies();
        String cookie = "";
        for (Cookie c : cookies) {
            cookie = cookie.concat(c.getName()).concat("\n");
        }
        System.out.println(cookie);
        System.out.println("——————————————————");
        OkHttpClient CLIENT = new OkHttpClient().newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
        RequestBody formBody = new FormBody.Builder()
                .add("autologin", "on")
                .add("login", "login")
                .add("username", OverallConsts.CABBAGE_CONFIG.getString("accountForDL"))
                .add("password", OverallConsts.CABBAGE_CONFIG.getString("accountForDLPwd"))
                .build();
        Request request = new Request.Builder()
                .url("https://osu.ppy.sh/forum/ucp.php?mode=login")
                .post(formBody)
                .build();
        StringBuilder cookie2 = new StringBuilder();
        try (Response response = CLIENT.newCall(request).execute()) {
            List<okhttp3.Cookie> cookies2 = okhttp3.Cookie.parseAll(request.url(), response.headers());
            for (okhttp3.Cookie c : cookies2) {
                cookie2.append(c.name()+"\n");
            }
        } catch (Exception ignored) { }
        System.out.println(cookie2.toString());


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
