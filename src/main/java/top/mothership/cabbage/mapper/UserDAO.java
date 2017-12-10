package top.mothership.cabbage.mapper;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;
import top.mothership.cabbage.pojo.User;

import java.util.List;

@Mapper
@Repository
public interface UserDAO {
    @Select("<script>" +
            "SELECT * FROM `userrole` " +
            "<choose>" +
            "<when test=\"qq != null\">" +
            "WHERE `qq` = #{qq}" +
            "</when>" +
            "<when test=\"userId != null\">" +
            "WHERE`user_id` = #{userId}" +
            "</when>" +
            "</choose>" +
            "</script>")
        //只能传一个，不能同时处理两个
    @Results(
            {
                    //手动绑定这个字段
                    @Result(column = "is_banned", property = "banned")
            })
    User getUser(@Param("qq") Long qq, @Param("userId") Integer userId);

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

    //改为Gson序列化，只需考虑在中间的问题，同时加入分隔符
    @Select("<script>"
            + "SELECT * FROM `userrole` "
            + "<if test=\"username != null\">"
            + "WHERE `legacy_uname` LIKE CONCAT('%\"',#{username},'\"%') "
            + "OR `current_uname` = #{username} "
           + " </if>"
            + "</script>")
    List<User> listUserIdByUname(@Param("username") String username);

    @Select("SELECT * FROM `userrole` order by `repeat_count`/`speaking_count` desc  limit 1")
    User getRepeatStar();

    @Update("<script>" + "update `userrole`"
            + "<set>"
            + "<if test=\"user.role != null\">role=#{user.role},</if>"
            + "<if test=\"user.qq != null\">qq=#{user.qq},</if>"
            + "<if test=\"user.legacyUname != null\">legacy_uname=#{user.legacyUname},</if>"
            + "<if test=\"user.currentUname != null\">current_uname=#{user.currentUname},</if>"
            + "<if test=\"user.banned != null\">is_banned=#{user.banned},</if>"
            + "<if test=\"user.repeatCount != null\">repeat_count=#{user.repeatCount},</if>"
            + "</set>"
            + " where `user_id` = #{user.userId}" + "</script>")
    Integer updateUser(@Param("user") User user);

    @Insert("INSERT INTO `userrole` VALUES (null,#{user.userId},#{user.role},#{user.qq}" +
            ",#{user.legacyUname},#{user.currentUname},#{user.banned},#{user.repeatCount},#{user.speakingCount})")
    Integer addUser(@Param("user") User user);



}
