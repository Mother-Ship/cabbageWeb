package top.mothership.cabbage.manager;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import top.mothership.cabbage.constant.Overall;
import top.mothership.cabbage.constant.pattern.RegularPattern;
import top.mothership.cabbage.constant.pattern.WebPagePattern;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.pojo.elo.Elo;
import top.mothership.cabbage.pojo.elo.EloChange;
import top.mothership.cabbage.pojo.osu.*;
import top.mothership.cabbage.util.osu.StringSimilarityUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The type Web page manager.
 */
@Component

public class WebPageManager {

    private static final String AVA_URL = "https://a.ppy.sh/";
    private static final String USERPAGE_URL = "https://osu.ppy.sh/u/";
    private static final String USERPAGE_INNER_URL = "https://osu.ppy.sh/pages/include/profile-general.php?u=";
    private static final String BLOODCAT_BG_URL = "http://bloodcat.com/osu/i/";
    private static final String OSU_FILE_URL = "https://osu.ppy.sh/osu/";
    private static final String OSUSEARCH_URL = "https://osusearch.com/query/";
    private static final String PP_PLUS_URL = "http://syrin.me/pp+/u/";
    private static final String OSU_PROFILE_DETAIL_URL = "https://osu.ppy.sh/pages/include/profile-general.php";
    private static final String OSU_CHAN_URL = "https://syrin.me/osuchan/u/";
    private static final String OSU_UPDATE_INFO_URL = "https://osu.ppy.sh/web/check-updates.php?action=check&stream=stable40";
    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private static OkHttpClient client = new OkHttpClient();

    @Autowired
    private ResDAO resDAO;
    private Logger logger = LogManager.getLogger(this.getClass());
    private HashMap<Integer, Document> map = new HashMap<>();
    @Autowired
    private CqManager cqManager;
    @Autowired
    private ApiManager apiManager;

    /**
     * Gets avatar.
     *
     * @param uid the uid
     * @return the avatar
     */
    public BufferedImage getAvatar(int uid) {
        URL avaurl;
        BufferedImage ava;
        BufferedImage resizedAva;
        logger.info("开始获取玩家" + uid + "的头像");
        try {
            avaurl = new URL(AVA_URL + uid + "?.png");
            ava = ImageIO.read(avaurl);
            if (ava != null) {
                //进行缩放
                if (ava.getHeight() > 128 || ava.getWidth() > 128) {
                    //获取原图比例，将较大的值除以128，然后把较小的值去除以这个f
                    int resizedHeight;
                    int resizedWidth;
                    if (ava.getHeight() > ava.getWidth()) {
                        float f = (float) ava.getHeight() / 128;
                        resizedHeight = 128;
                        resizedWidth = (int) (ava.getWidth() / f);
                    } else {
                        float f = (float) ava.getWidth() / 128;
                        resizedHeight = (int) (ava.getHeight() / f);
                        resizedWidth = 128;
                    }
                    resizedAva = new BufferedImage(resizedWidth, resizedHeight, ava.getType());
                    Graphics2D g = (Graphics2D) resizedAva.getGraphics();
                    g.drawImage(ava.getScaledInstance(resizedWidth, resizedHeight, Image.SCALE_SMOOTH), 0, 0, resizedWidth, resizedHeight, null);
                    g.dispose();
                    ava.flush();
                } else {
                    //如果不需要缩小，直接把引用转过来
                    resizedAva = ava;
                }
                return resizedAva;
            } else {
                return null;
            }

        } catch (IOException e) {
            //存在没设置头像的情况 不做提醒
            return null;
        }

    }

