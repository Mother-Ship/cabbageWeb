package top.mothership.cabbage.util.osu;


import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.OppaiResult;
import top.mothership.cabbage.pojo.osu.Score;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;

@Component
public class ScoreUtil {
    private static ResourceBundle rb = ResourceBundle.getBundle("cabbage");
    private Logger logger = LogManager.getLogger(this.getClass());
    private WebPageUtil webPageUtil;

    @Autowired
    public ScoreUtil(WebPageUtil webPageUtil) {
        this.webPageUtil = webPageUtil;
    }


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

    public OppaiResult calcPP(Score score, Beatmap beatmap) {
        logger.info("开始计算PP");
        String osuFile = webPageUtil.getOsuFile(beatmap);
        try (BufferedReader in = new BufferedReader(new StringReader(osuFile))) {
            //日后再说，暂时不去按自己的想法改造它……万一作者日后放出更新呢..
            //把这种充满静态内部类，PPv2Params没有有参构造、成员变量给包访问权限、没有get/set的危险东西局限在这个方法里，不要在外面用就是了……%
            Koohii.Map map = new Koohii.Parser().map(in);
            Koohii.DiffCalc stars = new Koohii.DiffCalc().calc(map);
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

            //            //单项PP大于1000直接取个位数……这个可能不需要了
//            if (Math.round(oppaiResult.getAimPp()) > 1000) {
//                oppaiResult.setAimPp(Math.round(oppaiResult.getAimPp()) % 10);
//                oppaiResult.setPp(oppaiResult.getAimPp() + oppaiResult.getAccPp() + oppaiResult.getSpeedPp());
//            }
//            if (Math.round(oppaiResult.getAccPp()) > 1000) {
//                oppaiResult.setAccPp(Math.round(oppaiResult.getAccPp()) % 10);
//                oppaiResult.setPp(oppaiResult.getAimPp() + oppaiResult.getAccPp() + oppaiResult.getSpeedPp());
//            }
//            if (Math.round(oppaiResult.getSpeedPp()) > 1000) {
//                oppaiResult.setSpeedPp(Math.round(oppaiResult.getSpeedPp()) % 10);
//                oppaiResult.setPp(oppaiResult.getAimPp() + oppaiResult.getAccPp() + oppaiResult.getSpeedPp());
//            }

            return new OppaiResult(Koohii.VERSION_MAJOR + "." + Koohii.VERSION_MINOR + "." + Koohii.VERSION_PATCH,
                    //Java实现如果出错会抛出异常，象征性给个0和null
                    0, null, map.artist, map.artist_unicode, map.title, map.title_unicode, map.creator, map.version, Koohii.mods_str(score.getEnabledMods()), score.getEnabledMods(),
                    //这里score的虽然叫MaxCombo，但实际上是这个分数的combo
                    map.od, map.ar, map.cs, map.hp, score.getMaxCombo(), map.max_combo(), map.ncircles, map.nsliders, map.nspinners, score.getCountMiss(),
                    //scoreVersion只能是V1了，
                    1, stars.total, stars.speed, stars.aim, stars.nsingles, stars.nsingles_threshold, pp.aim, pp.speed, pp.acc, pp.total);
        } catch (IOException e) {
            logger.error("离线计算PP出错");
            logger.error(e.getMessage());
            return null;
        }


    }
}
