package top.mothership.cabbage.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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
    List<Integer> baQiang = Arrays.asList(3924736,4110293,7680789,7853092,3665920,3004781,2992539);
    List<Integer> guanGuang = Arrays.asList(8794603,9003283,8012734,8762031,8214694,4439269,6226190);
    List<Integer> targetMapR1 = Arrays.asList(547229,261725,195548,923329,176549,880023,917430,1327955,307289,1053842,363008,178643,193523,418725);
    List<Integer> targetMapR2 = Arrays.asList(404550,1242792,1078109,776600,103403,
            270363,419189,
            1328722,742829,
            136910,1246270,363043,
            1442768,662095,825985,996113,
            870152);
    CqMsg cqMsg = new CqMsg();
//770677061
    @Autowired
    public Analyze(ApiUtil apiUtil, CqUtil cqUtil, ScoreUtil scoreUtil) {
        this.apiUtil = apiUtil;
        this.cqUtil = cqUtil;
        this.scoreUtil = scoreUtil;
        cqMsg.setUserId(1335734657L);
        cqMsg.setMessageType("private");
        //初始化时候加载一次
//        initScore();
    }

    private void initScore(){
        for(String aList:targetUser){
            Map<Integer,List<Score>> tmp2 = new HashMap<>();
            for(Integer bList:targetMap){
                List<Score> tmp = apiUtil.getScore(bList,aList);
                Beatmap beatmap = apiUtil.getBeatmap(bList);
                if(tmp.size()>0) {
                    for (Score score : tmp) {
                        cqMsg.setMessage(aList + "玩家在谱面" + bList + "的成绩为：\n"
                                +score.getMaxCombo()+"x/"+beatmap.getMaxCombo()+"x，"
                                + scoreUtil.convertMOD(score.getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "")
                                + " (" + new DecimalFormat("###.00").format(
                                100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50())
                                        / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()))) + "%)\n"
                                + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(score.getDate()) + ", "
                                + score.getPp() + "PP");
                        cqUtil.sendMsg(cqMsg);
                    }
                }else{
                    cqMsg.setMessage(aList + "玩家在谱面" + bList + "没有成绩。" );
                    cqUtil.sendMsg(cqMsg);
                }
                tmp2.put(bList,tmp);
            }
            scores.put(aList,tmp2);
        }

    }
    @Scheduled(cron = "0 0 * * * ? ")
    public void analyze(){

        for(String aList:targetUser){
            //对每个玩家有一个Map<Integer,List<Score>>
            Map<Integer,List<Score>> tmp2  = scores.get(aList);
            for(Integer bList:targetMap){
                //对每个bid
                List<Score> tmp = apiUtil.getScore(bList,aList);
                List<Score> lastTmp = tmp2.get(bList);
                Beatmap beatmap = apiUtil.getBeatmap(bList);
                if(tmp.size()!=lastTmp.size()){
                    for (int i=0;i<lastTmp.size();i++) {
                        cqMsg.setMessage(aList + "玩家在谱面" + bList + "有成绩更新，新的成绩为：\n"
                                +score.getMaxCombo()+"x/"+beatmap.getMaxCombo()+"x，"
                              + scoreUtil.convertMOD(score.getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "")
                                + " (" + new DecimalFormat("###.00").format(
                                100.0 * (6 * score.getCount300() + 2 * score.getCount100() + score.getCount50())
                                        / (6 * (score.getCount50() + score.getCount100() + score.getCount300() + score.getCountMiss()))) + "%)\n"
                                + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(score.getDate()) + ", "
                                + score.getPp() + "PP");
                        cqUtil.sendMsg(cqMsg);
                    }
                }else{
                    for(int i=0;i<lastTmp.size();i++){
                        if(!tmp.get(i).getDate().equals(lastTmp.get(i).getDate())){
                            cqMsg.setMessage(aList + "玩家在谱面" + bList + "有成绩更新，新的成绩为：\n"
                                    +tmp.get(i).getMaxCombo()+"x/"+beatmap.getMaxCombo()+"x，"
                                   + scoreUtil.convertMOD(tmp.get(i).getEnabledMods()).keySet().toString().replaceAll("\\[\\]", "")
                                    + " (" + new DecimalFormat("###.00").format(
                                    100.0 * (6 * tmp.get(i).getCount300() + 2 * tmp.get(i).getCount100() + tmp.get(i).getCount50())
                                            / (6 * (tmp.get(i).getCount50() + tmp.get(i).getCount100() + tmp.get(i).getCount300() + tmp.get(i).getCountMiss()))) + "%)\n"
                                     + new SimpleDateFormat("yy/MM/dd HH:mm:ss").format(tmp.get(i).getDate()) + ", "
                                    + tmp.get(i).getPp() + "PP");
                            cqUtil.sendMsg(cqMsg);
                        }
                    }
                }
                tmp2.put(bList,tmp);
            }
            scores.put(aList,tmp2);
        }

    }
}