    /**
     * Gets bg backup.
     *
     * @param beatmap the beatmap
     * @return the bg backup
     */
    public BufferedImage getBGBackup(Beatmap beatmap) {

        try {



            OsuFile osuFile = parseOsuFile(beatmap);
            if (osuFile == null) {
                cqManager.warn("解析谱面" + beatmap.getBeatmapId() + "的.osu文件中BG名失败。");
                return null;
            }
            byte[] img = (byte[]) resDAO.getBGBySidAndName(beatmap.getBeatmapSetId(), osuFile.getBgName());
            if (img != null) {
                try (ByteArrayInputStream in = new ByteArrayInputStream(img)) {
                    return ImageIO.read(in);
                } catch (IOException e) {
                    cqManager.warn("数据库中" + beatmap.getBeatmapId() + "的背景损坏。");
                }
            }

            Request request = new Request.Builder()
                    .url("https://osu.ppy.sh/home")
                    .build();
            String token = null;
            String session = null;
            try (Response response = client.newCall(request).execute()) {
                if (!Objects.equals(response.code(), HttpStatus.SC_OK)){
                    cqManager.warn("下载谱面" + beatmap.getBeatmapId() + "时访问官网失败");
                    return null;
                }
                for (String s : response.headers().toMultimap().get("Set-Cookie")) {
                    if (s.startsWith("XSRF-TOKEN")){
                        token = s.substring(s.indexOf("=")+1,s.indexOf(";"));
                    }
                    if (s.startsWith("osu_session")){
                        session = s.substring(s.indexOf("=")+1, s.indexOf(";"));
                        break;
                    }
                }
            }

            FormBody.Builder builder = new FormBody.Builder();

            builder.add("_token",token);
            builder.add("username", Overall.CABBAGE_CONFIG.getString("accountForDL"));
            builder.add("password",Overall.CABBAGE_CONFIG.getString("accountForDLPwd"));

            RequestBody body = builder.build();
            request = new Request.Builder()
                    .header("referer","https://osu.ppy.sh/home")
                    .header("cookie","XSRF-TOKEN="+token+"; osu_session="+session)
                    .url("https://osu.ppy.sh/session").post(body).build();

            try (Response response = client.newCall(request).execute()) {
                if (!Objects.equals(response.code(), HttpStatus.SC_OK)){
                    cqManager.warn("下载谱面" + beatmap.getBeatmapId() + "时登陆失败，返回code："+response.code());
                    return null;
                }
                for (String s : response.headers().toMultimap().get("Set-Cookie")) {
                    if (s.startsWith("osu_session")){
                        session = s.substring(s.indexOf("=")+1,s.indexOf(";"));
                        break;
                    }
                }
            }


            request = new Request.Builder()
                    .header("referer","https://osu.ppy.sh/beatmapsets/"+beatmap.getBeatmapSetId())
                    .header("cookie","osu_session="+session)
                    .url("https://osu.ppy.sh/beatmapsets/"+beatmap.getBeatmapSetId()+"/download").build();

            try (Response response = client.newCall(request).execute();
                 ZipInputStream zis = new ZipInputStream(new CheckedInputStream(response.body().byteStream(), new CRC32()))) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    logger.info("当前文件名为：" + entry.getName());
                    byte[] data = new byte[(int) entry.getSize()];
                    int start = 0, end = 0, flag = 0;
                    while (entry.getSize() - start > 0) {
                        end = zis.read(data, start, (int) entry.getSize() - start);
                        if (end <= 0) {
                            logger.info("正在读取" + 100 + "%");
                            break;
                        }
                        start += end;
                        //每20%输出一次，如果为100则为1%
                        if ((start - flag) > (int) entry.getSize() / 5) {
                            flag = start;
                            logger.info("正在读取" + (float) start / entry.getSize() * 100 + "%");
                        }

                    }
                    String filename = entry.getName();
                    if (filename.contains("/")) {
                        filename = filename.substring(filename.indexOf("/") + 1);
                    }
                    if (osuFile.getBgName().equals(filename)) {
                        ByteArrayInputStream in = new ByteArrayInputStream(data);
                        BufferedImage bg = ImageIO.read(in);
                        //懒得重构成方法了_(:з」∠)_
                        //我错了 我不偷懒了_(:з」∠)_
                        BufferedImage resizedBG = resizeImg(bg, 1366, 768);
                        //获取bp原分辨率，将宽拉到1366，然后算出高，减去768除以二然后上下各减掉这部分
                        //在谱面rank状态是Ranked或者Approved时，写入硬盘
                        if (beatmap.getApproved() == 1 || beatmap.getApproved() == 2) {
                            //扩展名直接从文件里取
                            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                                ImageIO.write(resizedBG, osuFile.getBgName().substring(osuFile.getBgName().lastIndexOf(".") + 1), out);
                                resizedBG.flush();
                                byte[] imgBytes = out.toByteArray();
                                resDAO.addBG(beatmap.getBeatmapSetId(), osuFile.getBgName(), imgBytes);
                            } catch (IOException e) {
                                cqManager.warn("解析谱面" + beatmap.getBeatmapId() + "的ZIP流时出现异常，", e);
                                return null;
                            }
                        }
                        in.close();
                        return resizedBG;
                    }

                }

            } catch (Exception e) {
                cqManager.warn("获取谱面" + beatmap.getBeatmapId() + "的ZIP流时出现异常，", e);
                return null;
            }
        } catch (Exception e) {
            cqManager.warn("从官网获取谱面" + beatmap.getBeatmapId() + "的背景时出现异常");
            return null;
        }
        return null;

    }

    /**
     * Gets bg.
     *
     * @param beatmap the beatmap
     * @return the bg
     * @throws NullPointerException the null pointer exception
     */
    public BufferedImage getBG(Beatmap beatmap) {
        return null;
//        logger.info("开始获取谱面" + beatmap.getBeatmapId() + "的背景");
//        HttpURLConnection httpConnection;
//        int retry = 0;
//        BufferedImage bg;
//        BufferedImage resizedBG = null;
//        OsuFile osuFile = parseOsuFile(beatmap);
//
//        if (osuFile == null) {
//            //08年老图是没有BG的……
//            cqManager.warn("解析谱面" + beatmap.getBeatmapId() + "的.osu文件中BG名失败。");
//            return null;
//        }
//        //这里dao层需要使用object，然后再这里转换为数组，于是判断非空就得用null而不是.length。
//        byte[] img = (byte[]) resDAO.getBGBySidAndName(beatmap.getBeatmapSetId(), osuFile.getBgName());
//        if (img != null) {
//            try (ByteArrayInputStream in = new ByteArrayInputStream(img)) {
//                return ImageIO.read(in);
//            } catch (IOException e) {
//                cqManager.warn("数据库中" + beatmap.getBeatmapId() + "的背景损坏。");
//            }
//        }
//        while (retry < 5) {
//            try {
//                httpConnection =
//                        (HttpURLConnection) new URL(BLOODCAT_BG_URL + beatmap.getBeatmapId()).openConnection();
//                httpConnection.setRequestMethod("GET");
//                httpConnection.setConnectTimeout((int) Math.pow(2, retry + 1) * 1000);
//                httpConnection.setReadTimeout((int) Math.pow(2, retry + 1) * 1000);
//                httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.40 Safari/537.36");
//                if (httpConnection.getResponseCode() != 200) {
//                    logger.error("HTTP GET请求失败: " + httpConnection.getResponseCode() + "，正在重试第" + (retry + 1) + "次");
//                    retry++;
//                    continue;
//                }
//                //读取返回结果
//                bg = ImageIO.read(httpConnection.getInputStream());
//                if (bg == null) {
//                    return null;
//                }
//
//                resizedBG = resizeImg(bg, 1366, 768);
//                //在谱面rank状态是Ranked或者Approved时，写入硬盘
//                if (beatmap.getApproved() == 1 || beatmap.getApproved() == 2) {
//                    //扩展名直接从文件里取
//                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//                        //修正扩展名为最后一个点后面的内容2017-11-15 13:17:56
//                        ImageIO.write(resizedBG, osuFile.getBgName().substring(osuFile.getBgName().lastIndexOf(".") + 1), out);
//                        resizedBG.flush();
//                        img = out.toByteArray();
//                        resDAO.addBG(beatmap.getBeatmapSetId(), osuFile.getBgName(), img);
//                    } catch (IOException e) {
//                        logger.error("写入图片时出现IO异常：" + e.getMessage());
//                        return null;
//                    }
//                }
//                //手动关闭流
//                httpConnection.disconnect();
//                break;
//            } catch (IOException e) {
//                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
//                retry++;
//            }
//
//        }
//        if (retry == 5) {
//            logger.error("获取" + beatmap.getBeatmapId() + "的背景图，失败五次");
//            return null;
//        }
//        return resizedBG;

    }

    /**
     * Gets rep watched.
     *
     * @param uid the uid
     * @return the rep watched
     */
    public int getRepWatched(int uid) {
        int retry = 0;
        Document doc = null;
        while (retry < 5) {
            try {
                logger.info("正在获取" + uid + "的Replays被观看次数");
                doc = Jsoup.connect(USERPAGE_INNER_URL + uid).timeout((int) Math.pow(2, retry + 1) * 1000).get();
                break;
            } catch (IOException e) {
                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                retry++;
            }
        }
        if (retry == 5) {
            logger.error("玩家" + uid + "请求API获取数据，失败五次");
            return 0;
        }
        Elements link = doc.select("div[title*=replays]");
        String a = link.text();
        a = a.substring(27).replace(" times", "").replace(",", "");
        return Integer.valueOf(a);
    }

    /**
     * Gets rank.
     *
     * @param rScore the r score
     * @param start  the start
     * @param end    the end
     * @return the rank
     */
    public int getRank(long rScore, int start, int end) {
        long endValue = getScore(end);
        if (rScore < endValue || endValue == 0) {
            map.clear();
            return 0;
        }
        if (rScore == endValue) {
            map.clear();
            return end;
        }
        //第一次写二分法……不过大部分时间都花在算准确页数，和拿页面元素上了
        while (start <= end) {
            int middle = (start + end) / 2;
            long middleValue = getScore(middle);

            if (middleValue == 0) {
                map.clear();
                return 0;
            }
            if (rScore == middleValue) {
                // 等于中值直接返回
                //清空掉缓存
                map.clear();
                return middle;
            } else if (rScore > middleValue) {
                //rank和分数成反比，所以大于反而rank要在前半部分找
                end = middle - 1;
            } else {
                start = middle + 1;
            }
        }
        map.clear();
        return 0;
    }

    /**
     * Gets last active.
     *
     * @param uid the uid
     * @return the last active
     */
    public Date getLastActive(int uid) {
        int retry = 0;
        Document doc = null;
        while (retry < 5) {
            try {
                logger.info("正在获取" + uid + "的上次活跃时间");
                doc = Jsoup.connect(USERPAGE_URL + uid).timeout((int) Math.pow(2, retry + 1) * 1000).get();
                break;
            } catch (IOException e) {
                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                retry++;
            }
        }
        if (retry == 5) {
            logger.error("玩家" + uid + "请求API获取数据，失败五次");
            return null;
        }
        Elements link = doc.select("time[class*=timeago]");
        if (link.size() == 0) {
            return null;
        }
        String a = link.get(1).text();
        a = a.substring(0, 19);
        try {
            //转换为北京时间
            return new Date(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(a).getTime() + 8 * 3600 * 1000);
        } catch (ParseException e) {
            logger.error("将时间转换为Date对象出错");
        }
        return null;
    }

    /**
     * Gets osu file.
     *
     * @param beatmap the beatmap
     * @return the osu file
     */
    public String getOsuFile(Beatmap beatmap) {
        HttpURLConnection httpConnection;
        String osuFile = resDAO.getOsuFileBybid(beatmap.getBeatmapId());
        if (osuFile != null) {
            return osuFile;
        }
        int retry = 0;
        //获取.osu的逻辑和获取BG不一样，Qua的图BG不缓存，而.osu必须缓存
        //即使是qua的图，也必须有sid的文件夹


        while (retry < 5) {
            try {
                httpConnection =
                        (HttpURLConnection) new URL(OSU_FILE_URL + beatmap.getBeatmapId()).openConnection();
                httpConnection.setRequestMethod("GET");
                httpConnection.setConnectTimeout((int) Math.pow(2, retry + 1) * 1000);
                httpConnection.setReadTimeout((int) Math.pow(2, retry + 1) * 1000);
                if (httpConnection.getResponseCode() != 200) {
                    logger.error("HTTP GET请求失败: " + httpConnection.getResponseCode() + "，正在重试第" + (retry + 1) + "次");
                    retry++;
                    continue;
                }
                //将返回结果读取为Byte数组
                osuFile = new String(readInputStream(httpConnection.getInputStream()), "UTF-8");
                if (beatmap.getApproved() == 1 || beatmap.getApproved() == 2) {
                    resDAO.addOsuFile(beatmap.getBeatmapId(), osuFile);
                }
                //手动关闭连接
                httpConnection.disconnect();
                return osuFile;
            } catch (IOException e) {
                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                retry++;
            }

        }
        if (retry == 5) {
            logger.error("获取" + beatmap.getBeatmapId() + "的.osu文件，失败五次");
        }
        return null;
    }

    /**
     * 对接osu search进行谱面搜索的方法。
     *
     * @return 谱面
     */
    public Beatmap searchBeatmap(SearchParam searchParam, Integer mode) {
        int retry = 0;
        Beatmap beatmap = null;
        DecimalFormat FOUR_DEMENSIONS = new DecimalFormat("#0.00");
        while (retry < 5) {
            HttpURLConnection httpConnection;
            try {
                String url = OSUSEARCH_URL;

                List<NameValuePair> params = new LinkedList<>();
                if (!"".equals(searchParam.getTitle())) {
                    params.add(new BasicNameValuePair("title", searchParam.getTitle()));
                }
                if (!"".equals(searchParam.getArtist())) {
                    params.add(new BasicNameValuePair("artist", searchParam.getArtist()));
                }
                if (!"".equals(searchParam.getMapper())) {
                    params.add(new BasicNameValuePair("mapper", searchParam.getMapper()));
                }
                if (!"".equals(searchParam.getDiffName())) {
                    params.add(new BasicNameValuePair("diff_name", searchParam.getDiffName()));
                }
                if (searchParam.getAr() != null) {
                    params.add(new BasicNameValuePair("ar",
                            "(" + FOUR_DEMENSIONS.format(searchParam.getAr()) + "," + FOUR_DEMENSIONS.format(searchParam.getAr()) + ")"));
                }
                if (searchParam.getOd() != null) {
                    params.add(new BasicNameValuePair("od",
                            "(" + FOUR_DEMENSIONS.format(searchParam.getOd()) + "," + FOUR_DEMENSIONS.format(searchParam.getOd()) + ")"));
                }
                if (searchParam.getCs() != null) {
                    params.add(new BasicNameValuePair("cs",
                            "(" + FOUR_DEMENSIONS.format(searchParam.getCs()) + "," + FOUR_DEMENSIONS.format(searchParam.getCs()) + ")"));
                }
                if (searchParam.getHp() != null) {
                    params.add(new BasicNameValuePair("hp",
                            "(" + FOUR_DEMENSIONS.format(searchParam.getHp()) + "," + FOUR_DEMENSIONS.format(searchParam.getHp()) + ")"));
                }
                //虽然osu search支持多模式搜索，但这个命令本来就只取一个结果，还是switch吧

                switch (mode) {
                    case 0:
                        params.add(new BasicNameValuePair("modes", "Standard"));
                        break;
                    case 1:
                        params.add(new BasicNameValuePair("modes", "Taiko"));
                        break;
                    case 2:
                        params.add(new BasicNameValuePair("modes", "CtB"));
                        break;
                    case 3:
                        params.add(new BasicNameValuePair("modes", "Mania"));
                        break;
                    default:
                        break;

                }

                params.add(new BasicNameValuePair("query_order", "play_count"));

                url += "?" + URLEncodedUtils.format(params, "utf-8");

                httpConnection =
                        (HttpURLConnection) new URL(url).openConnection();
                //设置请求头

                httpConnection.setRequestMethod("GET");
                httpConnection.setConnectTimeout((int) Math.pow(2, retry + 1) * 1000);
                httpConnection.setReadTimeout((int) Math.pow(2, retry + 1) * 1000);
                httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.40 Safari/537.36");
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
                OsuSearchResp osuSearchResp = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create().fromJson(tmp2.toString(), OsuSearchResp.class);
                if (osuSearchResp.getResultCount() > 0) {
                    beatmap = osuSearchResp.getBeatmaps().get(0);
                    if (searchParam.getDiffName() != null && beatmap != null) {
                        float maxSimilar = 0;
                        List<Beatmap> list = apiManager.getBeatmaps(beatmap.getBeatmapSetId());
                        for (Beatmap osuSearchRespBeatmap : list) {
                            float similar = StringSimilarityUtil.calc(osuSearchRespBeatmap.getVersion(), searchParam.getDiffName());
                            if (similar > maxSimilar) {
                                beatmap = osuSearchRespBeatmap;
                                maxSimilar = similar;
                            }
                        }
                    }
                }
                //手动关闭流
                httpConnection.disconnect();
                responseBuffer.close();
                return beatmap;
            } catch (IOException e) {
                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                retry++;
            }
        }

        if (retry == 5) {
            logger.error("搜索谱面失败");
            return null;
        }
        return null;
    }

    public Elo getElo(int uid) {
        return new RestTemplate().getForObject("http://api.osuwiki.cn:5005/api/users/elo/" + uid, Elo.class);
    }

    public EloChange getEloChange(int uid) {
        return new RestTemplate().getForObject("http://api.osuwiki.cn:5005/api/users/recentPlay/" + uid, EloChange.class);
    }

    /**
     * Gets pp plus.
     *
     * @param uid the uid
     * @return the pp plus
     */
    public Map<String, Double> getPPPlus(int uid) {
        Map<String, Double> map = new HashMap<>();

        Document doc = null;
        try {
            doc = Jsoup.connect(PP_PLUS_URL + uid).timeout(300000).get();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (doc == null) {
            return null;
        }
        Elements link = doc.select("tr[class*=perform]");
        if (link.size() == 0) {
            logger.error("玩家" + uid + "访问PP+失败");
            return null;
        }

        map.put("Jump", Double.valueOf(link.get(2).children().get(1).text().replaceAll("[p,]", "")));
        map.put("Flow", Double.valueOf(link.get(3).children().get(1).text().replaceAll("[p,]", "")));
        map.put("Precision", Double.valueOf(link.get(4).children().get(1).text().replaceAll("[p,]", "")));
        map.put("Speed", Double.valueOf(link.get(5).children().get(1).text().replaceAll("[p,]", "")));
        map.put("Stamina", Double.valueOf(link.get(6).children().get(1).text().replaceAll("[p,]", "")));
        map.put("Accuracy", Double.valueOf(link.get(7).children().get(1).text().replaceAll("[p,]", "")));
        return map;
    }

    /**
     * 让图片肯定不会变形，但是会切掉东西的拉伸
     *
     * @param bg     the bg
     * @param weight the weight
     * @param height the height
     * @return the buffered image
     */
    public BufferedImage resizeImg(BufferedImage bg, Integer weight, Integer height) {

        BufferedImage resizedBG;
        //获取bp原分辨率，将宽拉到1366，然后算出高，减去768除以二然后上下各减掉这部分
        int resizedWeight = weight;
        int resizedHeight = (int) Math.ceil((float) bg.getHeight() / bg.getWidth() * weight);
        int heightDiff = ((resizedHeight - height) / 2);
        int widthDiff = 0;
        //如果算出重画之后的高<768(遇到金盏花这种特别宽的)
        if (resizedHeight < height) {
            resizedWeight = (int) Math.ceil((float) bg.getWidth() / bg.getHeight() * height);
            resizedHeight = height;
            heightDiff = 0;
            widthDiff = ((resizedWeight - weight) / 2);
        }
        //把BG横向拉到1366;
        //忘记在这里处理了
        BufferedImage resizedBGTmp = new BufferedImage(resizedWeight, resizedHeight, bg.getType());
        Graphics2D g = resizedBGTmp.createGraphics();
        g.drawImage(bg.getScaledInstance(resizedWeight, resizedHeight, Image.SCALE_SMOOTH), 0, 0, resizedWeight, resizedHeight, null);
        g.dispose();

        //切割图片
        resizedBG = new BufferedImage(weight, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < weight; x++) {
            //这里之前用了原bg拉伸之前的分辨率，难怪报错
            for (int y = 0; y < height; y++) {
                resizedBG.setRGB(x, y, resizedBGTmp.getRGB(x + widthDiff, y + heightDiff));
            }
        }
        //刷新掉bg以及临时bg的缓冲，将其作废
        bg.flush();
        resizedBGTmp.flush();
        return resizedBG;
    }

    public List<Integer> getCorrectXAndSRank(Integer mode, Integer uid) {
        List<Integer> list = new ArrayList<>();
        int retry = 0;
        Document doc = null;
        while (retry < 5) {
            try {
                doc = Jsoup.connect(OSU_PROFILE_DETAIL_URL + "?u=" + uid + "&m=" + mode).timeout((int) Math.pow(2, retry + 1) * 1000).get();
                break;
            } catch (IOException e) {
                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                retry++;
            }
        }
        if (retry == 5) {
            logger.error("玩家" + uid + "访问官网失败五次");
            return null;
        }
        Matcher m = WebPagePattern.CORRECT_X_S.matcher(doc.outerHtml());
        if (m.find()) {
            if (!"".equals(m.group(1))) {
                list.add(Integer.valueOf(m.group(1)));
            }
            if (!"".equals(m.group(2))) {
                list.add(Integer.valueOf(m.group(2)));
            }
        }
        if (list.size() == 2) {
            return list;
        } else {
            return null;
        }
    }

    public Map<String, Integer> getOsuChanBestBpmAndLength(int uid) {
        Map<String, Integer> map = new HashMap<>();
        int retry = 0;
        Document doc = null;
        while (retry < 5) {
            try {
                logger.info("正在获取" + uid + "的osu! Chan数据");
                doc = Jsoup.connect(OSU_CHAN_URL + uid).timeout((int) Math.pow(2, retry + 1) * 1000).get();
                break;
            } catch (IOException e) {
                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                retry++;
            }
        }
        if (retry == 5) {
            logger.error("玩家" + uid + "访问osu! Chan失败五次");
            return null;
        }
        Elements link = doc.select("span[title*=Best BPM weighted by pp]");
        Elements link2 = doc.select("span[title*=Best length weighted by pp]");
        if (link.size() == 0 || link2.size() == 0) {
            logger.error("玩家" + uid + "访问osu! Chan失败五次");
            return null;
        }
        String bestLengthRaw = link2.parents().get(0).text();
        String[] tmp = bestLengthRaw.split(":");
        try {
            Integer bestBpm = Integer.valueOf(link.parents().get(0).text());
            Integer bestLength = Integer.valueOf(tmp[0]) * 60 + Integer.valueOf(tmp[1]);
            map.put("BPM", bestBpm);
            map.put("Length", bestLength);
            return map;
        } catch (Exception e) {
            logger.error("玩家" + uid + "访问osu! Chan失败五次");
            return null;
        }
    }

    public List<ClientFile> getOsuClientInfo() {
        int retry = 0;
        Document doc = null;
        while (retry < 5) {
            try {
                doc = Jsoup.connect(OSU_UPDATE_INFO_URL).timeout((int) Math.pow(2, retry + 1) * 1000).get();
                break;
            } catch (IOException e) {
                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                retry++;
            }
        }
        if (retry == 5) {
            logger.error("请求更新信息失败五次。");
            return null;
        }
        String text = doc.body().text();
        return new Gson().fromJson(text, new TypeToken<List<ClientFile>>() {
        }.getType());
    }

    private long getScore(int rank) {
        Document doc = null;
        int retry = 0;
        logger.info("正在抓取#" + rank + "的玩家的分数");
        //一定要把除出来的值强转
        //math.round好像不太对，应该是ceil
        int p = (int) Math.ceil((float) rank / 50);
        //获取当前rank在当前页的第几个
        int num = (rank - 1) % 50;
        //避免在同一页内的连续查询，将上次查询的doc和p缓存起来
        if (map.get(p) == null) {
            while (retry < 5) {
                try {
                    doc = Jsoup.connect("https://osu.ppy.sh/rankings/osu/score?page=" + p).timeout((int) Math.pow(2, retry + 1) * 1000).get();
                    break;
                } catch (IOException e) {
                    logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                    retry++;
                }

            }
            if (retry == 5) {
                logger.error("查询分数失败五次");
                return 0;
            }
            map.put(p, doc);
        } else {
            doc = map.get(p);
        }
        String score = doc.select("td[class*=focused]").get(num).child(0).attr("title");
        return Long.valueOf(score.replace(",", ""));

    }

    /**
     * Prase osu file osu file.
     * 这个方法只能处理ranked/approved/qualified的.osu文件,在目前的业务逻辑里默认.osu文件是存在的。
     * 方法名大包大揽，其实我只能处理出BG名字（
     *
     * @param beatmap the beatmap
     * @return the osu file
     */

    private OsuFile parseOsuFile(Beatmap beatmap) {
        //先获取
        //2017-12-30 18:53:37改为从网页获取（不是所有的osu文件都缓存了
        String osuFile = getOsuFile(beatmap);
        String bgName;
        Matcher m = RegularPattern.BGNAME_REGEX.matcher(osuFile);
        if (m.find()) {
            OsuFile result = new OsuFile();
            bgName = m.group(1);
            result.setBgName(bgName);
            return result;
        } else {
            return null;
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
