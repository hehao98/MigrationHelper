package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.CommitInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created by xuyul on 2020/2/7.
 */
public interface CommitInfoMapper {

    String tableName = "commit_info";

    @Insert("<script>" +
            "insert  ignore into " + tableName + " " +
            "(commit_id, code_library_version_ids, pom_library_version_ids) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.commitId}, #{e.codeLibraryVersionIds}, #{e.pomLibraryVersionIds})" +
            "</foreach> " +
//            "on duplicate key update commit_id = commit_id" +
            "</script>")
    int insert(List<CommitInfo> entities);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "commit_id = #{commitId}" +
            "</script>")
    CommitInfo findByCommitId(@Param("commitId") String commitId);
}
