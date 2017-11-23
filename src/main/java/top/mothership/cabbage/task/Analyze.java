package top.mothership.cabbage.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.mapper.ScoresDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.util.osu.ApiUtil;
import top.mothership.cabbage.util.osu.ScoreUtil;
import top.mothership.cabbage.util.qq.CqUtil;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class Analyze {
    private final ApiUtil apiUtil;
    private CqUtil cqUtil;
    private ScoreUtil scoreUtil;
    private ScoresDAO scoresDAO;
    List<Integer> baQiang = Arrays.asList(3924736, 4110293, 7680789, 7853092, 3665920, 3004781, 2992539);
    List<Integer> guanGuang = Arrays.asList(8794603, 9003283, 8012734, 8762031, 8214694, 4439269, 6226190);
    List<Integer> targetMapR2 = Arrays.asList(404550, 1242792, 1078109, 776600, 103403,
            270363, 419189,
            1328722, 742829,
            136910, 1246270, 363043,
            1442768, 662095, 825985, 996113,
            870152);
    CqMsg cqMsg = new CqMsg();

    //770677061
    @Autowired
    public Analyze(ApiUtil apiUtil, CqUtil cqUtil, ScoreUtil scoreUtil, ScoresDAO scoresDAO) {
        this.apiUtil = apiUtil;
        this.cqUtil = cqUtil;
        this.scoreUtil = scoreUtil;
        this.scoresDAO = scoresDAO;
        cqMsg.setMessageType("private");
    }

    @Scheduled(cron = "0 * * * * ? ")
    public void analyze() {
        cqMsg.setUserId(1335734657L);
        for (Integer aList : guanGuang) {
            for (Integer bList : targetMapR2) {
                //对每个bid
                List<Score> tmp = apiUtil.getScore(bList, aList);
                List<Score> lastTmp = scoresDAO.getLastScoreByUidAndBid(aList, bList);

                if (tmp.size() == 0) {
                    continue;
                }
                Beatmap beatmap = apiUtil.getBeatmap(bList);
                String username = apiUtil.getUser(null, String.valueOf(aList)).getUserName();
                if (tmp.size() != lastTmp.size()) {
                    for (int i = 0; i < tmp.size(); i++) {
                        //在原有成绩的范畴内
                        Score score;
                        if (i <= lastTmp.size() - 1) {
                            //如果取到的成绩相同，跳出本次循环
                            if (tmp.get(i).getDate().getTime()==(lastTmp.get(i).getDate().getTime())) {
                                continue;
                            }
                            //否则取到成绩
                            score = tmp.get(i);
                        } else {
                            //在原有成绩的范畴外，直接取成绩
                            score = tmp.get(i);
                        }

                        cqMsg.setMessage(username + "玩家在谱面" + bList + "有成绩更新，新的成绩为：\n"
                                + score.getMaxCombo() + "x/" + beatmap.getMaxCombo() + "x，"
                                + scoreUtil.convertMOD(score.getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "")
                                + " (" + new DecimalFormat("###.00").format(
                                100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50())
                                        / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()))) + "%)\n"
                                + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(score.getDate()) + ", "
                                + score.getPp() + "PP");

                        cqUtil.sendMsg(cqMsg);
                        score.setBeatmapId(bList);
                        scoresDAO.addScore(score);
                    }
                } else {
                    for (int i = 0; i < lastTmp.size(); i++) {
                        if (tmp.get(i).getDate().getTime()!=(lastTmp.get(i).getDate().getTime())) {
                            cqMsg.setMessage(username + "玩家在谱面" + bList + "有成绩更新，新的成绩为：\n"
                                    + tmp.get(i).getMaxCombo() + "x/" + beatmap.getMaxCombo() + "x，"
                                    + scoreUtil.convertMOD(tmp.get(i).getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "")
                                    + " (" + new DecimalFormat("###.00").format(
                                    100.0 * (6 * tmp.get(i).getCount300() + 2 * tmp.get(i).getCount100() + tmp.get(i).getCount50())
                                            / (6 * (tmp.get(i).getCount50() + tmp.get(i).getCount100() + tmp.get(i).getCount300() + tmp.get(i).getCountMiss()))) + "%)\n"
                                    + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(tmp.get(i).getDate()) + ", "
                                    + tmp.get(i).getPp() + "PP");

                            cqUtil.sendMsg(cqMsg);
                            tmp.get(i).setBeatmapId(bList);
                            scoresDAO.addScore(tmp.get(i));
                        }
                    }
                }

            }

        }

        cqMsg.setUserId(770677061L);
        for (Integer aList : baQiang) {
            for (Integer bList : targetMapR2) {
                //对每个bid
                List<Score> tmp = apiUtil.getScore(bList, aList);
                List<Score> lastTmp = scoresDAO.getLastScoreByUidAndBid(aList, bList);
                Beatmap beatmap = apiUtil.getBeatmap(bList);
                String username = apiUtil.getUser(null, String.valueOf(aList)).getUserName();
                if (tmp.size() == 0) {
                    continue;
                }
                if (tmp.size() != lastTmp.size()) {
                    for (int i = 0; i < tmp.size(); i++) {
                        //在原有成绩的范畴内
                        Score score;
                        if (i <= lastTmp.size() - 1) {
                            //如果取到的成绩相同，跳出本次循环
                            if (tmp.get(i).getDate().getTime()==(lastTmp.get(i).getDate().getTime())) {
                                continue;
                            }
                            //否则取到成绩
                            score = tmp.get(i);
                        } else {
                            //在原有成绩的范畴外，直接取成绩
                            score = tmp.get(i);
                        }
                        cqMsg.setMessage(username + "玩家在谱面" + bList + "有成绩更新，新的成绩为：\n"
                                + score.getMaxCombo() + "x/" + beatmap.getMaxCombo() + "x，"
                                + scoreUtil.convertMOD(score.getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "")
                                + " (" + new DecimalFormat("###.00").format(
                                100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50())
                                        / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()))) + "%)\n"
                                + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(score.getDate()) + ", "
                                + score.getPp() + "PP");

                        cqUtil.sendMsg(cqMsg);
        score.setBeatmapId(bList);
                        scoresDAO.addScore(score);
                    }
                } else {
                    for (int i = 0; i < lastTmp.size(); i++) {
                        if (tmp.get(i).getDate().getTime()!=(lastTmp.get(i).getDate().getTime())) {
                            cqMsg.setMessage(username + "玩家在谱面" + bList + "有成绩更新，新的成绩为：\n"
                                    + tmp.get(i).getMaxCombo() + "x/" + beatmap.getMaxCombo() + "x，"
                                    + scoreUtil.convertMOD(tmp.get(i).getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "")
                                    + " (" + new DecimalFormat("###.00").format(
                                    100.0 * (6 * tmp.get(i).getCount300() + 2 * tmp.get(i).getCount100() + tmp.get(i).getCount50())
                                            / (6 * (tmp.get(i).getCount50() + tmp.get(i).getCount100() + tmp.get(i).getCount300() + tmp.get(i).getCountMiss()))) + "%)\n"
                                    + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(tmp.get(i).getDate()) + ", "
                                    + tmp.get(i).getPp() + "PP");
                            cqUtil.sendMsg(cqMsg);
        tmp.get(i).setBeatmapId(bList);
                            scoresDAO.addScore(tmp.get(i));
                        }
                    }
                }

            }

        }
    }
}
