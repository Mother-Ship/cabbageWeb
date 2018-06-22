package top.mothership.cabbage.mapper.autogen;

import org.apache.ibatis.jdbc.SQL;
import top.mothership.cabbage.pojo.autogen.CbMatchTeam;

public class CbMatchTeamSqlProvider {

    public String insertSelective(CbMatchTeam record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("cb_match_team");
        
        if (record.getId() != null) {
            sql.VALUES("Id", "#{id,jdbcType=INTEGER}");
        }
        
        if (record.getTournamentId() != null) {
            sql.VALUES("tournament_id", "#{tournamentId,jdbcType=BIGINT}");
        }
        
        if (record.getTeamName() != null) {
            sql.VALUES("team_name", "#{teamName,jdbcType=VARCHAR}");
        }
        
        if (record.getTeamLeader() != null) {
            sql.VALUES("team_leader", "#{teamLeader,jdbcType=INTEGER}");
        }
        
        if (record.getTeamPoint() != null) {
            sql.VALUES("team_point", "#{teamPoint,jdbcType=INTEGER}");
        }
        
        if (record.getTeamToken() != null) {
            sql.VALUES("team_token", "#{teamToken,jdbcType=REAL}");
        }
        
        if (record.getTeamAvatar() != null) {
            sql.VALUES("team_avatar", "#{teamAvatar,jdbcType=LONGVARBINARY}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(CbMatchTeam record) {
        SQL sql = new SQL();
        sql.UPDATE("cb_match_team");
        
        if (record.getTournamentId() != null) {
            sql.SET("tournament_id = #{tournamentId,jdbcType=BIGINT}");
        }
        
        if (record.getTeamName() != null) {
            sql.SET("team_name = #{teamName,jdbcType=VARCHAR}");
        }
        
        if (record.getTeamLeader() != null) {
            sql.SET("team_leader = #{teamLeader,jdbcType=INTEGER}");
        }
        
        if (record.getTeamPoint() != null) {
            sql.SET("team_point = #{teamPoint,jdbcType=INTEGER}");
        }
        
        if (record.getTeamToken() != null) {
            sql.SET("team_token = #{teamToken,jdbcType=REAL}");
        }
        
        if (record.getTeamAvatar() != null) {
            sql.SET("team_avatar = #{teamAvatar,jdbcType=LONGVARBINARY}");
        }
        
        sql.WHERE("Id = #{id,jdbcType=INTEGER}");
        
        return sql.toString();
    }
}