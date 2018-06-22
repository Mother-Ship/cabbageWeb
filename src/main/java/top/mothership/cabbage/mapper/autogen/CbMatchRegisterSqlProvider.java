package top.mothership.cabbage.mapper.autogen;

import org.apache.ibatis.jdbc.SQL;
import top.mothership.cabbage.pojo.autogen.CbMatchRegister;

public class CbMatchRegisterSqlProvider {

    public String insertSelective(CbMatchRegister record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("cb_match_register");
        
        if (record.getId() != null) {
            sql.VALUES("Id", "#{id,jdbcType=INTEGER}");
        }
        
        if (record.getTournamentId() != null) {
            sql.VALUES("tournament_id", "#{tournamentId,jdbcType=BIGINT}");
        }
        
        if (record.getPlayerId() != null) {
            sql.VALUES("player_id", "#{playerId,jdbcType=INTEGER}");
        }
        
        if (record.getTeamId() != null) {
            sql.VALUES("team_id", "#{teamId,jdbcType=INTEGER}");
        }
        
        if (record.getPpAtRegister() != null) {
            sql.VALUES("pp_at_register", "#{ppAtRegister,jdbcType=INTEGER}");
        }
        
        if (record.getCostAtRegister() != null) {
            sql.VALUES("cost_at_register", "#{costAtRegister,jdbcType=INTEGER}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(CbMatchRegister record) {
        SQL sql = new SQL();
        sql.UPDATE("cb_match_register");
        
        if (record.getTournamentId() != null) {
            sql.SET("tournament_id = #{tournamentId,jdbcType=BIGINT}");
        }
        
        if (record.getPlayerId() != null) {
            sql.SET("player_id = #{playerId,jdbcType=INTEGER}");
        }
        
        if (record.getTeamId() != null) {
            sql.SET("team_id = #{teamId,jdbcType=INTEGER}");
        }
        
        if (record.getPpAtRegister() != null) {
            sql.SET("pp_at_register = #{ppAtRegister,jdbcType=INTEGER}");
        }
        
        if (record.getCostAtRegister() != null) {
            sql.SET("cost_at_register = #{costAtRegister,jdbcType=INTEGER}");
        }
        
        sql.WHERE("Id = #{id,jdbcType=INTEGER}");
        
        return sql.toString();
    }
}