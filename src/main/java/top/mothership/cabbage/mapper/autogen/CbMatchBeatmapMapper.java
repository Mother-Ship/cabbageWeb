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
import top.mothership.cabbage.pojo.autogen.CbMatchBeatmap;

public interface CbMatchBeatmapMapper {
    @Delete({
        "delete from cb_match_beatmap",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int deleteByPrimaryKey(Integer id);

    @Insert({
        "insert into cb_match_beatmap (Id, mod, ",
        "beatmap_id, mappool_id)",
        "values (#{id,jdbcType=INTEGER}, #{mod,jdbcType=INTEGER}, ",
        "#{beatmapId,jdbcType=INTEGER}, #{mappoolId,jdbcType=BIGINT})"
    })
    int insert(CbMatchBeatmap record);

    @InsertProvider(type=CbMatchBeatmapSqlProvider.class, method="insertSelective")
    int insertSelective(CbMatchBeatmap record);

    @Select({
        "select",
        "Id, mod, beatmap_id, mappool_id",
        "from cb_match_beatmap",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    @Results({
        @Result(column="Id", property="id", jdbcType=JdbcType.INTEGER, id=true),
        @Result(column="mod", property="mod", jdbcType=JdbcType.INTEGER),
        @Result(column="beatmap_id", property="beatmapId", jdbcType=JdbcType.INTEGER),
        @Result(column="mappool_id", property="mappoolId", jdbcType=JdbcType.BIGINT)
    })
    CbMatchBeatmap selectByPrimaryKey(Integer id);

    @UpdateProvider(type=CbMatchBeatmapSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(CbMatchBeatmap record);

    @Update({
        "update cb_match_beatmap",
        "set mod = #{mod,jdbcType=INTEGER},",
          "beatmap_id = #{beatmapId,jdbcType=INTEGER},",
          "mappool_id = #{mappoolId,jdbcType=BIGINT}",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int updateByPrimaryKey(CbMatchBeatmap record);
}