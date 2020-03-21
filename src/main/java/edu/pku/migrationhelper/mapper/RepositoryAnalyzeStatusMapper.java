package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.RepositoryAnalyzeStatus;
import org.apache.ibatis.annotations.*;

@Mapper
public interface RepositoryAnalyzeStatusMapper {

    String tableName = "repository_analyze_status";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"` (\n" +
            "                                             `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
            "                                             `repo_type` varchar(15) DEFAULT NULL,\n" +
            "                                             `repo_name` varchar(255) DEFAULT NULL,\n" +
            "                                             `start_time` datetime DEFAULT NULL,\n" +
            "                                             `end_time` datetime DEFAULT NULL,\n" +
            "                                             `analyze_status` varchar(15) DEFAULT NULL,\n" +
            "                                             PRIMARY KEY (`id`),\n" +
            "                                             UNIQUE KEY `index2` (`repo_type`,`repo_name`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;" +
            "</script>")
    void createTable();

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(repo_type, repo_name, start_time, end_time, analyze_status) values " +
            "(#{e.repoType}, #{e.repoName}, #{e.startTime}, #{e.endTime}, #{e.analyzeStatus})" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(@Param("e") RepositoryAnalyzeStatus entity);

    @Update("<script>" +
            "update " + tableName + " set " +
            "start_time = #{e.startTime}, " +
            "end_time = #{e.endTime}, " +
            "analyze_status = #{e.analyzeStatus} " +
            "where id = #{e.id} " +
            "</script>")
    int update(@Param("e") RepositoryAnalyzeStatus entity);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "repo_type = #{repoType} and " +
            "repo_name = #{repoName}" +
            "</script>")
    RepositoryAnalyzeStatus findByRepoTypeAndRepoName(
            @Param("repoType") RepositoryAnalyzeStatus.RepoType repoType,
            @Param("repoName") String repoName);
}
