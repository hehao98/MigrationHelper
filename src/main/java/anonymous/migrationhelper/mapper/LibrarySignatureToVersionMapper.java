package anonymous.migrationhelper.mapper;

import anonymous.migrationhelper.data.LibrarySignatureToVersion;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * Created by xxx on 2020/2/28.
 */
public interface LibrarySignatureToVersionMapper {

    String tableName = "library_signature_to_version_";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"${tableNum}` (\n" +
            "                                                   `signature_id` bigint(20) NOT NULL,\n" +
            "                                                   `version_ids` mediumblob,\n" +
            "                                                   `group_artifact_ids` mediumblob,\n" +
            "                                                   PRIMARY KEY (`signature_id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=ascii;" +
            "</script>")
    void createTable(@Param("tableNum") int tableNum);

    @Insert("<script>" +
            "insert into " + tableName + "${tableNum} " +
            "(signature_id, version_ids, group_artifact_ids) values " +
            "(#{e.signatureId}, #{e.versionIds}, #{e.groupArtifactIds})" +
            "on duplicate key update version_ids = values(version_ids), group_artifact_ids = values(group_artifact_ids)" +
            "</script>")
    int insertOne(@Param("tableNum") int tableNum, @Param("e") LibrarySignatureToVersion entity);

    @Select("<script>" +
            "select * from " + tableName + "${tableNum} where " +
            "signature_id = #{signatureId}" +
            "</script>")
    LibrarySignatureToVersion findById(@Param("tableNum") int tableNum, @Param("signatureId") long signatureId);

    @Select("<script>" +
            "select signature_id, group_artifact_ids from " + tableName + "${tableNum} where " +
            "<choose>" +
            "<when test = 'idIn == null || idIn.size() == 0'> false </when>" +
            "<otherwise> signature_id in (<foreach collection='idIn' item='e' separator=','>#{e}</foreach>) </otherwise>" +
            "</choose>" +
            "</script>")
    List<LibrarySignatureToVersion> findGroupArtifactByIdIn(
            @Param("tableNum") int tableNum,
            @Param("idIn") Collection<Long> signatureIds);

    @Select("<script>" +
            "select * from " + tableName + "${tableNum} limit #{offset}, #{limit} " +
            "</script>")
    List<LibrarySignatureToVersion> findList(@Param("tableNum") int tableNum, @Param("offset") long offset, @Param("limit") int limit);
}
