package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.lib.LibraryVersion;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

@Mapper
@Deprecated
public interface LibraryVersionMapper {

    String tableName = "library_version";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"` (\n" +
            "                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
            "                                 `group_artifact_id` bigint(20) NOT NULL,\n" +
            "                                 `version` varchar(63) DEFAULT NULL,\n" +
            "                                 `downloaded` bit(1) NOT NULL,\n" +
            "                                 `parsed` bit(1) NOT NULL,\n" +
            "                                 `parse_error` bit(1) NOT NULL,\n" +
            "                                 PRIMARY KEY (`id`),\n" +
            "                                 UNIQUE KEY `unique` (`group_artifact_id`,`version`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" +
            "</script>")
    void createTable();

    @Update("<script>" +
            "DROP TABLE `"+tableName+"` \n" +
            "</script>")
    void dropTable();

    @Insert("<script>" +
            "insert  ignore into " + tableName + " " +
            "(group_artifact_id, version, downloaded, parsed, parse_error) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.groupArtifactId}, #{e.version}, #{e.downloaded}, #{e.parsed}, #{e.parseError})" +
            "</foreach> " +
//            "on duplicate key update id=id" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(List<LibraryVersion> entities);

    @Update("<script>" +
            "update " + tableName + " set " +
            "downloaded = #{e.downloaded}, parsed = #{e.parsed}, parse_error = #{e.parseError} " +
            "where id = #{e.id} " +
            "</script>")
    int update(@Param("e") LibraryVersion entity);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "id = #{id} " +
            "</script>")
    LibraryVersion findById(
            @Param("id") long id);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "group_artifact_id = #{groupArtifactId} " +
            "</script>")
    List<LibraryVersion> findByGroupArtifactId(
            @Param("groupArtifactId") long groupArtifactId);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "group_artifact_id = #{groupArtifactId} and " +
            "version = #{version}" +
            "</script>")
    LibraryVersion findByGroupArtifactIdAndVersion(
            @Param("groupArtifactId") long groupArtifactId,
            @Param("version") String version);

    @Select("<script>" +
            "select v.* from " + tableName + " as v join " + LibraryGroupArtifactMapper.tableName + " as ga " +
            "on v.group_artifact_id = ga.id where " +
            "ga.group_id = #{groupId} and " +
            "ga.artifact_id = #{artifactId} and " +
            "v.version = #{version}" +
            "</script>")
    LibraryVersion findByGroupIdAndArtifactIdAndVersion(
            @Param("groupId") String groupId,
            @Param("artifactId") String artifactId,
            @Param("version") String version);

    @Select("<script>" +
            "select group_artifact_id from " + tableName + " where " +
            "<choose>" +
            "<when test = 'versionIds == null || versionIds.size() == 0'> false </when>" +
            "<otherwise> id in (<foreach collection='versionIds' item='e' separator=','>#{e}</foreach>) </otherwise>" +
            "</choose>" +
            "</script>")
    List<Long> findGroupArtifactIds(@Param("versionIds") Collection<Long> versionIds);

    public static class CountData {
        public long groupArtifactId;
        public long count;
    }

    @Select("<script>" +
            "select group_artifact_id, count(*) as `count` from " + tableName + " group by group_artifact_id " +
            "</script>")
    List<CountData> countByGroupArtifact();
}
