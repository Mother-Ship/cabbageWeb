package top.mothership.cabbage.mapper.autogen;

import org.apache.ibatis.jdbc.SQL;
import top.mothership.cabbage.pojo.autogen.CbTournament;

public class CbTournamentSqlProvider {

    public String insertSelective(CbTournament record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("cb_tournament");
        
        if (record.getId() != null) {
            sql.VALUES("Id", "#{id,jdbcType=INTEGER}");
        }
        
        if (record.getHost() != null) {
            sql.VALUES("host", "#{host,jdbcType=VARCHAR}");
        }
        
        if (record.getHelper() != null) {
            sql.VALUES("helper", "#{helper,jdbcType=VARCHAR}");
        }
        
        if (record.getRule() != null) {
            sql.VALUES("rule", "#{rule,jdbcType=INTEGER}");
        }
        
        if (record.getRegisterStartTime() != null) {
            sql.VALUES("register_start_time", "#{registerStartTime,jdbcType=DATE}");
        }
        
        if (record.getRegisterEndTime() != null) {
            sql.VALUES("register_end_time", "#{registerEndTime,jdbcType=DATE}");
        }
        
        if (record.getTeamStartTime() != null) {
            sql.VALUES("team_start_time", "#{teamStartTime,jdbcType=DATE}");
        }
        
        if (record.getTeamEndTime() != null) {
            sql.VALUES("team_end_time", "#{teamEndTime,jdbcType=DATE}");
        }
        
        if (record.getPlayerCount() != null) {
            sql.VALUES("player_count", "#{playerCount,jdbcType=INTEGER}");
        }
        
        if (record.getAllPlayerCount() != null) {
            sql.VALUES("all_player_count", "#{allPlayerCount,jdbcType=INTEGER}");
        }
        
        if (record.getIntroduction() != null) {
            sql.VALUES("introduction", "#{introduction,jdbcType=VARCHAR}");
        }
        
        if (record.getEnded() != null) {
            sql.VALUES("ended", "#{ended,jdbcType=BIT}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(CbTournament record) {
        SQL sql = new SQL();
        sql.UPDATE("cb_tournament");
        
        if (record.getHost() != null) {
            sql.SET("host = #{host,jdbcType=VARCHAR}");
        }
        
        if (record.getHelper() != null) {
            sql.SET("helper = #{helper,jdbcType=VARCHAR}");
        }
        
        if (record.getRule() != null) {
            sql.SET("rule = #{rule,jdbcType=INTEGER}");
        }
        
        if (record.getRegisterStartTime() != null) {
            sql.SET("register_start_time = #{registerStartTime,jdbcType=DATE}");
        }
        
        if (record.getRegisterEndTime() != null) {
            sql.SET("register_end_time = #{registerEndTime,jdbcType=DATE}");
        }
        
        if (record.getTeamStartTime() != null) {
            sql.SET("team_start_time = #{teamStartTime,jdbcType=DATE}");
        }
        
        if (record.getTeamEndTime() != null) {
            sql.SET("team_end_time = #{teamEndTime,jdbcType=DATE}");
        }
        
        if (record.getPlayerCount() != null) {
            sql.SET("player_count = #{playerCount,jdbcType=INTEGER}");
        }
        
        if (record.getAllPlayerCount() != null) {
            sql.SET("all_player_count = #{allPlayerCount,jdbcType=INTEGER}");
        }
        
        if (record.getIntroduction() != null) {
            sql.SET("introduction = #{introduction,jdbcType=VARCHAR}");
        }
        
        if (record.getEnded() != null) {
            sql.SET("ended = #{ended,jdbcType=BIT}");
        }
        
        sql.WHERE("Id = #{id,jdbcType=INTEGER}");
        
        return sql.toString();
    }
}