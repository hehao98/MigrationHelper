package anonymous.migrationhelper.mapper;

import anonymous.migrationhelper.data.LibraryGroupArtifact;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Created by xxx on 2020/1/3.
 */
@Mapper
public interface LibraryGroupArtifactMapper {

    String tableName = "library_group_artifact";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"` (\n" +
            "                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
            "                                        `group_id` varchar(63) DEFAULT NULL,\n" +
            "                                        `artifact_id` varchar(63) DEFAULT NULL,\n" +
            "                                        `version_extracted` bit(1) NOT NULL,\n" +
            "                                        `parsed` bit(1) NOT NULL,\n" +
            "                                        `parse_error` bit(1) NOT NULL,\n" +
            "                                        PRIMARY KEY (`id`),\n" +
            "                                        UNIQUE KEY `unique` (`group_id`,`artifact_id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" +
            "</script>")
    void createTable();

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(id, group_id, artifact_id) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.id}, #{e.groupId}, #{e.artifactId})" +
            "</foreach> " +
            "</script>")
    int insertWithId(List<LibraryGroupArtifact> entities);

    @Insert("<script>" +
            "insert  ignore into " + tableName + " " +
            "(group_id, artifact_id, version_extracted) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.groupId}, #{e.artifactId}, #{e.versionExtracted})" +
            "</foreach> " +
//            "on duplicate key update id=id" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(List<LibraryGroupArtifact> entities);

    @Update("<script>" +
            "update " + tableName + " set " +
            "version_extracted = #{e.versionExtracted}, " +
            "parsed = #{e.parsed}, " +
            "parse_error = #{e.parseError} " +
            "where id = #{e.id} " +
            "</script>")
    int update(@Param("e") LibraryGroupArtifact entity);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "id = #{id}" +
            "</script>")
    LibraryGroupArtifact findById(
            @Param("id") long id);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "group_id = #{groupId}" +
            "</script>")
    List<LibraryGroupArtifact> findByGroupId(
            @Param("groupId") String groupId);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "artifact_id = #{artifactId}" +
            "</script>")
    List<LibraryGroupArtifact> findByArtifactId(
            @Param("artifactId") String artifactId);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "group_id = #{groupId} and " +
            "artifact_id = #{artifactId}" +
            "</script>")
    LibraryGroupArtifact findByGroupIdAndArtifactId(
            @Param("groupId") String groupId,
            @Param("artifactId") String artifactId);

    @Select("<script>" +
            "select * from " + tableName + " " +
            "</script>")
    List<LibraryGroupArtifact> findAll();
}
