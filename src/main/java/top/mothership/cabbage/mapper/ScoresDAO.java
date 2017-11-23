package top.mothership.cabbage.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import top.mothership.cabbage.pojo.osu.Score;

import java.util.List;

@Mapper
@Repository
public interface ScoresDAO {
    @Insert("INSERT INTO `scores` VALUES (null,#{score.beatmapId},#{score.mode},#{score.scoreVersion}" +
            ",#{score.mapMd5},#{score.repMd5},#{score.size}" +
            ",#{score.score},#{score.maxCombo},#{score.count50}" +
            ",#{score.count100},#{score.count300},#{score.countMiss}" +
            ",#{score.countKatu},#{score.countGeki},#{score.perfect}" +
            ",#{score.enabledMods},#{score.date},#{score.rank}" +
            ",#{score.pp},#{score.userId},#{score.userName},#{score.onlineId})")
    Integer addScore(@Param("score") Score score);

    @Select("SELECT * FROM `scores` WHERE `user_id` = #{userId} ")
    List<Score> getScoreByUserid(@Param("userId")Integer userId);
//按score反向排列，以符合多mod成绩API的返回顺序
    @Select("select t.* from " +
            "(SELECT * FROM `scores` " +
            "WHERE `user_id` = #{userId} " +
            "AND `beatmap_id` = #{beatmapId} " +
            "group by `enabled_mods` order by `date` desc)" +
            "as t order by `score` desc")
    List<Score> getLastScoreByUidAndBid(@Param("userId")Integer userId,@Param("beatmapId") Integer beatmapId);
}
