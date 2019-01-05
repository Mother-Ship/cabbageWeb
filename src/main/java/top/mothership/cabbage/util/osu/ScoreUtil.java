package top.mothership.cabbage.util.osu;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.pojo.coolq.osu.Beatmap;
import top.mothership.cabbage.pojo.coolq.osu.OppaiResult;
import top.mothership.cabbage.pojo.coolq.osu.Score;

import java.io.BufferedReader;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * The type Score util.
 *
 * @author QHS
 */
@Component
public class ScoreUtil {
    private Logger logger = LogManager.getLogger(this.getClass());
    private WebPageManager webPageManager;

    /**
     * Instantiates a new Score util.
     *
     * @param webPageManager the web page manager
     */
    @Autowired
    public ScoreUtil(WebPageManager webPageManager) {
        this.webPageManager = webPageManager;
    }


    /**
     * 将Mod数字转换为字符串，用于ImgUtil类绘制Mod图标，所以不会包含Unrank Mod。
     *
     * @param mod 表示mod的数字
     * @return 带顺序的LinkedHashMap，用于存储Mod字符串（Key是简称，Value是全称(对应皮肤文件)）
     */
    public LinkedHashMap<String, String> convertModToHashMap(Integer mod) {
        String modBin = Integer.toBinaryString(mod);
        //反转mod
        modBin = new StringBuffer(modBin).reverse().toString();
        LinkedHashMap<String, String> mods = new LinkedHashMap<>();
        char[] c = modBin.toCharArray();
        if (mod != 0) {
            for (int i = c.length - 1; i >= 0; i--) {
                //字符串中第i个字符是1,意味着第i+1个mod被开启了
                if (c[i] == '1') {
                    switch (i) {
                        case 0:
                            mods.put("NF", "nofail");
                            break;
                        case 1:
                            mods.put("EZ", "easy");
                            break;
                        //虽然TD已经实装，但是MOD图标还是 不做 不画
                        case 3:
                            mods.put("HD", "hidden");
                            break;
                        case 4:
                            mods.put("HR", "hardrock");
                            break;
                        case 5:
                            mods.put("SD", "suddendeath");
                            break;
                        case 6:
                            mods.put("DT", "doubletime");
                            break;
                        //7是RX，不会上传成绩
                        case 8:
                            mods.put("HT", "halftime");
                            break;
                        case 9:
                            mods.put("NC", "nightcore");
                            break;
                        case 10:
                            mods.put("FL", "flashlight");
                            break;
                        //11是Auto
                        case 12:
                            mods.put("SO", "spunout");
                            break;
                        //13是AutoPilot
                        case 14:
                            mods.put("PF", "perfect");
                            break;
                        case 15:
                            mods.put("4K", "key4");
                            break;
                        case 16:
                            mods.put("5K", "key5");
                            break;
                        case 17:
                            mods.put("6K", "key6");
                            break;
                        case 18:
                            mods.put("7K", "key7");
                            break;
                        case 19:
                            mods.put("8K", "key8");
                            break;
                        case 20:
                            mods.put("FI", "fadein");
                            break;
                        //21是RD，Mania的Note重新排布
                        //22是Cinema，但是不知道为什么有一个叫LastMod的名字
                        //23是Target Practice
                        case 24:
                            mods.put("9K", "key9");
                            break;
                        //25是Mania的双人合作模式，Unrank
                        //Using 1K, 2K, or 3K mod will result in an unranked play.
                        //The mod does not work on osu!mania-specific beatmaps.
                        //26 1K，27 3K，28 2K

                        default:
                            break;
                    }
                }
            }
            if (mods.keySet().contains("NC")) {
                mods.remove("DT");
            }
            if (mods.keySet().contains("PF")) {
                mods.remove("SD");
            }
        } else {
            mods.put("None", "None");
        }
        return mods;
    }

    public String convertModToString(Integer mod) {
        return convertModToHashMap(mod).keySet().toString().replaceAll("\\[\\]", "");
    }

    public String genAccString(Score score, Integer mode) {
        return new DecimalFormat("###.00").format(genAccDouble(score, mode));
    }

