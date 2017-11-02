package top.mothership.cabbage.mapper;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.osu.Userinfo;

import java.sql.Date;
import java.util.List;

/*
"SELECT `role` FROM `userrole` WHERE `user_id` = ?" clear
"SELECT `user_id` FROM `userrole` WHERE `QQ` = ?"
"SELECT `QQ` FROM `userrole` WHERE `user_id` = ?"
"SELECT `user_id` FROM `userrole`"
"SELECT `user_id` FROM `userrole` WHERE `role` = ?"

"UPDATE `userrole` SET `QQ` = ? WHERE `user_id` = ?"
"UPDATE `userrole` SET `role` = ? WHERE `user_id` = ?"
"INSERT INTO `userrole` (`user_id`) VALUES (?)"


"INSERT INTO `userinfo` VALUES (NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
"SELECT * , abs(UNIX_TIMESTAMP(queryDate) - UNIX_TIMESTAMP(?)) AS ds FROM `userinfo`  WHERE `user_id` = ? ORDER BY ds ASC "
"SELECT * FROM `userinfo` WHERE `user_id` = ? AND `queryDate` = ?"
"DELETE FROM `userinfo` WHERE `queryDate` = ?"
 */
@Mapper
@Repository
public interface BaseMapper {
    @Select("<script>" +
            "SELECT * FROM `userrole` " +
            "<choose>" +
            "<when test=\"QQ != null\">" +
            "WHERE `QQ` = #{QQ}" +
            "</when>" +
            "<when test=\"userId != null\">" +
            "WHERE`user_id` = #{userId}" +
            "</when>" +
            "</choose>" +
            "</script>")
        //只能传一个，不能同时处理两个
    User getUser(@Param("QQ") String QQ, @Param("userId") Integer userId);
    //加入分隔符处理，在中间的，开头的，结尾的，只有这一个用户组的
    @Select("<script>"
            + "SELECT `user_id` FROM `userrole` "
            + "<if test=\"role != null\">"
            + "WHERE `role` LIKE CONCAT('%,',#{role},',%') "
            + "OR `role` LIKE CONCAT(#{role},',%') "
            + "OR `role` = #{role} "
            + "OR `role` LIKE CONCAT('%,',#{role}) </if>"
            + "</script>")
    List<Integer> listUserIdByRole(@Param("role") String role);

    @Update("<script>" + "update `userrole`"
            + "<set>"
            + "<if test=\"user.role != null\">role=#{user.role},</if>"
            + "<if test=\"user.QQ != null\">QQ=#{user.QQ},</if>"
            + "<if test=\"user.cookie != null\">cookie=#{user.cookie},</if>"
            + "</set>"
            + " where `user_id` = #{user.userId}" + "</script>")
    Integer updateUser(@Param("user") User user);

    @Insert("INSERT INTO `userrole` VALUES (null,#{user.userId},#{user.role},#{user.QQ},#{user.cookie})")
    Integer addUser(@Param("user") User user);


    @Insert("INSERT INTO `userinfo` VALUES(null," +
            "#{userinfo.userId},#{userinfo.count300},#{userinfo.count100}," +
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

    @Select("SELECT * , abs(UNIX_TIMESTAMP(queryDate) - UNIX_TIMESTAMP(#{queryDate})) AS ds FROM `userinfo`  WHERE `user_id` = #{userId} ORDER BY ds ASC LIMIT 1")
    Userinfo getNearestUserInfo(@Param("userId") Integer userId, @Param("queryDate") Date queryDate);

    @Select("SELECT * FROM `userinfo` WHERE `user_id` = #{userId} AND `queryDate` = #{queryDate}")
    Userinfo getUserInfo(@Param("userId") Integer userId, @Param("queryDate") Date queryDate);

    @Delete("DELETE FROM `userinfo` WHERE `queryDate` = #{queryDate}")
    void clearTodayInfo(@Param("queryDate") Date queryDate);

}
