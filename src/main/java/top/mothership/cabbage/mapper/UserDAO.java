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
            + "</set>"
            + " where `user_id` = #{user.userId}" + "</script>")
    Integer updateUser(@Param("user") User user);

    @Insert("INSERT INTO `userrole` VALUES (null,#{user.userId},#{user.role},#{user.QQ})")
    Integer addUser(@Param("user") User user);



}
