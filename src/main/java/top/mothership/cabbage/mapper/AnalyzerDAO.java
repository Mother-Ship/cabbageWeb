package top.mothership.cabbage.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import top.mothership.cabbage.pojo.coolq.osu.Score;

import java.util.List;

@Mapper
@Repository
public interface AnalyzerDAO {
    @Insert("REPLACE INTO `score` VALUES (" +
            "null," +
            "#{score.beatmapId}," +
            "#{score.mode}," +
            "#{score.scoreVersion}," +
            "#{score.mapMd5}," +
            "#{score.repMd5}," +
            "#{score.size}," +
            "#{score.score}," +
            "#{score.maxCombo}," +
            "#{score.count50}," +
            "#{score.count100}," +
            "#{score.count300}," +
            "#{score.countMiss}" +
            ",#{score.countKatu}," +
            "#{score.countGeki}," +
            "#{score.perfect}," +
            "#{score.enabledMods}," +
            "#{score.date}," +
            "#{score.rank}," +
            "#{score.pp}," +
            "#{score.userId}," +
            "#{score.userName}," +
            "#{score.onlineId})")
    Integer addScore(@Param("score") Score score);

    @Select("SELECT * FROM `score` WHERE `user_id` = #{userId} ")
    List<Score> getScoreByUserid(@Param("userId") Integer userId);

    //不排了， 之前成绩不保留在数据库
    @Select("SELECT * FROM `score` WHERE `user_id` = #{userId} AND `beatmap_id` = #{beatmapId} order by `score` desc")
    List<Score> getLastScoreByUidAndBid(@Param("userId") Integer userId, @Param("beatmapId") Integer beatmapId);

    @Select("SELECT `user_id` FROM `target_user`")
    List<Integer> listTargetUser();

    @Select("SELECT `beatmap_id` FROM `target_map`")
    List<Integer> listTargetMap();

    @Insert("INSERT INTO `target_user` VALUES(null,#{userId})")
    Integer addTargetUser(@Param("userId") Integer userId);

    @Insert("INSERT INTO `target_map` VALUES(null,#{beatmapId})")
    Integer addTargetMap(@Param("beatmapId") Integer beatmapId);

    @Insert("DELETE FROM `target_user` WHERE `user_id` = #{userId}")
    Integer delTargetUser(@Param("userId") Integer userId);

    @Insert("DELETE FROM `target_map` WHERE `beatmap_id` = #{beatmapId}")
    Integer delTargetMap(@Param("beatmapId") Integer beatmapId);

    @Insert("DELETE FROM `target_user`")
    Integer delAllTargetUser();

    @Insert("DELETE FROM `target_map`")
    Integer delAllTargetMap();

}
