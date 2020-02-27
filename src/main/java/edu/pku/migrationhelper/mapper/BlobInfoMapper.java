package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.BlobInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created by xuyul on 2020/2/7.
 */
@Mapper
public interface BlobInfoMapper {

    String tableName = "blob_info";

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(blob_id, blob_type, library_signature_ids, library_version_ids, library_group_artifact_ids) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.blobId}, #{e.blobType}, #{e.librarySignatureIds}, #{e.libraryVersionIds}, #{e.libraryGroupArtifactIds})" +
            "</foreach> " +
            "on duplicate key update blob_type = values(blob_type), library_signature_ids = values(library_signature_ids), " +
            "library_version_ids = values(library_version_ids), library_group_artifact_ids = values(library_group_artifact_ids)" +
            "</script>")
    int insert(List<BlobInfo> entities);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "blob_id = #{blobId}" +
            "</script>")
    BlobInfo findByBlobId(@Param("blobId") String blobId);

    @Select("<script>" +
            "select * from " + tableName + " order by blob_id limit #{offset}, #{limit} " +
            "</script>")
    List<BlobInfo> findList(@Param("offset") long offset, @Param("limit") int limit);
}
