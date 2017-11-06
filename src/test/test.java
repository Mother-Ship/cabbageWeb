import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import top.mothership.cabbage.mapper.BaseMapper;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.util.ApiUtil;
import top.mothership.cabbage.util.ScoreUtil;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring/spring-*.xml")
public class test {
    @Autowired
    private BaseMapper baseMapper;
    @Autowired
    private ApiUtil apiUtil;
    @Autowired
    private ScoreUtil scoreUtil;
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
//        final Path path = Paths.get(Overall.CABBAGE_CONFIG.getString("path") + "\\data\\image");
//        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                if (!file.toString().contains("resource")
//                        && !file.toString().contains("!help")
//                        && !file.toString().contains("!smokeAll")
//                        && !file.toString().contains("!helpTrick")) {
//                    System.out.println("正在删除" + file.toString());
//                    Files.delete(file);
//                }
//                return super.visitFile(file, attrs);
//            }
//        };
//        try {
//
//            Files.walkFileTree(path, finder);
//        } catch (IOException e) {
//
//        }
//        DataInputStream reader = new DataInputStream(new FileInputStream("D:\\scores.db"));
//        int version = readInt(reader);
//        int count = readInt(reader);
//        Map<Beatmap, List<Score>> map = new LinkedHashMap<>(count);
//        for (int i = 0; i < count; i++) {
//            String md5 = readString(reader);
//            Beatmap beatmap = apiUtil.getBeatmap(md5);
//            int scoreCount = readInt(reader);
//            List<Score> scores = new ArrayList<>(scoreCount);
//
//            for (int i2 = 0; i2 < scoreCount; i2++) {
//                Score score = new Score();
//                byte mode = readByte(reader);
//                int scoreVersion = readInt(reader);
//                String mapMd5 = readString(reader);
//                String username = readString(reader);
//                String repMd5 = readString(reader);
//                int count300 = readShort(reader);
//                int count100 = readShort(reader);
//                int count50 = readShort(reader);
//                int countGeki = readShort(reader);
//                int countKatu = readShort(reader);
//                int countMiss = readShort(reader);
//                int scoreValue = readInt(reader);
//                int maxCombo = readInt(reader);
//                boolean perfect = readBoolean(reader);
//                int mods = readInt(reader);
////                String empty = readString(reader);
//                long timestamps = readLong(reader);
//                int size = readInt(reader);
//                long onlineId = readLong(reader);
//                LinkedHashMap<String,String> modsMap = scoreUtil.convertMOD(mods);
//                score.setBeatmapId(Integer.valueOf(beatmap.getBeatmapId()));
//                score.setCount50(count50);
//                score.setCount100(count100);
//                score.setCount300(count300);
//                score.setCountGeki(countGeki);
//                score.setCountKatu(countKatu);
//                score.setCountMiss(countMiss);
//                score.setDate(new Date(timestamps));
//                score.setMaxCombo(maxCombo);
//                score.setEnabledMods(mods);
//                score.setScore((long) scoreValue);
//                score.setUserName(username);
//                if (perfect) {
//                    score.setPerfect(1);
//                } else {
//                    score.setPerfect(0);
//                }
//                int noteCount = count50 + count100 + count300 + countMiss;
//                float percent300 = (float) count300 / noteCount;
//                float percent50 = (float) count50 / noteCount;
//
//                if (percent300 < 0.7) {
//                    score.setRank("D");
//                } else if (percent300 <= 0.8) {
//                    score.setRank("C");
//                } else if (percent300 <= 0.85) {
//                    score.setRank("B");
//                }else if(percent300<1&&percent50<0.1){
//                    if(modsMap.keySet().contains("HD")||modsMap.keySet().contains("FL")) {
//                        score.setRank("SH");
//                    }else{
//                        score.setRank("S");
//                    }
//                }else{
//                    if(modsMap.keySet().contains("HD")||modsMap.keySet().contains("FL")) {
//                        score.setRank("XH");
//                    }else{
//                        score.setRank("X");
//                    }
//                }
//                scores.add(score);
//            }
//            map.put(beatmap, scores);
//        }
//        System.out.println("");

    }

    //
    private byte readByte(DataInputStream reader) throws IOException {
        // 1 byte
        return reader.readByte();
    }

    private short readShort(DataInputStream reader) throws IOException {
        // 2 bytes, little endian
        byte[] bytes = new byte[2];
        reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort();
    }

    private int readInt(DataInputStream reader) throws IOException {
        // 4 bytes, little endian
        byte[] bytes = new byte[4];
        reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    private long readLong(DataInputStream reader) throws IOException {
        // 8 bytes, little endian
        byte[] bytes = new byte[8];
        reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getLong();
    }

    private int readULEB128(DataInputStream reader) throws IOException {
        // variable bytes, little endian
        // MSB says if there will be more bytes. If cleared,
        // that byte is the last.
        int value = 0;
        for (int shift = 0; shift < 32; shift += 7) {
            byte b = reader.readByte();
            value |= ((int) b & 0x7F) << shift;

            if (b >= 0) return value; // MSB is zero. End of value.
        }
        throw new IOException("ULEB128 too large");
    }

    private float readSingle(DataInputStream reader) throws IOException {
        // 4 bytes, little endian
        byte[] bytes = new byte[4];
        reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getFloat();
    }

    private double readDouble(DataInputStream reader) throws IOException {
        // 8 bytes little endian
        byte[] bytes = new byte[8];
        reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getDouble();
    }

    private boolean readBoolean(DataInputStream reader) throws IOException {
        // 1 byte, zero = false, non-zero = true
        return reader.readBoolean();
    }

    private String readString(DataInputStream reader) throws IOException {
        // variable length
        // 00 = empty string
        // 0B <length> <char>* = normal string
        // <length> is encoded as an LEB, and is the byte length of the rest.
        // <char>* is encoded as UTF8, and is the string content.
        byte kind = reader.readByte();
        if (kind == 0) return "";
        if (kind != 11) {
            throw new IOException(String.format("String format error: Expected 0x0B or 0x00, found 0x%02X", (int) kind & 0xFF));
        }
        int length = readULEB128(reader);
        if (length == 0) return "";
        byte[] utf8bytes = new byte[length];
        reader.readFully(utf8bytes);
        return new String(utf8bytes, "UTF-8");
    }

    private Date readDate(DataInputStream reader) throws IOException {
        long ticks = readLong(reader);
        long TICKS_AT_EPOCH = 621355968000000000L;
        long TICKS_PER_MILLISECOND = 10000;

        return new Date((ticks - TICKS_AT_EPOCH) / TICKS_PER_MILLISECOND);
    }
//    private void mutual() {
//        User user = baseMapper.getUser("1335734657",null);
//        int uid = 124493;
//        DefaultHttpClient client = new DefaultHttpClient();
//        HttpParams params = client.getParams();
//        //禁用GET请求自动重定向
//        params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
//        HttpGet httpGet = new HttpGet("https://osu.ppy.sh/u/" + uid);
//        List<Cookie> list = new Gson().fromJson(user.getCookie(), new TypeToken<List<BasicClientCookie>>() {
//        }.getType());
//        CookieStore cookieStore = new BasicCookieStore();
//        for (Cookie c : list) {
//            cookieStore.addCookie(c);
//        }
//        client.setCookieStore(cookieStore);
//        HttpResponse response = null;
//        HttpEntity entity;
//        try {
//            response = client.execute(httpGet);
//            entity = response.getEntity();
//            String html = EntityUtils.toString(entity, "GBK");
//            httpGet.releaseConnection();
//            Matcher m = Pattern.compile("<div class='centrep'>\\n<a href='([^']*)").matcher(html);
//            m.find();
//            String addLink = m.group(1);
//            if (addLink.contains("remove")) {
//                System.out.println("你和Cookiezi已经是好友了，请不要重复添加。");
//                return;
//            }
//            httpGet = new HttpGet("https://osu.ppy.sh" + m.group(1));
//            response = client.execute(httpGet);
////            entity = response.getEntity();
////            html = EntityUtils.toString(entity, "GBK");
//            if (response.getStatusLine().getStatusCode() != 200) {
//                //这里是跳转了，获取当前Cookie存入数据库，然后使用verify命令
////                httpGet = new HttpGet("https://osu.ppy.sh/p/verify?r="+addLink);
//                List<Cookie> cookies = client.getCookieStore().getCookies();
//                String CookieNames = "";
//                for (Cookie c : cookies) {
//                    CookieNames = CookieNames.concat(c.getName());
//                }
//                String cookie = new Gson().toJson(cookies);
//                user.setCookie(cookie);
//                baseMapper.updateUser(user);
//                System.out.println("触发验证，请登录osu!并尝试使用!verify命令。");
//            } else {
//                System.out.println("添加成功。");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//    private byte[] readInputStream(InputStream inputStream) throws IOException {
//        byte[] buffer = new byte[1024];
//        int len = 0;
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        while ((len = inputStream.read(buffer)) != -1) {
//            bos.write(buffer, 0, len);
//        }
//        bos.close();
//        return bos.toByteArray();
//    }
}