package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.LibraryGroupArtifact;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Created by xuyul on 2020/1/3.
 */
@Mapper
public interface LibraryGroupArtifactMapper {

    String tableName = "library_group_artifact";

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(group_id, artifact_id, version_extracted) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.groupId}, #{e.artifactId}, #{e.versionExtracted})" +
            "</foreach> " +
            "on duplicate key update id=id" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(List<LibraryGroupArtifact> entities);

    @Update("<script>" +
            "update " + tableName + " set " +
            "version_extracted = #{e.versionExtracted} " +
            "where id = #{e.id} " +
            "</script>")
    int update(@Param("e") LibraryGroupArtifact entity);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "group_id = #{groupId}" +
            "</script>")
    List<LibraryGroupArtifact> findByGroupId(
            @Param("groupId") String groupId);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "group_id = #{groupId} and " +
            "artifact_id = #{artifactId}" +
            "</script>")
    LibraryGroupArtifact findByGroupIdAndArtifactId(
            @Param("groupId") String groupId,
            @Param("artifactId") String artifactId);
}
