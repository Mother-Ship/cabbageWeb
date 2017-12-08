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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

@Component
public class Analyze {
    private final ApiManager apiManager;
    private final CqManager cqManager;
    private final ScoreUtil scoreUtil;
    private final ScoreDAO scoreDAO;
    List<Integer> targetUser = Arrays.asList(7839397, 6678196, 6253301, 7183040,9314367, 7534323);
    List<Integer> targetMapR3 = Arrays.asList(199535, 556638, 183460, 758189, 1236367,
            977450, 119853,
            834589, 982793,
            335299, 127313, 274365,
            853094, 582773, 1043393, 202217,
            368845);
    private CqMsg cqMsg = new CqMsg();

    @Autowired
    public Analyze(ApiManager apiManager, CqManager cqManager, ScoreUtil scoreUtil, ScoreDAO scoreDAO) {
        this.apiManager = apiManager;
        this.cqManager = cqManager;
        this.scoreUtil = scoreUtil;
        this.scoreDAO = scoreDAO;

        cqMsg.setMessageType("private");
    }

    @Scheduled(cron = "0 * * * * ? ")
    public void analyze() {
        cqMsg.setUserId(1335734657L);
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
