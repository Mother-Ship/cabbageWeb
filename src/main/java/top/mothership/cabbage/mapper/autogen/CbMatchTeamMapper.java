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
import top.mothership.cabbage.pojo.autogen.CbMatchTeam;

public interface CbMatchTeamMapper {
    @Delete({
        "delete from cb_match_team",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int deleteByPrimaryKey(Integer id);

    @Insert({
        "insert into cb_match_team (Id, tournament_id, ",
        "team_name, team_leader, ",
        "team_point, team_token, ",
        "team_avatar)",
        "values (#{id,jdbcType=INTEGER}, #{tournamentId,jdbcType=BIGINT}, ",
        "#{teamName,jdbcType=VARCHAR}, #{teamLeader,jdbcType=INTEGER}, ",
        "#{teamPoint,jdbcType=INTEGER}, #{teamToken,jdbcType=REAL}, ",
        "#{teamAvatar,jdbcType=LONGVARBINARY})"
    })
    int insert(CbMatchTeam record);

    @InsertProvider(type=CbMatchTeamSqlProvider.class, method="insertSelective")
    int insertSelective(CbMatchTeam record);

    @Select({
        "select",
        "Id, tournament_id, team_name, team_leader, team_point, team_token, team_avatar",
        "from cb_match_team",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    @Results({
        @Result(column="Id", property="id", jdbcType=JdbcType.INTEGER, id=true),
        @Result(column="tournament_id", property="tournamentId", jdbcType=JdbcType.BIGINT),
        @Result(column="team_name", property="teamName", jdbcType=JdbcType.VARCHAR),
        @Result(column="team_leader", property="teamLeader", jdbcType=JdbcType.INTEGER),
        @Result(column="team_point", property="teamPoint", jdbcType=JdbcType.INTEGER),
        @Result(column="team_token", property="teamToken", jdbcType=JdbcType.REAL),
        @Result(column="team_avatar", property="teamAvatar", jdbcType=JdbcType.LONGVARBINARY)
    })
    CbMatchTeam selectByPrimaryKey(Integer id);

    @UpdateProvider(type=CbMatchTeamSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(CbMatchTeam record);

    @Update({
        "update cb_match_team",
        "set tournament_id = #{tournamentId,jdbcType=BIGINT},",
          "team_name = #{teamName,jdbcType=VARCHAR},",
          "team_leader = #{teamLeader,jdbcType=INTEGER},",
          "team_point = #{teamPoint,jdbcType=INTEGER},",
          "team_token = #{teamToken,jdbcType=REAL},",
          "team_avatar = #{teamAvatar,jdbcType=LONGVARBINARY}",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int updateByPrimaryKeyWithBLOBs(CbMatchTeam record);

    @Update({
        "update cb_match_team",
        "set tournament_id = #{tournamentId,jdbcType=BIGINT},",
          "team_name = #{teamName,jdbcType=VARCHAR},",
          "team_leader = #{teamLeader,jdbcType=INTEGER},",
          "team_point = #{teamPoint,jdbcType=INTEGER},",
          "team_token = #{teamToken,jdbcType=REAL}",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int updateByPrimaryKey(CbMatchTeam record);
}