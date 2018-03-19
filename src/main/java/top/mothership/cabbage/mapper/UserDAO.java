package top.mothership.cabbage.mapper;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;
import top.mothership.cabbage.pojo.User;

import java.util.List;

/**
 * The interface User dao.
 */
@Mapper
@Repository
public interface UserDAO {
    /**
     * Gets user.
     *
     * @param qq     the qq
     * @param userId the user id
     * @return the user
     */
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

    /**
     * List user id by role list.
     *
     * @param role the role
     * @return the list
     */
//加入分隔符处理，在中间的，开头的，结尾的，只有这一个用户组的
    @Select("<script>"
            + "SELECT `user_id` FROM `userrole` "
            + "<where>"
            + "<if test=\"role != null\">"
            + "(`role` LIKE CONCAT('%,',#{role},',%') "
            + "OR `role` LIKE CONCAT(#{role},',%') "
            + "OR `role` = #{role} "
            + "OR `role` LIKE CONCAT('%,',#{role})) </if>"
            + "<if test=\"unbanned = true\">"
            + "AND `is_banned` = '0' </if> " +
            "</where>"
            + "</script>")
    List<Integer> listUserIdByRole(@Param("role") String role, @Param("unbanned") Boolean unbanned);

    /**
     * List user id by uname list.
     *改为Gson序列化，只需考虑在中间的问题，同时加入分隔符
     * 2018-3-12 16:26:59改为模糊查询
     * 2018-3-16 13:29:44没必要用动态sql吧？试试改为多个字段查询
     * @param keyword 搜索关键字
     * @return the list
     */
    @Select("SELECT * FROM `userrole` "
            + "WHERE `legacy_uname` LIKE CONCAT('%',#{keyword},'%') "
            + "OR `current_uname` LIKE CONCAT('%',#{keyword},'%')"
            + "OR `user_id` = #{keyword}"
            + "OR `qq` = #{keyword}")
    @Results(
            {
                    //手动绑定这个字段
                    @Result(column = "is_banned", property = "banned")
            })
    List<User> searchUser(@Param("keyword") String keyword);

    @Select("SELECT * FROM `userrole` "
            + "WHERE `is_banned` =1 ")
    @Results(
            {
                    //手动绑定这个字段
                    @Result(column = "is_banned", property = "banned")
            })
    List<User> listBannedUser();
    /**
     * Gets repeat star.
     * 去掉100%复读的
     *
     * @return the repeat star
     */
    @Results(
            {
                    //手动绑定这个字段
                    @Result(column = "is_banned", property = "banned")
            })
    @Select("SELECT * FROM `userrole` WHERE `speaking_count` >10 order by `repeat_count`/`speaking_count` desc limit 1")
    User getRepeatStar();

    /**
     * Update user integer.
     * 由于采用动态SQL，QQ只能是0不能是null
     * @param user the user
     * @return the integer
     */

    @Update("<script>" + "update `userrole`"
            + "<set>"
            + "<if test=\"user.role != null\">role=#{user.role},</if>"
            + "<if test=\"user.qq != null\">qq=#{user.qq},</if>"
            + "<if test=\"user.legacyUname != null\">legacy_uname=#{user.legacyUname},</if>"
            + "<if test=\"user.currentUname != null\">current_uname=#{user.currentUname},</if>"
            + "<if test=\"user.banned != null\">is_banned=#{user.banned},</if>"
            + "<if test=\"user.repeatCount != null\">repeat_count=#{user.repeatCount},</if>"
            + "<if test=\"user.speakingCount != null\">speaking_count=#{user.speakingCount},</if>"
            + "<if test=\"user.mode != null\">mode=#{user.mode},</if>"
            + "</set>"
            + " where `user_id` = #{user.userId}" + "</script>")
    Integer updateUser(@Param("user") User user);

    /**
     * Add user integer.
     *
     * @param user the user
     * @return the integer
     */
    @Insert("INSERT INTO `userrole` VALUES (null,#{user.userId},#{user.role},#{user.qq}" +
            ",#{user.legacyUname},#{user.currentUname},#{user.banned},#{user.repeatCount},#{user.speakingCount},#{user.mode})")
    Integer addUser(@Param("user") User user);


}
