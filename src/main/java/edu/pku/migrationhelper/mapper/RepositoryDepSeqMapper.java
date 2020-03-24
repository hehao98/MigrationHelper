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
            "                           `dep_seq` mediumblob,\n" +
            "                           PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=ascii;" +
            "</script>")
    void createTable();

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(id, dep_seq) values " +
            "(#{e.id}, #{e.depSeq}) " +
            "on duplicate key update dep_seq = values(dep_seq)" +
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
