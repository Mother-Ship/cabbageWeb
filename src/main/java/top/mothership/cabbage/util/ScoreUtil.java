package top.mothership.cabbage.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.Beatmap;
import top.mothership.cabbage.pojo.OppaiResult;
import top.mothership.cabbage.pojo.Score;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
import java.util.Set;
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
        String accS = new DecimalFormat("###.00").format(100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50()) / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss())));
        float acc = Float.valueOf(accS);
        String cmd = "\"" + rb.getString("path") + "\\data\\image\\resource\\oppai.exe\" "
                + "\"" + rb.getString("path") + "\\data\\image\\resource\\osu\\";
        String osuFile = webPageUtil.getOsuFile(score.getBeatmapId(), beatmap);
        Set<String> mods = convertMOD(score.getEnabledMods()).keySet();
        BufferedReader bufferedReader;

        try {
            //移除掉里面的SD、PF和None
            Set<String> modsWithoutSD = new HashSet<>();
            if (mods.contains("SD") || mods.contains("PF") || mods.contains("None")) {
                for (String mod : mods) {
                    if (!mod.equals("SD") && !mod.equals("PF") && !mod.contains("None")) {
                        modsWithoutSD.add(mod);
                    }
                }
            } else {
                modsWithoutSD = mods;
            }
            //拼接命令
            cmd = cmd + osuFile + "\" -ojson ";
            if (modsWithoutSD.size() > 0) {
                cmd = cmd.concat("+" + modsWithoutSD.toString().replaceAll("[\\[\\] ,]", "") + " ");

            }
            cmd = cmd + acc + "% " + score.getCountMiss() + "m " + score.getMaxCombo() + "x";
//            logger.info("正在启动进程"+cmd);
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"), 1024);
            String result = bufferedReader.readLine();
//            logger.info("oppai计算结果："+result);
            OppaiResult oppaiResult = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(result, OppaiResult.class);
            //一个小补丁
            if (Math.round(oppaiResult.getAimPp()) == Integer.MAX_VALUE) {
                oppaiResult.setAimPp(0);
                oppaiResult.setPp(oppaiResult.getAimPp() + oppaiResult.getAccPp() + oppaiResult.getSpeedPp());
            }
            if (Math.round(oppaiResult.getAccPp()) == Integer.MAX_VALUE) {
                oppaiResult.setAccPp(0);
                oppaiResult.setPp(oppaiResult.getAimPp() + oppaiResult.getAccPp() + oppaiResult.getSpeedPp());
            }
            if (Math.round(oppaiResult.getSpeedPp()) == Integer.MAX_VALUE) {
                oppaiResult.setSpeedPp(0);
                oppaiResult.setPp(oppaiResult.getAimPp() + oppaiResult.getAccPp() + oppaiResult.getSpeedPp());
            }
            return oppaiResult;

        } catch (InterruptedException | IOException e) {
            logger.error("离线计算PP出错");
            logger.error(e.getMessage());
            return null;
        }


    }
}
