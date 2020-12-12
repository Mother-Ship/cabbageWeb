package top.mothership.cabbage.service;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mothership.cabbage.annotation.UserAuthorityControl;
import top.mothership.cabbage.constant.Tip;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.constant.pattern.RegularPattern;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.osu.Beatmap;
import top.mothership.cabbage.pojo.osu.Score;
import top.mothership.cabbage.pojo.osu.Userinfo;
import top.mothership.cabbage.util.osu.ScoreUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;


@Service
@UserAuthorityControl({1335734657L})
public class AnalyzeServiceImpl {
    private static CqMsg cqMsg = new CqMsg();
    private final AnalyzerDAO analyzerDAO;
    private final UserDAO userDAO;
    private final ScoreUtil scoreUtil;
    private final ApiManager apiManager;
    private final CqManager cqManager;

    @Autowired
    public AnalyzeServiceImpl(AnalyzerDAO analyzerDAO, UserDAO userDAO, ScoreUtil scoreUtil, ApiManager apiManager, CqManager cqManager) {
        this.analyzerDAO = analyzerDAO;
        this.userDAO = userDAO;
        this.scoreUtil = scoreUtil;
        this.apiManager = apiManager;
        this.cqManager = cqManager;
        cqMsg.setMessageType("private");
        cqMsg.setUserId(1335734657L);
        cqMsg.setSelfId(1020640876L);
    }

    public void addTargetMap(CqMsg cqMsg) {
        Matcher m = RegularPattern.ANALYZE_BID_PARAM.matcher(cqMsg.getMessage());
        if (m.find()) {
            Integer targetMap = Integer.valueOf(m.group(2));
            analyzerDAO.addTargetMap(targetMap);
            cqMsg.setMessage("添加成功：http://osu.ppy.sh/b/" + targetMap);
        } else {
            cqMsg.setMessage("只支持bid");
        }
        cqManager.sendMsg(cqMsg);
    }

    public void delTargetMap(CqMsg cqMsg) {
        Matcher m = RegularPattern.ANALYZE_BID_PARAM.matcher(cqMsg.getMessage());
        if (m.find()) {
            Integer targetMap = Integer.valueOf(m.group(2));
            analyzerDAO.delTargetMap(targetMap);
            cqMsg.setMessage("删除成功：http://osu.ppy.sh/b/" + targetMap);
        } else {
            cqMsg.setMessage("只支持bid");
        }
        cqManager.sendMsg(cqMsg);
    }

    public void delAllTargetMap(CqMsg cqMsg) {
        analyzerDAO.delAllTargetMap();
        cqMsg.setMessage("清空所有目标谱面成功");
        cqManager.sendMsg(cqMsg);
    }

    public void listTargetMap(CqMsg cqMsg) {
        StringBuilder resp;
        List<Integer> list = analyzerDAO.listTargetMap();
        if (list.size() > 0) {
            resp = new StringBuilder("目标谱面：\n");
            for (Integer i : list) {
                Beatmap beatmap = apiManager.getBeatmap(i);
                if (beatmap != null) {
                    resp.append(beatmap.getBeatmapSetId()).append(" ").append(beatmap.getArtist()).append(" - ").append(beatmap.getTitle()).append(" [").append(beatmap.getVersion()).append("]\n");
                } else {
                    resp.append("错误：谱面").append(i).append("获取失败，请检查bid");
                }
            }
        } else {
            resp = new StringBuilder("没有任何目标谱面。");
        }

        cqMsg.setMessage(resp.toString());
        cqManager.sendMsg(cqMsg);
    }

    public void addTargetUser(CqMsg cqMsg) {
        Matcher m = RegularPattern.REG_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        Userinfo userinfo = apiManager.getUser(0, m.group(2));
        if (userinfo != null) {
            analyzerDAO.addTargetUser(userinfo.getUserId());
            cqMsg.setMessage("增加成功：" + userinfo.getUserName());
        } else {
            cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, m.group(2)));
        }
        cqManager.sendMsg(cqMsg);
    }

    public void delTargetUser(CqMsg cqMsg) {
        Matcher m = RegularPattern.REG_CMD_REGEX.matcher(cqMsg.getMessage());
        m.find();
        Userinfo userinfo = apiManager.getUser(0, m.group(2));
        if (userinfo != null) {
            analyzerDAO.delTargetUser(userinfo.getUserId());
            cqMsg.setMessage("删除成功：" + userinfo.getUserName());
        } else {
            cqMsg.setMessage(String.format(Tip.USER_GET_FAILED, m.group(2)));
        }
        cqManager.sendMsg(cqMsg);
    }

    public void listTargetUser(CqMsg cqMsg) {
        List<Integer> list = analyzerDAO.listTargetUser();
        StringBuilder resp;
        if (list.size() > 0) {
            resp = new StringBuilder("目标玩家：\n");
            for (Integer i : list) {
                Userinfo userinfo = apiManager.getUser(0, i);
                if (userinfo != null) {
                    resp.append(userinfo.getUserName()).append("\n");
                } else {
                    resp.append("没有从osu!api获取到uid为").append(i).append("的玩家信息。");
                }
            }
        } else {
            resp = new StringBuilder("没有任何目标玩家。");
        }

        cqMsg.setMessage(resp.toString());
        cqManager.sendMsg(cqMsg);
    }

    public void delAllTargetUser(CqMsg cqMsg) {
        analyzerDAO.delAllTargetUser();
        cqMsg.setMessage("清空所有目标玩家成功");
        cqManager.sendMsg(cqMsg);
    }


    public void analyze() {
        List<Integer> targetUser = analyzerDAO.listTargetUser();
        List<Integer> targetMap = analyzerDAO.listTargetMap();
        for (Integer aList : targetUser) {
            for (Integer bList : targetMap) {
                //对每个bid去API取到分数
                List<Score> tmp = apiManager.getScore(0, bList, aList);
                List<Score> lastTmp = analyzerDAO.getLastScoreByUidAndBid(aList, bList);

                Map<Integer, Score> lastTmpMap = new HashMap<>();
                if (tmp.size() == 0) {
                    continue;
                }
                for (Score score : lastTmp) {
                    lastTmpMap.put(score.getEnabledMods(), score);
                }
                for (Score score : tmp) {
                    if (lastTmpMap.get(score.getEnabledMods()) == null
                            || lastTmpMap.get(score.getEnabledMods()).getDate().getTime() != (score.getDate().getTime())) {
                        cqManager.warn(new Gson().toJson(lastTmp));
                        cqManager.warn(new Gson().toJson(tmp));
                        Beatmap beatmap = apiManager.getBeatmap(bList);
                        String username = userDAO.getUser(null, aList).getCurrentUname();
                        cqMsg.setMessage(scoreUtil.genScoreString(score, beatmap, username, null));
                        cqManager.sendMsg(cqMsg);
                        score.setBeatmapId(bList);
                        analyzerDAO.addScore(score);
                    }
                }


            }

        }
    }
}
