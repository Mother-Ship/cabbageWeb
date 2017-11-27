package top.mothership.cabbage.util.qq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.OppaiResult;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.util.Overall;
import top.mothership.cabbage.util.osu.ScoreUtil;
import top.mothership.cabbage.util.osu.WebPageUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//2017-11-6 12:50:14改为全部返回BASE64编码
@Component
//采用原型模式注入，避免出现错群问题
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "prototype")
public class ImgUtil {
    //2017-9-8 13:55:42我他妈是个智障……没初始化的map我在下面用
    public static Map<String, BufferedImage> images;
    private WebPageUtil webPageUtil;
    private ScoreUtil scoreUtil;

    @Autowired
    public ImgUtil(WebPageUtil webPageUtil, ScoreUtil scoreUtil) {
        this.webPageUtil = webPageUtil;
        this.scoreUtil = scoreUtil;
    }

    //为线程安全，将当前时间毫秒数加入文件名并返回
    public String drawUserInfo(Userinfo userFromAPI, Userinfo userInDB, String role, int day, boolean near, int scoreRank) {
        BufferedImage ava = webPageUtil.getAvatar(userFromAPI.getUserId());
        BufferedImage bg;
        BufferedImage layout = getCopyImage(images.get(Overall.CABBAGE_CONFIG.getString("layout")));
        BufferedImage scoreRankBG = getCopyImage(images.get(Overall.CABBAGE_CONFIG.getString("scoreRankBG")));
        BufferedImage roleBg = getCopyImage(images.get("role-" + role + ".png"));
        try {
            bg = getCopyImage(images.get(String.valueOf(userFromAPI.getUserId()) + ".png"));
        } catch (NullPointerException e) {
            //现在已经不用再去扫描硬盘了啊
            try {
                bg = getCopyImage(images.get(role + ".png"));
            } catch (NullPointerException e2) {
                //这个异常是在出现了新用户组，但是没有准备这个用户组的右下角标志时候出现
                return null;
            }

        }
        Graphics2D g2 = (Graphics2D) bg.getGraphics();
        //绘制布局和用户组

        g2.drawImage(layout, 0, 0, null);

        g2.drawImage(roleBg, 0, 0, null);
        try {
            g2.drawImage(ava, Integer.decode(Overall.CABBAGE_CONFIG.getString("avax")), Integer.decode(Overall.CABBAGE_CONFIG.getString("avay")), null);
        } catch (NullPointerException e) {
            e.getMessage();
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //将scorerank比用户名先画

        if (scoreRank > 0) {
            //把scorerank用到的bg画到bg上
            g2.drawImage(scoreRankBG, 653, 7, null);
            if (scoreRank < 100) {
                draw(g2, "scoreRankColor", "scoreRankFont", "scoreRankSize", "#" + Integer.toString(scoreRank), "scoreRank2x", "scoreRank2y");
            } else {
                draw(g2, "scoreRankColor", "scoreRankFont", "scoreRankSize", "#" + Integer.toString(scoreRank), "scoreRankx", "scoreRanky");
            }
        }

        //绘制用户名
        draw(g2, "unameColor", "unameFont", "unameSize", userFromAPI.getUserName(), "namex", "namey");
        //绘制Rank
        draw(g2, "defaultColor", "numberFont", "rankSize", "#" + userFromAPI.getPpRank(), "rankx", "ranky");
        //绘制PP
        draw(g2, "ppColor", "numberFont", "ppSize", String.valueOf(userFromAPI.getPpRaw()), "ppx", "ppy");
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
        return drawImage(bg);
    }

    public String drawUserBP(Userinfo userFromAPI, List<Score> list) {

        //计算最终宽高
        int Height = images.get(Overall.CABBAGE_CONFIG.getString("bptop")).getHeight();
        int HeightPoint = 0;
        int Width = images.get(Overall.CABBAGE_CONFIG.getString("bptop")).getWidth();
        for (Score aList : list) {
            if (aList.getBeatmapName().length() <= Integer.valueOf(Overall.CABBAGE_CONFIG.getString("bplimit"))) {
                Height = Height + images.get(Overall.CABBAGE_CONFIG.getString("bpmid2")).getHeight();
            } else {
                Height = Height + images.get(Overall.CABBAGE_CONFIG.getString("bpmid3")).getHeight();
            }
        }
        BufferedImage result = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();

        //头部
        BufferedImage bpTop = getCopyImage(images.get(Overall.CABBAGE_CONFIG.getString("bptop")));
        Graphics2D g2 = (Graphics2D) bpTop.getGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        draw(g2, "bpUnameColor", "bpUnameFont", "bpUnameSize", "Best Performance of " + userFromAPI.getUserName(), "bpUnamex", "bpUnamey");
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
        HeightPoint = HeightPoint + bpTop.getHeight();

        //开始绘制每行的bp
        for (Score aList : list) {
            String acc = new DecimalFormat("###.00").format(
                    100.0 * (6 * aList.getCount300() + 2 * aList.getCount100() + aList.getCount50())
                            / (6 * (aList.getCount50() + aList.getCount100() + aList.getCount300() + aList.getCountMiss())));

            String mods = scoreUtil.convertMOD(aList.getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "");
            int a;
            if (aList.getBeatmapName().length() <= Integer.valueOf(Overall.CABBAGE_CONFIG.getString("bplimit"))) {
                a = 2;
            } else {
                a = 3;
            }

            BufferedImage bpMid = getCopyImage(images.get(Overall.CABBAGE_CONFIG.getString("bpmid" + a)));
            Graphics2D g3 = bpMid.createGraphics();
            //小图标
            g3.drawImage(images.get(aList.getRank() + "_small.png"), Integer.decode(Overall.CABBAGE_CONFIG.getString("bp" + a + "Rankx")), Integer.decode(Overall.CABBAGE_CONFIG.getString("bp" + a + "Ranky")), null);
            //绘制文字
            g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //绘制日期(给的就是北京时间，不转)
            draw(g3, "bpDateColor", "bpDateFont", "bpDateSize",
                    new SimpleDateFormat("MM-dd HH:mm").format(aList.getDate().getTime()), "bp" + a + "Datex", "bp" + a + "Datey");
            //绘制Num和Weight
            draw(g3, "bpNumColor", "bpNumFont", "bpNumSize",
                    String.valueOf(aList.getBpId() + 1), "bp" + a + "Numx", "bp" + a + "Numy");

            draw(g3, "bpWeightColor", "bpWeightFont", "bpWeightSize",
                    new DecimalFormat("##0.00").format(100 * Math.pow(0.95, aList.getBpId())) + "%", "bp" + a + "Weightx", "bp" + a + "Weighty");

            //绘制MOD
            draw(g3, "bpModColor", "bpModFont", "bpModSize", mods, "bp" + a + "Modx", "bp" + a + "Mody");
            //绘制PP
            draw(g3, "bpPPColor", "bpPPFont", "bpPPSize", Integer.toString(Math.round(aList.getPp())) + "pp", "bp" + a + "PPx", "bp" + a + "PPy");
            switch (a) {
                case 2:
                    draw(g3, "bpNameColor", "bpNameFont", "bpNameSize",
                            aList.getBeatmapName() + "(" + acc + "%)", "bp2Namex", "bp2Namey");
                    break;
                case 3:
                    draw(g3, "bpNameColor", "bpNameFont", "bpNameSize", aList.getBeatmapName().substring(0, aList.getBeatmapName().substring(0, Integer.valueOf(Overall.CABBAGE_CONFIG.getString("bplimit")) + 1).lastIndexOf(" ") + 1),
                            "bp3Namex", "bp3Namey");
                    draw(g3, "bpNameColor", "bpNameFont", "bpNameSize", aList.getBeatmapName().substring(aList.getBeatmapName().substring(0, Integer.valueOf(Overall.CABBAGE_CONFIG.getString("bplimit")) + 1).lastIndexOf(" ") + 1, aList.getBeatmapName().length())
                                    + "(" + acc + "%)",
                            "bp3Name+1x", "bp3Name+1y");
                    break;
            }
            g3.dispose();
            bpMid.flush();
            g.drawImage(bpMid, 0, HeightPoint, null);
            HeightPoint = HeightPoint + bpMid.getHeight();
        }
        g.dispose();
        //不，文件名最好还是数字
        return drawImage(result);

    }

    public String drawResult(Userinfo userFromAPI, Score score, Beatmap beatmap) {
        String accS = new DecimalFormat("###.00").format(100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50()) / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss())));
        float acc = Float.valueOf(accS);
        BufferedImage bg;
        BufferedImage result;
        Map<String, String> mods = scoreUtil.convertMOD(score.getEnabledMods());
        //这个none是为了BP节省代码，在这里移除掉
        mods.remove("None");
        //离线计算PP
        OppaiResult oppaiResult = null;
        try {
            oppaiResult = scoreUtil.calcPP(score, beatmap);
        }catch (Exception ignore){
            //如果acc过低
        }
        boolean defaultBG = false;
        try {
            bg = webPageUtil.getBG(beatmap);
        }catch (NullPointerException e){
            bg = webPageUtil.getBGBackup(beatmap);
            if (bg == null) {
                //随机抽取一个bg
                String RandomBG = "defaultBG1" + ((int) (Math.random() * 2) + 2) + ".png";
                bg = getCopyImage(images.get(RandomBG));
                defaultBG = true;
            }
        }
        //2017-11-3 17:51:47这里有莫名的空指针，比较迷，在webpageUtil.getBG里加一个判断为空则抛出空指针看看
        Graphics2D g2 = (Graphics2D) bg.getGraphics();
        //画上各个元素，这里Images按文件名排序
        //顶端banner(下方也暗化了20%，JAVA自带算法容易导致某些图片生成透明图片)
        g2.drawImage(images.get("bpBanner.png"), 0, 0, null);


        //Rank
        g2.drawImage(images.get("ranking-" + score.getRank() + ".png").getScaledInstance(images.get("ranking-" + score.getRank() + ".png").getWidth(), images.get("ranking-" + score.getRank() + ".png").getHeight(), Image.SCALE_SMOOTH), 1131 - 245, 341 - 242, null);

        //FC
        if (score.getPerfect() == 1) {
            g2.drawImage(images.get("ranking-perfect.png"), 296 - 30, 675 - 37, null);
        }
        //分数 图片扩大到1.27倍
        //分数是否上e，每个数字的位置都不一样
        if (score.getScore() > 99999999) {
            char[] Score = String.valueOf(score.getScore()).toCharArray();
            for (int i = 0; i < Score.length; i++) {
                //第二个参数是数字之间的距离+第一个数字离最左边的距离
                g2.drawImage(images.get("score-" + String.valueOf(Score[i]) + ".png"), 55 * i + 128 - 21, 173 - 55, null);
            }
        } else {
            char[] Score = String.valueOf(score.getScore()).toCharArray();
            for (int i = 0; i < 8; i++) {
                if (Score.length < 8) {
                    //如果分数不到8位，左边用0补全
                    //获取Score的长度和8的差距，然后把i小于等于这个差距的时候画的数字改成0
                    if (i < 8 - Score.length) {
                        g2.drawImage(images.get("score-0.png"), 55 * i + 141 - 6, 173 - 55, null);
                    } else {
                        //第一次应该拿的是数组里第0个字符
                        g2.drawImage(images.get("score-" + String.valueOf(Score[i - 8 + Score.length]) + ".png"), 55 * i + 141 - 6, 173 - 55, null);
                    }
                } else {
                    //直接绘制
                    g2.drawImage(images.get("score-" + String.valueOf(Score[i]) + ".png"), 55 * i + 141 - 6, 173 - 55, null);
                }
            }
        }


        //combo
        char[] Combo = String.valueOf(score.getMaxCombo()).toCharArray();
        for (int i = 0; i < Combo.length; i++) {
            //第二个参数是数字之间的距离+第一个数字离最左边的距离
            g2.drawImage(images.get("score-" + String.valueOf(Combo[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 30 - 7, 576 - 55 + 10, null);
        }
        //画上结尾的x
        g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * Combo.length + 30 - 7, 576 - 55 + 10, null);

        //300
        char[] Count300 = String.valueOf(score.getCount300()).toCharArray();

        for (int i = 0; i < Count300.length; i++) {
            //第二个参数是数字之间的距离+第一个数字离最左边的距离
            g2.drawImage(images.get("score-" + String.valueOf(Count300[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 238 - 7, null);
        }
        //画上结尾的x
        g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * Count300.length + 134 - 7, 238 - 7, null);

        //激
        char[] CountGeki = String.valueOf(score.getCountGeki()).toCharArray();
        for (int i = 0; i < CountGeki.length; i++) {
            //第二个参数是数字之间的距离+第一个数字离最左边的距离
            g2.drawImage(images.get("score-" + String.valueOf(CountGeki[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 238 - 7, null);
        }
        //画上结尾的x
        g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * CountGeki.length + 455 - 8, 238 - 7, null);

        //100
        char[] Count100 = String.valueOf(score.getCount100()).toCharArray();
        for (int i = 0; i < Count100.length; i++) {
            //第二个参数是数字之间的距离+第一个数字离最左边的距离
            g2.drawImage(images.get("score-" + String.valueOf(Count100[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 374 - 55, null);
        }
        //画上结尾的x
        g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * Count100.length + 134 - 7, 374 - 55, null);

        //喝
        char[] CountKatu = String.valueOf(score.getCountKatu()).toCharArray();
        for (int i = 0; i < CountKatu.length; i++) {
            //第二个参数是数字之间的距离+第一个数字离最左边的距离
            g2.drawImage(images.get("score-" + String.valueOf(CountKatu[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 374 - 55, null);
        }
        //画上结尾的x
        g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * CountKatu.length + 455 - 8, 374 - 55, null);


        char[] Count50 = String.valueOf(score.getCount50()).toCharArray();
        for (int i = 0; i < Count50.length; i++) {
            //第二个参数是数字之间的距离+第一个数字离最左边的距离
            g2.drawImage(images.get("score-" + String.valueOf(Count50[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 470 - 55, null);
        }
        //画上结尾的x
        g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * Count50.length + 134 - 7, 470 - 55, null);

        //x
        char[] Count0 = String.valueOf(score.getCountMiss()).toCharArray();
        for (int i = 0; i < Count0.length; i++) {
            //第二个参数是数字之间的距离+第一个数字离最左边的距离
            g2.drawImage(images.get("score-" + String.valueOf(Count0[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 470 - 55, null);
        }
        //画上结尾的x
        g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * Count0.length + 455 - 8, 470 - 55, null);

        //acc
        if (acc == 100) {
            //从最左边的数字开始，先画出100
            g2.drawImage(images.get("score-1.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * 0 + 317 - 8, 576 - 55 + 10, null);
            g2.drawImage(images.get("score-0.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * 1 + 317 - 8, 576 - 55 + 10, null);
            g2.drawImage(images.get("score-0.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * 2 + 317 - 8, 576 - 55 + 10, null);
            //打点
            g2.drawImage(images.get("score-dot.png").getScaledInstance(20, 45, Image.SCALE_SMOOTH), 37 * 1 + 407 - 8, 576 - 55 + 10, null);
            //从点的右边（+27像素）开始画两个0
            g2.drawImage(images.get("score-0.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 27 * 1 + 37 * 1 + 407 - 8, 576 - 55 + 10, null);
            g2.drawImage(images.get("score-0.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 27 * 1 + 37 * 2 + 407 - 8, 576 - 55 + 10, null);
            g2.drawImage(images.get("score-percent.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 27 * 1 + 407 - 8 + 37 * 3, 576 - 55 + 10, null);
        } else {
            //将ACC转化为整数部分、小数点和小数部分
            char[] accArray = accS.toCharArray();
            g2.drawImage(images.get("score-" + String.valueOf(accArray[0]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * 0 + 317 - 8 + 15, 576 - 55 + 10, null);
            g2.drawImage(images.get("score-" + String.valueOf(accArray[1]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * 1 + 317 - 8 + 15, 576 - 55 + 10, null);
            //打点
            g2.drawImage(images.get("score-dot.png").getScaledInstance(20, 45, Image.SCALE_SMOOTH), 407 - 8, 576 - 55 + 15, null);

            g2.drawImage(images.get("score-" + String.valueOf(accArray[3]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 27 * 1 + 407 - 8, 576 - 55 + 10, null);
            g2.drawImage(images.get("score-" + String.valueOf(accArray[4]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 27 * 1 + 407 - 8 + 37 * 1, 576 - 55 + 10, null);
            g2.drawImage(images.get("score-percent.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 27 * 1 + 407 - 8 + 37 * 2, 576 - 55 + 10, null);
        }
        if (!mods.isEmpty()) {
            int i = 0;
            for (Map.Entry<String, String> entry : mods.entrySet()) {
                //第一个mod画在1237，第二个画在1237+30,第三个1237-30(没有实现)
                g2.drawImage(images.get("selection-mod-" + entry.getValue() + ".png"), 1237 - (50 * i), 375, null);
                i++;
            }
        }
        //写字
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //指定颜色
        g2.setPaint(Color.decode("#FFFFFF"));
        //指定字体
        g2.setFont(new Font("Aller light", 0, 29));
        //指定坐标
        g2.drawString(beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]", 7, 26);
        g2.setFont(new Font("Aller", 0, 20));
        g2.drawString("Beatmap by " + beatmap.getCreator(), 7, 52);
        g2.drawString("Played by " + userFromAPI.getUserName() + " on " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(score.getDate()) + ".", 7, 74);

        if (oppaiResult != null) {

            int progress = 300 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss())
                    / (oppaiResult.getNumCircles() + oppaiResult.getNumSliders() + oppaiResult.getNumSpinners());
            //进度条
            if (score.getRank().equals("F")) {
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(Color.decode("#99cc31"));
                g2.drawLine(262, 615, 262 + progress, 615);
                g2.setColor(Color.decode("#fe0000"));
                g2.drawLine(262 + progress, 615, 262 + progress, 753);
            }

            //底端PP面板，在oppai计算结果不是null的时候
            if (oppaiResult != null) {
                g2.drawImage(images.get("ppBanner.png"), 570, 700, null);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(Color.decode("#ff66a9"));
                g2.setFont(new Font("Gayatri", 0, 60));
                if (String.valueOf(Math.round(oppaiResult.getPp())).contains("1")) {
                    g2.drawString(String.valueOf(Math.round(oppaiResult.getPp())), 616, 753);
                } else {
                    g2.drawString(String.valueOf(Math.round(oppaiResult.getPp())), 601, 753);
                }
                g2.setFont(new Font("Gayatri", 0, 48));
                g2.drawString(String.valueOf(Math.round(oppaiResult.getAimPp())), 834, 758);
                g2.drawString(String.valueOf(Math.round(oppaiResult.getSpeedPp())), 932, 758);
                g2.drawString(String.valueOf(Math.round(oppaiResult.getAccPp())), 1030, 758);
            }
        }
        g2.dispose();
        if (!defaultBG) {
            //用了默认bg不压图
            result = new BufferedImage(1366, 768, BufferedImage.TYPE_USHORT_555_RGB);
            Graphics2D g3 = result.createGraphics();
            g3.clearRect(0, 0, 1366, 768);
            g3.drawImage(bg.getScaledInstance(1366, 768, Image.SCALE_SMOOTH), 0, 0, null);
            g3.dispose();
            result.flush();
        } else {
            result = bg;
            bg.flush();
        }
        return drawImage(result);
    }

    public String drawFirstRank(Beatmap beatmap, Score score, Userinfo userFromAPI, Long xE) {
        BufferedImage bg;
        Image bg2;
        //头像
        BufferedImage ava = webPageUtil.getAvatar(userFromAPI.getUserId());
        OppaiResult oppaiResult = scoreUtil.calcPP(score, beatmap);

        try {
            bg = webPageUtil.getBG(beatmap);
        } catch (NullPointerException e) {
            //随机抽取一个bg
            String RandomBG = "defaultBG1" + ((int) (Math.random() * 2) + 2) + ".png";
            bg = getCopyImage(images.get(RandomBG));
        }
        //缩略图

        bg2 = getCopyImage(bg).getScaledInstance(161, 121, Image.SCALE_SMOOTH);

        //拉伸裁剪原bg

        Image bgTmp = bg.getScaledInstance(1580, 888, Image.SCALE_SMOOTH);

        bg = new BufferedImage(1580, 888, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bg.createGraphics();
        bGr.drawImage(bgTmp, 0, 0, null);
        bGr.dispose();
        bg = bg.getSubimage(0, 0, 1580, 286);


        Graphics2D g2 = bg.createGraphics();
        //全局平滑
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //画好布局
        g2.drawImage(images.get("fpLayout.png"), 0, 0, null);
        //Ranked状态
        g2.drawImage(images.get("fpRank" + beatmap.getApproved() + ".png"), 0, 0, null);
        //歌曲信息（这里不能是null）
        String title = "";
        //source artist
        if (!beatmap.getSource().equals("")) {
            title = unicodeToString(beatmap.getSource());
            //换用Java实现之后这里不是Null而是""了
            if (!"".equals(oppaiResult.getArtistUnicode())) {
                title = title.concat(" (" + oppaiResult.getArtistUnicode() + ") ");
            } else {
                title = title.concat(" (" + oppaiResult.getArtist() + ") ");
            }
        } else {
            if (!"".equals(oppaiResult.getArtistUnicode() )) {
                title = title.concat(oppaiResult.getArtistUnicode());
            } else {
                title = title.concat(oppaiResult.getArtist());
            }
        }
        //artist

        //title
        if (!"".equals(oppaiResult.getTitleUnicode() )) {
            title = title.concat(" - " + oppaiResult.getTitleUnicode());
        } else {
            title = title.concat(" - " + oppaiResult.getTitle());
        }
        title = title.concat(" [" + oppaiResult.getVersion() + "]");

        //白色字体
        g2.setPaint(Color.decode("#FFFFFF"));

        g2.setFont(new Font("微软雅黑", Font.PLAIN, 32));
        g2.drawString(title, 54, 31);

        //作者信息
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 23));
        g2.drawString("作者：" + oppaiResult.getCreator(), 54, 54);
        //长度、bpm、物件数
        g2.setFont(new Font("微软雅黑", Font.BOLD, 23));
        String length = "";
        //加入自动补0
        g2.drawString("长度：" + String.format("%02d", Integer.valueOf(beatmap.getTotalLength()) / 60) + ":"
                + String.format("%02d", Integer.valueOf(beatmap.getTotalLength()) % 60)
                + "  BPM：" + Math.round(Float.valueOf(beatmap.getBpm()))
                + "  物件数：" + new DecimalFormat("###,###").format((oppaiResult.getNumCircles() + oppaiResult.getNumSliders() + oppaiResult.getNumSpinners())), 7, 80);

        //圈数、滑条数、转盘数
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 23));
        g2.drawString("圈数：" + new DecimalFormat("###,###").format(oppaiResult.getNumCircles())
                + "  滑条数：" + new DecimalFormat("###,###").format(oppaiResult.getNumSliders())
                + "  转盘数：" + new DecimalFormat("###,###").format(oppaiResult.getNumSpinners()), 7, 108);

        //四围、难度
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        g2.drawString("CS:" + beatmap.getDiffSize() + " AR:" + beatmap.getDiffApproach()
                + " OD:" + beatmap.getDiffOverall() + " HP:" + beatmap.getDiffDrain()
                + " Stars:" + new DecimalFormat("###.00").format(Double.valueOf(beatmap.getDifficultyRating())), 7, 125);
        //小头像
        g2.drawImage(ava.getScaledInstance(66, 66, Image.SCALE_SMOOTH), 14, 217, null);
        //id
        g2.setFont(new Font("Ubuntu", Font.PLAIN, 32));
        //投影
        g2.setPaint(Color.decode("#000000"));
        g2.drawString(userFromAPI.getUserName(), 144, 245);
        g2.setPaint(Color.decode("#FFFFFF"));
        //本体
        g2.drawString(userFromAPI.getUserName(), 143, 244);
        //分数+cb
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 22));
        //投影
        g2.setPaint(Color.decode("#000000"));
        g2.drawString("得分：" + new DecimalFormat("###,###").format(score.getScore()) + " (" + score.getMaxCombo() + "x)", 141, 277);
        g2.setPaint(Color.decode("#FFFFFF"));
        g2.drawString("得分：" + new DecimalFormat("###,###").format(score.getScore()) + " (" + score.getMaxCombo() + "x)", 140, 276);

        //mod
        if (score.getEnabledMods() > 0) {
            List<String> mods = new ArrayList<>();
            for (Map.Entry<String, String> entry : scoreUtil.convertMOD(score.getEnabledMods()).entrySet()) {
                mods.add(entry.getKey());
            }
            g2.setFont(new Font("Arial", Font.PLAIN, 17));
            int a = g2.getFontMetrics(new Font("Arial", Font.PLAIN, 17)).stringWidth(mods.toString().replaceAll("[\\[\\]]", ""));

            //投影
            g2.setPaint(Color.decode("#000000"));
            g2.drawString(mods.toString().replaceAll("[\\[\\]]", ""), 532 - a, 232);
            g2.drawString(mods.toString().replaceAll("[\\[\\]]", ""), 534 - a, 234);
            g2.setPaint(Color.decode("#FFFFFF"));
            g2.drawString(mods.toString().replaceAll("[\\[\\]]", ""), 533 - a, 233);
        }
        //acc
        g2.setFont(new Font("Ubuntu", Font.PLAIN, 17));
        String accS = new DecimalFormat("###.00").format(100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50()) / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss())));
        int a = g2.getFontMetrics(new Font("Ubuntu", Font.PLAIN, 17)).stringWidth(accS);

        //投影
        g2.setPaint(Color.decode("#000000"));
        g2.drawString(accS + "%", 517 - a, 255);
        g2.drawString(accS + "%", 519 - a, 257);
        g2.setPaint(Color.decode("#FFFFFF"));
        g2.drawString(accS + "%", 518 - a, 256);


        //分差
        g2.setFont(new Font("Ubuntu", Font.PLAIN, 17));
        a = g2.getFontMetrics(new Font("Ubuntu", Font.PLAIN, 17)).stringWidth("+ " + String.valueOf(xE));

        g2.setPaint(Color.decode("#000000"));
        g2.drawString("+" + new DecimalFormat("###,###").format(xE), 532 - a, 278);
        g2.drawString("+" + new DecimalFormat("###,###").format(xE), 534 - a, 280);
        g2.setPaint(Color.decode("#FFFFFF"));
        g2.drawString("+" + new DecimalFormat("###,###").format(xE), 533 - a, 279);

        //Rank标志
        g2.drawImage(images.get("fp" + score.getRank() + ".png"), 0, 0, null);
        //头像上的灰板
        g2.drawImage(images.get("fpMark.png"), 0, 0, null);
        //谱面的Rank状态
        g2.drawImage(images.get("fpRank" + beatmap.getApproved() + ".png"), 0, 0, null);
        //右侧title
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 31));
        g2.setPaint(Color.decode("#000000"));
        if (!"".equals(oppaiResult.getTitleUnicode())) {
            g2.drawString(oppaiResult.getTitleUnicode(), 982, 196);
        } else {
            g2.drawString(oppaiResult.getTitle(), 982, 196);
        }
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 22));
        //artist//creator
        if (!"".equals(oppaiResult.getArtistUnicode())) {
            g2.drawString(oppaiResult.getArtistUnicode() + " // " + oppaiResult.getCreator(), 982, 223);
        } else {
            g2.drawString(oppaiResult.getArtist() + " // " + oppaiResult.getCreator(), 982, 223);
        }
        //难度名
        g2.setFont(new Font("微软雅黑", Font.BOLD, 22));
        g2.drawString(oppaiResult.getVersion(), 982, 245);
        //小星星
        String[] b = String.valueOf(beatmap.getDifficultyRating()).split("\\.");
        //取出难度的整数部分，画上对应的star
        for (int i = 0; i < Integer.valueOf(b[0]); i++) {
            g2.drawImage(images.get("fpStar.png"), 984 + 44 * i, 250, null);
        }

        //取出小数部分，缩放star并绘制在对应的地方
        float c = Integer.valueOf(b[1].substring(0, 1)) / 10F;
        //小于0.04的宽高会是0，
        if (c > 0.04) {
            g2.drawImage(images.get("fpStar.png").getScaledInstance((int) (25 * c), (int) (25 * c), Image.SCALE_SMOOTH),
                    (int) (984 + (Integer.valueOf(b[0])) * 44), (int) (250 + (1 - c) * 12.5), null);
        }
        //缩略图
        g2.drawImage(bg2, 762, 162, null);
        g2.dispose();
        return drawImage(bg);

    }


    public BufferedImage getCopyImage(BufferedImage bi) {
//        return bi.getSubimage(0, 0, bi.getWidth(), bi.getHeight());

        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
        //这他妈不就是自己重写了getSubimage方法吗……为什么getSubimage返回的是原图，这个就能复制一份woc
        //不采用这套方案（会抹掉透明通道
//        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.SCALE_SMOOTH);
//        Graphics g = b.getGraphics();
//        g.drawImage(source, 0, 0, null);
//        g.dispose();
//        return b;
    }


    private void draw(Graphics2D g2, String color, String font, String size, String text, String x, String y) {
        //指定颜色
        g2.setPaint(Color.decode(Overall.CABBAGE_CONFIG.getString(color)));
        //指定字体
        g2.setFont(new Font(Overall.CABBAGE_CONFIG.getString(font), Font.PLAIN, Integer.decode(Overall.CABBAGE_CONFIG.getString(size))));
        //指定坐标
        g2.drawString(text, Integer.decode(Overall.CABBAGE_CONFIG.getString(x)), Integer.decode(Overall.CABBAGE_CONFIG.getString(y)));

    }

    public String drawImage(BufferedImage img) {
        org.apache.commons.codec.binary.Base64 base64 = new org.apache.commons.codec.binary.Base64();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            img.flush();
            byte[] imgBytes = out.toByteArray();
            return base64.encodeToString(imgBytes);
        } catch (IOException e) {
            e.getMessage();
            return null;
        }
    }


    private String unicodeToString(String str) {

        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(str);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            str = str.replace(matcher.group(1), ch + "");
        }
        return str;
    }


}
