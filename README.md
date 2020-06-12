# MigrationHelper

## 整体流程

### 数据挖掘阶段

- CreateTableJob

  创建MySQL数据表

- LibrariesIoImportJob

  将LibrariesIO的库数据导入数据库

- LioJarParseJob

  从Maven下载之前导入的LibrariesIO库数据并分析，构建库与API签名映射关系

- WocRepoAnalysisJob

  在WoC上分析一系列Java仓库，获得API替换关系、依赖变更等数据

### 数据导出为文件阶段

- DataExportJob： APISupport

  导出APISupport指标计算所需数据

- DataExportJob： RepositoryDepSeq

  导出依赖变更序列

- DataExportJob： GroundTruth

  根据原始的GroundTruth手动标记文件，导出GroundTruth

### 库推荐阶段

- LibraryRecommendJob

  使用数据导出阶段的APISupport和RepositoryDepSeq，根据文件输入的依赖库列表，进行替代库推荐

