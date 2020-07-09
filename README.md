# MigrationHelper

## Compile

### Local

First, install JDK 1.8 and Maven. Use `mvn clean package` to compile the whole project into a runnable JAR file.

### Remote on World of Code

We rely on [this](https://github.com/hehao98/MigrationHelperJAR) GitHub repository for uploading JAR to remote servers 
(as direct connection is very slow). 
With push access to the above repository, you can use this to compile and upload JAR.

```shell script
bash +x ./compile_upload.sh
```

Then, on one of the World of Code servers, clone this repository and clone the JAR repository in this repository,
before executing any job for this tool.

```shell script
git clone https://github.com/hehao98/MigrationHelper.git
cd MigrationHelper
git clone https://github.com/hehao98/MigrationHelperJAR.git
```

## Test

We have only a limited number of unit tests (also deadly simple and not rigorous) which can be run as follows. 

```shell script
mvn clean test
```

(What a pity on our poor software engineering practices!) 
More patches that do rigorous testing of any module is always welcome.

We also rely on logging and exception handling for debugging. 

## Usage Instructions

In this section, we detail on how to run this tool either locally or remotely on World of Code servers.

We have two utility scripts for executing our tool: `run-local.sh` is for running locally,
 and `run-woc.sh` is for running on any of the World of Code servers. 
However, some jobs may not work properly without access to blob database, so we strongly recommend running jobs on da4.

For local usage, you need to have a running MySQL server with the configurations specified 
 in `src/main/resources/application-local.yaml`

### Mining Data

- CreateTableJob

  创建MySQL数据表
  
  ```shell script
  bash -x ./run-local.sh CreateTableJob
  ```
  
  ```shell script
  bash -x ./run-woc.sh CreateTableJob
  ```
  
- MongoDbInitializeJob

  Create indexes for library data in MongoDB
  
  ```shell script
  bash -x ./run-local.sh MongoDbInitializeJob
  ```

- LibrariesIoImportJob

  将LibrariesIO的库数据导入数据库
  
  ```shell script
  bash -x ./run-local.sh LibrariesIoImportJob
  ```
    
  ```shell script
  bash -x ./run-woc.sh LibrariesIoImportJob
  ```

- LioJarParseJob

  从Maven下载之前导入的LibrariesIO库数据并分析，构建库与API签名映射关系

  ```shell script
  bash -x ./run-local.sh LioJarParseJob 
  ```

  ```shell script
  bash -x ./run-woc.sh LioJarParseJob
  ```

- WocRepoAnalysisJob

  在WoC上分析一系列Java仓库，获得API替换关系、依赖变更等数据

### Exporting Data

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
  bash -x ./run-woc.sh DataExportJob RepositoryDepSeq export/RepositoryDepSeq.csv 100000
  bash -x ./run-woc.sh DataExportJob GroundTruth test_data/ground_truth.csv \
     test_data/rules-2014-raw.csv test_data/rules-2014-artifactList.csv
  ```

### Recommending Libraries

- LibraryRecommendJob

  使用数据导出阶段的APISupport和RepositoryDepSeq，根据文件输入的依赖库列表，进行替代库推荐
  
  ```shell script
  bash -x ./run-woc.sh LibraryRecommendJob -q test_data/test-lib-input.txt -o test_data/test-recommend-output.csv
  ```
  
- LibraryUpgradeRecommendJob

  ```shell script
  bash -x ./run-local.sh LibraryUpgradeRecommendJob test_data/test-upgrade-input.csv test_output/
  ```
  
  ```shell script
  bash -x ./run-woc.sh LibraryUpgradeRecommendJob test_data/test-upgrade-input.csv test_output/
  ```

