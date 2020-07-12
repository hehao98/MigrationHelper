package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.BlobInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BlobInfoMapper {

    int MAX_TABLE_COUNT = 128;

    String tableName = "blob_info_";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"${tableNum}` (\n" +
            "                           `blob_id` binary(20) NOT NULL,\n" +
            "                           `blob_type` int NOT NULL,\n" +
            "                           `library_signature_ids` mediumblob,\n" +
            "                           `library_version_ids` mediumblob,\n" +
            "                           `library_group_artifact_ids` mediumblob,\n" +
            "                           PRIMARY KEY (`blob_id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=ascii;" +
            "</script>")
    void createTable(@Param("tableNum") int tableNum);

    @Update("<script>" +
            "ALTER TABLE `"+tableName+"${tableNum}` \n" +
            "CHANGE COLUMN `library_signature_ids` `library_signature_ids` MEDIUMBLOB ,\n" +
            "CHANGE COLUMN `library_version_ids` `library_version_ids` MEDIUMBLOB ,\n" +
            "CHANGE COLUMN `library_group_artifact_ids` `library_group_artifact_ids` MEDIUMBLOB ;\n" +
            "</script>")
    void alterTable(@Param("tableNum") int tableNum);

    @Update("<script>" +
            "DROP TABLE `"+tableName+"${tableNum}` \n" +
            "</script>")
    void dropTable(@Param("tableNum") int tableNum);

    @Insert("<script>" +
            "insert into " + tableName + "${tableNum} " +
            "(blob_id, blob_type, library_signature_ids, library_version_ids, library_group_artifact_ids) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.blobId}, #{e.blobType}, #{e.librarySignatureIds}, #{e.libraryVersionIds}, #{e.libraryGroupArtifactIds})" +
            "</foreach> " +
            "on duplicate key update blob_type = values(blob_type), library_signature_ids = values(library_signature_ids), " +
            "library_version_ids = values(library_version_ids), library_group_artifact_ids = values(library_group_artifact_ids)" +
            "</script>")
    int insert(@Param("tableNum") int tableNum, @Param("list") List<BlobInfo> entities);

    @Insert("<script>" +
            "insert into " + tableName + "${tableNum} " +
            "(blob_id, blob_type, library_signature_ids, library_version_ids, library_group_artifact_ids) values " +
            "(#{e.blobId}, #{e.blobType}, #{e.librarySignatureIds}, #{e.libraryVersionIds}, #{e.libraryGroupArtifactIds})" +
            "on duplicate key update blob_type = values(blob_type), library_signature_ids = values(library_signature_ids), " +
            "library_version_ids = values(library_version_ids), library_group_artifact_ids = values(library_group_artifact_ids)" +
            "</script>")
    int insertOne(@Param("tableNum") int tableNum, @Param("e") BlobInfo e);

    @Select("<script>" +
            "select * from " + tableName + "${tableNum} where " +
            "blob_id = unhex(#{blobId})" +
            "</script>")
    BlobInfo findByBlobId(@Param("tableNum") int tableNum, @Param("blobId") String blobId);

    @Select("<script>" +
            "select * from " + tableName + "${tableNum} order by blob_id limit #{offset}, #{limit} " +
            "</script>")
    List<BlobInfo> findList(@Param("tableNum") int tableNum, @Param("offset") long offset, @Param("limit") int limit);
}
