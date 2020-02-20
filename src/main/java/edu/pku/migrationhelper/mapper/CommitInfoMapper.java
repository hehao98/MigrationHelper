package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.CommitInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created by xuyul on 2020/2/7.
 */
@Mapper
public interface CommitInfoMapper {

    String tableName = "commit_info";

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(commit_id, method_change_ids, " +
            "code_library_version_ids, code_group_artifact_ids, code_delete_group_artifact_ids, code_add_group_artifact_ids, " +
            "pom_library_version_ids, pom_group_artifact_ids, pom_delete_group_artifact_ids, pom_add_group_artifact_ids) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.commitId}, #{e.methodChangeIds}, " +
            "#{e.codeLibraryVersionIds}, #{e.codeGroupArtifactIds}, #{e.codeDeleteGroupArtifactIds}, #{e.codeAddGroupArtifactIds}, " +
            "#{e.pomLibraryVersionIds}, #{e.pomGroupArtifactIds}, #{e.pomDeleteGroupArtifactIds}, #{e.pomAddGroupArtifactIds})" +
            "</foreach> " +
            "on duplicate key update method_change_ids = values(method_change_ids), " +
            "code_library_version_ids = values(code_library_version_ids), code_group_artifact_ids = values(code_group_artifact_ids), " +
            "code_delete_group_artifact_ids = values(code_delete_group_artifact_ids), code_add_group_artifact_ids = values(code_add_group_artifact_ids), " +
            "pom_library_version_ids = values(pom_library_version_ids), pom_group_artifact_ids = values(pom_group_artifact_ids), " +
            "pom_delete_group_artifact_ids = values(pom_delete_group_artifact_ids), pom_add_group_artifact_ids = values(pom_add_group_artifact_ids)" +
            "</script>")
    int insert(List<CommitInfo> entities);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "commit_id = #{commitId}" +
            "</script>")
    CommitInfo findByCommitId(@Param("commitId") String commitId);
}
