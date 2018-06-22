package top.mothership.cabbage.mapper.autogen;

import org.apache.ibatis.jdbc.SQL;
import top.mothership.cabbage.pojo.autogen.CbMatchEvent;

public class CbMatchEventSqlProvider {

    public String insertSelective(CbMatchEvent record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("cb_match_event");
        
        if (record.getId() != null) {
            sql.VALUES("Id", "#{id,jdbcType=INTEGER}");
        }
        
        if (record.getMatchId() != null) {
            sql.VALUES("match_id", "#{matchId,jdbcType=BIGINT}");
        }
        
        if (record.getEventType() != null) {
            sql.VALUES("event_type", "#{eventType,jdbcType=INTEGER}");
        }
        
        if (record.getOperatorTeam() != null) {
            sql.VALUES("operator_team", "#{operatorTeam,jdbcType=INTEGER}");
        }
        
        if (record.getBeatmapId() != null) {
            sql.VALUES("beatmap_id", "#{beatmapId,jdbcType=BIGINT}");
        }
        
        if (record.getWinner() != null) {
            sql.VALUES("winner", "#{winner,jdbcType=INTEGER}");
        }
        
        if (record.getPlayerId() != null) {
            sql.VALUES("player_id", "#{playerId,jdbcType=INTEGER}");
        }
        
        if (record.getScore() != null) {
            sql.VALUES("score", "#{score,jdbcType=BIGINT}");
        }
        
        if (record.getRollPoint() != null) {
            sql.VALUES("roll_point", "#{rollPoint,jdbcType=INTEGER}");
        }
        
        if (record.getNote() != null) {
            sql.VALUES("note", "#{note,jdbcType=VARCHAR}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(CbMatchEvent record) {
        SQL sql = new SQL();
        sql.UPDATE("cb_match_event");
        
        if (record.getMatchId() != null) {
            sql.SET("match_id = #{matchId,jdbcType=BIGINT}");
        }
        
        if (record.getEventType() != null) {
            sql.SET("event_type = #{eventType,jdbcType=INTEGER}");
        }
        
        if (record.getOperatorTeam() != null) {
            sql.SET("operator_team = #{operatorTeam,jdbcType=INTEGER}");
        }
        
        if (record.getBeatmapId() != null) {
            sql.SET("beatmap_id = #{beatmapId,jdbcType=BIGINT}");
        }
        
        if (record.getWinner() != null) {
            sql.SET("winner = #{winner,jdbcType=INTEGER}");
        }
        
        if (record.getPlayerId() != null) {
            sql.SET("player_id = #{playerId,jdbcType=INTEGER}");
        }
        
        if (record.getScore() != null) {
            sql.SET("score = #{score,jdbcType=BIGINT}");
        }
        
        if (record.getRollPoint() != null) {
            sql.SET("roll_point = #{rollPoint,jdbcType=INTEGER}");
        }
        
        if (record.getNote() != null) {
            sql.SET("note = #{note,jdbcType=VARCHAR}");
        }
        
        sql.WHERE("Id = #{id,jdbcType=INTEGER}");
        
        return sql.toString();
    }
}