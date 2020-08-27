# Deliverable

这个文件夹包含了此合作项目第一阶段交付件：

1. 库迁移研究现状调研报告
2. 大规模库迁移实例数据集

## 库迁移研究现状调研报告

报告参见`report.pdf`，此报告的LaTeX源码参见`report/`文件夹

## 大规模库迁移实例数据集

本数据集是当前库替换推荐工具的输出结果，为了便于使用，包含两种不同的数据集

1. 对华为重点关心的库（`libs.csv`）进行推荐的数据集
2. 对Maven Central Repository上我们能抓取的所有库（`libs-all.csv`，约18万个库）进行推荐的数据集

为了获得所需的数据，需在World of Code da1服务器`/da1_data/play/heh/MigrationHelper`执行如下命令

```shell script
bash -x run-woc.sh LibraryRecommendJob -q deliverable/libs.csv -o deliverable/recommendation-libs.csv -r deliverable/commits-libs.csv -e > deliverable/libs.log
bash -x run-woc.sh LibraryRecommendJob -q deliverable/libs-all.csv -o deliverable/recommendation-libs-all.csv -r deliverable/commits-libs-all.csv -e > deliverable/libs-all.log
```

对于`libs.csv`和`lib-all.csv`，推荐算法都会输出库推荐结果和其相应的Commit列表，前者取`minPatternSupport=26`，保证准确率；后者取`minPatternSupport=8`，保证召回率。
对于`libs.csv`，替换库推荐结果位于`recommendation-libs.csv`中，相关开源仓库Commit位于`commits-libs.csv`中，程序运行的日志在`libs.log`中。
对于`libs-all.csv`，替换库推荐结果位于`recommendation-libs-all.csv`中，相关开源仓库Commit位于`commits-libs-all.csv`中，程序运行的日志在`libs-all.log`中。

由于两种数据集的格式相同，下面仅以`libs.csv`对应的数据集为例介绍其数据格式。
`recommendation-libs.csv`包含了推荐的不同的替换库，及其相关的指标信息。其中，`isCorrect`列，TRUE表示替换关系经人工验证正确，FALSE表示正确性未知。
`commits-libs.csv`包含了疑似出现过库替换的开源项目名称及其对应的Commit，每一个库替换关系占一行，按逗号分隔格式如下：`fromLibrary,toLibrary,Project1,Project2,...,`。其中，每一个`Projectn`是一个`;`号分割的三元组`projectName;firstCommitId;lastCommitId`。`projectName`包含GitHub用户/组织名和项目名，用`_`号分割。
`libs.log`包含了推荐工具运行时的日志信息，包括汇报缺失数据和评测准确率的日志等。

目前作为初步交付件的算法输出结果还存在误报率高，对热门库以外的库结果不好等问题，尚待后续改进。
