package top.mothership.cabbage.mapper.autogen;

import org.apache.ibatis.jdbc.SQL;
import top.mothership.cabbage.pojo.autogen.CbMatch;

public class CbMatchSqlProvider {

    public String insertSelective(CbMatch record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("cb_match");
        
        if (record.getId() != null) {
            sql.VALUES("Id", "#{id,jdbcType=INTEGER}");
        }
        
        if (record.getTournamentId() != null) {
            sql.VALUES("tournament_id", "#{tournamentId,jdbcType=BIGINT}");
        }
        
        if (record.getBlueTeamId() != null) {
            sql.VALUES("blue_team_id", "#{blueTeamId,jdbcType=BIGINT}");
        }
        
        if (record.getRedTeamId() != null) {
            sql.VALUES("red_team_id", "#{redTeamId,jdbcType=BIGINT}");
        }
        
        if (record.getBlueTeamScore() != null) {
            sql.VALUES("blue_team_score", "#{blueTeamScore,jdbcType=INTEGER}");
        }
        
        if (record.getRedTeamScore() != null) {
            sql.VALUES("red_team_score", "#{redTeamScore,jdbcType=INTEGER}");
        }
        
        if (record.getMatchType() != null) {
            sql.VALUES("match_type", "#{matchType,jdbcType=INTEGER}");
        }
        
        if (record.getMappoolId() != null) {
            sql.VALUES("mappool_id", "#{mappoolId,jdbcType=BIGINT}");
        }
        
        if (record.getLiveAddress() != null) {
            sql.VALUES("live_address", "#{liveAddress,jdbcType=VARCHAR}");
        }
        
        if (record.getRecordAddress() != null) {
            sql.VALUES("record_address", "#{recordAddress,jdbcType=VARCHAR}");
        }
        
        if (record.getStartTime() != null) {
            sql.VALUES("start_time", "#{startTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getTimeConfirmed() != null) {
            sql.VALUES("time_confirmed", "#{timeConfirmed,jdbcType=BIT}");
        }
        
        if (record.getMpLink() != null) {
            sql.VALUES("mp_link", "#{mpLink,jdbcType=VARCHAR}");
        }
        
        if (record.getReferee() != null) {
            sql.VALUES("referee", "#{referee,jdbcType=INTEGER}");
        }
        
        if (record.getEnded() != null) {
            sql.VALUES("ended", "#{ended,jdbcType=BIT}");
        }
        
        if (record.getNote() != null) {
            sql.VALUES("note", "#{note,jdbcType=VARCHAR}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(CbMatch record) {
        SQL sql = new SQL();
        sql.UPDATE("cb_match");
        
        if (record.getTournamentId() != null) {
            sql.SET("tournament_id = #{tournamentId,jdbcType=BIGINT}");
        }
        
        if (record.getBlueTeamId() != null) {
            sql.SET("blue_team_id = #{blueTeamId,jdbcType=BIGINT}");
        }
        
        if (record.getRedTeamId() != null) {
            sql.SET("red_team_id = #{redTeamId,jdbcType=BIGINT}");
        }
        
        if (record.getBlueTeamScore() != null) {
            sql.SET("blue_team_score = #{blueTeamScore,jdbcType=INTEGER}");
        }
        
        if (record.getRedTeamScore() != null) {
            sql.SET("red_team_score = #{redTeamScore,jdbcType=INTEGER}");
        }
        
        if (record.getMatchType() != null) {
            sql.SET("match_type = #{matchType,jdbcType=INTEGER}");
        }
        
        if (record.getMappoolId() != null) {
            sql.SET("mappool_id = #{mappoolId,jdbcType=BIGINT}");
        }
        
        if (record.getLiveAddress() != null) {
            sql.SET("live_address = #{liveAddress,jdbcType=VARCHAR}");
        }
        
        if (record.getRecordAddress() != null) {
            sql.SET("record_address = #{recordAddress,jdbcType=VARCHAR}");
        }
        
        if (record.getStartTime() != null) {
            sql.SET("start_time = #{startTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getTimeConfirmed() != null) {
            sql.SET("time_confirmed = #{timeConfirmed,jdbcType=BIT}");
        }
        
        if (record.getMpLink() != null) {
            sql.SET("mp_link = #{mpLink,jdbcType=VARCHAR}");
        }
        
        if (record.getReferee() != null) {
            sql.SET("referee = #{referee,jdbcType=INTEGER}");
        }
        
        if (record.getEnded() != null) {
            sql.SET("ended = #{ended,jdbcType=BIT}");
        }
        
        if (record.getNote() != null) {
            sql.SET("note = #{note,jdbcType=VARCHAR}");
        }
        
        sql.WHERE("Id = #{id,jdbcType=INTEGER}");
        
        return sql.toString();
    }
}