package top.mothership.cabbage.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.Beatmap;
import top.mothership.cabbage.pojo.OsuFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebPageUtil {
    private Logger logger = LogManager.getLogger(this.getClass());
    private final String getAvaURL = "https://a.ppy.sh/";
    private final String getUserURL = "https://osu.ppy.sh/u/";
    private final String getUserProfileURL = "https://osu.ppy.sh/pages/include/profile-general.php?u=";
    private final String getBGURL = "http://bloodcat.com/osu/i/";
    private final String getOsuURL = "https://osu.ppy.sh/osu/";
    private HashMap<Integer, Document> map = new HashMap<>();
    private static ResourceBundle rb = ResourceBundle.getBundle("cabbage");

    public BufferedImage getAvatar(int uid) {
        URL avaurl;
        BufferedImage ava;
        BufferedImage resizedAva = null;
        logger.info("开始获取玩家"+uid+"的头像");
        try {
            avaurl = new URL(getAvaURL + uid + "?.png");
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
            logger.error("从官网获取头像失败");
            logger.error(e.getMessage());
            return null;
        }

    }
    public BufferedImage getBGBackup(Beatmap beatmap){
        logger.info("开始从官网获取谱面"+beatmap.getBeatmapId()+"的背景");
        return null;
    }
    public BufferedImage getBG(Beatmap beatmap) {
        logger.info("开始获取谱面"+beatmap.getBeatmapId()+"的背景");
        HttpURLConnection httpConnection;
        int retry = 0;
        BufferedImage bg;
        BufferedImage resizedBG = null;
        OsuFile osuFile = praseOsuFile(beatmap);
        File bgFile = new File(rb.getString("path") + "\\data\\image\\resource\\osu\\"
                + beatmap.getBeatmapSetId()+"\\" +osuFile.getBgName());
        logger.debug(bgFile.length());
        if (bgFile.length() > 0 && (beatmap.getApproved() == 1 || beatmap.getApproved() == 2)) {
            //如果osu文件大小大于0，并且状态是ranked
            try {
                return ImageIO.read(new File(rb.getString("path") + "\\data\\image\\resource\\osu\\" + beatmap.getBeatmapSetId()+"\\"+ osuFile.getBgName()));
                //这个异常几乎肯定是不会出现的……
            } catch (IOException e) {
            }
        }

        while (retry < 5) {
            try {
                httpConnection =
                        (HttpURLConnection) new URL(getBGURL + beatmap.getBeatmapId()).openConnection();
                httpConnection.setRequestMethod("GET");
                httpConnection.setConnectTimeout((int) Math.pow(2, retry + 1) * 1000);
                httpConnection.setReadTimeout((int) Math.pow(2, retry + 1) * 1000);
                httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.40 Safari/537.36");
                if (httpConnection.getResponseCode() != 200) {
                    logger.error("HTTP GET请求失败: " + httpConnection.getResponseCode() + "，正在重试第" + (retry + 1) + "次");
                    retry++;
                    continue;
                }
                //读取返回结果
                bg = ImageIO.read(httpConnection.getInputStream());
                Matcher m = Pattern.compile(Constant.DOWNLOAD_FILENAME_REGEX)
                        .matcher(httpConnection.getHeaderFields().get("Content-Disposition").get(0));
                m.find();

                //获取bp原分辨率，将宽拉到1366，然后算出高，减去768除以二然后上下各减掉这部分
                int resizedWeight = 1366;
                int resizedHeight = (int) Math.ceil((float) bg.getHeight() / bg.getWidth() * 1366);
                int heightDiff = ((resizedHeight - 768) / 2);
                int widthDiff = 0;
                //如果算出重画之后的高<768(遇到金盏花这种特别宽的)
                if (resizedHeight < 768) {
                    resizedWeight = (int) Math.ceil((float) bg.getWidth() / bg.getHeight() * 768);
                    resizedHeight = 768;
                    heightDiff = 0;
                    widthDiff = ((resizedWeight - 1366) / 2);
                }
                //把BG横向拉到1366;
                //忘记在这里处理了
                BufferedImage resizedBGTmp = new BufferedImage(resizedWeight, resizedHeight, bg.getType());
                Graphics2D g = resizedBGTmp.createGraphics();
                g.drawImage(bg.getScaledInstance(resizedWeight, resizedHeight, Image.SCALE_SMOOTH), 0, 0, resizedWeight, resizedHeight, null);
                g.dispose();

                //切割图片
                resizedBG = new BufferedImage(1366, 768, BufferedImage.TYPE_INT_RGB);
                for (int x = 0; x < 1366; x++) {
                    //这里之前用了原bg拉伸之前的分辨率，难怪报错
                    for (int y = 0; y < 768; y++) {
                        resizedBG.setRGB(x, y, resizedBGTmp.getRGB(x + widthDiff, y + heightDiff));
                    }
                }
                //刷新掉bg以及临时bg的缓冲，将其作废
                resizedBGTmp.flush();
                bg.flush();
                //在谱面rank状态是Ranked或者Approved时，写入硬盘
                if(beatmap.getApproved() == 1 || beatmap.getApproved() == 2) {
                    //扩展名直接从文件里取
                    ImageIO.write(resizedBG, m.group(0).substring(m.group(0).indexOf(".")+1),
                            new File(rb.getString("path") + "\\data\\image\\resource\\osu\\" +
                                    beatmap.getBeatmapSetId()+"\\" + m.group(0)));
                }
                //手动关闭流
                httpConnection.disconnect();
                break;
            } catch (IOException e) {
                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                retry++;
            }

        }
        if (retry == 5) {
            logger.error("获取" + beatmap.getBeatmapId()+ "的背景图，失败五次");
        }
        return resizedBG;

    }

    public int getRepWatched(int uid) {
        int retry = 0;
        Document doc = null;
        while (retry < 5) {
            try {
                logger.info("正在获取" + uid + "的Replays被观看次数");
                doc = Jsoup.connect(getUserProfileURL + uid).timeout((int) Math.pow(2, retry + 1) * 1000).get();
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

    public Date getLastActive(int uid) {
        int retry = 0;
        Document doc = null;
        while (retry < 5) {
            try {
                logger.info("正在获取" + uid + "的上次活跃时间");
                doc = Jsoup.connect(getUserURL + uid).timeout((int) Math.pow(2, retry + 1) * 1000).get();
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

    public void getOsuFile(Beatmap beatmap) {
        HttpURLConnection httpConnection;
        byte[] osuFile;
        int retry = 0;
        //获取.osu的逻辑和获取BG不一样，Qua的图BG不缓存，而.osu必须缓存
        //即使是qua的图，也必须有sid的文件夹
        File sidPath =new File(rb.getString("path") + "\\data\\image\\resource\\osu\\" + beatmap.getBeatmapSetId());
        if(!sidPath.exists()){
            sidPath.mkdir();
        }
        File osu = new File(rb.getString("path") + "\\data\\image\\resource\\osu\\" + beatmap.getBeatmapSetId()+"\\"+beatmap.getBeatmapId() + ".osu");

        if (osu.length() > 0 && (beatmap.getApproved() == 1 || beatmap.getApproved() == 2)) {
            //如果beatmap状态是ranked,直接读取
            return ;
        }
        while (retry < 5) {
            try (FileOutputStream fs = new FileOutputStream(osu)) {
                httpConnection =
                        (HttpURLConnection) new URL(getOsuURL + beatmap.getBeatmapId()).openConnection();
                httpConnection.setRequestMethod("GET");
                httpConnection.setConnectTimeout((int) Math.pow(2, retry + 1) * 1000);
                httpConnection.setReadTimeout((int) Math.pow(2, retry + 1) * 1000);
                if (httpConnection.getResponseCode() != 200) {
                    logger.error("HTTP GET请求失败: " + httpConnection.getResponseCode() + "，正在重试第" + (retry + 1) + "次");
                    retry++;
                    continue;
                }
                //将返回结果读取为Byte数组
                osuFile = readInputStream(httpConnection.getInputStream());

                fs.write(osuFile);

                //手动关闭连接
                httpConnection.disconnect();
                break;
            } catch (IOException e) {
                logger.error("出现IO异常：" + e.getMessage() + "，正在重试第" + (retry + 1) + "次");
                retry++;
            }

        }
        if (retry == 5) {
            logger.error("获取" + beatmap.getBeatmapId() + "的.osu文件，失败五次");
        }
    }
    //这个方法只能处理ranked/approved/qualified的.osu文件,在目前的业务逻辑里默认.osu文件是存在的。
    public OsuFile praseOsuFile(Beatmap beatmap){
        //先获取
        String osuFile;
        String bgName;

        File osu = new File(rb.getString("path") + "\\data\\image\\resource\\osu\\" + beatmap.getBeatmapSetId()+"\\"+beatmap.getBeatmapId() + ".osu");
        try(FileInputStream fis = new FileInputStream(osu)) {
            osuFile = new String(readInputStream(fis), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Matcher m = Pattern.compile(Constant.BGLINE_REGEX).matcher(osuFile);
        m.find();
        if("//Background and Video events".equals(m.group(1))){
            bgName = m.group(2);
        }else{
            bgName = m.group(1);
        }
        m=Pattern.compile(Constant.BGNAME_REGEX).matcher(bgName);
        if(m.find()) {
            OsuFile result = new OsuFile();
            bgName = m.group(0);
            result.setBgName(bgName);
            return result;
        }else return null;

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
