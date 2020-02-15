package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.LibrarySignatureMap;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface LibrarySignatureMapMapper {

    String tableName = "library_signature_map";

    @Insert("<script>" +
            "insert  ignore into " + tableName + " " +
            "(library_version_id, method_signature_id) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.libraryVersionId}, #{e.methodSignatureId})" +
            "</foreach> " +
//            "on duplicate key update library_version_id=library_version_id" +
            "</script>")
    int insert(List<LibrarySignatureMap> entities);

    @Select("<script>" +
            "select library_version_id from " + tableName + " where " +
            "<choose>" +
            "<when test = 'signatureIds == null || signatureIds.size() == 0'> false </when>" +
            "<otherwise> method_signature_id in (<foreach collection='signatureIds' item='e' separator=','>#{e}</foreach>) </otherwise>" +
            "</choose>" +
            "</script>")
    List<Long> findVersionIds(@Param("signatureIds") Collection<Long> signatureIds);
}
