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
import top.mothership.cabbage.pojo.autogen.CbMatch;

public interface CbMatchMapper {
    @Delete({
        "delete from cb_match",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int deleteByPrimaryKey(Integer id);

    @Insert({
        "insert into cb_match (Id, tournament_id, ",
        "blue_team_id, red_team_id, ",
        "blue_team_score, red_team_score, ",
        "match_type, mappool_id, ",
        "live_address, record_address, ",
        "start_time, time_confirmed, ",
        "mp_link, referee, ",
        "ended, note)",
        "values (#{id,jdbcType=INTEGER}, #{tournamentId,jdbcType=BIGINT}, ",
        "#{blueTeamId,jdbcType=BIGINT}, #{redTeamId,jdbcType=BIGINT}, ",
        "#{blueTeamScore,jdbcType=INTEGER}, #{redTeamScore,jdbcType=INTEGER}, ",
        "#{matchType,jdbcType=INTEGER}, #{mappoolId,jdbcType=BIGINT}, ",
        "#{liveAddress,jdbcType=VARCHAR}, #{recordAddress,jdbcType=VARCHAR}, ",
        "#{startTime,jdbcType=TIMESTAMP}, #{timeConfirmed,jdbcType=BIT}, ",
        "#{mpLink,jdbcType=VARCHAR}, #{referee,jdbcType=INTEGER}, ",
        "#{ended,jdbcType=BIT}, #{note,jdbcType=VARCHAR})"
    })
    int insert(CbMatch record);

    @InsertProvider(type=CbMatchSqlProvider.class, method="insertSelective")
    int insertSelective(CbMatch record);

    @Select({
        "select",
        "Id, tournament_id, blue_team_id, red_team_id, blue_team_score, red_team_score, ",
        "match_type, mappool_id, live_address, record_address, start_time, time_confirmed, ",
        "mp_link, referee, ended, note",
        "from cb_match",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    @Results({
        @Result(column="Id", property="id", jdbcType=JdbcType.INTEGER, id=true),
        @Result(column="tournament_id", property="tournamentId", jdbcType=JdbcType.BIGINT),
        @Result(column="blue_team_id", property="blueTeamId", jdbcType=JdbcType.BIGINT),
        @Result(column="red_team_id", property="redTeamId", jdbcType=JdbcType.BIGINT),
        @Result(column="blue_team_score", property="blueTeamScore", jdbcType=JdbcType.INTEGER),
        @Result(column="red_team_score", property="redTeamScore", jdbcType=JdbcType.INTEGER),
        @Result(column="match_type", property="matchType", jdbcType=JdbcType.INTEGER),
        @Result(column="mappool_id", property="mappoolId", jdbcType=JdbcType.BIGINT),
        @Result(column="live_address", property="liveAddress", jdbcType=JdbcType.VARCHAR),
        @Result(column="record_address", property="recordAddress", jdbcType=JdbcType.VARCHAR),
        @Result(column="start_time", property="startTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="time_confirmed", property="timeConfirmed", jdbcType=JdbcType.BIT),
        @Result(column="mp_link", property="mpLink", jdbcType=JdbcType.VARCHAR),
        @Result(column="referee", property="referee", jdbcType=JdbcType.INTEGER),
        @Result(column="ended", property="ended", jdbcType=JdbcType.BIT),
        @Result(column="note", property="note", jdbcType=JdbcType.VARCHAR)
    })
    CbMatch selectByPrimaryKey(Integer id);

    @UpdateProvider(type=CbMatchSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(CbMatch record);

    @Update({
        "update cb_match",
        "set tournament_id = #{tournamentId,jdbcType=BIGINT},",
          "blue_team_id = #{blueTeamId,jdbcType=BIGINT},",
          "red_team_id = #{redTeamId,jdbcType=BIGINT},",
          "blue_team_score = #{blueTeamScore,jdbcType=INTEGER},",
          "red_team_score = #{redTeamScore,jdbcType=INTEGER},",
          "match_type = #{matchType,jdbcType=INTEGER},",
          "mappool_id = #{mappoolId,jdbcType=BIGINT},",
          "live_address = #{liveAddress,jdbcType=VARCHAR},",
          "record_address = #{recordAddress,jdbcType=VARCHAR},",
          "start_time = #{startTime,jdbcType=TIMESTAMP},",
          "time_confirmed = #{timeConfirmed,jdbcType=BIT},",
          "mp_link = #{mpLink,jdbcType=VARCHAR},",
          "referee = #{referee,jdbcType=INTEGER},",
          "ended = #{ended,jdbcType=BIT},",
          "note = #{note,jdbcType=VARCHAR}",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int updateByPrimaryKey(CbMatch record);
}