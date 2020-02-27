package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.CommitInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
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

    @Select("<script>" +
            "select * from " + tableName + " order by commit_id limit #{offset}, #{limit} " +
            "</script>")
    List<CommitInfo> findList(@Param("offset") long offset, @Param("limit") int limit);

    @Select("<script>" +
            "select count(*) from " + tableName + " where " +
            "<choose>" +
            "<when test = 'idIn == null || idIn.size() == 0'> false </when>" +
            "<otherwise> commit_id in (<foreach collection='idIn' item='e' separator=','>#{e}</foreach>) </otherwise>" +
            "</choose>" +
            "</script>")
    Long countIdIn(@Param("idIn") Collection<String> idIn);
}
