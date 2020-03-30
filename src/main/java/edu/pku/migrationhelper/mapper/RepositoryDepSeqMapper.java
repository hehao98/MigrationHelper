package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.RepositoryDepSeq;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RepositoryDepSeqMapper {

    String tableName = "repository_dep_seq";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"` (\n" +
            "                           `id` bigint(20) NOT NULL,\n" +
            "                           `pom_only` mediumblob,\n" +
            "                           `code_with_dup` mediumblob,\n" +
            "                           `code_without_dup` mediumblob,\n" +
            "                           PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=ascii;" +
            "</script>")
    void createTable();

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(id, pom_only, code_with_dup, code_without_dup) values " +
            "(#{e.id}, #{e.pomOnly}, #{e.codeWithDup}, #{e.codeWithoutDup}) " +
            "on duplicate key update " +
            "pom_only = values(pom_only), " +
            "code_with_dup = values(code_with_dup), " +
            "code_without_dup = values(code_without_dup) " +
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
