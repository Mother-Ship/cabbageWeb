package top.mothership.cabbage.util.osu;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.pojo.osu.*;
import top.mothership.cabbage.pojo.osu.apiv2.response.ApiV2Score;

import java.io.BufferedReader;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.stream.Collectors;

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
     * 将 V2 Score (Lazer Score) 转换为 V1 Score
     */
    public Score convertV2ToV1(ApiV2Score.ScoreLazer scoreLazer) {
        if (scoreLazer == null) {
            return null;
        }

        Score scoreV1 = new Score();
        scoreV1.setBeatmapId((int) scoreLazer.getBeatmapId());
        scoreV1.setOnlineId(scoreLazer.getLegacyScoreId() != null ?
                scoreLazer.getLegacyScoreId() : scoreLazer.getId());
        scoreV1.setScore(scoreLazer.getLegacyTotalScore());
        scoreV1.setMaxCombo((int) scoreLazer.getMaxCombo());

        // 从统计信息中提取计数
        ApiV2Score.ScoreStatisticsLazer stats = scoreLazer.getStatistics();
        if (stats != null) {
            scoreV1.setCount50((int) stats.getCountMeh());
            scoreV1.setCount100((int) stats.getCountOk());
            scoreV1.setCount300((int) stats.getCountGreat());
            scoreV1.setCountMiss((int) stats.getCountMiss());
            scoreV1.setCountKatu((int) stats.getCountKatu());
            scoreV1.setCountGeki((int) stats.getCountGeki());
        }

        scoreV1.setPerfect(scoreLazer.isLegacyPerfect() ? 1 : 0);

        scoreV1.setEnabledMods(reverseConvertMod(
                Arrays.stream(scoreLazer.getMods())
                        .map(ApiV2Score.Mod::getAcronym)
                        .collect(Collectors.joining(",")), true));

        scoreV1.setUserId((int) scoreLazer.getUserId());

        // 把 beatmap的yyyy-MM-dd'T'HH:mm:ssX格式ended_at字符串
        // 转换成UTC时间的yyyy-MM-dd HH:mm:ss字符串
        scoreV1.setDate(convertToUtcString(scoreLazer.getEndedAt()));
        scoreV1.setRank(scoreLazer.getRank());
        scoreV1.setPp(scoreLazer.getPp() != null ? scoreLazer.getPp().floatValue() : null);


        return scoreV1;
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
     * 2025-8-6 支持lazer的 mod
     *
     * @param mods the mods
     * @return the integer
     */
    public Integer reverseConvertMod(String mods, boolean hasComma) {
        String[] modList;

        Integer m = null;

        if (hasComma) {
            modList = mods.split(",");
        } else {

            if (mods.length() % 2 != 0) {
                //双字母MOD字符串长度必然是偶数
                return null;
            }
            if (mods.toLowerCase(Locale.CHINA).equals("none")) {
                return 0;
            }
            int j = 0;
            modList = new String[mods.length() / 2];
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
        }

        for (String s : modList) {
            switch (s.toUpperCase(Locale.CHINA)) {
                case "CL":
                    if (m == null) {
                        m = 0;
                    }
                    break;
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
     * 将yyyy-MM-dd'T'HH:mm:ssX格式的时间字符串转换为UTC时间的yyyy-MM-dd HH:mm:ss格式
     *
     * @param endedAtStr 输入的时间字符串，格式：yyyy-MM-dd'T'HH:mm:ssX
     * @return UTC时间的字符串，格式：yyyy-MM-dd HH:mm:ss
     */
    public String convertToUtcString(String endedAtStr) {
        // 定义输入格式的解析器
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

        // 定义输出格式的格式化器
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 解析输入字符串为OffsetDateTime
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(endedAtStr, inputFormatter);

        // 转换为UTC时间
        OffsetDateTime utcDateTime = offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC);

        // 格式化为目标字符串格式
        return utcDateTime.format(outputFormatter);
    }
    /**
     * 返回输入的UTC时间对应的Instant
     */
    public Instant toInstant(String utcTimeStr) {
        // 定义UTC时间字符串的格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 解析UTC时间字符串为LocalDateTime
        LocalDateTime utcLocalDateTime = LocalDateTime.parse(utcTimeStr, formatter);

        // 将LocalDateTime转换为UTC的Instant
        Instant utcInstant = utcLocalDateTime.atZone(ZoneOffset.UTC).toInstant();

        return utcInstant;
    }
    /**
     * Gen score string string.
     *
     * @param score    the score
     * @param beatmap  the beatmap
     * @param username the username
     * @return the string
     */
    public String genScoreString(Score score, Beatmap beatmap, String username, Integer count) {
        OppaiResult oppaiResult = calcPP(score, beatmap);
        String resp = "官网链接：https://osu.ppy.sh/b/" + beatmap.getBeatmapId() + "\n"
                + "模式：" + convertGameModeToString(beatmap.getMode()) + "\n"
                + beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]\n"
                + score.getMaxCombo() + "x/" + beatmap.getMaxCombo() + "x，" + score.getCountMiss() + "*miss , "
                + convertModToString(score.getEnabledMods())
                + " (" + new DecimalFormat("###.00").format(
                100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50())
                        / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()))) + "%)";
        if (oppaiResult != null) {
            resp += "，" + Math.round(oppaiResult.getPp()) + "PP";
        }
        if (count != null) {
            resp += "\n在该玩家24小时内游戏记录中，该谱面出现了" + count + "次。";
        }
        //由于比较分数等涉及到其他时区问题，懒得重构
        //将成绩的时间使用小技巧显示成UTC+8
        resp += "\nPlayed by " + username + ", " +
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                        .withZone(ZoneId.of("UTC+8"))
                        .format(toInstant(score.getDate()));
        return resp;
    }

    /**
     * Calc pp oppai result.
     */
    public OppaiResult calcPP(Score score, Beatmap beatmap) {
        logger.info("开始计算PP");
        String osuFile = webPageManager.getOsuFile(beatmap);
        try (BufferedReader in = new BufferedReader(new StringReader(osuFile));
             //似乎oppai会自动关闭流？用同一个流会出现第二个PP计算为0的情况
             BufferedReader in2 = new BufferedReader(new StringReader(osuFile))) {
            Koohii.Map map = new Koohii.Parser().map(in);
            map.mode = beatmap.getMode();
            Koohii.DiffCalc stars = new Koohii.DiffCalc().calc(map, score.getEnabledMods());

            KoohiiLegacy.Map mapLegacy = new KoohiiLegacy.Parser().map(in2);
            KoohiiLegacy.DiffCalc starsLegacy = new KoohiiLegacy.DiffCalc().calc(mapLegacy, score.getEnabledMods());

            Koohii.MapStats mapstats = new Koohii.MapStats();
            mapstats.ar = beatmap.getDiffApproach();
            mapstats.cs = beatmap.getDiffSize();
            mapstats.od = beatmap.getDiffOverall();
            mapstats.hp = beatmap.getDiffDrain();
            mapstats = Koohii.mods_apply(score.getEnabledMods(), mapstats, 15);



            OppaiResult result = new OppaiResult(Koohii.VERSION_MAJOR + "." + Koohii.VERSION_MINOR + "." + Koohii.VERSION_PATCH,
                    //Java实现如果出错会抛出异常，象征性给个0和null
                    0, null, map.artist, map.artist_unicode, map.title, map.title_unicode, map.creator, map.version, Koohii.mods_str(score.getEnabledMods()), score.getEnabledMods(),
                    //这里score的虽然叫MaxCombo，但实际上是这个分数的combo
                    mapstats.od, mapstats.ar, mapstats.cs, mapstats.hp, score.getMaxCombo(), map.max_combo(), map.ncircles, map.nsliders, map.nspinners, score.getCountMiss(),
                    //scoreVersion只能是V1了，
                    1, stars.total, starsLegacy.total, stars.speed, stars.aim, stars.nsingles, stars.nsingles_threshold,
                    0, 0, 0, 0, 0, mapstats.speed, 0);

            CalculateByBidRequest request = new CalculateByBidRequest();
            request.setBid(beatmap.getBeatmapId());
            request.setRefresh(!Integer.valueOf(1).equals(beatmap.getApproved()));

            UserScore userScore = new UserScore();
            userScore.setCombo(score.getMaxCombo());
            userScore.setCount50(score.getCount50());
            userScore.setCount100(score.getCount100());
            userScore.setCount300(score.getCount300());
            userScore.setCountMiss(score.getCountMiss());
            userScore.setMode(map.mode);
            userScore.setMods(score.getEnabledMods());
            request.setUserScore(userScore);

            CalcResult remoteResult = getCalcResult(request);
            if (remoteResult != null && remoteResult.getScoreResult() != null) {
                result.setAimPp(remoteResult.getScoreResult().getAim());
                result.setSpeedPp(remoteResult.getScoreResult().getSpeed());
                result.setPp(remoteResult.getScoreResult().getPp());
                result.setAccPp(remoteResult.getScoreResult().getAcc());
            }

            //计算FC PP
            int objects = map.ncircles + map.nsliders + map.nspinners;
            userScore.setCombo(map.max_combo());
            userScore.setCount50(score.getCount50());
            userScore.setCount100(score.getCount100());
            userScore.setCount300(objects - score.getCount50() - score.getCount100());
            userScore.setCountMiss(0);
            userScore.setMode(map.mode);
            userScore.setMods(score.getEnabledMods());
            request.setUserScore(userScore);
            remoteResult = getCalcResult(request);
            if (remoteResult != null && remoteResult.getScoreResult() != null) {
                result.setMaxPP(remoteResult.getScoreResult().getPp());
            }

            return result;
        } catch (Exception e) {
            logger.error("离线计算PP出错");
            logger.error(e);
            return null;
        }
    }

    private CalcResult getCalcResult(CalculateByBidRequest request) {
        try {
            return new RestTemplate().postForObject("http://k3.mothership.top:5000/api/PPCalc/CalculateByBeatmapId", request, CalcResult.class);
        } catch (Exception e) {
            return null;
        }
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

    public String convertGameModeToV2String(Integer mode) {
        switch (mode) {
            case 0:
                return "osu";
            case 1:
                return "taiko";
            case 2:
                return "fruits";
            case 3:
                return "mania";
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
