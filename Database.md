# MySQL表结构文档

## 特殊存储方式说明

### 分表存储

分表存储时，表名带有表编号的数字后缀"_${tableNum}"，tableNum从0开始

例如method_signature表分128表存储，有效的表名是method_signature_0 到 method_signature_127

分表存储时，primary key内编码了表编号，可以直接将其右移35位得到所在表编号

例如某个method_signature的id，id >> 35 == 0x7F，那么我们可以在method_signature_127表中找到该id对应数据。

其他查询方式参考表相关文档说明

### BER数字列表压缩方式

部分存储在mediumblob类型中的数字列表，采用了BER的压缩方式，该方式与WoC中采用的数字列表压缩方式相同，参考文档：

Perl BER unpacking

Format definition: from http://perldoc.perl.org/functions/pack.html

(see "w" template description)

同时可参考 oscar.py 项目 oscar.py 文件的 unber(s) 函数的实现，以及本项目的 edu.pku.migrationhelper.util.MathUtils 的 unberNumberList 函数的实现。同时本项目还有反向的实现函数 berNumberList

## 库标识符映射相关表

### library_group_artifact
Library信息

包含索引：

UNIQUE KEY (group_id,artifact_id)

| Field | Type | Description |
| ----- | ---- | ----------- |
| id | bigint | primary key |
| group_id | varchar(63) | Maven中的groupId |
| artifact_id | varchar(63) | Maven中的artifactId |
| version_extracted | bit(1) | 是否已获取所有版本号并存入library_version表中 |
| parsed | bit(1) | 是否已完成jar包分析 |
| parse_error | bit(1) | jar包分析中是否包含错误 |

### library_version 

Library版本信息

包含索引：

UNIQUE KEY (group_artifact_id,version)

| Field | Type | Description |
| ----- | ---- | ----------- |
| id | bigint | primary key |
| group_artifact_id | bigint | 该版本属于的库，关联library_group_artifact表中的id |
| version | varchar(63) | Maven中的version |
| downloaded | bit(1) | 是否已下载，暂时无用 |
| parsed | bit(1) | 是否已完成jar包分析 |
| parse_error | bit(1) | jar包分析中是否包含错误 |

### method_signature_${tableNum}

代码标识符信息，分表存储，共128表

通过id查询时，表编号查询方法见“分表存储”说明。

通过unique key查询时，至少需要package_name和class_name才可确定表编号，将package_name和class_name如下拼接后，通过 FNV1A_32 哈希，再按位与 0x7F 即可得到表编号。此方法与WoC中按文本（author、project等）查询时的分表方法一致，可参考WoC的perl实现与perl文档。本项目的FNV1A_32实现来自com.twitter:util-hashing_2.13

```Java
public static int getMethodSignatureSliceKey(String packageName, String className) {
    String key = packageName + ":" + className;
    return (int)(KeyHasher.FNV1A_32().hashKey(key.getBytes()) & 0x7F;
}
```

包含索引：

UNIQUE KEY (package_name,class_name,method_name,param_list)

| Field        | Type          | Description |
| ------------ | ------------- | ----------- |
| id           | bigint        | primary key |
| package_name | varchar(255)  | 包名        |
| class_name   | varchar(255)  | 类名        |
| method_name  | varchar(255)  | 方法名      |
| param_list   | varchar(2047) | 参数列表    |

### library_signature_to_version_${tableNum}

代码标识符到Library版本的映射，分表存储，共128表

| Field              | Type       | Description                                               |
| ------------------ | ---------- | --------------------------------------------------------- |
| signature_id       | bigint     | primary key，关联method_signature的id                     |
| version_ids        | mediumblob | 包含该标识符的所有library_version的id列表，ber压缩        |
| group_artifact_ids | mediumblob | 包含该标识符的所有library_group_artifact的id列表，ber压缩 |

### library_version_to_signature 

Library版本到代码标识符的映射

| Field         | Type       | Description                                     |
| ------------- | ---------- | ----------------------------------------------- |
| version_id    | bigint     | primary key，关联library_version的id            |
| signature_ids | mediumblob | 该库版本包含的method_signature的id列表，ber压缩 |
