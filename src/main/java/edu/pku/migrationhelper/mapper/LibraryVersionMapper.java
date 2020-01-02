package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.LibraryVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Created by xuyul on 2020/1/2.
 */
@Mapper
public interface LibraryVersionMapper {

    String tableName = "library_version";

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(group_id, artifact_id, version) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.groupId}, #{e.artifactId}, #{e.version})" +
            "</foreach>" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(List<LibraryVersion> entities);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "group_id = #{groupId} and " +
            "artifact_id = #{artifactId}" +
            "</script>")
    List<LibraryVersion> findByGroupIdAndArtifactId(
            @Param("groupId") String groupId,
            @Param("artifactId") String artifactId);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "group_id = #{groupId} and " +
            "artifact_id = #{artifactId} and " +
            "version = #{version}" +
            "</script>")
    LibraryVersion findByGroupIdAndArtifactIdAndVersion(
            @Param("groupId") String groupId,
            @Param("artifactId") String artifactId,
            @Param("version") String version);
}
