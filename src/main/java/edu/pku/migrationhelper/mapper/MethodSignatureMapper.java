package edu.pku.migrationhelper.mapper;

import edu.pku.migrationhelper.data.api.MethodSignatureOld;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MethodSignatureMapper {

    int MAX_ID_BIT = 35;

    int MAX_TABLE_COUNT = 128;

    String tableName = "method_signature_";

    @Update("<script>" +
            "CREATE TABLE `"+tableName+"${tableNum}` (\n" +
            "                                  `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
            "                                  `package_name` varchar(255) NOT NULL,\n" +
            "                                  `class_name` varchar(255) NOT NULL,\n" +
            "                                  `method_name` varchar(255) NOT NULL,\n" +
            "                                  `param_list` varchar(2047) NOT NULL,\n" +
            "                                  PRIMARY KEY (`id`),\n" +
            "                                  UNIQUE KEY `unique` (`package_name`,`class_name`,`method_name`,`param_list`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=ascii;" +
            "</script>")
    void createTable(@Param("tableNum") int tableNum);

    @Update("<script>" +
            "ALTER TABLE `"+tableName+"${tableNum}` auto_increment = #{ai};" +
            "</script>")
    void setAutoIncrement(@Param("tableNum") int tableNum, @Param("ai") long ai);

    @Insert("<script>" +
            "insert  ignore into " + tableName + "${tableNum} " +
            "(package_name, class_name, method_name, param_list) values " +
            "<foreach collection='list' item='e' separator=','>" +
            "(#{e.packageName}, #{e.className}, #{e.methodName}, #{e.paramList})" +
            "</foreach> " +
//            "on duplicate key update id=id" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(@Param("tableNum") int tableNum, @Param("list") List<MethodSignatureOld> entities);

    @Insert("<script>" +
            "insert into " + tableName + "${tableNum} " +
            "(package_name, class_name, method_name, param_list) values " +
            "(#{e.packageName}, #{e.className}, #{e.methodName}, #{e.paramList})" +
//            "on duplicate key update id=id" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "e.id", keyColumn = "id")
    int insertOne(@Param("tableNum") int tableNum, @Param("e") MethodSignatureOld entity);

    @Select("<script>" +
            "select * from " + tableName + "${tableNum} where " +
            "package_name = #{packageName} and " +
            "class_name = #{className} and " +
            "method_name = #{methodName} and " +
            "param_list = #{paramList} " +
            "</script>")
    MethodSignatureOld findOne(
            @Param("tableNum") int tableNum,
            @Param("packageName") String packageName,
            @Param("className") String className,
            @Param("methodName") String methodName,
            @Param("paramList") String paramList);

    @Select("<script>" +
            "select * from " + tableName + "${tableNum} where " +
            "package_name = #{packageName} and " +
            "class_name = #{className} and " +
            "method_name = #{methodName} " +
            "</script>")
    List<MethodSignatureOld> findList(
            @Param("tableNum") int tableNum,
            @Param("packageName") String packageName,
            @Param("className") String className,
            @Param("methodName") String methodName);

    @Select("<script>" +
            "select id from " + tableName + "${tableNum} where " +
            "package_name = #{packageName} and " +
            "class_name = #{className} and " +
            "method_name = #{methodName} and " +
            "param_list = #{paramList} " +
            "</script>")
    Long findId(
            @Param("tableNum") int tableNum,
            @Param("packageName") String packageName,
            @Param("className") String className,
            @Param("methodName") String methodName,
            @Param("paramList") String paramList);

    @Select("<script>" +
            "select id from " + tableName + "${tableNum} where " +
            "package_name = #{packageName} and " +
            "class_name = #{className} and " +
            "method_name = #{methodName} " +
            "</script>")
    List<Long> findIds(
            @Param("tableNum") int tableNum,
            @Param("packageName") String packageName,
            @Param("className") String className,
            @Param("methodName") String methodName);

    @Select("<script>" +
            "select * from " + tableName + "${tableNum} where " +
            "id = #{signatureId} " +
            "</script>")
    MethodSignatureOld findById(
            @Param("tableNum") int tableNum,
            @Param("signatureId") long signatureId);

    /**
     * Be careful, this will only return signatureIds in one table
     * We recommend to use MapperUtil.getMethodSignatureByIds() instead
     */
    @Select("<script>" +
            "select * from " + tableName + "${tableNum} where " +
            "<choose>" +
            "<when test = 'signatureIds == null || signatureIds.size() == 0'> false </when>" +
            "<otherwise> id in (<foreach collection='signatureIds' item='e' separator=','>#{e}</foreach>) </otherwise>" +
            "</choose>" +
            "</script>")
    List<MethodSignatureOld> findByIds(
            @Param("tableNum") int tableNum,
            @Param("signatureIds") Collection<Long> signatureIds);
}
