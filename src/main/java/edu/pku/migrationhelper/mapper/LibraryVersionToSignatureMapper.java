package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.lib.LibraryVersionToSignature;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * Created by xuyul on 2020/2/28.
 */
@Deprecated
public interface LibraryVersionToSignatureMapper {

    String tableName = "library_version_to_signature";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"` (\n" +
            "                                                   `version_id` bigint(20) NOT NULL,\n" +
            "                                                   `signature_ids` mediumblob,\n" +
            "                                                   PRIMARY KEY (`version_id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=ascii;" +
            "</script>")
    void createTable();

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(version_id, signature_ids) values " +
            "(#{e.versionId}, #{e.signatureIds})" +
            "on duplicate key update signature_ids = values(signature_ids)" +
            "</script>")
    int insertOne(@Param("e") LibraryVersionToSignature entity);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "version_id = #{versionId}" +
            "</script>")
    LibraryVersionToSignature findById(@Param("versionId") long versionId);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "<choose>" +
            "<when test = 'versionIds == null || versionIds.size() == 0'> false </when>" +
            "<otherwise> version_id in (<foreach collection='versionIds' item='e' separator=','>#{e}</foreach>) </otherwise>" +
            "</choose>" +
            "</script>")
    List<LibraryVersionToSignature> findByIdIn(@Param("versionIds") Collection<Long> versionIds);
}
