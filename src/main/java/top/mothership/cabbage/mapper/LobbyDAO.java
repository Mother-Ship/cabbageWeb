package top.mothership.cabbage.mapper;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;
import top.mothership.cabbage.pojo.coolq.osu.Lobby;
import top.mothership.cabbage.pojo.coolq.osu.MpBeatmap;

import java.util.List;

/**
 * 对mp系列命令进行数据库操作的接口
 */
@Mapper
@Repository
public interface LobbyDAO {
    /**
     * 新建一个预约mp
     *
     * @param lobby the lobby
     */
    @Insert("INSERT INTO `mp_lobby` VALUES(null," +
            "#{lobby.creator},#{lobby.group},null,null,#{lobby.reservedStartTime},null)")
    void addReserveLobby(@Param("lobby") Lobby lobby);

    /**
     * 向数据库更新比赛数据
     *
     * @param lobby the lobby
     */
    @Update("<script>" + "update `mp_lobby`"
            + "<set>"
            + "<if test=\"lobby.group != null\">group=#{lobby.group},</if>"
            + "<if test=\"lobby.match != null\">match=#{lobby.match},</if>"
            + "<if test=\"lobby.games != null\">lobby.games=#{lobby.games},</if>"
            + "<if test=\"lobby.reservedStartTime != null\">rsrv_start_time=#{lobby.reservedStartTime},</if>"
            + "<if test=\"lobby.match.endTime != null\">logged_end_time=#{lobby.match.endTime},</if>"
            + "</set>"
            + " where `user_id` = #{user.userId}" + "</script>")
    void updateLobby(@Param("lobby") Lobby lobby);

    /**
     * 获取需要被开始的比赛，首先当前时间要比预约的开始时间要晚，其次match还没有被更新
     * 每分钟调用一次这个方法，然后在代码里判定当前时间 和 预约的开始时间，然后视情况调用IRC和API，开始房间/登记数据
     *
     * @return the lobby not started
     */
    @Select("SELECT * FROM `mp_lobby` WHERE NOW()<= `rsrv_start_time` AND `match` IS null")
    List<Lobby> getLobbyNotStarted();

    /**
     * 获取已经开始、没有结束的比赛，每当mp被取消或者房间结束时就更新logged_time，因此这个值是null的比赛就是还没结束
     *
     * @return the lobby not ended
     */
    @Select("SELECT * FROM `mp_lobby` WHERE `match` IS NOT null AND `logged_end_time` IS null ")
    List<Lobby> getLobbyNotEnded();

    /**
     * 查找这个人是否已经开了可以加入的房间：match非空+没有结束
     *
     * @param creator 提供房间创建者
     */
    @Select("SELECT * FROM `mp_lobby` WHERE `creator` = #{creator} AND `match` IS NOT null AND `logged_end_time` IS null")
    Lobby getStartedLobbyByCreator(@Param("creator") Integer creator);

    /**
     * 查找这个人是否已经有预约mp了
     *
     * @param creator 提供房间创建者
     */
    @Select("SELECT * FROM `mp_lobby` WHERE `creator` = #{creator} AND `match` IS null")
    Lobby getReservedLobbyByCreator(@Param("creator") Integer creator);

    /**
     * 对谱面简单的增删查，INSERT暂时不在数据库层面采用措施，insert之前先get一下
     *
     * @param mpBeatmap the beatmap
     */
    @Insert("INSERT INTO `mp_beatmap` VALUES (null,#{mpBeatmap.beatmapId},#{mpBeatmap.recommender},#{mpBeatmap.group},#{mpBeatmap.mods})")
    void addLobbyBeatmap(@Param("mpBeatmap") MpBeatmap mpBeatmap);

    @Update("UPDATE `mp_beatmap` SET `group` = #{mpBeatmap.group} WHERE `beatmap_id` = #{mpBeatmap.beatmapId}")
    void updateBeatmapGroup(@Param("mpBeatmap") MpBeatmap mpBeatmap);

    /**
     * Gets lobby beatmap by group.
     *
     * @param group the group
     * @return the lobby beatmap by group
     */
    @Select("SELECT * FROM `mp_beatmap` WHERE `group` = #{group}")
    List<MpBeatmap> getLobbyBeatmapByGroup(@Param("group") String group);

    @Select("SELECT * FROM `mp_beatmap` WHERE `group` = #{group} AND `beatmap_id` = #{beatmap_id}")
    MpBeatmap getLobbyBeatmapByGroupAndBid(@Param("group") String group, @Param("beatmap_id") Integer beatmap_id);

    /**
     * Del lobby beatmap.
     *
     * @param mpBeatmap the beatmap
     */
    @Delete("DELETE FROM `mp_beatmap` WHERE `beatmap_id` = #{mpBeatmap.beatmapId}")
    void delLobbyBeatmap(@Param("mpBeatmap") MpBeatmap mpBeatmap);

}
