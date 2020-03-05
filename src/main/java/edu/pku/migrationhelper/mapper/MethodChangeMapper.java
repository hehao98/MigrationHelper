package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.MethodChange;
import org.apache.ibatis.annotations.*;

/**
 * Created by xuyul on 2020/3/5.
 */
@Mapper
public interface MethodChangeMapper {

    int MAX_ID_BIT = 35;

    int MAX_TABLE_COUNT = 128;

    String tableName = "method_change_";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"${tableNum}` (\n" +
            "                               `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
            "                               `delete_signature_ids` varbinary(255) NOT NULL,\n" +
            "                               `add_signature_ids` varbinary(255) NOT NULL,\n" +
            "                               `delete_group_artifact_ids` varbinary(255) NOT NULL,\n" +
            "                               `add_group_artifact_ids` varbinary(255) NOT NULL,\n" +
            "                               `counter` bigint(20) NOT NULL,\n" +
            "                               PRIMARY KEY (`id`),\n" +
            "                               UNIQUE KEY `unique_index` (`delete_signature_ids`,`add_signature_ids`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=ascii;" +
            "</script>")
    void createTable(@Param("tableNum") int tableNum);

    @Update("<script>" +
            "ALTER TABLE `"+tableName+"${tableNum}` auto_increment = #{ai};" +
            "</script>")
    void setAutoIncrement(@Param("tableNum") int tableNum, @Param("ai") long ai);

    @Insert("<script>" +
            "insert into " + tableName + "${tableNum} " +
            "(delete_signature_ids, add_signature_ids, delete_group_artifact_ids, add_group_artifact_ids, counter) values " +
            "(#{e.deleteSignatureIds}, #{e.addSignatureIds}, #{e.deleteGroupArtifactIds}, #{e.addGroupArtifactIds}, #{e.counter})" +
            "on duplicate key update counter = counter + values(counter)" +
            "</script>")
    int insertOne(@Param("tableNum") int tableNum, @Param("e") MethodChange e);

    @Select("<script>" +
            "select id from " + tableName + "${tableNum} where " +
            "delete_signature_ids = #{deleteSignatureIds} and add_signature_ids = #{addSignatureIds}" +
            "</script>")
    Long findId(@Param("tableNum") int tableNum, @Param("deleteSignatureIds") byte[] deleteSignatureIds, @Param("addSignatureIds") byte[] addSignatureIds);

    @Select("<script>" +
            "select * from " + tableName + "${tableNum} where " +
            "id = #{id}" +
            "</script>")
    MethodChange findById(@Param("tableNum") int tableNum, @Param("id") long id);
}
