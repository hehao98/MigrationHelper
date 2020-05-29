package anonymous.migrationhelper.mapper;

import anonymous.migrationhelper.data.RepositoryDepSeq;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RepositoryDepSeqMapper {

    String tableName = "repository_dep_seq";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"` (\n" +
            "                           `id` bigint(20) NOT NULL,\n" +
            "                           `repo_name` varchar(255),\n" +
            "                           `pom_only` mediumblob,\n" +
            "                           `pom_only_commits` mediumblob,\n" +
            "                           `code_with_dup` mediumblob,\n" +
            "                           `code_without_dup` mediumblob,\n" +
            "                           `pom_with_code_del` mediumblob,\n" +
            "                           `pom_with_code_add` mediumblob,\n" +
            "                           PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=ascii;" +
            "</script>")
    void createTable();

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(id, repo_name, pom_only, pom_only_commits, code_with_dup, code_without_dup, pom_with_code_del, pom_with_code_add) values " +
            "(#{e.id}, #{e.repoName}, #{e.pomOnly}, #{e.pomOnlyCommits}, #{e.codeWithDup}, #{e.codeWithoutDup}, #{e.pomWithCodeDel}, #{e.pomWithCodeAdd}) " +
            "on duplicate key update " +
            "repo_name = values(repo_name), " +
            "pom_only = values(pom_only), " +
            "pom_only_commits = values(pom_only_commits), " +
            "code_with_dup = values(code_with_dup), " +
            "code_without_dup = values(code_without_dup), " +
            "pom_with_code_del = values(pom_with_code_del), " +
            "pom_with_code_add = values(pom_with_code_add) " +
            "</script>")
    int insert(@Param("e") RepositoryDepSeq entity);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "id = #{id}" +
            "</script>")
    RepositoryDepSeq findById(@Param("id") long id);

    @Select("<script>" +
            "select * from " + tableName + "" +
            "</script>")
    List<RepositoryDepSeq> findAll();
}
