package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.CommitInfo;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

/**
 * Created by xuyul on 2020/2/7.
 */
@Mapper
public interface CommitInfoMapper {

    int MAX_TABLE_COUNT = 128;

    String tableName = "commit_info_";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"${tableNum}` (\n" +
            "                             `commit_id` binary(20) NOT NULL,\n" +
            "                             `code_library_version_ids` mediumblob,\n" +
            "                             `code_group_artifact_ids` mediumblob,\n" +
            "                             `code_delete_group_artifact_ids` mediumblob,\n" +
            "                             `code_add_group_artifact_ids` mediumblob,\n" +
            "                             `pom_library_version_ids` mediumblob,\n" +
            "                             `pom_group_artifact_ids` mediumblob,\n" +
            "                             `pom_delete_group_artifact_ids` mediumblob,\n" +
            "                             `pom_add_group_artifact_ids` mediumblob,\n" +
            "                             `method_change_ids` mediumblob,\n" +
            "                             PRIMARY KEY (`commit_id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=ascii;" +
            "</script>")
    void createTable(@Param("tableNum") int tableNum);

    @Update("<script>" +
            "ALTER TABLE `"+tableName+"${tableNum}` \n" +
            "CHANGE COLUMN `code_library_version_ids` `code_library_version_ids` MEDIUMBLOB ,\n" +
            "CHANGE COLUMN `code_group_artifact_ids` `code_group_artifact_ids` MEDIUMBLOB ,\n" +
            "CHANGE COLUMN `code_delete_group_artifact_ids` `code_delete_group_artifact_ids` MEDIUMBLOB ,\n" +
            "CHANGE COLUMN `code_add_group_artifact_ids` `code_add_group_artifact_ids` MEDIUMBLOB ,\n" +
            "CHANGE COLUMN `pom_library_version_ids` `pom_library_version_ids` MEDIUMBLOB ,\n" +
            "CHANGE COLUMN `pom_group_artifact_ids` `pom_group_artifact_ids` MEDIUMBLOB ,\n" +
            "CHANGE COLUMN `pom_delete_group_artifact_ids` `pom_delete_group_artifact_ids` MEDIUMBLOB ,\n" +
            "CHANGE COLUMN `pom_add_group_artifact_ids` `pom_add_group_artifact_ids` MEDIUMBLOB ,\n" +
            "CHANGE COLUMN `method_change_ids` `method_change_ids` MEDIUMBLOB ;\n" +
            "</script>")
    void alterTable(@Param("tableNum") int tableNum);

    @Update("<script>" +
            "DROP TABLE `"+tableName+"${tableNum}` \n" +
            "</script>")
    void dropTable(@Param("tableNum") int tableNum);

    @Insert("<script>" +
            "insert into " + tableName + "${tableNum} " +
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
    int insert(@Param("tableNum") int tableNum, @Param("list") List<CommitInfo> entities);

    @Insert("<script>" +
            "insert into " + tableName + "${tableNum} " +
            "(commit_id, method_change_ids, " +
            "code_library_version_ids, code_group_artifact_ids, code_delete_group_artifact_ids, code_add_group_artifact_ids, " +
            "pom_library_version_ids, pom_group_artifact_ids, pom_delete_group_artifact_ids, pom_add_group_artifact_ids) values " +
            "(#{e.commitId}, #{e.methodChangeIds}, " +
            "#{e.codeLibraryVersionIds}, #{e.codeGroupArtifactIds}, #{e.codeDeleteGroupArtifactIds}, #{e.codeAddGroupArtifactIds}, " +
            "#{e.pomLibraryVersionIds}, #{e.pomGroupArtifactIds}, #{e.pomDeleteGroupArtifactIds}, #{e.pomAddGroupArtifactIds})" +
            "on duplicate key update method_change_ids = values(method_change_ids), " +
            "code_library_version_ids = values(code_library_version_ids), code_group_artifact_ids = values(code_group_artifact_ids), " +
            "code_delete_group_artifact_ids = values(code_delete_group_artifact_ids), code_add_group_artifact_ids = values(code_add_group_artifact_ids), " +
            "pom_library_version_ids = values(pom_library_version_ids), pom_group_artifact_ids = values(pom_group_artifact_ids), " +
            "pom_delete_group_artifact_ids = values(pom_delete_group_artifact_ids), pom_add_group_artifact_ids = values(pom_add_group_artifact_ids)" +
            "</script>")
    int insertOne(@Param("tableNum") int tableNum, @Param("e") CommitInfo e);

    @Select("<script>" +
            "select * from " + tableName + "${tableNum} where " +
            "commit_id = unhex(#{commitId})" +
            "</script>")
    CommitInfo findByCommitId(@Param("tableNum") int tableNum, @Param("commitId") String commitId);

    @Select("<script>" +
            "select * from " + tableName + "${tableNum} order by commit_id limit #{offset}, #{limit} " +
            "</script>")
    List<CommitInfo> findList(@Param("tableNum") int tableNum, @Param("offset") long offset, @Param("limit") int limit);

    @Select("<script>" +
            "select count(*) from " + tableName + "${tableNum} where " +
            "<choose>" +
            "<when test = 'idIn == null || idIn.size() == 0'> false </when>" +
            "<otherwise> commit_id in (<foreach collection='idIn' item='e' separator=','>unhex(#{e})</foreach>) </otherwise>" +
            "</choose>" +
            "</script>")
    Long countIdIn(@Param("tableNum") int tableNum, @Param("idIn") Collection<String> idIn);
}
