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
import top.mothership.cabbage.pojo.autogen.CbMatchMapPool;

public interface CbMatchMapPoolMapper {
    @Delete({
        "delete from cb_match_map_pool",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int deleteByPrimaryKey(Integer id);

    @Insert({
        "insert into cb_match_map_pool (Id, tournament_id, ",
        "mappool_type)",
        "values (#{id,jdbcType=INTEGER}, #{tournamentId,jdbcType=VARCHAR}, ",
        "#{mappoolType,jdbcType=INTEGER})"
    })
    int insert(CbMatchMapPool record);

    @InsertProvider(type=CbMatchMapPoolSqlProvider.class, method="insertSelective")
    int insertSelective(CbMatchMapPool record);

    @Select({
        "select",
        "Id, tournament_id, mappool_type",
        "from cb_match_map_pool",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    @Results({
        @Result(column="Id", property="id", jdbcType=JdbcType.INTEGER, id=true),
        @Result(column="tournament_id", property="tournamentId", jdbcType=JdbcType.VARCHAR),
        @Result(column="mappool_type", property="mappoolType", jdbcType=JdbcType.INTEGER)
    })
    CbMatchMapPool selectByPrimaryKey(Integer id);

    @UpdateProvider(type=CbMatchMapPoolSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(CbMatchMapPool record);

    @Update({
        "update cb_match_map_pool",
        "set tournament_id = #{tournamentId,jdbcType=VARCHAR},",
          "mappool_type = #{mappoolType,jdbcType=INTEGER}",
        "where Id = #{id,jdbcType=INTEGER}"
    })
    int updateByPrimaryKey(CbMatchMapPool record);
}