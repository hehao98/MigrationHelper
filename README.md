# MigrationHelper

## 整体流程

### 数据挖掘阶段

- CreateTableJob

  创建MySQL数据表

- LibrariesIoImportJob

  将LibrariesIO的库数据导入数据库

- LioJarParseJob

  从Maven下载之前导入的LibrariesIO库数据并分析，构建库与API签名映射关系

  ```shell script
  bash +x ./run-woc.sh LioJarParseJob -Xms160g -Xmx160g -XX:+UseG1GC -XX:ParallelGCThreads=8 \
        -XX:ConcGCThreads=4 -XX:MaxGCPauseMillis=600000 -XX:+UnlockExperimentalVMOptions \
        -XX:G1NewSizePercent=20 -XX:G1MaxNewSizePercent=20
  ```

- WocRepoAnalysisJob

  在WoC上分析一系列Java仓库，获得API替换关系、依赖变更等数据

### 数据导出为文件阶段

- DataExportJob: LibraryGroupArtifact
  
  ```shell script
  bash -x ./run-woc.sh DataExportJob LibraryGroupArtifact export/GroupArtifact.csv
  ```

- DataExportJob: APIMapping

  导出库与API的映射关系（仅用于测试）
  
  ```shell script
  bash -x ./run-woc.sh DataExportJob APIMapping export/Lib.csv export/API.csv test_data/test-lib-input.txt
  ```

- DataExportJob： APISupport

  导出APISupport指标计算所需数据

- DataExportJob： RepositoryDepSeq

  导出依赖变更序列

- DataExportJob： GroundTruth

  根据原始的GroundTruth手动标记文件，导出GroundTruth
  
  ```shell script
  bash -x ./run-woc.sh DataExportJob APISupport export/APISupport.csv
  bash -x ./run-woc.sh DataExportJob RepositoryDepSeq export/RepositoryDepSeq.csv
  bash -x ./run-woc.sh DataExportJob GroundTruth test_data/ground_truth.csv \
     test_data/rules-2014-raw.csv test_data/rules-2014-artifactList.csv
  ```

### 库推荐阶段

- LibraryRecommendJob

  使用数据导出阶段的APISupport和RepositoryDepSeq，根据文件输入的依赖库列表，进行替代库推荐
  
  ```shell script
  bash -x ./run-woc.sh LibraryRecommendJob
  ```

