package top.mothership.cabbage.mapper.autogen;

import org.apache.ibatis.jdbc.SQL;
import top.mothership.cabbage.pojo.autogen.CbMatchMapPool;

public class CbMatchMapPoolSqlProvider {

    public String insertSelective(CbMatchMapPool record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("cb_match_map_pool");
        
        if (record.getId() != null) {
            sql.VALUES("Id", "#{id,jdbcType=INTEGER}");
        }
        
        if (record.getTournamentId() != null) {
            sql.VALUES("tournament_id", "#{tournamentId,jdbcType=VARCHAR}");
        }
        
        if (record.getMappoolType() != null) {
            sql.VALUES("mappool_type", "#{mappoolType,jdbcType=INTEGER}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(CbMatchMapPool record) {
        SQL sql = new SQL();
        sql.UPDATE("cb_match_map_pool");
        
        if (record.getTournamentId() != null) {
            sql.SET("tournament_id = #{tournamentId,jdbcType=VARCHAR}");
        }
        
        if (record.getMappoolType() != null) {
            sql.SET("mappool_type = #{mappoolType,jdbcType=INTEGER}");
        }
        
        sql.WHERE("Id = #{id,jdbcType=INTEGER}");
        
        return sql.toString();
    }
}