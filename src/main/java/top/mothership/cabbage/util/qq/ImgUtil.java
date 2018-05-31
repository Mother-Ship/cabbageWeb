package top.mothership.cabbage.util.qq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.pattern.RegularPattern;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.OppaiResult;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.util.osu.ScoreUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;

import static top.mothership.cabbage.util.qq.CompressLevelEnum.*;


/**
 * 绘图工具类。
 *
 * @author QHS
 */
@Component
//采用原型模式注入，避免出现错群问题
//2017-11-6 12:50:14改为全部返回BASE64编码
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS, value = "prototype")


public class ImgUtil {
    /**
     * 2017-9-8 13:55:42我他妈是个智障……没初始化的map我在下面用
     */
    public static Map<String, BufferedImage> images;
    private static Logger logger = LogManager.getLogger(ImgUtil.class);
    private WebPageManager webPageManager;
    private ScoreUtil scoreUtil;

    /**
     * Instantiates a new Img util.
     *
     * @param webPageManager the web page manager
     * @param scoreUtil      the score util
     */
    @Autowired
    public ImgUtil(WebPageManager webPageManager, ScoreUtil scoreUtil) {
        this.webPageManager = webPageManager;
        this.scoreUtil = scoreUtil;
        //我明白了 loadcache不能放在这个类的构造函数里，因为每次要绘图都会实例化一个新的ImgUtil，然后这个静态的缓存都会被重新刷新一次
        //又因为我没考虑线程安全，所以才有几率出null
        //而且resDAO也不能在这个类里声明……反正是初始化顺序的原因
    }


