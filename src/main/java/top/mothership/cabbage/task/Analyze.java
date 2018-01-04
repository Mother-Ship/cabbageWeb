package top.mothership.cabbage.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.ScoreDAO;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.util.osu.ScoreUtil;

import java.util.Arrays;
import java.util.List;
//比赛打完了 这个类也用不着了
@Component
public class Analyze {
    private final ApiManager apiManager;
    private final CqManager cqManager;
    private final ScoreUtil scoreUtil;
    private final ScoreDAO scoreDAO;
    List<Integer> targetUser = Arrays.asList(954557, 434479, 1243669, 2043341, 1634748, 5791401, 245276, 418699);
    List<Integer> targetMapR3 = Arrays.asList(1220408, 1311241, 1281079, 804828, 1294885, 1301043, 951818,
            1329559, 942050, 854741, 1177700,
            392220, 1398810, 1203968, 1160295,
            1295867, 1095875, 89229,
            801841, 1410075, 427962, 1003915, 328523,
            1386449,
            1372384, 1291532, 289318, 178721, 1065689);
    private CqMsg cqMsg = new CqMsg();

    @Autowired
    public Analyze(ApiManager apiManager, CqManager cqManager, ScoreUtil scoreUtil, ScoreDAO scoreDAO) {
        this.apiManager = apiManager;
        this.cqManager = cqManager;
        this.scoreUtil = scoreUtil;
        this.scoreDAO = scoreDAO;

        cqMsg.setMessageType("group");
//        cqMsg.setMessageType("private");
    }

    @Scheduled(cron = "0 0/5 * * * ? ")
    public void analyze() {
//        cqMsg.setUserId(1335734657L);
        cqMsg.setGroupId(692339245L);
        for (Integer aList : targetUser) {
            for (Integer bList : targetMapR3) {
                //对每个bid
                List<Score> tmp = apiManager.getScore(bList, aList);
                List<Score> lastTmp = scoreDAO.getLastScoreByUidAndBid(aList, bList);

                if (tmp.size() == 0) {
                    continue;
                }
                Beatmap beatmap = apiManager.getBeatmap(bList);
                String username = apiManager.getUser(null, aList).getUserName();
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

                        cqMsg.setMessage(username + "玩家有成绩更新，新的成绩为：\n"
                                +scoreUtil.genScoreString(score,beatmap,username));
                        cqManager.sendMsg(cqMsg);
                        score.setBeatmapId(bList);
                        scoreDAO.addScore(score);
                    }
                } else {
                    for (int i = 0; i < lastTmp.size(); i++) {
                        if (tmp.get(i).getDate().getTime()!=(lastTmp.get(i).getDate().getTime())) {
                            cqMsg.setMessage(username + "玩家有成绩更新，新的成绩为：\n"
                                    +scoreUtil.genScoreString(tmp.get(i),beatmap,username));
                            cqManager.sendMsg(cqMsg);
                            tmp.get(i).setBeatmapId(bList);
                            scoreDAO.addScore(tmp.get(i));
                        }
                    }
                }

            }

        }




    }
}
