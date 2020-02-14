package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.BlobInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created by xuyul on 2020/2/7.
 */
public interface BlobInfoMapper {

    String tableName = "blob_info";

    @Insert("<script>" +
            "insert or ignore into " + tableName + " " +
            "(blob_id, blob_type, library_signature_ids, library_version_ids) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.blobId}, #{e.blobType}, #{e.librarySignatureIds}, #{e.libraryVersionIds})" +
            "</foreach> " +
//            "on duplicate key update blob_id = blob_id" +
            "</script>")
    int insert(List<BlobInfo> entities);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "blob_id = #{blobId}" +
            "</script>")
    BlobInfo findByBlobId(@Param("blobId") String blobId);
}