    /**
     * 绘制Stat图片（已改造完成）
     * 为线程安全，将当前时间毫秒数加入文件名并返回(被废弃：已经采用base64编码)
     *
     * @param userFromAPI 最新用户信息
     * @param userInDB    作为对比的信息
     * @param role        用户组
     * @param day         对比的天数
     * @param approximate 是否是接近的数据
     * @param scoreRank   scoreRank
     * @param mode        模式，只支持0/1/2/3
     * @return Base64字串 string
     */
    public String drawUserInfo(Userinfo userFromAPI, Userinfo userInDB, String role, int day, boolean approximate, int scoreRank, Integer mode) {
        BufferedImage ava = webPageManager.getAvatar(userFromAPI.getUserId());
        BufferedImage bg;
        BufferedImage layout = getCopyImage(images.get("layout.png"));
        BufferedImage scoreRankBG = getCopyImage(images.get("scorerank.png"));
        BufferedImage roleBg;
        try {
            roleBg = getCopyImage(images.get("role-" + role + ".png"));
        } catch (NullPointerException e) {
            roleBg = getCopyImage(images.get("role-creep.png"));
        }

        bg = images.get(String.valueOf(userFromAPI.getUserId()) + ".png");
        if (bg != null) {
            bg = getCopyImage(bg);
        } else {
            //现在已经不用再去扫描硬盘了啊
            try {
                bg = getCopyImage(images.get(role + ".png"));
            } catch (NullPointerException ignore) {
                //这个异常是在出现了新用户组，但是没有准备这个用户组的背景时候出现
                //2018-2-27 09:34:46 调整逻辑，没准备背景时候使用默认
                bg = getCopyImage(images.get("creep.png"));
            }
        }

        Graphics2D g2 = (Graphics2D) bg.getGraphics();
        //绘制布局
        g2.drawImage(layout, 0, 0, null);
        //模式的图标
        g2.drawImage(images.get("mode-" + mode + ".png"), 365, 80, null);
        //用户组对应的背景
        g2.drawImage(roleBg, 0, 0, null);
        try {
            //绘制头像
            g2.drawImage(ava, 160, 22, null);
        } catch (NullPointerException ignore) {
            //没有头像。不做处理
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //将score rank比用户名先画

        if (scoreRank > 0) {
            //把score rank用到的bg画到bg上
            g2.drawImage(scoreRankBG, 653, 7, null);
            Integer x;
            if (scoreRank < 100) {
                x = 722;
            } else {
                x = 697;
            }
            drawTextToImage(g2, "#FFFFFF", "Gayatri", 50, "#" + scoreRank, x, 58);
        }

        //绘制用户名
        drawTextToImage(g2, "#000000", "Aller light", 48, userFromAPI.getUserName(), 349, 60);
        //绘制Rank
        drawTextToImage(g2, "#222222", "Futura Std Medium", 48, "#" + userFromAPI.getPpRank(), 415, 114);
        //绘制PP
        drawTextToImage(g2, "#555555", "Futura Std Medium", 36, String.valueOf(userFromAPI.getPpRaw()), 469, 157);
        //绘制RankedScore
        drawTextToImage(g2, "#222222", "Futura Std Medium", 30,
                new DecimalFormat("###,###").format(userFromAPI.getRankedScore()), 370, 206);
        //绘制acc
        drawTextToImage(g2, "#222222", "Futura Std Medium", 30,
                new DecimalFormat("##0.00").format(userFromAPI.getAccuracy()) + "%", 357, 255);
        //绘制pc
        drawTextToImage(g2, "#222222", "Futura Std Medium", 30,
                new DecimalFormat("###,###").format(userFromAPI.getPlayCount()), 344, 302);
        //绘制tth
        drawTextToImage(g2, "#222222", "Futura Std Medium", 30,
                new DecimalFormat("###,###").format(userFromAPI.getCount50() + userFromAPI.getCount100() + userFromAPI.getCount300()),
                333, 350);
        //绘制Level
        drawTextToImage(g2, "#222222", "Futura Std Medium", 30,
                Integer.toString((int) Math.floor(userFromAPI.getLevel())) + " (" + (int) ((userFromAPI.getLevel() - Math.floor(userFromAPI.getLevel())) * 100) + "%)",
                320, 398);
        //绘制SS计数
        drawTextToImage(g2, "#222222", "Futura Std Medium", 24, Integer.toString(userFromAPI.getCountRankSs()), 343, 445);
        //绘制S计数
        drawTextToImage(g2, "#222222", "Futura Std Medium", 24, Integer.toString(userFromAPI.getCountRankS()), 496, 445);
        //绘制A计数
        drawTextToImage(g2, "#222222", "Futura Std Medium", 24, Integer.toString(userFromAPI.getCountRankA()), 666, 445);
        //绘制当时请求的时间
        drawTextToImage(g2, "#BC2C00", "Ubuntu Medium", 18, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss"))
//        new SimpleDateFormat("yy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())
                , 802, 452);
        //---------------------------以上绘制在线部分完成--------------------------------
        //试图查询数据库中指定日期的user
        //这里应该是不需要防null判断的
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
                if (approximate) {
                    //如果取到的是模糊数据
                    drawTextToImage(g2, "#666666", "宋体", 15, "请求的日期没有数据", 718, 138);
                    //算出天数差别
                    drawTextToImage(g2, "#666666", "宋体", 15, "『对比于" +
                            ChronoUnit.DAYS.between(userInDB.getQueryDate(), LocalDate.now())
//                            Long.valueOf(((Calendar.getInstance().getTime().getTime() - .getTime()) / 1000 / 60 / 60 / 24)).toString()
                            + "天前』", 725, 155);
                } else {
                    //如果取到的是精确数据
                    drawTextToImage(g2, "#666666", "宋体", 15, "『对比于" + day + "天前』", 725, 155);
                }

            }
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //这样确保了userInDB不是空的
            //绘制Rank变化
            if (userInDB.getPpRank() > userFromAPI.getPpRank()) {
                //如果查询的rank比凌晨中的小
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + Integer.toString(userInDB.getPpRank() - userFromAPI.getPpRank()), 633, 109);
            } else if (userInDB.getPpRank() < userFromAPI.getPpRank()) {
                //如果掉了rank
                drawTextToImage(g2, "#4466FF", "苹方", 24,
                        "↓" + Integer.toString(userFromAPI.getPpRank() - userInDB.getPpRank()), 633, 109);
            } else {
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + Integer.toString(0), 633, 109);
            }
            //绘制PP变化
            if (userInDB.getPpRaw() > userFromAPI.getPpRaw()) {
                //如果查询的pp比凌晨中的小
                drawTextToImage(g2, "#4466FF", "苹方", 24,
                        "↓" + new DecimalFormat("##0.00").format(userInDB.getPpRaw() - userFromAPI.getPpRaw()), 633, 153);
            } else if (userInDB.getPpRaw() < userFromAPI.getPpRaw()) {
                //刷了PP
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + new DecimalFormat("##0.00").format(userFromAPI.getPpRaw() - userInDB.getPpRaw()), 633, 153);
            } else {
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + Integer.toString(0), 633, 153);
            }

            //绘制RankedScore变化
            if (userInDB.getRankedScore() < userFromAPI.getRankedScore()) {
                //因为RankedScore不会变少，所以不写蓝色部分
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + new DecimalFormat("###,###").format(userFromAPI.getRankedScore() - userInDB.getRankedScore()), 650, 203);
            } else {
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + Integer.toString(0), 650, 203);
            }
            //绘制ACC变化
            //在这里把精度砍掉
            if (Float.valueOf(new DecimalFormat("##0.00").format(userInDB.getAccuracy())) > Float.valueOf(new DecimalFormat("##0.00").format(userFromAPI.getAccuracy()))) {
                //如果acc降低了
                drawTextToImage(g2, "#4466FF", "苹方", 24,
                        "↓" + new DecimalFormat("##0.00").format(userInDB.getAccuracy() - userFromAPI.getAccuracy()) + "%", 636, 251);
            } else if (Float.valueOf(new DecimalFormat("##0.00").format(userInDB.getAccuracy())) < Float.valueOf(new DecimalFormat("##0.00").format(userFromAPI.getAccuracy()))) {
                //提高
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + new DecimalFormat("##0.00").format(userFromAPI.getAccuracy() - userInDB.getAccuracy()) + "%", 636, 251);

            } else {
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + new DecimalFormat("##0.00").format(0.00) + "%", 636, 251);
            }

            //绘制pc变化
            if (userInDB.getPlayCount() < userFromAPI.getPlayCount()) {
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + new DecimalFormat("###,###").format(userFromAPI.getPlayCount() - userInDB.getPlayCount()), 622, 299);

            } else {
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + Integer.toString(0), 622, 299);

            }

            //绘制tth变化,此处开始可以省去颜色设置
            if (userInDB.getCount50() + userInDB.getCount100() + userInDB.getCount300()
                    < userFromAPI.getCount50() + userFromAPI.getCount100() + userFromAPI.getCount300()) {
                //同理不写蓝色部分
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + new DecimalFormat("###,###").format(userFromAPI.getCount50() + userFromAPI.getCount100() + userFromAPI.getCount300() - (userInDB.getCount50() + userInDB.getCount100() + userInDB.getCount300())), 609, 347);
            } else {
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + Integer.toString(0), 609, 347);
            }
            //绘制level变化
            if (Float.valueOf(new DecimalFormat("##0.00").format(userInDB.getLevel())) < Float.valueOf(new DecimalFormat("##0.00").format(userFromAPI.getLevel()))) {
                //同理不写蓝色部分
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + (int) ((userFromAPI.getLevel() - userInDB.getLevel()) * 100) + "%", 597, 394);
            } else {
                drawTextToImage(g2, "#FF6060", "苹方", 24,
                        "↑" + Integer.toString(0) + "%", 597, 394);
            }
            //绘制SS count 变化
            //这里需要改变字体大小
            if (userInDB.getCountRankSs() > userFromAPI.getCountRankSs()) {
                //如果查询的SS比凌晨的少
                drawTextToImage(g2, "#4466FF", "苹方", 18,
                        "↓" + Integer.toString(userInDB.getCountRankSs() - userFromAPI.getCountRankSs()), 414, 444);
            } else if (userInDB.getCountRankSs() < userFromAPI.getCountRankSs()) {
                //如果SS变多了
                drawTextToImage(g2, "#FF6060", "苹方", 18,
                        "↑" + Integer.toString(userFromAPI.getCountRankSs() - userInDB.getCountRankSs()), 414, 444);
            } else {
                drawTextToImage(g2, "#FF6060", "苹方", 18,
                        "↑" + Integer.toString(0), 414, 444);
            }
            //s
            if (userInDB.getCountRankS() > userFromAPI.getCountRankS()) {
                //如果查询的S比凌晨的少
                drawTextToImage(g2, "#4466FF", "苹方", 18,
                        "↓" + Integer.toString(userInDB.getCountRankS() - userFromAPI.getCountRankS()), 568, 444);
            } else if (userInDB.getCountRankS() < userFromAPI.getCountRankS()) {
                //如果S变多了
                drawTextToImage(g2, "#FF6060", "苹方", 18,
                        "↑" + Integer.toString(userFromAPI.getCountRankS() - userInDB.getCountRankS()), 568, 444);
            } else {
                drawTextToImage(g2, "#FF6060", "苹方", 18,
                        "↑" + Integer.toString(0), 568, 444);
            }
            //a
            if (userInDB.getCountRankA() > userFromAPI.getCountRankA()) {
                //如果查询的S比凌晨的少
                drawTextToImage(g2, "#4466FF", "苹方", 18,
                        "↓" + Integer.toString(userInDB.getCountRankA() - userFromAPI.getCountRankA()), 738, 444);
            } else if (userInDB.getCountRankA() < userFromAPI.getCountRankA()) {
                //如果S变多了
                drawTextToImage(g2, "#FF6060", "苹方", 18,
                        "↑" + Integer.toString(userFromAPI.getCountRankA() - userInDB.getCountRankA()), 738, 444);
            } else {
                drawTextToImage(g2, "#FF6060", "苹方", 18,
                        "↑" + Integer.toString(0), 738, 444);
            }
        }
        g2.dispose();
        return drawImage(bg, USHORT_555_RGB_PNG);
    }

    /**
     * 绘制BP列表（已改造完成）
     *
     * @param userFromAPI the user from api
     * @param list        the list
     * @param mode        the mode
     * @return the string
     */
    public String drawUserBP(Userinfo userFromAPI, List<Score> list, Integer mode, boolean mixedmode) {

        //计算最终宽高
        int height = images.get("bptop.png").getHeight();
        int heightpoint = 0;
        int width = images.get("bptop.png").getWidth();
        for (Score aList : list) {
            if (aList.getBeatmapName().length() <= 80) {
                height = height + images.get("bpmid2.png").getHeight();
            } else {
                height = height + images.get("bpmid3.png").getHeight();
            }
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();

        //头部
        BufferedImage bpTop = getCopyImage(images.get("bptop.png"));
        Graphics2D g2 = (Graphics2D) bpTop.getGraphics();
        //模式图标(所有bp为同一模式的时候画在顶上)
        if (!mixedmode) {
            g2.drawImage(images.get("mode-" + mode + ".png"), 650, 4, null);
        }
        //那行字
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawTextToImage(g2, "#de3397", "Tahoma", 20, "Best Performance of " + userFromAPI.getUserName(), 27, 25);
        Calendar c = Calendar.getInstance();
        //日期补丁
        if (c.get(Calendar.HOUR_OF_DAY) < 4) {
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        drawTextToImage(g2, "#666666", "Tahoma Bold", 16, new SimpleDateFormat("yy-MM-dd").format(c.getTime()), 707, 31);
        g2.dispose();
        //生成好的画上去
        g.drawImage(bpTop, 0, heightpoint, null);
        //移动这个类似指针的东西
        heightpoint = heightpoint + bpTop.getHeight();

        //开始绘制每行的bp
        for (Score aList : list) {
            String acc = scoreUtil.genAccString(aList, (int) aList.getMode());
            String mods = scoreUtil.convertModToString(aList.getEnabledMods());
            int a;
            if (aList.getBeatmapName().length() <= 80) {
                a = 2;
            } else {
                a = 3;
            }
            BufferedImage bpMid = null;
            Graphics2D g3 = null;
            switch (a) {
                case 2:
                    bpMid = getCopyImage(images.get("bpmid2.png"));
                    g3 = bpMid.createGraphics();
                    //小图标
                    g3.drawImage(images.get(aList.getRank() + "_small.png"), 10, 2, null);
                    //绘制文字
                    g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    //绘制日期(给的就是北京时间，不转)
                    drawTextToImage(g3, "#696969", "Tahoma", 14,
                            new SimpleDateFormat("MM-dd HH:mm").format(aList.getDate().getTime()), 31, 34);
                    //绘制Num和Weight
                    drawTextToImage(g3, "#a12e1e", "Ubuntu Medium", 13,
                            String.valueOf(aList.getBpId() + 1), 136, 34);

                    drawTextToImage(g3, "#a12e1e", "Ubuntu Medium", 14,
                            new DecimalFormat("##0.00").format(100 * Math.pow(0.95, aList.getBpId())) + "%", 221, 34);

                    //绘制MOD
                    drawTextToImage(g3, "#222222", "Tahoma Bold", 14, mods, 493, 34);
                    //绘制PP
                    drawTextToImage(g3, "#9492dc", "Tahoma Bold", 24, Integer.toString(Math.round(aList.getPp())) + "pp", 709, 28);
                    //歌名
                    drawTextToImage(g3, "#3843a6", "Ubuntu Medium", 16,
                            aList.getBeatmapName() + "(" + acc + "%)", 26, 16);
                    if (mixedmode) {
                        g3.drawImage(images.get("mode-" + aList.getMode() + ".png"), 650, 4, null);
                    }
                    break;
                case 3:
                    bpMid = getCopyImage(images.get("bpmid3.png"));
                    g3 = bpMid.createGraphics();
                    //小图标
                    g3.drawImage(images.get(aList.getRank() + "_small.png"), 10, 1, null);
                    //绘制文字
                    g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    //绘制日期(给的就是北京时间，不转)
                    drawTextToImage(g3, "#696969", "Tahoma", 14,
                            new SimpleDateFormat("MM-dd HH:mm").format(aList.getDate().getTime()), 31, 48);
                    //绘制Num和Weight
                    drawTextToImage(g3, "#a12e1e", "Ubuntu Medium", 13,
                            String.valueOf(aList.getBpId() + 1), 136, 48);

                    drawTextToImage(g3, "#a12e1e", "Ubuntu Medium", 14,
                            new DecimalFormat("##0.00").format(100 * Math.pow(0.95, aList.getBpId())) + "%", 221, 48);

                    //绘制MOD
                    drawTextToImage(g3, "#222222", "Tahoma Bold", 14, mods, 493, 48);
                    //绘制PP
                    drawTextToImage(g3, "#9492dc", "Tahoma Bold", 24, Integer.toString(Math.round(aList.getPp())) + "pp", 709, 35);

                    //两行的歌名
                    drawTextToImage(g3, "#3843a6", "Ubuntu Medium", 16, aList.getBeatmapName().substring(0, aList.getBeatmapName().substring(0, 81).lastIndexOf(" ") + 1),
                            26, 15);
                    drawTextToImage(g3, "#3843a6", "Ubuntu Medium", 16, aList.getBeatmapName().substring(aList.getBeatmapName().substring(0, 81).lastIndexOf(" ") + 1, aList.getBeatmapName().length())
                                    + "(" + acc + "%)",
                            7, 30);
                    //模式图标
                    if (mixedmode) {
                        g3.drawImage(images.get("mode-" + aList.getMode() + ".png"), 650, 4, null);
                    }
                    break;
                default:
                    break;
            }
            g3.dispose();
            bpMid.flush();
            g.drawImage(bpMid, 0, heightpoint, null);
            heightpoint = heightpoint + bpMid.getHeight();
        }
        g.dispose();
        //不，文件名最好还是数字
        return drawImage(result, USHORT_555_RGB_PNG);

    }

    /**
     * 绘制结算界面
     *
     * @param userFromAPI the user from api
     * @param score       the score
     * @param beatmap     the beatmap
     * @param mode        the mode
     * @return the string
     */
    public String drawResult(Userinfo userFromAPI, Score score, Beatmap beatmap, int mode) {
        String accS = scoreUtil.genAccString(score, mode);
        float acc = Float.valueOf(accS);
        BufferedImage bg;
        Map<String, String> mods = scoreUtil.convertModToHashMap(score.getEnabledMods());
        //这个none是为了BP节省代码，在这里移除掉
        mods.remove("None");
        //离线计算PP
        OppaiResult oppaiResult = null;
        try {
            oppaiResult = scoreUtil.calcPP(score, beatmap);
        } catch (Exception ignore) {
            //如果acc过低或者不是std
        }

        try {
            bg = webPageManager.getBG(beatmap);
        } catch (NullPointerException e) {
            bg = webPageManager.getBGBackup(beatmap);
        }
        if (bg == null) {
            bg = webPageManager.getBGBackup(beatmap);
            if (bg == null) {
                //随机抽取一个bg
                String randomBG = "defaultBG1" + ((int) (Math.random() * 2) + 2) + ".png";
                bg = getCopyImage(images.get(randomBG));
            }
        }
        //2017-11-3 17:51:47这里有莫名的空指针，比较迷，在webPageManager.getBG里加一个判断为空则抛出空指针看看
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


        char[] count300 = String.valueOf(score.getCount300()).toCharArray();
        char[] countgeki = String.valueOf(score.getCountGeki()).toCharArray();
        char[] count100 = String.valueOf(score.getCount100()).toCharArray();
        char[] countkatu = String.valueOf(score.getCountKatu()).toCharArray();
        char[] count50 = String.valueOf(score.getCount50()).toCharArray();
        char[] count0 = String.valueOf(score.getCountMiss()).toCharArray();
        switch (mode) {
            case 0:
                //STD

                g2.drawImage(images.get("hit300.png").getScaledInstance(73, 73, Image.SCALE_SMOOTH), 30 - 4, 245 - 27, null);
                for (int i = 0; i < count300.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count300[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 238 - 7, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count300.length + 134 - 7, 238 - 7, null);

                //激
                g2.drawImage(images.get("hit300.png").getScaledInstance(73, 73, Image.SCALE_SMOOTH), 350 - 4, 245 - 27, null);
                for (int i = 0; i < countgeki.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(countgeki[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 238 - 7, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * countgeki.length + 455 - 8, 238 - 7, null);
                //100
                g2.drawImage(images.get("hit100.png").getScaledInstance(50, 30, Image.SCALE_SMOOTH), 44 - 5, 346 - 8, null);
                for (int i = 0; i < count100.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count100[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 374 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count100.length + 134 - 7, 374 - 55, null);

                //喝
                g2.drawImage(images.get("hit100.png").getScaledInstance(50, 30, Image.SCALE_SMOOTH), 364 - 5, 346 - 8, null);
                for (int i = 0; i < countkatu.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(countkatu[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 374 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * countkatu.length + 455 - 8, 374 - 55, null);

                //50
                g2.drawImage(images.get("hit50.png").getScaledInstance(35, 30, Image.SCALE_SMOOTH), 51 - 5, 455 - 21, null);
                for (int i = 0; i < count50.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count50[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 470 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count50.length + 134 - 7, 470 - 55, null);

                //x
                g2.drawImage(images.get("hit0.png").getScaledInstance(32, 32, Image.SCALE_SMOOTH), 376 - 4, 437 - 5, null);
                for (int i = 0; i < count0.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count0[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 470 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count0.length + 455 - 8, 470 - 55, null);

                if (oppaiResult != null) {

                    int progress = 300 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss())
                            / (oppaiResult.getNumCircles() + oppaiResult.getNumSliders() + oppaiResult.getNumSpinners());
                    //进度条
                    if (score.getRank().equals("F")) {
                        //设置直线断点平滑
                        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.setColor(Color.decode("#99cc31"));
                        g2.drawLine(262, 615, 262 + progress, 615);
                        g2.setColor(Color.decode("#fe0000"));
                        g2.drawLine(262 + progress, 615, 262 + progress, 753);
                    }

                    //底端PP面板，在oppai计算结果不是null的时候
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
                break;
            case 1:
                //太鼓
                //300
                g2.drawImage(images.get("taiko-hit300.png").getScaledInstance(45, 40, Image.SCALE_SMOOTH), 40 - 4, 263 - 27, null);
                for (int i = 0; i < count300.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count300[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 238 - 7, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count300.length + 134 - 7, 238 - 7, null);

                //激
                g2.drawImage(images.get("taiko-hit300g.png").getScaledInstance(45, 40, Image.SCALE_SMOOTH), 360 - 4, 263 - 27, null);
                for (int i = 0; i < countgeki.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(countgeki[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 238 - 7, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * countgeki.length + 455 - 8, 238 - 7, null);
                //100
                g2.drawImage(images.get("taiko-hit100.png").getScaledInstance(45, 40, Image.SCALE_SMOOTH), 42 - 5, 341 - 8, null);
                for (int i = 0; i < count100.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count100[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 374 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count100.length + 134 - 7, 374 - 55, null);

                //喝
                g2.drawImage(images.get("taiko-hit100k.png").getScaledInstance(45, 40, Image.SCALE_SMOOTH), 362 - 5, 341 - 8, null);
                for (int i = 0; i < countkatu.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(countkatu[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 374 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * countkatu.length + 455 - 8, 374 - 55, null);

                //x
                g2.drawImage(images.get("taiko-hit0.png").getScaledInstance(56, 56, Image.SCALE_SMOOTH), 42 - 5, 440 - 21, null);
                for (int i = 0; i < count0.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count0[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 470 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count0.length + 134 - 7, 470 - 55, null);
                if (score.getPp() != null) {
                    g2.drawImage(images.get("ppBanner2.png"), 570, 700, null);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(Color.decode("#ff66a9"));
                    g2.setFont(new Font("Gayatri", 0, 60));
                    if (String.valueOf(Math.round(score.getPp())).contains("1")) {
                        g2.drawString(String.valueOf(Math.round(score.getPp())), 616, 753);
                    } else {
                        g2.drawString(String.valueOf(Math.round(score.getPp())), 601, 753);
                    }
                }
                break;
            case 2:
                //CTB
                //300
                g2.drawImage(images.get("fruit-orange-overlay.png").getScaledInstance(64, 64, Image.SCALE_SMOOTH), 40 - 4, 250 - 27, null);
                for (int i = 0; i < count300.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count300[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 238 - 7, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count300.length + 134 - 7, 238 - 7, null);

                //100

                g2.drawImage(images.get("fruit-drop.png").getScaledInstance(54, 62, Image.SCALE_SMOOTH), 44 - 5, 330 - 8, null);
                for (int i = 0; i < count100.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count100[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 374 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count100.length + 134 - 7, 374 - 55, null);


                //50
                //翻转180度

                g2.drawImage(images.get("fruit-drop-reverse.png").getScaledInstance(45, 51, Image.SCALE_SMOOTH), 51 - 5, 440 - 21, null);
                for (int i = 0; i < count50.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count50[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 7, 470 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count50.length + 134 - 7, 470 - 55, null);


                //x
                //将橘子图标缩小
                g2.drawImage(images.get("fruit-orange-overlay.png").getScaledInstance(50, 50, Image.SCALE_SMOOTH), 360 - 4, 255 - 27, null);
                //补上那个小x
                g2.drawImage(images.get("hit0.png").getScaledInstance(10, 10, Image.SCALE_SMOOTH), 360 - 4, 255 - 27, null);
                for (int i = 0; i < count0.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count0[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 238 - 7, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count0.length + 455 - 8, 238 - 7, null);
                if (score.getPp() != null) {
                    g2.drawImage(images.get("ppBanner2.png"), 570, 700, null);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(Color.decode("#ff66a9"));
                    g2.setFont(new Font("Gayatri", 0, 60));
                    if (String.valueOf(Math.round(score.getPp())).contains("1")) {
                        g2.drawString(String.valueOf(Math.round(score.getPp())), 616, 753);
                    } else {
                        g2.drawString(String.valueOf(Math.round(score.getPp())), 601, 753);
                    }
                }
                break;
            case 3:
                //mania
                g2.drawImage(images.get("mania-hit300.png").getScaledInstance(137, 30, Image.SCALE_SMOOTH), 10 - 4, 263 - 27, null);
                for (int i = 0; i < count300.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count300[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 2, 238 - 7, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count300.length + 134 - 2, 238 - 7, null);

                //320
                g2.drawImage(images.get("mania-hit300g.png").getScaledInstance(137, 30, Image.SCALE_SMOOTH), 320 - 4, 263 - 27, null);
                for (int i = 0; i < countgeki.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(countgeki[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 238 - 7, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * countgeki.length + 455 - 8, 238 - 7, null);

                //200
                g2.drawImage(images.get("mania-hit200.png").getScaledInstance(137, 30, Image.SCALE_SMOOTH), 14 - 5, 336 - 8, null);
                for (int i = 0; i < countkatu.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(countkatu[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 2, 374 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * countkatu.length + 134 - 2, 374 - 55, null);

                //100
                g2.drawImage(images.get("mania-hit100.png").getScaledInstance(137, 30, Image.SCALE_SMOOTH), 320 - 5, 336 - 8, null);
                for (int i = 0; i < count100.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count100[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 374 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count100.length + 455 - 8, 374 - 55, null);

                //50
                g2.drawImage(images.get("mania-hit50.png").getScaledInstance(137, 30, Image.SCALE_SMOOTH), 1, 442 - 21, null);
                for (int i = 0; i < count50.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count50[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 134 - 2, 470 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count50.length + 134 - 2, 470 - 55, null);

                //x
                g2.drawImage(images.get("mania-hit0.png").getScaledInstance(137, 30, Image.SCALE_SMOOTH), 318 - 4, 429 - 5, null);
                for (int i = 0; i < count0.length; i++) {
                    //第二个参数是数字之间的距离+第一个数字离最左边的距离
                    g2.drawImage(images.get("score-" + String.valueOf(count0[i]) + ".png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * i + 455 - 8, 470 - 55, null);
                }
                //画上结尾的x
                g2.drawImage(images.get("score-x.png").getScaledInstance(40, 51, Image.SCALE_SMOOTH), 37 * count0.length + 455 - 8, 470 - 55, null);
                if (score.getPp() != null) {
                    g2.drawImage(images.get("ppBanner2.png"), 570, 700, null);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(Color.decode("#ff66a9"));
                    g2.setFont(new Font("Gayatri", 0, 60));
                    if (String.valueOf(Math.round(score.getPp())).contains("1")) {
                        g2.drawString(String.valueOf(Math.round(score.getPp())), 616, 753);
                    } else {
                        g2.drawString(String.valueOf(Math.round(score.getPp())), 601, 753);
                    }
                }
                break;
            default:
                break;
        }
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
        g2.setFont(new Font("Aller", 0, 21));
        g2.drawString("Beatmap by " + beatmap.getCreator(), 7, 52);
        g2.drawString("Played by " + userFromAPI.getUserName() + " on " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(score.getDate()) + ".", 7, 74);


        g2.dispose();

        return drawImage(bg, USHORT_555_RGB_PNG);
    }

    /**
     * 绘制某个谱面的#1
     *
     * @param beatmap     the beatmap
     * @param score       the score
     * @param userFromAPI the user from api
     * @param xE          the x e
     * @param mode        the mode
     * @return the string
     */
    public String drawFirstRank(Beatmap beatmap, Score score, Userinfo userFromAPI, Long xE, int mode) {
        BufferedImage bg;
        Image bg2;
        boolean unicode = false;
        //头像
        BufferedImage ava = webPageManager.getAvatar(userFromAPI.getUserId());
        OppaiResult oppaiResult = scoreUtil.calcPP(score, beatmap);

        bg = webPageManager.getBG(beatmap);

        if (bg == null) {
            bg = webPageManager.getBGBackup(beatmap);
            if (bg == null) {
                //随机抽取一个bg
                String randomBG = "defaultBG1" + ((int) (Math.random() * 2) + 2) + ".png";
                bg = getCopyImage(images.get(randomBG));
            }
        }
        //缩略图

        bg2 = webPageManager.resizeImg(getCopyImage(bg), 161, 121);

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
                unicode = true;
                title = title.concat(" (" + oppaiResult.getArtistUnicode() + ") ");
            } else {
                title = title.concat(" (" + oppaiResult.getArtist() + ") ");
            }
        } else {
            if (!"".equals(oppaiResult.getArtistUnicode())) {
                title = title.concat(oppaiResult.getArtistUnicode());
                unicode = true;
            } else {
                title = title.concat(oppaiResult.getArtist());
            }
        }
        //title
        if (!"".equals(oppaiResult.getTitleUnicode())) {
            title = title.concat(" - " + oppaiResult.getTitleUnicode());
            unicode = true;
        } else {
            title = title.concat(" - " + oppaiResult.getTitle());
        }
        title = title.concat(" [" + oppaiResult.getVersion() + "]");

        //白色字体
        g2.setPaint(Color.decode("#FFFFFF"));
        if (unicode) {
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 32));
        } else {
            g2.setFont(new Font("Aller light", Font.PLAIN, 32));
        }
        g2.drawString(title, 54, 31);


        //作者信息
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 23));
        g2.drawString("作者：" + oppaiResult.getCreator(), 54, 54);
        //长度、bpm、物件数
        g2.setFont(new Font("微软雅黑", Font.BOLD, 23));
        String length = "";
        //加入自动补0
        g2.drawString("长度：" + String.format("%02d", beatmap.getTotalLength() / 60) + ":"
                + String.format("%02d", beatmap.getTotalLength() % 60)
                + "  BPM：" + Math.round(beatmap.getBpm())
                + "  物件数：" + new DecimalFormat("###,###").format((oppaiResult.getNumCircles() + oppaiResult.getNumSliders() + oppaiResult.getNumSpinners())), 7, 80);

        //圈数、滑条数、转盘数
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 23));
        g2.drawString("圈数：" + new DecimalFormat("###,###").format(oppaiResult.getNumCircles())
                + "  滑条数：" + new DecimalFormat("###,###").format(oppaiResult.getNumSliders())
                + "  转盘数：" + new DecimalFormat("###,###").format(oppaiResult.getNumSpinners()), 7, 108);

        //四围、难度
        String arms = scoreUtil.calcMilliSecondForFourDimensions("AR", oppaiResult.getAr());
        String od300ms = scoreUtil.calcMilliSecondForFourDimensions("OD300", oppaiResult.getOd());
        String od100ms = scoreUtil.calcMilliSecondForFourDimensions("OD100", oppaiResult.getOd());
        String od50ms = scoreUtil.calcMilliSecondForFourDimensions("OD50", oppaiResult.getOd());
        g2.setFont(new Font("Aller", Font.PLAIN, 13));
        g2.drawString("CS:" + (double) Math.round(oppaiResult.getCs() * 100) / 100
                + " AR:" + (double) Math.round(oppaiResult.getAr() * 100) / 100
                + "(" + arms + ")"
                + " OD:" + (double) Math.round(oppaiResult.getOd() * 100) / 100
                + "(" + od300ms + "/" + od100ms + "/" + od50ms + ")"
                + " HP:" + (double) Math.round(oppaiResult.getHp() * 100) / 100
                + " Stars:" + new DecimalFormat("###.00").format(Double.valueOf(oppaiResult.getStars())), 7, 125);
        //小头像
        g2.drawImage(ava.getScaledInstance(66, 66, Image.SCALE_SMOOTH), 14, 217, null);
        //id
        g2.setFont(new Font("Aller", Font.PLAIN, 32));
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
            for (Map.Entry<String, String> entry : scoreUtil.convertModToHashMap(score.getEnabledMods()).entrySet()) {
                mods.add(entry.getKey());
            }
            g2.setFont(new Font("Aller Light", Font.PLAIN, 17));
            int a = g2.getFontMetrics(new Font("Aller Light", Font.PLAIN, 17)).stringWidth(mods.toString().replaceAll("[\\[\\]]", ""));

            //投影
            g2.setPaint(Color.decode("#000000"));
            g2.drawString(mods.toString().replaceAll("[\\[\\]]", ""), 532 - a, 232);
            g2.drawString(mods.toString().replaceAll("[\\[\\]]", ""), 534 - a, 234);
            g2.setPaint(Color.decode("#FFFFFF"));
            g2.drawString(mods.toString().replaceAll("[\\[\\]]", ""), 533 - a, 233);
        }
        //acc
        g2.setFont(new Font("Aller Light", Font.PLAIN, 17));
        String accS = new DecimalFormat("###.00").format(100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50()) / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss())));
        int a = g2.getFontMetrics(new Font("Aller Light", Font.PLAIN, 17)).stringWidth(accS);

        //投影
        g2.setPaint(Color.decode("#000000"));
        g2.drawString(accS + "%", 517 - a, 255);
        g2.drawString(accS + "%", 519 - a, 257);
        g2.setPaint(Color.decode("#FFFFFF"));
        g2.drawString(accS + "%", 518 - a, 256);


        //分差
        g2.setFont(new Font("Aller Light", Font.PLAIN, 17));
        a = g2.getFontMetrics(new Font("Aller Light", Font.PLAIN, 17)).stringWidth("+ " + String.valueOf(xE));

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

        g2.setPaint(Color.decode("#000000"));
        int x;
        switch (mode) {
            case 0:
                x = 942;
                break;
            default:
                x = 982;
                g2.drawImage(images.get("mode-" + mode + ".png"), x, 166, null);
                break;
        }
        if (unicode) {
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 30));
            g2.drawString(oppaiResult.getTitleUnicode(), x, 196);
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 21));
            g2.drawString(oppaiResult.getArtistUnicode() + " // " + oppaiResult.getCreator(), x, 223);
        } else {
            g2.setFont(new Font("Aller light", Font.PLAIN, 30));
            g2.drawString(oppaiResult.getTitle(), x, 196);
            g2.setFont(new Font("Aller", Font.PLAIN, 21));
            g2.drawString(oppaiResult.getArtist() + " // " + oppaiResult.getCreator(), x, 223);
        }

        //难度名
        g2.setFont(new Font("Aller", Font.BOLD, 22));
        g2.drawString(oppaiResult.getVersion(), x, 245);
        //小星星
        String[] b = String.valueOf(beatmap.getDifficultyRating()).split("\\.");
        //取出难度的整数部分，画上对应的star
        for (int i = 0; i < Integer.valueOf(b[0]); i++) {
            g2.drawImage(images.get("fpStar.png"), x + 44 * i, 250, null);
        }

        //取出小数部分，缩放star并绘制在对应的地方
        float c = Integer.valueOf(b[1].substring(0, 1)) / 10F;
        //小于0.04的宽高会是0，
        if (c > 0.04) {
            g2.drawImage(images.get("fpStar.png").getScaledInstance((int) (25 * c), (int) (25 * c), Image.SCALE_SMOOTH),
                    x + (Integer.valueOf(b[0])) * 44, (int) (250 + (1 - c) * 12.5), null);
        }
        //缩略图
        g2.drawImage(bg2, 762, 162, null);
        g2.dispose();
        return drawImage(bg, 不压缩);

    }

    /**
     * 绘制Search结果
     *
     * @param beatmap the beatmap
     * @param mods    the mods
     * @param mode    the mode
     * @return the string
     */
    public String drawBeatmap(Beatmap beatmap, Integer mods, OppaiResult oppaiResult, int mode) {
        boolean unicode = false;
        BufferedImage bg;
        Image bg2;

        beatmap.setTotalLength((int) (beatmap.getTotalLength() / oppaiResult.getSpeedMultiplier()));
        beatmap.setBpm(beatmap.getBpm() * oppaiResult.getSpeedMultiplier());
        Map<String, String> modsMap = scoreUtil.convertModToHashMap(mods);
        //这个none是为了BP节省代码，在这里移除掉
        modsMap.remove("None");
        try {
            bg = webPageManager.getBG(beatmap);
        } catch (NullPointerException e) {
            bg = webPageManager.getBGBackup(beatmap);
        }
        if (bg == null) {
            bg = webPageManager.getBGBackup(beatmap);
            if (bg == null) {
                //随机抽取一个bg
                String randomBG = "defaultBG1" + ((int) (Math.random() * 2) + 2) + ".png";
                bg = getCopyImage(images.get(randomBG));
            }
        }
        //缩略图
        bg2 = webPageManager.resizeImg(getCopyImage(bg), 161, 121);

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
        g2.drawImage(images.get("infoLayout.png"), 0, 0, null);
        //Ranked状态
        g2.drawImage(images.get("fpRank" + beatmap.getApproved() + ".png"), 0, 0, null);

        if (!modsMap.isEmpty()) {
            int i = 0;
            for (Map.Entry<String, String> entry : modsMap.entrySet()) {
                //2018-2-27 09:31:41上移Mod图标位置
                g2.drawImage(images.get("selection-mod-" + entry.getValue() + ".png"), 460 + (50 * i), 46, null);
                i++;
            }
        }

        //歌曲信息（这里不能是null）
        String title = "";
        //source artist
        if (!beatmap.getSource().equals("")) {
            title = unicodeToString(beatmap.getSource());
            //换用Java实现之后这里不是Null而是""了
            if (!"".equals(oppaiResult.getArtistUnicode())) {
                unicode = true;
                title = title.concat(" (" + oppaiResult.getArtistUnicode() + ") ");
            } else {
                title = title.concat(" (" + oppaiResult.getArtist() + ") ");
            }
        } else {
            if (!"".equals(oppaiResult.getArtistUnicode())) {
                title = title.concat(oppaiResult.getArtistUnicode());
                unicode = true;
            } else {
                title = title.concat(oppaiResult.getArtist());
            }
        }
        //title
        if (!"".equals(oppaiResult.getTitleUnicode())) {
            title = title.concat(" - " + oppaiResult.getTitleUnicode());
            unicode = true;
        } else {
            title = title.concat(" - " + oppaiResult.getTitle());
        }
        title = title.concat(" [" + oppaiResult.getVersion() + "]");

        //白色字体
        g2.setPaint(Color.decode("#FFFFFF"));
        if (unicode) {
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 32));
        } else {
            g2.setFont(new Font("Aller light", Font.PLAIN, 32));
        }
        g2.drawString(title, 54, 31);

        //作者信息
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 23));
        g2.drawString("作者：" + oppaiResult.getCreator(), 54, 54);

        //圈数、滑条数、转盘数
        g2.setFont(new Font("微软雅黑", Font.PLAIN, 23));
        g2.drawString("圈数：" + new DecimalFormat("###,###").format(oppaiResult.getNumCircles())
                + "  滑条数：" + new DecimalFormat("###,###").format(oppaiResult.getNumSliders())
                + "  转盘数：" + new DecimalFormat("###,###").format(oppaiResult.getNumSpinners()), 7, 108);

        //长度、bpm、物件数
        if (modsMap.containsKey("HT")) {
            //如果官网的bpm比实际的高（EZ）
            g2.setPaint(Color.decode("#add8e6"));
        } else if (modsMap.containsKey("DT") || modsMap.containsKey("NC")) {
            //如果官网的bpm比实际的低（DT）
            g2.setPaint(Color.decode("#f69aa1"));
        } else {
            g2.setPaint(Color.decode("#FFFFFF"));
        }
        g2.setFont(new Font("微软雅黑", Font.BOLD, 23));
        //加入自动补0
        g2.drawString("长度：" + String.format("%02d", beatmap.getTotalLength() / 60) + ":"
                + String.format("%02d", beatmap.getTotalLength() % 60)
                + "  BPM：" + Math.round(beatmap.getBpm())
                + "  物件数：" + new DecimalFormat("###,###").format((oppaiResult.getNumCircles() + oppaiResult.getNumSliders() + oppaiResult.getNumSpinners())), 7, 80);


        //四围、难度
        if (modsMap.containsKey("EZ") || modsMap.containsKey("HT")) {
            //如果官网的AR比实际的高（EZ）
            g2.setPaint(Color.decode("#add8e6"));
        } else if (modsMap.containsKey("DT") || modsMap.containsKey("NC") || modsMap.containsKey("HR")) {
            //如果官网的AR比实际的低（DTHR）
            g2.setPaint(Color.decode("#f69aa1"));
        } else {
            g2.setPaint(Color.decode("#FFFFFF"));
        }

        g2.setFont(new Font("Aller", Font.PLAIN, 13));
        String arms = scoreUtil.calcMilliSecondForFourDimensions("AR", oppaiResult.getAr());
        String od300ms = scoreUtil.calcMilliSecondForFourDimensions("OD300", oppaiResult.getOd());
        String od100ms = scoreUtil.calcMilliSecondForFourDimensions("OD100", oppaiResult.getOd());
        String od50ms = scoreUtil.calcMilliSecondForFourDimensions("OD50", oppaiResult.getOd());
        g2.drawString("CS:" + (double) Math.round(oppaiResult.getCs() * 100) / 100
                + " AR:" + (double) Math.round(oppaiResult.getAr() * 100) / 100
                + "(" + arms + ")"
                + " OD:" + (double) Math.round(oppaiResult.getOd() * 100) / 100
                + "(" + od300ms + "/" + od100ms + "/" + od50ms + ")"
                + " HP:" + (double) Math.round(oppaiResult.getHp() * 100) / 100
                + " Stars:" + new DecimalFormat("###.00").format(Double.valueOf(oppaiResult.getStars())), 7, 125);


        //谱面的Rank状态
        g2.drawImage(images.get("fpRank" + beatmap.getApproved() + ".png"), 0, 0, null);
        //右侧title

        g2.setPaint(Color.decode("#000000"));
        if (unicode) {
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 31));
            g2.drawString(oppaiResult.getTitleUnicode(), 942, 196);
            g2.setFont(new Font("微软雅黑", Font.PLAIN, 22));
            g2.drawString(oppaiResult.getArtistUnicode() + " // " + oppaiResult.getCreator(), 942, 223);
        } else {
            g2.setFont(new Font("Aller light", Font.PLAIN, 31));
            g2.drawString(oppaiResult.getTitle(), 942, 196);
            g2.setFont(new Font("Aller", Font.PLAIN, 22));
            g2.drawString(oppaiResult.getArtist() + " // " + oppaiResult.getCreator(), 942, 223);
        }

        //artist//creator

        //难度名
        g2.setFont(new Font("Aller", Font.BOLD, 22));
        g2.drawString(oppaiResult.getVersion(), 942, 245);
        //小星星
        String[] b = String.valueOf(oppaiResult.getStars()).split("\\.");
        //取出难度的整数部分，画上对应的star
        for (int i = 0; i < Integer.valueOf(b[0]); i++) {
            g2.drawImage(images.get("fpStar.png"), 944 + 44 * i, 250, null);
        }

        //取出小数部分，缩放star并绘制在对应的地方
        float c = Integer.valueOf(b[1].substring(0, 1)) / 10F;
        //小于0.04的宽高会是0，
        if (c > 0.04) {
            g2.drawImage(images.get("fpStar.png").getScaledInstance((int) (25 * c), (int) (25 * c), Image.SCALE_SMOOTH),
                    944 + (Integer.valueOf(b[0])) * 44, (int) (250 + (1 - c) * 12.5), null);
        }
        //缩略图
        g2.drawImage(bg2, 762, 162, null);
        //PP面板
        g2.setPaint(Color.decode("#ff66a9"));
        g2.setFont(new Font("Gayatri", 0, 60));
        if (String.valueOf(Math.round(oppaiResult.getPp())).contains("1")) {
            g2.drawString(String.valueOf(Math.round(oppaiResult.getPp())), 55, 271);
        } else {
            g2.drawString(String.valueOf(Math.round(oppaiResult.getPp())), 40, 271);
        }
        g2.setFont(new Font("Gayatri", 0, 48));
        g2.drawString(String.valueOf(Math.round(oppaiResult.getAimPp())), 273, 276);
        g2.drawString(String.valueOf(Math.round(oppaiResult.getSpeedPp())), 371, 276);
        g2.drawString(String.valueOf(Math.round(oppaiResult.getAccPp())), 469, 276);
        return drawImage(bg, 不压缩);

    }

    /**
     * 简单的复制一份图片……
     */
    private BufferedImage getCopyImage(BufferedImage bi) {
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

    public String drawRadarImage(Map<String, Double> ppPlus, Userinfo userinfo) {
//                    + "\nJump：" + map.get("Jump")
//                    + "\nFlow：" + map.get("Flow")
//                    + "\nPrecision：" + map.get("Precision")
//                    + "\nSpeed：" + map.get("Speed")
//                    + "\nStamina：" + map.get("Stamina")
//                    + "\nAccuracy：" + map.get("Accuracy")
        Double jump = ppPlus.get("Jump");
        Double flow = ppPlus.get("Flow");
        Double prec = ppPlus.get("Precision");
        Double speed = ppPlus.get("Speed");
        Double stamina = ppPlus.get("Stamina");
        Double acc = ppPlus.get("Accuracy");
        //准备一份用于计算比例的数据
        double jump2 = jump;
        double flow2 = flow;
        double prec2 = prec;
        double speed2 = speed;
        double stamina2 = stamina;
        double acc2 = acc;
        BufferedImage bg = getCopyImage(images.get("costbg.png"));
        Graphics2D g2 = bg.createGraphics();
        double x = userinfo.getPpRaw() / 1000d;
        BigDecimal b = new BigDecimal(x);
        //得出PP/1000的两位小数
        x = b.setScale(2, RoundingMode.HALF_UP).doubleValue();
        //少于2kPP的按2K算
        if (x < 2) x = 2;
        //根据PP值计算平均值应该在的百分比
        double percent = (0.2785d * Math.log(x) + 0.0053d);

        //计算每个象限的平均值 最大值
        double jmpAva = (500d * x - 92d);
        double jmpMax = jmpAva / percent;
        //如果单项这个分段平均值超过2k，计算超出部分的值，然后用本人的能力值减去超出部分，平均值则按2k计算
        if (jmpAva > 2000) {
            double tmp = jmpAva - 2000;
            jump2 -= tmp;
            jmpMax = 2000/percent;
        }
        //控制不爆表
        if (jmpMax < jump) jmpMax = jump;

        double flowAva = (22.6d * Math.pow(x, 2) - 8.6d * x + 71.3d);
        double flowMax = flowAva / percent;
        if (flowAva > 2000) {
            double tmp = flowAva - 2000;
           flow2 -= tmp;
            flowMax = 2000/percent;
        }
        if (flowMax < flow) flowMax = flow;

        double precAva = (13.5d * Math.pow(x, 2) + 22.4d * x + 89.6d);
        double precMax = precAva / percent;
        if (precAva > 2000) {
            double tmp = precAva - 2000;
            prec2 -= tmp;
            precMax = 2000/percent;
        }
        if (precMax < prec) precMax = prec;

        double spdAva = (234d * x + 307d);
        double spdMax = spdAva / percent;
        if (spdAva > 2000) {
            double tmp = spdAva - 2000;
            speed2 -= tmp;
            spdMax = 2000/percent;
        }
        if (spdMax < speed) spdMax = speed;

        double staminaAva = (214.3 * x + 51.1d);
        double staminaMax = staminaAva / percent;
        if (staminaAva > 2000) {
            double tmp = staminaAva - 2000;
            stamina2 -= tmp;
            staminaMax = 2000/percent;
        }
        if (staminaMax < stamina) staminaMax = stamina;

        double accAva = (20.4d * Math.pow(x, 2) + 159.7d * x - 77.6d);
        double accMax = accAva / percent;
        if (accAva > 2000) {
            double tmp = accAva - 2000;
            acc2 -= tmp;
            accMax = 2000/percent;
        }
        if (accMax < acc) accMax = acc;

        int jumpX = (int) (152D - jump2 / jmpMax * 40D);
        int jumpY = (int) (131D - jump2 / jmpMax * 40D * Math.sqrt(3));
        int flowX = (int) (152D + flow2 / flowMax * 40D);
        int flowY = (int) (131D - flow2/ flowMax * 40D * Math.sqrt(3));
        int precisionX = (int) (152D - prec2 / precMax * 80D);
        int precisionY = 131;
        int speedX = (int) (152D - speed2 / spdMax * 40D);
        int speedY = (int) (131D + speed2 / spdMax * 40D * Math.sqrt(3));
        int staminaX = (int) (152D + stamina2 / staminaMax * 40D);
        int staminaY = (int) (131D + stamina2 / staminaMax * 40D * Math.sqrt(3));
        int accuracyX = (int) (152D + acc2 / accMax * 80D);
        int accuracyY = 131;
        //构造多边形
        Polygon p = new Polygon();
        p.addPoint(jumpX, jumpY);
        p.addPoint(precisionX, precisionY);
        p.addPoint(speedX, speedY);
        p.addPoint(staminaX, staminaY);
        p.addPoint(accuracyX, accuracyY);
        p.addPoint(flowX, flowY);
        //开启平滑、端点圆形
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        Stroke s = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
        g2.setStroke(s);
        //多边形边框颜色
        g2.setColor(new Color(253, 148, 62));
        g2.drawPolygon(p);
        //端点
        g2.fillOval(jumpX - 3, jumpY - 3, 6, 6);
        g2.fillOval(precisionX - 3, precisionY - 3, 6, 6);
        g2.fillOval(speedX - 3, speedY - 3, 6, 6);
        g2.fillOval(staminaX - 3, staminaY - 3, 6, 6);
        g2.fillOval(accuracyX - 3, accuracyY - 3, 6, 6);
        g2.fillOval(flowX - 3, flowY - 3, 6, 6);
        //填充六边形

        g2.setColor(new Color(253, 148, 62, 100));
        g2.fill(p);
        //具体值
        g2.setColor(new Color(10, 13, 16, 200));
        g2.fillRoundRect(jumpX - 45, jumpY, 36, 12, 4, 4);
        g2.fillRoundRect(precisionX - 45, precisionY, 36, 12, 4, 4);
        g2.fillRoundRect(speedX - 45, speedY, 36, 12, 4, 4);
        g2.fillRoundRect(staminaX + 10, staminaY, 36, 12, 4, 4);
        g2.fillRoundRect(accuracyX + 10, accuracyY, 36, 12, 4, 4);
        g2.fillRoundRect(flowX + 10, flowY, 36, 12, 4, 4);


        drawTextToImage(g2, "#f9f9f9", "Aller", 12, flow.toString(), flowX + 13, flowY + 10);
        drawTextToImage(g2, "#f9f9f9", "Aller", 12, stamina.toString(), staminaX + 13, staminaY + 10);
        drawTextToImage(g2, "#f9f9f9", "Aller", 12, acc.toString(), accuracyX + 13, accuracyY + 10);
        drawTextToImage(g2, "#f9f9f9", "Aller", 12, speed.toString(), speedX - 42, speedY + 10);
        drawTextToImage(g2, "#f9f9f9", "Aller", 12, prec.toString(), precisionX - 42, precisionY + 10);
        drawTextToImage(g2, "#f9f9f9", "Aller", 12, jump.toString(), jumpX - 42, jumpY + 10);
        drawTextToImage(g2, "#f9f9f9", "Aller", 12, userinfo.getUserName(), 182, 245);

        return drawImage(bg, 不压缩);

    }

    /**
     * 向图片上绘制字符串的方法……当时抽出来复用，但是方法名没取好
     * 2018-1-24 17:05:11去除配置文件的设定，反正以后要改也不可能去除旧命令。
     */
    private void drawTextToImage(Graphics2D g2, String color, String font,
                                 Integer size, String text, Integer x, Integer y) {
        //指定颜色
        g2.setPaint(Color.decode(color));
        //指定字体
        g2.setFont(new Font(font, Font.PLAIN, size));
        //指定坐标
        g2.drawString(text, x, y);

    }

    /**
     * 将图片转换为Base64字符串……
     *
     * @param img the img
     * @return the string
     */
    public String drawImage(BufferedImage img, CompressLevelEnum level) {
        BufferedImage result = img;

        switch (level) {
            case 不压缩:
                //什么也不做
                break;
            case JPG:
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    ImageIO.write(result, "jpg", out);
                    byte[] imgBytes = out.toByteArray();
                    return Base64.getEncoder().encodeToString(imgBytes);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                    return null;
                }
            case USHORT_555_RGB_PNG:
                result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_USHORT_555_RGB);
                Graphics2D g3 = result.createGraphics();
                g3.clearRect(0, 0, img.getWidth(), img.getHeight());
                g3.drawImage(img.getScaledInstance(img.getWidth(), img.getHeight(), Image.SCALE_SMOOTH), 0, 0, null);
                g3.dispose();
                break;
            default:
                return null;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(result, "png", out);
            byte[] imgBytes = out.toByteArray();
            return Base64.getEncoder().encodeToString(imgBytes);
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    private String unicodeToString(String str) {
        Matcher matcher = RegularPattern.UNICODE_TO_STRING.matcher(str);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            str = str.replace(matcher.group(1), ch + "");
        }
        return str;
    }


}
