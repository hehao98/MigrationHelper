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
            "insert or ignore into " + tableName + " " +
            "(group_artifact_id, version, downloaded, parsed) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.groupArtifactId}, #{e.version}, #{e.downloaded}, #{e.parsed})" +
            "</foreach> " +
//            "on duplicate key update id=id" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(List<LibraryVersion> entities);

    @Update("<script>" +
            "update " + tableName + " set " +
            "downloaded = #{e.downloaded}, parsed = #{e.parsed} " +
            "where id = #{e.id} " +
            "</script>")
    int update(@Param("e") LibraryVersion entity);

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
}
