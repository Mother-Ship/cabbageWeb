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
import top.mothership.cabbage.pojo.autogen.CbMatchRegister;

public interface CbMatchRegisterMapper {
    @Delete({
        "delete from cb_match_register",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int deleteByPrimaryKey(Integer id);

    @Insert({
        "insert into cb_match_register (Id, tournament_id, ",
        "player_id, team_id, ",
        "pp_at_register, cost_at_register)",
        "values (#{id,jdbcType=INTEGER}, #{tournamentId,jdbcType=BIGINT}, ",
        "#{playerId,jdbcType=INTEGER}, #{teamId,jdbcType=INTEGER}, ",
        "#{ppAtRegister,jdbcType=INTEGER}, #{costAtRegister,jdbcType=INTEGER})"
    })
    int insert(CbMatchRegister record);

    @InsertProvider(type=CbMatchRegisterSqlProvider.class, method="insertSelective")
    int insertSelective(CbMatchRegister record);

    @Select({
        "select",
        "Id, tournament_id, player_id, team_id, pp_at_register, cost_at_register",
        "from cb_match_register",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    @Results({
        @Result(column="Id", property="id", jdbcType=JdbcType.INTEGER, id=true),
        @Result(column="tournament_id", property="tournamentId", jdbcType=JdbcType.BIGINT),
        @Result(column="player_id", property="playerId", jdbcType=JdbcType.INTEGER),
        @Result(column="team_id", property="teamId", jdbcType=JdbcType.INTEGER),
        @Result(column="pp_at_register", property="ppAtRegister", jdbcType=JdbcType.INTEGER),
        @Result(column="cost_at_register", property="costAtRegister", jdbcType=JdbcType.INTEGER)
    })
    CbMatchRegister selectByPrimaryKey(Integer id);

    @UpdateProvider(type=CbMatchRegisterSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(CbMatchRegister record);

    @Update({
        "update cb_match_register",
        "set tournament_id = #{tournamentId,jdbcType=BIGINT},",
          "player_id = #{playerId,jdbcType=INTEGER},",
          "team_id = #{teamId,jdbcType=INTEGER},",
          "pp_at_register = #{ppAtRegister,jdbcType=INTEGER},",
          "cost_at_register = #{costAtRegister,jdbcType=INTEGER}",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int updateByPrimaryKey(CbMatchRegister record);
}