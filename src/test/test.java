import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class test {
    @Test
    public void Test() throws IOException {
//        System.out.println("mp4".compareTo("mp5"));
//        System.out.println("mp3".compareTo("mp4"));
//        System.out.println("dev".compareTo("mp4"));
//        System.out.println("creep".compareTo("mp4chart"));
//        System.out.println("mp4chart".compareTo("mp5chart"));
//        List<String> roles = Arrays.asList("mp5,mp4,mp4chart,mp5chart,dev,creep,mp3,mp5mc".split(","));
//        Collections.reverse(roles);
//        System.out.println(roles);
//        roles = Arrays.asList("mp5,mp4,mp4chart,mp5chart,dev,creep,mp3,mp5mc".split(","));
//        Collections.sort(roles);
//        System.out.println(roles);
//        roles = Arrays.asList("mp5,mp4,mp4chart,mp5chart,dev,creep,mp3,mp5mc".split(","));
//        Collections.sort(roles);
//        Collections.reverse(roles);
//        System.out.println(roles);
//        [
//        {
//            "domain": ".ppy.sh",
//                "expirationDate": 1510886700,
//                "hostOnly": false,
//                "httpOnly": true,
//                "name": "phpbb3_2cjk5_sid",
//                "path": "/",
//                "sameSite": "no_restriction",
//                "secure": false,
//                "session": false,
//                "storeId": "0",
//                "value": "f8b253e739f284ad2e89fc831f55b34f",
//                "id": 7
//        }
//        ]
//        DefaultHttpClient client = new DefaultHttpClient();
//        HttpPost post = new HttpPost("https://osu.ppy.sh/forum/ucp.php?mode=login");
//        //添加请求头
//        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
//        urlParameters.add(new BasicNameValuePair("autologin", "on"));
//        urlParameters.add(new BasicNameValuePair("login", "login"));
//        urlParameters.add(new BasicNameValuePair("username", "Mother Ship"));
//        urlParameters.add(new BasicNameValuePair("password", "3133170-="));
//        post.setEntity(new UrlEncodedFormEntity(urlParameters));
//        HttpResponse response = client.execute(post);
//        CookieStore cookiestore=client.getCookieStore();
//        DefaultHttpClient httpclient2=new DefaultHttpClient();
//        httpclient2.setCookieStore(cookiestore);
//        HttpGet httpGet = new HttpGet("https://osu.ppy.sh/d/10217");
//       HttpResponse httpResponse=  httpclient2.execute(httpGet);
//        // 获取响应消息实体
//        HttpEntity entity = httpResponse.getEntity();
//        // 响应状态
//        System.out.println("status:" + httpResponse.getStatusLine());
//        System.out.println("headers:");
//        HeaderIterator iterator = httpResponse.headerIterator();
//        while (iterator.hasNext()) {
//            System.out.println("\t" + iterator.next());
//        }
//        // 判断响应实体是否为空
//        if (entity != null) {
//            String responseString = EntityUtils.toString(entity);
//            System.out.println("response length:" + responseString.length());
//            System.out.println("response content:"
//                    + responseString.replace("\r\n", ""));
//        }
//
    }


}