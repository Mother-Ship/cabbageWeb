package top.mothership.cabbage.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface ResDAO {


    @Insert("INSERT INTO `osufile` VALUES (null,#{bid},#{filedata})")
    Integer addOsuFile(@Param("bid")Integer bid,@Param("filedata") String filedata);

    @Select("SELECT `filedata` FROM osufile WHERE `bid` = #{bid} ")
    String getOsuFileBybid(@Param("bid")Integer bid);



    @Insert("INSERT INTO `bgfile` VALUES (null,#{sid},#{name},#{data})")
    Integer addBG(@Param("sid")Integer sid, @Param("name") String name,@Param("data") byte[] data);

    @Select("SELECT `data` FROM `bgfile` WHERE `sid` = #{sid} AND `name` = #{name} ")
    //这里似乎不能用byte[]？
    Object getBGBySidAndName(@Param("sid")Integer sid,@Param("name")String name);



    @Select("SELECT `name`,`data` FROM `resource`")
    List<Map<String,Object>> getResource();

    //使用MYSQL特有语法ON DUPLICATE KEY 节省代码量，name必须为unique索引
    //干掉报错，换成replace，同样name必须有unique索引
    @Insert("REPLACE INTO `resource` VALUES (null,#{name},#{data})")
    Integer addResource(@Param("name")String name ,@Param("data") byte[] data);

}