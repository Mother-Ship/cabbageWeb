package top.mothership.cabbage.util.osu;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.OppaiResult;
import top.mothership.cabbage.pojo.osu.Score;

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
     * Convert mod linked hash map.
     *
     * @param bp the bp
     * @return the linked hash map
     */
    public LinkedHashMap<String, String> convertMOD(Integer bp) {
        String modBin = Integer.toBinaryString(bp);
        //反转mod
        modBin = new StringBuffer(modBin).reverse().toString();
        LinkedHashMap<String, String> mods = new LinkedHashMap<>();
        char[] c = modBin.toCharArray();
        if (bp != 0) {
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
                        case 8:
                            mods.put("HT", "halftime");
                            break;
                        case 9:
                            mods.put("NC", "nightcore");
                            break;
                        case 10:
                            mods.put("FL", "flashlight");
                            break;
                        case 12:
                            mods.put("SO", "spunout");
                            break;
                        case 14:
                            mods.put("PF", "perfect");
                            break;
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

    /**
     * 先有蔓蔓后有天，反向转换日神仙
     *
     * @param mods the mods
     * @return the integer
     */
    public Integer reverseConvertMod(String mods) {
        Integer m = 0;
        if(mods.length()%2!=0){
            //双字母MOD字符串长度必然是偶数
            return null;
        }
        int j=0;
        String[] modList = new String[mods.length()/2];
        //例如 HDHR
        for(int i=0;i<mods.length();i++){
            if(i%2==0){
                //先取H
                modList[j]=""+mods.charAt(i);
            }else{
                //取出D，取出H，拼一起放回去
                modList[j]=modList[j]+mods.charAt(i);
                j++;
            }
        }
        for (String s : modList) {
            switch (s.toUpperCase(Locale.CHINA)) {
                case "NF":
                    m += 1;
                    break;
                case "EZ":
                    m += 2;
                    break;
                case "HD":
                    m += 8;
                    break;
                case "HR":
                    m += 16;
                    break;
                case "SD":
                    m += 32;
                    break;
                case "DT":
                    m += 64;
                    break;
                case "HT":
                    m += 256;
                    break;
                case "NC":
                    //NCDT
                    m += 576;
                    break;
                case "FL":
                    m += 1024;
                    break;
                case "SO":
                    m += 4096;
                    break;
                case "PF":
                    m += 16384;
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
    public String genScoreString(Score score, Beatmap beatmap, String username) {
        OppaiResult oppaiResult = calcPP(score, beatmap);
        String resp = "https://osu.ppy.sh/b/" + beatmap.getBeatmapId() + "\n"
                + "http://bloodcat.com/osu/s/" + beatmap.getBeatmapSetId() + "\n"
                + beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]\n"
                + score.getMaxCombo() + "x/" + beatmap.getMaxCombo() + "x，" + score.getCountMiss() + "*miss , "
                + convertMOD(score.getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "")
                + " (" + new DecimalFormat("###.00").format(
                100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50())
                        / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()))) + "%)";
        if (oppaiResult != null) {
            resp += "，" + String.valueOf(Math.round(oppaiResult.getPp())) + "PP\n";
        }
        resp += "Played by " + username + ", " + DateTimeFormatter.ofPattern("yy/MM/dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(score.getDate().toInstant()) + ", ";
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
        try (BufferedReader in = new BufferedReader(new StringReader(osuFile))) {
            //日后再说，暂时不去按自己的想法改造它……万一作者日后放出更新呢..
            //把这种充满静态内部类，PPv2Params没有有参构造、成员变量给包访问权限、没有get/set的危险东西局限在这个方法里，不要在外面用就是了……%
            Koohii.Map map = new Koohii.Parser().map(in);
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
            Koohii.PPv2 pp = new Koohii.PPv2(p);
            return new OppaiResult(Koohii.VERSION_MAJOR + "." + Koohii.VERSION_MINOR + "." + Koohii.VERSION_PATCH,
                    //Java实现如果出错会抛出异常，象征性给个0和null
                    0, null, map.artist, map.artist_unicode, map.title, map.title_unicode, map.creator, map.version, Koohii.mods_str(score.getEnabledMods()), score.getEnabledMods(),
                    //这里score的虽然叫MaxCombo，但实际上是这个分数的combo
                    map.od, map.ar, map.cs, map.hp, score.getMaxCombo(), map.max_combo(), map.ncircles, map.nsliders, map.nspinners, score.getCountMiss(),
                    //scoreVersion只能是V1了，
                    1, stars.total, stars.speed, stars.aim, stars.nsingles, stars.nsingles_threshold, pp.aim, pp.speed, pp.acc, pp.total);
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
}
