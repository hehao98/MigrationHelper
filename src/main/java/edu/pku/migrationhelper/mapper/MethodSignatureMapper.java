package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.MethodSignature;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MethodSignatureMapper {

    String tableName = "method_signature";

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(package_name, class_name, method_name, param_list) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.packageName}, #{e.className}, #{e.methodName}, #{e.paramList})" +
            "</foreach> " +
            "on duplicate key update id=id" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(List<MethodSignature> entities);

    @Insert("<script>" +
            "insert into " + tableName + " " +
            "(package_name, class_name, method_name, param_list) values " +
            "(#{e.packageName}, #{e.className}, #{e.methodName}, #{e.paramList})" +
            "on duplicate key update id=id" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "e.id", keyColumn = "id")
    int insertOne(@Param("e") MethodSignature entity);

    @Select("<script>" +
            "select * from " + tableName + " where " +
            "package_name = #{packageName} and " +
            "class_name = #{className} and " +
            "method_name = #{methodName} and " +
            "param_list = #{paramList} " +
            "</script>")
    MethodSignature findOne(
            @Param("packageName") String packageName,
            @Param("className") String className,
            @Param("methodName") String methodName,
            @Param("paramList") String paramList);

    @Select("<script>" +
            "select id from " + tableName + " where " +
            "package_name = #{packageName} and " +
            "class_name = #{className} and " +
            "method_name = #{methodName} and " +
            "param_list = #{paramList} " +
            "</script>")
    Long findId(
            @Param("packageName") String packageName,
            @Param("className") String className,
            @Param("methodName") String methodName,
            @Param("paramList") String paramList);
}
