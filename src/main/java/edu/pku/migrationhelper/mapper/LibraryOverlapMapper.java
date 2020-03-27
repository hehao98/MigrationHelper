package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.LibraryOverlap;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

@Mapper
public interface LibraryOverlapMapper {

    String tableName = "library_overlap";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"` (\n" +
            "                                   `group_artifact_id1` bigint(20) NOT NULL,\n" +
            "                                   `group_artifact_id2` bigint(20) NOT NULL,\n" +
            "                                   `signature_count` int(11) NOT NULL,\n" +
            "                                   PRIMARY KEY (`group_artifact_id1`,`group_artifact_id2`),\n" +
            "                                   KEY `ga2` (`group_artifact_id2`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=ascii;" +
            "</script>")
    void createTable();

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(group_artifact_id1, group_artifact_id2, signature_count) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.groupArtifactId1}, #{e.groupArtifactId2}, #{e.signatureCount})" +
            "</foreach> " +
            "on duplicate key update signature_count = signature_count + values(signature_count)" +
            "</script>")
    int insert(@Param("list") Collection<LibraryOverlap> list);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "group_artifact_id1 = #{id} or group_artifact_id2 = #{id}" +
            "</script>")
    List<LibraryOverlap> findByGroupArtifactId(@Param("id") long id);

    @Select("<script>" +
            "select * from " + tableName + " " +
            "</script>")
    List<LibraryOverlap> findAll();
}
