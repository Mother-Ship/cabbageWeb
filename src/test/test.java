import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import top.mothership.cabbage.util.Constant;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
//       List<Cookie> cookies =  client.getCookieStore().getCookies();
//       if(cookies.size()>1){
//           System.out.println("Login Success");
//           String cookie = new Gson().toJson(cookies);
//           System.out.println(cookie);
//           DefaultHttpClient client2 = new DefaultHttpClient();
//           CookieStore cookieStore2 = new BasicCookieStore();
//           List<Cookie> list =  new Gson().fromJson(cookie, new TypeToken<List<BasicClientCookie>>() {}.getType());
//           for (Cookie c:list){
//               cookieStore2.addCookie(c);
//           }
//           client2.setCookieStore(cookieStore2);
//           HttpGet httpGet = new HttpGet("https://osu.ppy.sh/u/124493");
//           response = client2.execute(httpGet);
//           HttpEntity entity = response.getEntity();
//           entity = response.getEntity();
//           String html = EntityUtils.toString(entity, "GBK");
//           httpGet.releaseConnection();
//           System.out.println(html);
//           Matcher m = Pattern.compile("<div class='centrep'>\\n<a href='([^']*)").matcher(html);
//           m.find();
//
//           System.out.println(m.group(1));
//
//
//
//       }else{
//           System.out.println("Login Failed");
//       }


//

//        DefaultHttpClient httpclient2 = new DefaultHttpClient();
//        httpclient2.setCookieStore(cookiestore);
//        HttpGet httpGet = new HttpGet("https://osu.ppy.sh/d/10217");
//        HttpResponse httpResponse = httpclient2.execute(httpGet);
//        // 获取响应消息实体
//        HttpEntity entity = httpResponse.getEntity();
//        InputStream is = entity.getContent();
//        //直接包装为ZipInputStream
//        //得实现
//        ZipInputStream zis = new ZipInputStream(new CheckedInputStream(is, new CRC32()));
//        ZipEntry entry = null;
//        while ((entry = zis.getNextEntry()) != null) {
//
//            int count;
//            byte data[] = new byte[(int) entry.getSize()];
//            int start = 0, end = 0;
//            while (entry.getSize() - start > 0) {
//                end = zis.read(data, start, (int)entry.getSize()-start);
//                if (end <= 0) {
//                    break;
//                }
//                start += end;
//            }
//            System.out.println(entry.getName());
//            System.out.println(data);
//
//        }
        // 响应状态
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
//        String osuFile = null;
//        File osu = new File("c:\\coolq pro\\data\\image\\resource\\osu\\403612\\877625.osu");
//        try (FileInputStream fis = new FileInputStream(osu);
//             ByteArrayOutputStream bos = new ByteArrayOutputStream()){
//            byte[] buffer = new byte[1024];
//            int len = 0;
//            while ((len = fis.read(buffer)) != -1) {
//                bos.write(buffer, 0, len);
//            }
//            osuFile = new String(bos.toByteArray(), Charset.forName("UTF-8"));
//            System.out.println(osuFile);
//        } catch(IOException e){
//            e.printStackTrace();
//        }
//        Matcher m = Pattern.compile("(?<=\\[Events]\\r\\n)([^\\r\\n]*)\\r\\n([^\\r\\n]*)").matcher(osuFile);
//        m.find();
//        osuFile="0,0,\"bg.png\",0,0";
//        osuFile = "0,0,\"deetz.jpg\",0,0";
//        System.out.println(osuFile);
//        Matcher m=Pattern.compile("(?<=[\\d*],[\\d*],\")(.*\\.(jpg)|.*\\.(png))").matcher(osuFile);
//        m.find();
//        System.out.println(m.group(0));
        final Path path = Paths.get(Constant.CABBAGE_CONFIG.getString("path") + "\\data\\image");
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.toString().contains("resource")
                        && !file.toString().contains("!help")
                        && !file.toString().contains("!smokeAll")
                        && !file.toString().contains("!helpTrick")) {
                    System.out.println("正在删除" + file.toString());
                    Files.delete(file);
                }
                return super.visitFile(file, attrs);
            }
        };
        try {

            Files.walkFileTree(path, finder);
        } catch (IOException e) {

        }
    }

    private byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        return bos.toByteArray();
    }

}