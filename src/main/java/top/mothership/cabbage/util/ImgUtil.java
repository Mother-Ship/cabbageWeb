package top.mothership.cabbage.util;

import org.apache.ibatis.jdbc.Null;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.exception.BgNotFoundException;
import top.mothership.cabbage.pojo.Beatmap;
import top.mothership.cabbage.pojo.Score;
import top.mothership.cabbage.pojo.Userinfo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@Component
public class ImgUtil {
    private static ResourceBundle rb = ResourceBundle.getBundle("cabbage");
    private static Logger logger = LogManager.getLogger("ImgUtil.class");
    //2017-9-8 13:55:42我他妈是个智障……没初始化的map我在下面用
    private static Map<String, BufferedImage> images = new HashMap<>();
    private final WebPageUtil webPageUtil;

    static {
        //调用NIO遍历那些可以加载一次的文件
        final Path path = Paths.get(rb.getString("path") + "\\data\\image\\resource\\img");
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                images.put(file.getFileName().toString(), ImageIO.read(file.toFile()));
                return super.visitFile(file, attrs);
            }
        };
        try {
            java.nio.file.Files.walkFileTree(path, finder);
        } catch (IOException e) {
            logger.info("读取本地资源失败");
            logger.error(e.getMessage());
        }
    }

    @Autowired
    public ImgUtil(WebPageUtil webPageUtil) {
        this.webPageUtil = webPageUtil;
    }

    public void drawUserInfo(Userinfo userFromAPI, Userinfo userInDB, String role, int day, boolean near, int scoreRank) {

        BufferedImage ava = webPageUtil.getAvatar(userFromAPI.getUserId());
        BufferedImage bg;
        BufferedImage layout = getCopyImage(images.get(rb.getString("layout")));
        BufferedImage scoreRankBG = getCopyImage(images.get(rb.getString("scoreRankBG")));
        BufferedImage roleBg = getCopyImage(images.get("role-" + role + ".png"));
        try {
            bg = getCopyImage(images.get(String.valueOf(userFromAPI.getUserId())));
        } catch (NullPointerException e) {
            try {
                bg = getCopyImage(images.get(role + ".png"));
            } catch (NullPointerException e1) {
                logger.error("读取!stat命令所需的本地资源失败");
                logger.error(e1.getMessage());
                return;
            }
        }

        Graphics2D g2 = (Graphics2D) bg.getGraphics();
        //绘制布局和用户组
        g2.drawImage(roleBg, 0, 0, null);
        g2.drawImage(layout, 0, 0, null);
        try {
            g2.drawImage(ava, Integer.decode(rb.getString("avax")), Integer.decode(rb.getString("avay")), null);
        } catch (NullPointerException e) {
            logger.warn(userFromAPI.getUserName() + "玩家没有头像");
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //绘制用户名
        draw(g2, "unameColor", "unameFont", "unameSize", userFromAPI.getUserName(), "namex", "namey");

        //绘制Rank
        draw(g2, "defaultColor", "numberFont", "rankSize", "#" + userFromAPI.getPpRank(), "rankx", "ranky");

        //绘制PP
        draw(g2, "ppColor", "numberFont", "ppSize", String.valueOf(userFromAPI.getPpRaw()), "ppx", "ppy");


        if (scoreRank > 0) {
            //把scorerank用到的bg画到bg上
            g2.drawImage(scoreRankBG, 653, 7, null);
            if (scoreRank < 100) {
                draw(g2, "scoreRankColor", "scoreRankFont", "scoreRankSize", "#" + Integer.toString(scoreRank), "scoreRank2x", "scoreRank2y");
            } else {
                draw(g2, "scoreRankColor", "scoreRankFont", "scoreRankSize", "#" + Integer.toString(scoreRank), "scoreRankx", "scoreRanky");
            }
        }

        //绘制RankedScore
        draw(g2, "defaultColor", "numberFont", "numberSize",
                new DecimalFormat("###,###").format(userFromAPI.getRankedScore()), "rScorex", "rScorey");
        //绘制acc
        draw(g2, "defaultColor", "numberFont", "numberSize",
                new DecimalFormat("##0.00").format(userFromAPI.getAccuracy()) + "%", "accx", "accy");

        //绘制pc
        draw(g2, "defaultColor", "numberFont", "numberSize",
                new DecimalFormat("###,###").format(userFromAPI.getPlayCount()), "pcx", "pcy");

        //绘制tth
        draw(g2, "defaultColor", "numberFont", "numberSize",
                new DecimalFormat("###,###").format(userFromAPI.getCount50() + userFromAPI.getCount100() + userFromAPI.getCount300()), "tthx", "tthy");
        //绘制Level

        draw(g2, "defaultColor", "numberFont", "numberSize",
                Integer.toString((int) Math.floor(userFromAPI.getLevel())) + " (" + (int) ((userFromAPI.getLevel() - Math.floor(userFromAPI.getLevel())) * 100) + "%)", "levelx", "levely");

        //绘制SS计数
        draw(g2, "defaultColor", "numberFont", "countSize", Integer.toString(userFromAPI.getCountRankSs()), "ssCountx", "ssCounty");

        //绘制S计数
        draw(g2, "defaultColor", "numberFont", "countSize", Integer.toString(userFromAPI.getCountRankS()), "sCountx", "sCounty");

        //绘制A计数
        draw(g2, "defaultColor", "numberFont", "countSize", Integer.toString(userFromAPI.getCountRankA()), "aCountx", "aCounty");
        //绘制当时请求的时间

        draw(g2, "timeColor", "timeFont", "timeSize",
                new SimpleDateFormat("yy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()), "timex", "timey");


        //---------------------------以上绘制在线部分完成--------------------------------
        //试图查询数据库中指定日期的user
        if (day > 0) {
                /*
                不带参数：day=1，调用dbUtil拿当天凌晨（数据库中数值是昨天）的数据进行对比
                带day = 0:进入本方法，不读数据库，不进行对比
                day>1，例如day=2，21号进入本方法，查的是19号结束时候的成绩
                */
            if (day > 1) {
                //临时关闭平滑
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                //只有day>1才会出现文字
                if (near) {
                    //如果取到的是模糊数据
                    draw(g2, "tipColor", "tipFont", "tipSize", "请求的日期没有数据", "tipx", "tipy");
                    //算出天数差别
                    draw(g2, "tipColor", "tipFont", "tipSize", "『对比于" + Long.valueOf(((Calendar.getInstance().getTime().getTime() -
                            userInDB.getQueryDate().getTime()) / 1000 / 60 / 60 / 24)).toString() + "天前』", "tip2x", "tip2y");
                } else {
                    //如果取到的是精确数据
                    draw(g2, "tipColor", "tipFont", "tipSize", "『对比于" + day + "天前』", "tip2x", "tip2y");
                }

            }
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            //这样确保了userInDB不是空的
            //绘制Rank变化
            if (userInDB.getPpRank() > userFromAPI.getPpRank()) {
                //如果查询的rank比凌晨中的小
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + Integer.toString(userInDB.getPpRank() - userFromAPI.getPpRank()), "rankDiffx", "rankDiffy");
            } else if (userInDB.getPpRank() < userFromAPI.getPpRank()) {
                //如果掉了rank
                draw(g2, "downColor", "diffFont", "diffSize",
                        "↓" + Integer.toString(userFromAPI.getPpRank() - userInDB.getPpRank()), "rankDiffx", "rankDiffy");
            } else {
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + Integer.toString(0), "rankDiffx", "rankDiffy");
            }
            //绘制PP变化
            if (userInDB.getPpRaw() > userFromAPI.getPpRaw()) {
                //如果查询的pp比凌晨中的小
                draw(g2, "downColor", "diffFont", "diffSize",
                        "↓" + new DecimalFormat("##0.00").format(userInDB.getPpRaw() - userFromAPI.getPpRaw()), "ppDiffx", "ppDiffy");
            } else if (userInDB.getPpRaw() < userFromAPI.getPpRaw()) {
                //刷了PP
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + new DecimalFormat("##0.00").format(userFromAPI.getPpRaw() - userInDB.getPpRaw()), "ppDiffx", "ppDiffy");
            } else {
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + Integer.toString(0), "ppDiffx", "ppDiffy");
            }

            //绘制RankedScore变化
            if (userInDB.getRankedScore() < userFromAPI.getRankedScore()) {
                //因为RankedScore不会变少，所以不写蓝色部分
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + new DecimalFormat("###,###").format(userFromAPI.getRankedScore() - userInDB.getRankedScore()), "rScoreDiffx", "rScoreDiffy");
            } else {
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + Integer.toString(0), "rScoreDiffx", "rScoreDiffy");
            }
            //绘制ACC变化
            //在这里把精度砍掉
            if (Float.valueOf(new DecimalFormat("##0.00").format(userInDB.getAccuracy())) > Float.valueOf(new DecimalFormat("##0.00").format(userFromAPI.getAccuracy()))) {
                //如果acc降低了
                draw(g2, "downColor", "diffFont", "diffSize",
                        "↓" + new DecimalFormat("##0.00").format(userInDB.getAccuracy() - userFromAPI.getAccuracy()) + "%", "accDiffx", "accDiffy");
            } else if (Float.valueOf(new DecimalFormat("##0.00").format(userInDB.getAccuracy())) < Float.valueOf(new DecimalFormat("##0.00").format(userFromAPI.getAccuracy()))) {
                //提高
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + new DecimalFormat("##0.00").format(userFromAPI.getAccuracy() - userInDB.getAccuracy()) + "%", "accDiffx", "accDiffy");

            } else {
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + new DecimalFormat("##0.00").format(0.00) + "%", "accDiffx", "accDiffy");
            }

            //绘制pc变化
            if (userInDB.getPlayCount() < userFromAPI.getPlayCount()) {
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + new DecimalFormat("###,###").format(userFromAPI.getPlayCount() - userInDB.getPlayCount()), "pcDiffx", "pcDiffy");

            } else {
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + Integer.toString(0), "pcDiffx", "pcDiffy");

            }

            //绘制tth变化,此处开始可以省去颜色设置
            if (userInDB.getCount50() + userInDB.getCount100() + userInDB.getCount300()
                    < userFromAPI.getCount50() + userFromAPI.getCount100() + userFromAPI.getCount300()) {
                //同理不写蓝色部分
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + new DecimalFormat("###,###").format(userFromAPI.getCount50() + userFromAPI.getCount100() + userFromAPI.getCount300() - (userInDB.getCount50() + userInDB.getCount100() + userInDB.getCount300())), "tthDiffx", "tthDiffy");
            } else {
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + Integer.toString(0), "tthDiffx", "tthDiffy");
            }
            //绘制level变化
            if (Float.valueOf(new DecimalFormat("##0.00").format(userInDB.getLevel())) < Float.valueOf(new DecimalFormat("##0.00").format(userFromAPI.getLevel()))) {
                //同理不写蓝色部分
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + (int) ((userFromAPI.getLevel() - userInDB.getLevel()) * 100) + "%", "levelDiffx", "levelDiffy");
            } else {
                draw(g2, "upColor", "diffFont", "diffSize",
                        "↑" + Integer.toString(0) + "%", "levelDiffx", "levelDiffy");
            }
            //绘制SS count 变化
            //这里需要改变字体大小
            if (userInDB.getCountRankSs() > userFromAPI.getCountRankSs()) {
                //如果查询的SS比凌晨的少
                draw(g2, "downColor", "diffFont", "countDiffSize",
                        "↓" + Integer.toString(userInDB.getCountRankSs() - userFromAPI.getCountRankSs()), "ssCountDiffx", "ssCountDiffy");
            } else if (userInDB.getCountRankSs() < userFromAPI.getCountRankSs()) {
                //如果SS变多了
                draw(g2, "upColor", "diffFont", "countDiffSize",
                        "↑" + Integer.toString(userFromAPI.getCountRankSs() - userInDB.getCountRankSs()), "ssCountDiffx", "ssCountDiffy");
            } else {
                draw(g2, "upColor", "diffFont", "countDiffSize",
                        "↑" + Integer.toString(0), "ssCountDiffx", "ssCountDiffy");
            }
            //s
            if (userInDB.getCountRankS() > userFromAPI.getCountRankS()) {
                //如果查询的S比凌晨的少
                draw(g2, "downColor", "diffFont", "countDiffSize",
                        "↓" + Integer.toString(userInDB.getCountRankS() - userFromAPI.getCountRankS()), "sCountDiffx", "sCountDiffy");
            } else if (userInDB.getCountRankS() < userFromAPI.getCountRankS()) {
                //如果S变多了
                draw(g2, "upColor", "diffFont", "countDiffSize",
                        "↑" + Integer.toString(userFromAPI.getCountRankS() - userInDB.getCountRankS()), "sCountDiffx", "sCountDiffy");
            } else {
                draw(g2, "upColor", "diffFont", "countDiffSize",
                        "↑" + Integer.toString(0), "sCountDiffx", "sCountDiffy");
            }
            //a
            if (userInDB.getCountRankA() > userFromAPI.getCountRankA()) {
                //如果查询的S比凌晨的少
                draw(g2, "downColor", "diffFont", "countDiffSize",
                        "↓" + Integer.toString(userInDB.getCountRankA() - userFromAPI.getCountRankA()), "aCountDiffx", "aCountDiffy");
            } else if (userInDB.getCountRankA() < userFromAPI.getCountRankA()) {
                //如果S变多了
                draw(g2, "upColor", "diffFont", "countDiffSize",
                        "↑" + Integer.toString(userFromAPI.getCountRankA() - userInDB.getCountRankA()), "aCountDiffx", "aCountDiffy");
            } else {
                draw(g2, "upColor", "diffFont", "countDiffSize",
                        "↑" + Integer.toString(0), "aCountDiffx", "aCountDiffy");
            }
        }
        g2.dispose();
        drawImage(bg, userFromAPI.getUserName());
    }

    public void drawUserBP(String userName, Map<Score,Integer > map) {
        //计算最终宽高
        int Height = images.get(rb.getString("bptop")).getHeight();
        int HeightPoint = 0;
        int Width = images.get(rb.getString("bptop")).getWidth();
        for (Score aList : map.keySet()) {
            if (aList.getBeatmapName().length() <= Integer.valueOf(rb.getString("bplimit"))) {
                Height = Height + images.get(rb.getString("bpmid3")).getHeight();
            } else {
                Height = Height + images.get(rb.getString("bpmid2")).getHeight();
            }
        }
        BufferedImage result = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();

        //头部
        BufferedImage bpTop = getCopyImage(images.get(rb.getString("bptop")));
        Graphics2D g2 = (Graphics2D) bpTop.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        draw(g2, "bpUnameColor", "bpUnameFont", "bpUnameSize", "Best Performance of " + userName, "bpUnamex", "bpUnamey");
        Calendar c = Calendar.getInstance();
        //日期补丁
        if (c.get(Calendar.HOUR_OF_DAY) < 4) {
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        draw(g2, "bpQueryDateColor", "bpQueryDateFont", "bpQueryDateSize", new SimpleDateFormat("yy-MM-dd").format(c.getTime()), "bpQueryDatex", "bpQueryDatey");
        g2.dispose();
        //生成好的画上去
        g.drawImage(bpTop, 0, HeightPoint, null);
        //移动这个类似指针的东西
        HeightPoint = HeightPoint+bpTop.getHeight();

        //开始绘制每行的bp
        for (Score aList : map.keySet()) {
            String acc = new DecimalFormat("###.00").format(
                    100.0 * (6 * aList.getCount300() + 2 * aList.getCount100() + aList.getCount50())
                    / (6 * aList.getCount50() + aList.getCount100() + aList.getCount300() + aList.getCountMiss()));

            String mods = convertMOD(aList.getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "");
            int a;
            if (aList.getBeatmapName().length() <= Integer.valueOf(rb.getString("bplimit"))) {
                a = 2;
            } else {
                a = 3;
            }
            //小图标
            BufferedImage bpMid = getCopyImage(images.get(rb.getString("bpmid" + a)));
            Graphics2D g3 = bpMid.createGraphics();
            g3.drawImage(getCopyImage(images.get(aList.getRank() + "_small.png")), Integer.decode(rb.getString("bp" + a + "Rankx")), Integer.decode(rb.getString("bp" + a + "Ranky")), null);
            //绘制文字
            g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //绘制日期(给的就是北京时间，不转)
            draw(g3, "bpDateColor", "bpDateFont", "bpDateSize",
                    new SimpleDateFormat("MM-dd HH:mm").format(aList.getDate().getTime()), "bp" + a + "Datex", "bp" + a + "Datey");
            //绘制Num和Weight
            draw(g3, "bpNumColor", "bpNumFont", "bpNumSize",
                    String.valueOf(map.get(aList) + 1), "bp" + a +"Numx", "bp" + a + "Numy");

            draw(g3, "bpWeightColor", "bpWeightFont", "bpWeightSize",
                    new DecimalFormat("##0.00").format(100 * Math.pow(0.95, map.get(aList))) + "%", "bp" + a + "Weightx", "bp" + a + "Weighty");

            //绘制MOD
            draw(g3, "bpModColor", "bpModFont", "bpModSize", mods, "bp" + a + "Modx", "bp" + a + "Mody");
            //绘制PP
            draw(g3, "bpPPColor", "bpPPFont", "bpPPSize", Integer.toString(Math.round(aList.getPp())) + "pp", "bp" + a + "PPx", "bp" + a + "PPy");
            switch (a){
                case 2:
                    draw(g3, "bpNameColor", "bpNameFont", "bpNameSize",
                            aList.getBeatmapName() + "(" + acc + "%)", "bp2Namex", "bp2Namey");
                    break;
                case 3:
                    draw(g3, "bpNameColor", "bpNameFont", "bpNameSize", aList.getBeatmapName().substring(0, aList.getBeatmapName().substring(0, Integer.valueOf(rb.getString("bplimit")) + 1).lastIndexOf(" ") + 1),
                            "bp3Namex", "bp3Namey");
                    draw(g3, "bpNameColor", "bpNameFont", "bpNameSize", aList.getBeatmapName().substring(aList.getBeatmapName().substring(0, Integer.valueOf(rb.getString("bplimit")) + 1).lastIndexOf(" ") + 1, aList.getBeatmapName().length())
                                    + "(" +acc + "%)",
                            "bp3Name+1x", "bp3Name+1y");
                    break;
            }
            g3.dispose();
            HeightPoint = HeightPoint+bpMid.getWidth();
            g.drawImage(bpMid,0,HeightPoint,null);
        }
        g.dispose();
        //因为这个方法只需要一个username……
        drawImage(result,userName);

    }

    public void drawResult(Userinfo userinfo, Score score, Beatmap beatmap) throws BgNotFoundException {
        String accS = new DecimalFormat("###.00").format(100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50()) / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss())));
        float acc = Float.valueOf(accS);
        BufferedImage bg;

        try {
            bg = webPageUtil.getBG(score.getBeatmapId(), beatmap);
        } catch (NullPointerException e) {
            logger.error("从血猫抓取谱面背景失败");
            logger.error(e.getMessage());
            throw new BgNotFoundException("谱面背景获取失败 ");
        }


    }



    private BufferedImage getCopyImage(BufferedImage image) {
        return image.getSubimage(0, 0, image.getWidth(), image.getHeight());
    }


    private void draw(Graphics2D g2, String color, String font, String size, String text, String x, String y) {
        //指定颜色
        g2.setPaint(Color.decode(rb.getString(color)));
        //指定字体
        g2.setFont(new Font(rb.getString(font), Font.PLAIN, Integer.decode(rb.getString(size))));
        //指定坐标
        g2.drawString(text, Integer.decode(rb.getString(x)), Integer.decode(rb.getString(y)));

    }

    private void drawImage(BufferedImage img, String filename) {
        try {
            ImageIO.write(img, "png", new File(rb.getString("path") + "\\data\\image\\" + filename + ".png"));
            img.flush();
        } catch (IOException e) {
            logger.error("将图片写入硬盘失败");
            logger.error(e.getMessage());
        }
    }


    private Map<String,String> convertMOD(Integer bp) {
        String modBin = Integer.toBinaryString(bp);
        //反转mod
        modBin = new StringBuffer(modBin).reverse().toString();
        Map<String,String> mods = new HashMap<>();
        char[] c = modBin.toCharArray();
        if (bp != 0) {
            for (int i = c.length - 1; i >= 0; i--) {
                //字符串中第i个字符是1,意味着第i+1个mod被开启了
                switch (i) {
                    case 0:
                        mods.put("NF","nofail");
                        break;
                    case 1:
                        mods.put("EZ","easy");
                        break;
                    case 3:
                        mods.put("HD","hidden");
                        break;
                    case 4:
                        mods.put("HR","hardrock");
                        break;
                    case 5:
                        mods.put("SD","suddendeath");
                        break;
                    case 6:
                        mods.put("DT","doubletime");
                        break;
                    case 8:
                        mods.put("HT","halftime");
                        break;
                    case 9:
                        mods.put("NC","nightcore");
                        break;
                    case 10:
                        mods.put("FL","flashlight");
                        break;
                    case 12:
                        mods.put("SO","spunout");
                        break;
                    case 14:
                        mods.put("PF","perfect");
                        break;
                }
            }
            if (mods.keySet().contains("NC")) {
                mods.remove("DT");
            }
            if (mods.keySet().contains("PF")) {
                mods.remove("SD");
            }
        } else {
            mods.put("None","None");
        }

        return mods;
    }



}
