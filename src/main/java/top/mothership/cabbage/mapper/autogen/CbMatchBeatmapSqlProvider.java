package top.mothership.cabbage.mapper.autogen;

import org.apache.ibatis.jdbc.SQL;
import top.mothership.cabbage.pojo.autogen.CbMatchBeatmap;

public class CbMatchBeatmapSqlProvider {

    public String insertSelective(CbMatchBeatmap record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("cb_match_beatmap");
        
        if (record.getId() != null) {
            sql.VALUES("Id", "#{id,jdbcType=INTEGER}");
        }
        
        if (record.getMod() != null) {
            sql.VALUES("mod", "#{mod,jdbcType=INTEGER}");
        }
        
        if (record.getBeatmapId() != null) {
            sql.VALUES("beatmap_id", "#{beatmapId,jdbcType=INTEGER}");
        }
        
        if (record.getMappoolId() != null) {
            sql.VALUES("mappool_id", "#{mappoolId,jdbcType=BIGINT}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(CbMatchBeatmap record) {
        SQL sql = new SQL();
        sql.UPDATE("cb_match_beatmap");
        
        if (record.getMod() != null) {
            sql.SET("mod = #{mod,jdbcType=INTEGER}");
        }
        
        if (record.getBeatmapId() != null) {
            sql.SET("beatmap_id = #{beatmapId,jdbcType=INTEGER}");
        }
        
        if (record.getMappoolId() != null) {
            sql.SET("mappool_id = #{mappoolId,jdbcType=BIGINT}");
        }
        
        sql.WHERE("Id = #{id,jdbcType=INTEGER}");
        
        return sql.toString();
    }
}