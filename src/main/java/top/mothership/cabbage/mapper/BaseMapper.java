package top.mothership.cabbage.mapper;

import org.apache.ibatis.annotations.*;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.Userinfo;

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
public interface BaseMapper {
    @Select("<script>" +
            "SELECT * FROM `userrole` " +
            "<choose>" +
            "<when test=\"QQ != null\">" +
            "WHERE `QQ` = #{QQ}" +
            "</when>" +
            "<when test=\"user_id != null\">" +
            "WHERE`user_id` = #{userId}" +
            "</when>" +
            "</choose>")
    User getUser(@Param("QQ") String QQ, @Param("userId") Integer userId);

    @Select("SELECT `user_id` FROM `userrole` WHERE `role` = #{role}")
    List<Integer> listUserIdByRole(@Param("role") String role);

    @Update("<script>" + "update `userrole`"
            + "<set>"
            + "<if test=\"user.role != null\">role=#{user.role},</if>"
            + "<if test=\"user.QQ != null\">QQ=#{user.QQ},</if>"
            + "</set>"
            + " where id = #{user.userId}" + "</script>")
    Integer updateUser(@Param("user") User user);

    @Insert("INSERT INTO `userrole` VALUES (null,#{user.userId},#{user.role},#{user.QQ})")
    Integer addUser(@Param("user") User user);


    @Insert("<script>" + "INSERT INTO `userinfo` " +
            "VALUES" +
            "<foreach item='Userinfo' collection='list' open='' separator=',' close=''>" +
            "(null," +
            "#{userinfo.userName},#{userinfo.userId}," +
            "#{userinfo.count300},#{userinfo.count100}," +
            "#{userinfo.count50},#{userinfo.playCount}," +
            "#{userinfo.accuracy},#{userinfo.ppRaw}," +
            "#{userinfo.rankedScore},#{userinfo.totalScore}," +
            "#{userinfo.level},#{userinfo.ppRank}," +
            "#{userinfo.countRankSs},#{userinfo.countRankS}," +
            "#{userinfo.countRankA},#{userinfo.queryDate}," +
            ")" +
            "</foreach>" +
            "</script>")
    Integer addUserInfo(@Param("userInfo") List<Userinfo> list);

    @Select("SELECT * , abs(UNIX_TIMESTAMP(queryDate) - UNIX_TIMESTAMP(#{queryDate})) AS ds FROM `userinfo`  WHERE `user_id` = #{userId} ORDER BY ds ASC")
    Userinfo getNearestUserInfo(@Param("queryDate") Date queryDate, @Param("userId") Integer userId);

    @Select("SELECT * FROM `userinfo` WHERE `user_id` = #{userId} AND `queryDate` = #{queryDate}")
    Userinfo getUserInfo(@Param("queryDate") Date queryDate, @Param("userId") Integer userId);

    @Delete("DELETE FROM `userinfo` WHERE `queryDate` = #{queryDate}")
    void clearTodayInfo(@Param("queryDate") Date queryDate);


}
