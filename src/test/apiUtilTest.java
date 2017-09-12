import com.google.gson.Gson;
import org.jsoup.Connection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import top.mothership.cabbage.mapper.BaseMapper;
import top.mothership.cabbage.pojo.Beatmap;
import top.mothership.cabbage.pojo.CqMsg;
import top.mothership.cabbage.pojo.Score;
import top.mothership.cabbage.pojo.Userinfo;
import top.mothership.cabbage.util.ApiUtil;
import top.mothership.cabbage.util.CqUtil;
import top.mothership.cabbage.util.ImgUtil;

import java.sql.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:/spring/spring-*.xml"})
public class apiUtilTest {
    @Autowired
    private ApiUtil apiUtil;
    @Autowired
    private ImgUtil imgUtil;
    @Autowired
    private CqUtil cqUtil;
    @Autowired
    private BaseMapper baseMapper;

    @Test
    public void Test() {

        CqMsg cqMsg = new CqMsg();
        cqMsg.setPostType("event");
        cqMsg.setMessageType("group");
        cqMsg.setGroupId(532783765L);
        cqMsg.setUserId(1335734657L);
        cqMsg.setMessage("[CQ:at,qq="+cqMsg.getUserId()+"]，欢迎加入本群。");
        cqUtil.sendMsg(cqMsg);

//        Userinfo u1 = baseMapper.getNearestUserInfo(Date.valueOf("2017-9-11"), 2545898);
//        Userinfo u = apiUtil.getUser("Mother Ship", null);
//        imgUtil.drawUserInfo(u, u1, "creep", 3, true, 0);
//        Score score = apiUtil.getRecent("Mother Ship",null);
//        Beatmap beatmap = apiUtil.getBeatmap(score.getBeatmapId());
//        imgUtil.drawResult("Mother Ship",score,beatmap);
//        List<Score> list = apiUtil.getBP("Mother Ship",null);
//        //这里用LinkedHashMap 保证顺序
//        Map<Score,Integer> map = new LinkedHashMap<>();
//        for(int i=0;i<list.size();i++){
//           Beatmap beatmap =  apiUtil.getBeatmap(list.get(i).getBeatmapId());
//            list.get(i).setBeatmapName(beatmap.getArtist() + " - " + beatmap.getTitle() + " [" + beatmap.getVersion() + "]");
//            map.put(list.get(i), i);
//        }
//        imgUtil.drawUserBP("Mother Ship",map);


    }


}
