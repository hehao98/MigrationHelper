# MigrationHelper

This is our main repository for the ongoing paper "Recommending Library for Migration". It contains all the necessary 
code, configuration files, evaluation scripts and documentations to fully replicate this paper on any machine 
with or without World of Code access. It is also a migration recommendation tool, with command line utilities to 
recommend migration targets, summarize API diffs across library versions, and making library update suggestions for a Maven 
managed project.

## Compile

### Local

First, install JDK 1.8 and Maven. Use `mvn clean package` to compile the whole project into a runnable JAR file. 
The former command will take some time to run a test suite, which will be useful if you are compiling this tool for 
the first time. Note that the test suite will only pass if you have database properly configured (see latter sections). 
If you want to skip the test, use `mvn clean package -DskipTests` instead.

### Remote on World of Code

Contact Professor [Audris Mockus](http://mockus.org) for access to World of Code servers, if you are/will be a PhD student of Minghui. 
For others, you can contact Hao He and use his ssh account. (Remove this paragraph in the anonymous version)

We strongly recommend all collaborators to create a stand-alone repository like 
[this](https://github.com/hehao98/MigrationHelperJAR) GitHub repository for uploading JAR to remote WoC servers 
(as direct connection is very slow). Collisions might happen if multiple people push to the same JAR repository.

With push access to the JAR repository you created, you can pull the repository into the `MigrationHelperJAR` folder, and 
use this script to compile and upload JAR.

```shell script
bash -x ./compile_upload.sh
```

Then, on one of the World of Code servers, clone this repository and clone the JAR repository in this repository,
before executing any job for this tool.

```shell script
git clone https://github.com/hehao98/MigrationHelper.git
cd MigrationHelper
git clone https://github.com/hehao98/MigrationHelperJAR.git
```

You are likely to create your own `MigrationHelper` repository on World of Code. If you do this, please copy the 
`/da1_data/play/heh/MigrationHelper/export` folder to your cloned repository, as some files might be useful 
in some cases.

## Test

We have only a limited number of unit tests (also deadly simple and not rigorous) which can be run as follows. 

```shell script
mvn clean test
```

(What a pity on our poor software engineering practices!) 
More patches that do rigorous testing of any module is always welcome.

To run a specific test (which is very useful during development), use a command like this.

```shell script
mvn clean test -Dtest=AbcDefTest
```

We strongly recommend create a new test or modify existing tests for any new features that you added, and make sure
they can be passed locally before committing and pushing your work, unless your work can only be tested in the remote 
environment. Refer to existing tests on how to set up database related tests.

We also rely on logging and exception handling for debugging. 

## Database Configuration

For local usage, you need to have a running MySQL server, and a running MongoDB server with the configurations specified 
 in `src/main/resources/application-local.yaml`. Note that you need to have
For World of Code usage, the servers should be already running on `da1.eecs.utk.edu`.

**Currently we are undergoing an migration from MySQL to MongoDB, but it is still in progress, so you will see a lot of 
messy code in the folder. We are sorry for that and we are actively working to fix this. As long as all the World of Code 
repository analysis code are refactored, we can safely discard all MySQL related code and the MySQL Server.**

### Start MongoDB Server on World of Code da1

Make sure MongoDB server is not already running. Run the following command at `/da1_data/play/heh/mongodb/bin`.

```shell script
nohup ./mongod --auth --dbpath /da1_data/play/heh/mongodb/data \
    --logpath /da1_data/play/heh/mongodb/db.log --fork --port 27020 --wiredTigerCacheSizeGB 100 \
    --bind_ip localhost,da1.eecs.utk.edu &
```

### Start MySQL Server on World of Code da1

Make sure the MySQL server is not already running. Use the `run.sh` at `/da1_data/play/heh/mysql`.

## Usage Instructions

In this section, we detail on how to run this tool either locally or remotely on World of Code servers.

We have two utility scripts for executing our tool: `run-local.sh` is for running locally,
 and `run-woc.sh` is for running on any of the World of Code servers. 
However, some jobs may not work properly without access to blob database, so we strongly recommend running jobs on da4.

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
  bash -x ./run-woc.sh LibraryUpgradeRecommendJob -l com.google.code.gson:gson -v 2.8.5
  bash -x ./run-woc.sh LibraryUpgradeRecommendJob -i test_data/test-upgrade-input.csv
  ```
  
## Contributing
 
In this section, I will describe how to get comfortable and make new contributions to this project.

For each collaborator, please create a new branch with your name, and do your work in your own branch. After you have 
finished doing some work, please open a pull request to `master` and let someone review it. We do not set a `dev` branch
because this project is still in very early stage with no need for such separation. 

It will be useful if we can have a CI pipeline. However, due to the unique environment requirements of this project, 
we cannot utilize GitHub Action directly and need to set up our own server for CI. It is currently not done but we 
welcome any effort on this.

### Preliminaries

To get handy on this project, you will need some preliminary knowledge. Here we list something you might need to know
before you can understand every part of this project.

1. **Intermediate knowledge of Java and OOP**. You can learn this through tons of books, tutorials, courses or websites.
Personally I learned Java from [Core Java](https://www.amazon.com/Core-Java-I-Fundamentals-11th-Horstmann/dp/0135166306),
but there are really many good alternatives.

2. **Basic knowledge of Spring**, especially what is IoC and why we use IoC. Spring is most powerful when building web 
applications, but we are only using its IoC features (`@Service`, `@Autowired`, etc) and database facilities 
(`@Mapper` for MySQL and `@Repository` for MongoDB). To understand them, I personally read through tutorials 
[here](https://www.liaoxuefeng.com/) and first few chapters of 
[Spring In Action](https://www.amazon.com/Spring-Action-Covers-4/dp/161729120X) to have a basic understanding of them.
Refer to the Spring documentation if you want to know more about the database facilities.

3. **Basic Knowledge of MySQL and MongoDB**. MongoDB is very simple to learn. Going through the official 
[documentation](https://docs.mongodb.com/manual/introduction/) will suffice. MySQL is a little bit complex and 
its documentation is not very good for beginners. I recommend this [book](https://book.douban.com/subject/24250054/) 
which tells you the basics of all SQL-like databases. It is also very simple and have a very good coverage 
for everything you need to know.

4. **Basic Knowledge of Software Engineering Best Practices**. The importance of tests, code reviews, etc.

5. **(Optional) Knowledge about Java bytecode, Web and Code Analysis**, if you want to modify the Maven library 
analysis module.

6. **(Optional) Knowledge about Git and World of Code**, if you want to modify the repository analysis module.

### Repository Structure

All Java related code follows the best practices of a Java project, with `pom.xml` for dependency management, 
`src/main/java` for placing source code, `src/main/resources` for runtime files, `src/test/java` for test code,
and `src/test/resources` for test related files. The `tool` and `doc` folder is not currently used. The `pic` folder contain 
scripts for plotting figures in the paper. The `notebook` and `embedding` folder contains preliminary results which are
not included in current paper. The `test_data` folder contain several test input and test output files, along with ground
truth data.

### Code Structure

All the source code are placed in the `src/main/java/edu/pku/migrationhelper` folder. In this folder, `data/` contains 
basic types used throughout this project. `config/` folder is basic Spring configurations for MySQL, MongoDB and thread
pool. The `mapper` and `repository` folder places MySQL and MongoDB accessors, respectively. The `util` and `woc` folder contain
code for accessing World of Code. The `service` folder contains classes that provide some functionalities 
(i.e. singleton components), and `job` folder contains command line runners that uses these components to accomplish
certain tasks.