    public Double genAccDouble(Score score, Integer mode) {
        switch (mode) {
            case 0:
                return 100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50())
                        / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()));
            case 1:
                //太鼓
                return 100.0 * (2 * score.getCount300() + score.getCount100())
                        / (2 * (score.getCount100() + score.getCount300() + score.getCountMiss()));
            case 2:
                //ctb
                return 100.0 * (score.getCount50() + score.getCount100() + score.getCount300())
                        / (score.getCountKatu() + score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss());
            case 3:
                //mania
                return 100.0 * (300 * (score.getCount300() + score.getCountGeki()) + 200 * score.getCountKatu() + 100 * score.getCount100() + 50 * score.getCount50())
                        / (300 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss() + score.getCountKatu() + score.getCountGeki()));
            default:
                return 0D;
        }

    }

    /**
     * 先有蔓蔓后有天，反向转换日神仙
     * 用于处理Search命令传入的Mod，所以应该不必支持STD以外的模式……
     * 其实并不完善，对所有的偶数长度 不包含mod的字符串都会返回0
     * 2018-3-8 11:43:38已修复：现在没有匹配的mod会返回null，同时也能正确的识别none了
     * 2018-3-19 12:27:46默认-1触发了bug，已修正
     *
     * @param mods the mods
     * @return the integer
     */
    public Integer reverseConvertMod(String mods) {
        Integer m = null;
        if (mods.length() % 2 != 0) {
            //双字母MOD字符串长度必然是偶数
            return null;
        }
        if (mods.toLowerCase(Locale.CHINA).equals("none")) {
            return 0;
        }
        int j = 0;
        String[] modList = new String[mods.length() / 2];
        //例如 HDHR
        for (int i = 0; i < mods.length(); i++) {
            if (i % 2 == 0) {
                //先取H
                modList[j] = "" + mods.charAt(i);
            } else {
                //取出D，取出H，拼一起放回去
                modList[j] = modList[j] + mods.charAt(i);
                j++;
            }
        }
        for (String s : modList) {
            switch (s.toUpperCase(Locale.CHINA)) {
                case "NF":
                    if (m == null) {
                        m = 0;
                    }
                    m += 1;
                    break;
                case "EZ":
                    if (m == null) {
                        m = 0;
                    }
                    m += 2;
                    break;
                case "TD":
                    if (m == null) {
                        m = 0;
                    }
                    //但是在字符串的输入处还是更新一下 支持一下吧
                    m += 4;
                    break;
                case "HD":
                    if (m == null) {
                        m = 0;
                    }
                    m += 8;
                    break;
                case "HR":
                    if (m == null) {
                        m = 0;
                    }
                    m += 16;
                    break;
                case "SD":
                    if (m == null) {
                        m = 0;
                    }
                    m += 32;
                    break;
                case "DT":
                    if (m == null) {
                        m = 0;
                    }
                    m += 64;
                    break;
                case "HT":
                    if (m == null) {
                        m = 0;
                    }
                    m += 256;
                    break;
                case "NC":
                    if (m == null) {
                        m = 0;
                    }
                    //NCDT
                    m += 576;
                    break;
                case "FL":
                    if (m == null) {
                        m = 0;
                    }
                    m += 1024;
                    break;
                case "SO":
                    if (m == null) {
                        m = 0;
                    }
                    m += 4096;
                    break;
                case "PF":
                    if (m == null) {
                        m = 0;
                    }
                    m += 16384;
                    break;
                case "4K":
                    if (m == null) {
                        m = 0;
                    }
                    m += 32768;
                    break;
                case "5K":
                    if (m == null) {
                        m = 0;
                    }
                    m += 65536;
                    break;
                case "6K":
                    if (m == null) {
                        m = 0;
                    }
                    m += 131072;
                    break;
                case "7K":
                    if (m == null) {
                        m = 0;
                    }
                    m += 262144;
                    break;
                case "8K":
                    if (m == null) {
                        m = 0;
                    }
                    m += 524288;
                    break;
                case "FI":
                    if (m == null) {
                        m = 0;
                    }
                    m += 1048576;
                    break;
                case "9K":
                    if (m == null) {
                        m = 0;
                    }
                    m += 16777216;
                    break;
                default:
                    break;
            }
        }
        return m;
    }

    /**
     * Gen score string string.
     *
     * @param score    the score
     * @param beatmap  the beatmap
     * @param username the username
     * @return the string
     */
    public String genScoreString(Score score, Beatmap beatmap, String username,Integer count) {
        OppaiResult oppaiResult = calcPP(score, beatmap);
        String resp = "官网链接：https://osu.ppy.sh/b/" + beatmap.getBeatmapId() + "\n"
                + "血猫链接：（不保证有效）http://bloodcat.com/osu/s/" + beatmap.getBeatmapSetId() + "\n"
                + "inso链接：http://inso.link/yukiho/?m=" + beatmap.getBeatmapSetId() + "\n"
                + "模式：" + convertGameModeToString(beatmap.getMode()) + "\n"
                + beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]\n"
                + score.getMaxCombo() + "x/" + beatmap.getMaxCombo() + "x，" + score.getCountMiss() + "*miss , "
                + convertModToString(score.getEnabledMods())
                + " (" + new DecimalFormat("###.00").format(
                100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50())
                        / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()))) + "%)";
        if (oppaiResult != null) {
            resp += "，" + String.valueOf(Math.round(oppaiResult.getPp())) + "PP";

        }
        if(count !=null){
            resp+="\n在该玩家24小时内游戏记录中，该谱面出现了"+count+"次。";
        }
        //由于比较分数等涉及到其他时区问题，懒得重构
        //将成绩的时间使用小技巧显示成UTC+8
        resp += "\nPlayed by " + username + ", " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC-8")).format(score.getDate().toInstant());
        return resp;
    }


    /**
     * Calc pp oppai result.
     *
     * @param score   the score
     * @param beatmap the beatmap
     * @return the oppai result
     */
    public OppaiResult calcPP(Score score, Beatmap beatmap) {
        logger.info("开始计算PP");
        String osuFile = webPageManager.getOsuFile(beatmap);
        try (BufferedReader in = new BufferedReader(new StringReader(osuFile));
             //似乎oppai会自动关闭流？用同一个流会出现第二个PP计算为0的情况
        BufferedReader in2 = new BufferedReader(new StringReader(osuFile))) {
            //日后再说，暂时不去按自己的想法改造它……万一作者日后放出更新呢..
            //把这种充满静态内部类，PPv2Params没有有参构造、成员变量给包访问权限、没有get/set的危险东西局限在这个方法里，不要在外面用就是了……%
            Koohii.Map map = new Koohii.Parser().map(in);
            map.mode = beatmap.getMode();
            Koohii.DiffCalc stars = new Koohii.DiffCalc().calc(map, score.getEnabledMods());
            Koohii.PPv2Parameters p = new Koohii.PPv2Parameters();
            p.beatmap = map;
            p.aim_stars = stars.aim;
            p.speed_stars = stars.speed;
            p.mods = score.getEnabledMods();
            p.n300 = score.getCount300();
            p.n100 = score.getCount100();
            p.n50 = score.getCount50();
            p.nmiss = score.getCountMiss();
            p.combo = score.getMaxCombo();
            Koohii.MapStats mapstats = new Koohii.MapStats();
            mapstats.ar = beatmap.getDiffApproach();
            mapstats.cs = beatmap.getDiffSize();
            mapstats.od = beatmap.getDiffOverall();
            mapstats.hp = beatmap.getDiffDrain();
            //APPLY_AR OD CS HP是1 2 4 8，把flag改成15好像就会 四维都进行计算？
            mapstats = Koohii.mods_apply(score.getEnabledMods(), mapstats, 15);


            KoohiiLegacy.Map map2 = new KoohiiLegacy.Parser().map(in2);
            map2.mode = beatmap.getMode();
            KoohiiLegacy.DiffCalc stars2 = new KoohiiLegacy.DiffCalc().calc(map2, score.getEnabledMods());
            KoohiiLegacy.PPv2Parameters p2 = new KoohiiLegacy.PPv2Parameters();
            p2.beatmap = map2;
            p2.aim_stars = stars2.aim;
            p2.speed_stars = stars2.speed;
            p2.mods = score.getEnabledMods();
            p2.n300 = score.getCount300();
            p2.n100 = score.getCount100();
            p2.n50 = score.getCount50();
            p2.nmiss = score.getCountMiss();
            p2.combo = score.getMaxCombo();



            KoohiiLegacy.MapStats mapstats2 = new KoohiiLegacy.MapStats();
            mapstats2.ar = beatmap.getDiffApproach();
            mapstats2.cs = beatmap.getDiffSize();
            mapstats2.od = beatmap.getDiffOverall();
            mapstats2.hp = beatmap.getDiffDrain();
            mapstats2 = KoohiiLegacy.mods_apply(score.getEnabledMods(), mapstats2, 15);
            if (map.mode == 0) {
                Koohii.PPv2 pp = new Koohii.PPv2(p);
                KoohiiLegacy.PPv2 pp2 = new KoohiiLegacy.PPv2(p2);
                return new OppaiResult(Koohii.VERSION_MAJOR + "." + Koohii.VERSION_MINOR + "." + Koohii.VERSION_PATCH,
                        //Java实现如果出错会抛出异常，象征性给个0和null
                        0, null, map.artist, map.artist_unicode, map.title, map.title_unicode, map.creator, map.version, Koohii.mods_str(score.getEnabledMods()), score.getEnabledMods(),
                        //这里score的虽然叫MaxCombo，但实际上是这个分数的combo
                        mapstats.od, mapstats.ar, mapstats.cs, mapstats.hp, score.getMaxCombo(), map.max_combo(), map.ncircles, map.nsliders, map.nspinners, score.getCountMiss(),
                        //scoreVersion只能是V1了，
                        1, stars.total, stars.speed, stars.aim, stars.nsingles, stars.nsingles_threshold,
                        pp.aim, pp.speed, pp.acc, pp.total, pp2.total,mapstats.speed);

            } else {
                return new OppaiResult(Koohii.VERSION_MAJOR + "." + Koohii.VERSION_MINOR + "." + Koohii.VERSION_PATCH,
                        //Java实现如果出错会抛出异常，象征性给个0和null
                        0, null, map.artist, map.artist_unicode, map.title, map.title_unicode, map.creator, map.version, Koohii.mods_str(score.getEnabledMods()), score.getEnabledMods(),
                        //这里score的虽然叫MaxCombo，但实际上是这个分数的combo
                        mapstats.od, mapstats.ar, mapstats.cs, mapstats.hp, score.getMaxCombo(), map.max_combo(), map.ncircles, map.nsliders, map.nspinners, score.getCountMiss(),
                        //scoreVersion只能是V1了，非STD的计算应该是没有PP的，给四个0
                        1, stars.total, stars.speed, stars.aim, stars.nsingles, stars.nsingles_threshold, 0, 0,0, 0, 0, mapstats.speed);

            }
        } catch (Exception e) {
            logger.error("离线计算PP出错");
            logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * Convert score v 1 to v 2 integer.
     *
     * @param score   the score
     * @param beatmap the beatmap
     * @return the integer
     */
    public Integer convertScoreV1ToV2(Score score, Beatmap beatmap) {
        return 0;
    }

    public Integer calcNoneXScore(Beatmap beatmap) {
        return 0;
    }

    public String calcMilliSecondForFourDimensions(String dimensions, Double value) {
        switch (dimensions) {
            case "AR":
                if (value > 5.0D) {
                    return new DecimalFormat("##0.0").format(1200D - (value - 5.0D) * 150D) + "ms";
                } else {
                    return new DecimalFormat("##0.0").format(1200D - (value - 5.0D) * 120D) + "ms";
                }
            case "OD300":
                return "300:" + new DecimalFormat("##0.0").format(79.5D - 6D * value) + "ms";
            case "OD100":
                return "100:" + new DecimalFormat("##0.0").format(139.5D - 8D * value) + "ms";
            case "OD50":
                return "50:" + new DecimalFormat("##0.0").format(199.5D - 10D * value) + "ms";
            default:
                return null;
        }

    }

    public String convertGameModeToString(Integer mode) {
        switch (mode) {
            case 0:
                return "Standard";
            case 1:
                return "Taiko";
            case 2:
                return "Catch The Beat";
            case 3:
                return "osu!Mania";
            default:
                return null;

        }
    }
}
