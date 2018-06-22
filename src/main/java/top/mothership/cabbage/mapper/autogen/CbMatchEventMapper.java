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
import top.mothership.cabbage.pojo.autogen.CbMatchEvent;

public interface CbMatchEventMapper {
    @Delete({
        "delete from cb_match_event",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int deleteByPrimaryKey(Integer id);

    @Insert({
        "insert into cb_match_event (Id, match_id, ",
        "event_type, operator_team, ",
        "beatmap_id, winner, ",
        "player_id, score, ",
        "roll_point, note)",
        "values (#{id,jdbcType=INTEGER}, #{matchId,jdbcType=BIGINT}, ",
        "#{eventType,jdbcType=INTEGER}, #{operatorTeam,jdbcType=INTEGER}, ",
        "#{beatmapId,jdbcType=BIGINT}, #{winner,jdbcType=INTEGER}, ",
        "#{playerId,jdbcType=INTEGER}, #{score,jdbcType=BIGINT}, ",
        "#{rollPoint,jdbcType=INTEGER}, #{note,jdbcType=VARCHAR})"
    })
    int insert(CbMatchEvent record);

    @InsertProvider(type=CbMatchEventSqlProvider.class, method="insertSelective")
    int insertSelective(CbMatchEvent record);

    @Select({
        "select",
        "Id, match_id, event_type, operator_team, beatmap_id, winner, player_id, score, ",
        "roll_point, note",
        "from cb_match_event",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    @Results({
        @Result(column="Id", property="id", jdbcType=JdbcType.INTEGER, id=true),
        @Result(column="match_id", property="matchId", jdbcType=JdbcType.BIGINT),
        @Result(column="event_type", property="eventType", jdbcType=JdbcType.INTEGER),
        @Result(column="operator_team", property="operatorTeam", jdbcType=JdbcType.INTEGER),
        @Result(column="beatmap_id", property="beatmapId", jdbcType=JdbcType.BIGINT),
        @Result(column="winner", property="winner", jdbcType=JdbcType.INTEGER),
        @Result(column="player_id", property="playerId", jdbcType=JdbcType.INTEGER),
        @Result(column="score", property="score", jdbcType=JdbcType.BIGINT),
        @Result(column="roll_point", property="rollPoint", jdbcType=JdbcType.INTEGER),
        @Result(column="note", property="note", jdbcType=JdbcType.VARCHAR)
    })
    CbMatchEvent selectByPrimaryKey(Integer id);

    @UpdateProvider(type=CbMatchEventSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(CbMatchEvent record);

    @Update({
        "update cb_match_event",
        "set match_id = #{matchId,jdbcType=BIGINT},",
          "event_type = #{eventType,jdbcType=INTEGER},",
          "operator_team = #{operatorTeam,jdbcType=INTEGER},",
          "beatmap_id = #{beatmapId,jdbcType=BIGINT},",
          "winner = #{winner,jdbcType=INTEGER},",
          "player_id = #{playerId,jdbcType=INTEGER},",
          "score = #{score,jdbcType=BIGINT},",
          "roll_point = #{rollPoint,jdbcType=INTEGER},",
          "note = #{note,jdbcType=VARCHAR}",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int updateByPrimaryKey(CbMatchEvent record);
}