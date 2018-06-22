package top.mothership.cabbage.mapper.autogen;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.type.JdbcType;
import top.mothership.cabbage.pojo.autogen.CbTournament;

public interface CbTournamentMapper {
    @Delete({
        "delete from cb_tournament",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int deleteByPrimaryKey(Integer id);

    @Insert({
        "insert into cb_tournament (Id, host, ",
        "helper, rule, register_start_time, ",
        "register_end_time, team_start_time, ",
        "team_end_time, player_count, ",
        "all_player_count, introduction, ",
        "ended)",
        "values (#{id,jdbcType=INTEGER}, #{host,jdbcType=VARCHAR}, ",
        "#{helper,jdbcType=VARCHAR}, #{rule,jdbcType=INTEGER}, #{registerStartTime,jdbcType=DATE}, ",
        "#{registerEndTime,jdbcType=DATE}, #{teamStartTime,jdbcType=DATE}, ",
        "#{teamEndTime,jdbcType=DATE}, #{playerCount,jdbcType=INTEGER}, ",
        "#{allPlayerCount,jdbcType=INTEGER}, #{introduction,jdbcType=VARCHAR}, ",
        "#{ended,jdbcType=BIT})"
    })
    int insert(CbTournament record);

    @InsertProvider(type=CbTournamentSqlProvider.class, method="insertSelective")
    int insertSelective(CbTournament record);

    @Select({
        "select",
        "Id, host, helper, rule, register_start_time, register_end_time, team_start_time, ",
        "team_end_time, player_count, all_player_count, introduction, ended",
        "from cb_tournament",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    @Results({
        @Result(column="Id", property="id", jdbcType=JdbcType.INTEGER, id=true),
        @Result(column="host", property="host", jdbcType=JdbcType.VARCHAR),
        @Result(column="helper", property="helper", jdbcType=JdbcType.VARCHAR),
        @Result(column="rule", property="rule", jdbcType=JdbcType.INTEGER),
        @Result(column="register_start_time", property="registerStartTime", jdbcType=JdbcType.DATE),
        @Result(column="register_end_time", property="registerEndTime", jdbcType=JdbcType.DATE),
        @Result(column="team_start_time", property="teamStartTime", jdbcType=JdbcType.DATE),
        @Result(column="team_end_time", property="teamEndTime", jdbcType=JdbcType.DATE),
        @Result(column="player_count", property="playerCount", jdbcType=JdbcType.INTEGER),
        @Result(column="all_player_count", property="allPlayerCount", jdbcType=JdbcType.INTEGER),
        @Result(column="introduction", property="introduction", jdbcType=JdbcType.VARCHAR),
        @Result(column="ended", property="ended", jdbcType=JdbcType.BIT)
    })
    CbTournament selectByPrimaryKey(Integer id);

    @UpdateProvider(type=CbTournamentSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(CbTournament record);

    @Update({
        "update cb_tournament",
        "set host = #{host,jdbcType=VARCHAR},",
          "helper = #{helper,jdbcType=VARCHAR},",
          "rule = #{rule,jdbcType=INTEGER},",
          "register_start_time = #{registerStartTime,jdbcType=DATE},",
          "register_end_time = #{registerEndTime,jdbcType=DATE},",
          "team_start_time = #{teamStartTime,jdbcType=DATE},",
          "team_end_time = #{teamEndTime,jdbcType=DATE},",
          "player_count = #{playerCount,jdbcType=INTEGER},",
          "all_player_count = #{allPlayerCount,jdbcType=INTEGER},",
          "introduction = #{introduction,jdbcType=VARCHAR},",
          "ended = #{ended,jdbcType=BIT}",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int updateByPrimaryKey(CbTournament record);
}