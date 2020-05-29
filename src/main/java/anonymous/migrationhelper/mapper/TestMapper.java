package anonymous.migrationhelper.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Created by xuyul on 2020/2/27.
 */
@Mapper
public interface TestMapper {

    String methodSignatureTable = "method_signature";

    String signatureMapTable = "library_signature_map";

    String s2vb = "library_signature_to_version_blob";

    String v2sb = "library_version_to_signature_blob";

    String s2vj = "library_signature_to_version_json";

    String v2sj = "library_version_to_signature_json";

    @Select("<script>" +
            "select id from " + methodSignatureTable +
            "</script>")
    List<Long> findAllSignatureIds();

    @Select("<script>" +
            "select library_version_id from " + signatureMapTable + " where " +
            "method_signature_id = #{signatureId}" +
            "</script>")
    List<Long> findVersionIdsBySignatureId(@Param("signatureId") long signatureId);

    @Insert("<script>" +
            "insert into " + s2vb + " " +
            "(signature_id, version_ids) values " +
            "(#{signatureId}, #{b})" +
            "</script>")
    int insertS2VB(@Param("signatureId") long signatureId, @Param("b") byte[] blob);

    @Insert("<script>" +
            "insert into " + v2sb + " " +
            "(version_id, signature_ids) values " +
            "(#{versionId}, #{b})" +
            "</script>")
    int insertV2SB(@Param("versionId") long versionId, @Param("b") byte[] blob);

    @Insert("<script>" +
            "insert into " + s2vj + " " +
            "(signature_id, version_ids) values " +
            "(#{signatureId}, #{j})" +
            "</script>")
    int insertS2VJ(@Param("signatureId") long signatureId, @Param("j") String json);

    @Insert("<script>" +
            "insert into " + v2sj + " " +
            "(version_id, signature_ids) values " +
            "(#{versionId}, #{j})" +
            "</script>")
    int insertV2SJ(@Param("versionId") long versionId, @Param("j") String json);
}
