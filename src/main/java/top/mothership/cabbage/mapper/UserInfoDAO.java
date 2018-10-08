package top.mothership.cabbage.mapper;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;
import top.mothership.cabbage.pojo.coolq.osu.Userinfo;

import java.time.LocalDate;
import java.util.List;

@Mapper
@Repository
public interface UserInfoDAO {

    @Insert("INSERT INTO `userinfo` VALUES(null," +
            "#{userinfo.mode},#{userinfo.userId}," +
            "#{userinfo.count300},#{userinfo.count100}," +
            "#{userinfo.count50},#{userinfo.playCount}," +
            "#{userinfo.accuracy},#{userinfo.ppRaw}," +
            "#{userinfo.rankedScore},#{userinfo.totalScore}," +
            "#{userinfo.level},#{userinfo.ppRank}," +
            "#{userinfo.countRankSs},#{userinfo.countRankS}," +
            "#{userinfo.countRankA},#{userinfo.queryDate}" +
            ")")
    Integer addUserInfo(@Param("userinfo") Userinfo userinfo);

    @Select("SELECT * FROM `userinfo` WHERE `user_id` = #{userId}")
    List<Userinfo> listUserInfoByUserId(@Param("userId") Integer userId);
    @Select("SELECT * FROM `userinfo` WHERE `user_id` = #{userId} AND `mode` = #{mode}")
    List<Userinfo> listUserInfoByUserIdAndMode(@Param("userId") Integer userId,@Param("mode") Integer mode);
    @Select("SELECT * , abs(UNIX_TIMESTAMP(queryDate) - UNIX_TIMESTAMP(#{queryDate})) AS ds " +
            "FROM `userinfo`  WHERE `user_id` = #{userId} AND `mode` = #{mode} ORDER BY ds ASC LIMIT 1")
    Userinfo getNearestUserInfo(@Param("mode") Integer mode, @Param("userId") Integer userId, @Param("queryDate") LocalDate queryDate);

    @Select("SELECT * FROM `userinfo` WHERE `user_id` = #{userId} AND `queryDate` = #{queryDate} AND `mode` = #{mode}")
    Userinfo getUserInfo(@Param("mode") Integer mode, @Param("userId") Integer userId, @Param("queryDate") LocalDate queryDate);

    @Delete("DELETE FROM `userinfo` WHERE `queryDate` = #{queryDate}")
    void clearTodayInfo(@Param("queryDate") LocalDate queryDate);

    @Select("<script>" +
            "SELECT * FROM `userinfo` WHERE `user_id` in " +
            "<foreach item=\"item\" index=\"index\" collection=\"list\"" +
            "      open=\"(\" separator=\",\" close=\")\">" +
            "        #{item}" +
            "</foreach>" +
            "AND `queryDate` = DATE_SUB(#{start}, INTERVAL 2 DAY) AND mode= 0" +
            "</script>")
    List<Userinfo> batchGetNowUserinfo(@Param("list")List<Integer> uidList,@Param("start")LocalDate start);

    @Select("SELECT * FROM userinfo WHERE queryDate = DATE_SUB(#{start}, INTERVAL 30 DAY) AND mode=0 " +
            "AND user_id IN (" +
            "(SELECT user_id FROM (SELECT user_id,pp_raw FROM userinfo WHERE queryDate = DATE_SUB(#{start}, INTERVAL 2 DAY) " +
            "AND mode =0 AND pp_raw<#{ppMax} AND pp_raw>#{ppMin} ) AS u)" +
            ");")
    List<Userinfo> getStdUserRegisteredInOneMonth(@Param("ppMin") Integer ppMin, @Param("ppMax") Integer ppMax, @Param("start")LocalDate start);

}


