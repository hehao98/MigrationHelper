package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.LibrarySignatureMap;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LibrarySignatureMapMapper {

    String tableName = "library_signature_map";

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(library_version_id, method_signature_id) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.libraryVersionId}, #{e.methodSignatureId})" +
            "</foreach> " +
            "on duplicate key update library_version_id=library_version_id" +
            "</script>")
    int insert(List<LibrarySignatureMap> entities);
}
