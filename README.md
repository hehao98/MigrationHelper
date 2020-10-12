# MigrationHelper

Welcome to the main repository for paper "Recommending Library Migration Targets via Mining Dependency Change Sequences."

This repository contains everything for paper replication, some command-line utility tools, and a RESTful backend for our website demo.

## Note for Double-Blind Review

Despite our best effort in preserving anonymity here, reviewing materials here indeed poses a risk in revealing our identity. The source code may not be runnable due to package renaming and partial removals. Some too large data are not shared (e.g., the 100GB MongoDB dump). We will release all code and full data in the Camera Ready version of this repository. The following section provides some interesting destinations for you to look at.

## For SANER 2021 Reviewers and Paper Readers

### Web Demo

A Web Demo is available at this link (TODO: Add a website link). 

Note that the Web Demo only stores a limited amount of data and is just a very simple conceptual prototype. We are still actively developing it to make it more mature and user-friendly.

### Migration Dataset

We list where to find the migration dataset mentioned in the paper here.

1. `evaluation/manual/ground-truth-all.xlsx`. List of all ground truth migration rules.
2. `evaluation/manual/confirmed-migration-all.xlsx`. List of all confirmed migration commits.

We also have some partial database dumps, including dependency change sequences, available [here](https://drive.google.com/drive/folders/1UBTp9betjAi6gSly4OmGvLwIl7AJvUfg). See the `doc/` folder for documentation about the MongoDB database.

### Recommendation Examples

Please download `evaluation/recommend-output.csv` (~50MB) for all recommendation output in the 190 source libraries used in RQ1 and RQ2. For other recommendation example, you can either try the web demo above, or download dumped data in the Google Drive link mentioned above.

### Evaluation

The evaluation results are all put in the `evaluation/` folder. Not that the RQs are an earlier formulation different from that in paper. Therefore, we list where to find evaluation results in the paper here.

1. For labelling of ground truth used in RQ1 and RQ2, see `evaluation/rq1_*.ipynb`, `evaluation/manual/confirmed-migrations-*.xlsx`, and `evaluation/ground-truth.xlsx`.
2. For RQ1, see `evaluation/rq2_metrics.ipynb`.
3. For RQ2, see `evaluation/rq3_ranking.ipynb`.
4. For RQ3, see `evaluation/rq4_*.ipynb`, and `evaluation/manual/extended-migrations-extended.xlsx`.

All the intermediate data during labelling of ground truth are also kept in this folder.

### Implementation Details for the Paper

For those interested in implementation details of the paper, here are some starting points to look at:

1. `src/.../DepSeqAnalysisService.java`  implements the core migration target recommendation algorithm.
2. `src/LibraryIdentityService.java` downloads JARs from Maven, analyzes them and store the classes.
3. `scripts/depseq_build3.py` implements dependency sequence construction on World of Code.

## Replication and Deployment

This repository contains all necessary code, configuration files, evaluation scripts and documentations to fully replicate this paper on any machine with World of Code access. You have to re-implement the module for dependency change sequence construction (`scripts/depseq_*.py`), if you want to replicate this paper by cloning from GitHub. The command-line utilities will only work with a deployed MongoDB server and you may have to modify database configuration files for a new deployment. We are currently finding somewhere to put on the whole MongoDB database dump (~100GB).

## Compile

### Local

First, install JDK 1.8 and Maven. Use `mvn clean package` to compile the whole project into a runnable JAR file. The former command will take some time to run a test suite, which will be useful if you are compiling this tool for the first time. Note that the test suite will only pass if you have database properly configured (see latter sections). If you want to skip the test, use `mvn clean package -DskipTests` instead.

### Remote on World of Code

Contact the maintainers of this repository for how to access World of Code.

If you choose to develop locally, we recommend all collaborators to create a stand-alone repository like 
[this](https://github.com/hehao98/MigrationHelperJAR) GitHub repository for uploading JAR to remote WoC servers (as direct connection is very slow). Collisions might happen if multiple people push to the same JAR repository.

With push access to the JAR repository you created, you can pull the repository into the `MigrationHelperJAR` folder, and use this script to compile and upload JAR.

```shell script
bash -x ./compile_upload.sh
```

Then, on one of the World of Code servers, clone this repository and clone the JAR repository in this repository, before executing any job for this tool, using commands like

```shell script
git clone https://github.com/hehao98/MigrationHelper.git
cd MigrationHelper
git clone https://github.com/hehao98/MigrationHelperJAR.git
```

If you choose to develop remotely on World of Code da1, you can compile using the following command

```shell script
export JAVA_HOME=/home/heh/jdk1.8.0_144/ && /da1_data/play/heh/apache-maven-3.6.3/bin/mvn clean package -DskipTests
```

You are likely to create your own `MigrationHelper` repository on World of Code. If you do this, please copy the `/da1_data/play/heh/MigrationHelper/export` folder to your cloned repository, as some files might be useful in some cases (Like the API and library data, I will put them in database soon).

## Test

We have a limited number of tests which can be run as follows (some of them will take some time to finish). 

```shell script
mvn clean test
```

More patches that do rigorous testing of any module is always welcome.

To run a specific test (which is very useful during development), use a command like this.

```shell script
mvn clean test -Dtest=AbcDefTest
```

Some tests need a running MongoDb Server to work, or otherwise it will be skipped. 
Some tests need to be run on WoC Servers, using the following command.

```shell script
export JAVA_HOME=/home/heh/jdk1.8.0_144/ && /da1_data/play/heh/apache-maven-3.6.3/bin/mvn clean test
```

We strongly recommend create a new test or modify existing tests for any new features that you added, and make sure they can be passed locally before committing and pushing your work, unless your work can only be tested in the remote environment. Refer to existing tests on how to set up database related tests.

We also rely on logging and exception handling for debugging. 

## Database Configuration

For local usage, you need to have a running MongoDB server with the configurations specified in `src/main/resources/application-local.yaml`.
For World of Code usage, the servers should be already running on `da1.eecs.utk.edu`.

**We have undergone a migration from MySQL to MongoDB. All MySQL related classes have a `@Deprecated` tag and will be removed in the future.**

### Start MongoDB Server on World of Code da1

Make sure MongoDB server is not already running. Run the following command.

```shell script
nohup /da1_data/play/heh/mongodb/bin/mongod --auth --dbpath /da1_data/play/heh/mongodb/data \
    --logpath /da1_data/play/heh/mongodb/db.log --fork --port 27020 --wiredTigerCacheSizeGB 100 \
    --bind_ip localhost,da1.eecs.utk.edu &
```

### Start MySQL Server on World of Code da1 (No Longer in Use)

Make sure the MySQL server is not already running. Use the `run.sh` at `/da1_data/play/heh/mysql`.

## Usage Instructions on Command-Line

In this section, we detail on how to run this tool either locally or remotely on World of Code servers.

We have two utility scripts for executing our tool: `run-local.sh` is for running locally, and `run-woc.sh` is for running on any of the World of Code servers. For brevity, we will only use `run-woc.sh` in the script examples. For most of the jobs, if you specify the wrong parameters, usage info will be printed
in the output.

However, some jobs may not work properly without access to blob database, so we recommend running jobs on da4 if you do not know whether the job uses any World of Code functionalities.

### Mining Library Data and APIs

First, we need to initialize the database.

- MongoDbInitializeJob

  Create indexes for all data in MongoDB. This can be run repeatedly without unsafe side effects.
  
  ```shell script
  bash -x ./run-woc.sh MongoDbInitializeJob
  ```
  

Next, we need to import or mine data from multiple data sources.

- LibrariesIoImportJob

  Import data from the [libraries.io] dataset. It requires an additional parameter specifying which one to import.
  Before using this, make sure the import data path in `application-xxx.yaml` is properly configured.
  
  ```shell script
  bash -x ./run-woc.sh LibrariesIoImportJob <collection-name>
  ```

- LioJarParseJob

  Download and parse JARs from Maven Central, based on previous Libraries.io entries.

  ```shell script
  bash -x ./run-woc.sh LioJarParseJob 
  ```

  ```shell script
  bash -x ./run-woc.sh LioJarParseJob
  ```


* We are still reusing API Count data from an earlier version of this work

### Construct Dependency Change Sequences

See `scripts/README.md`.

### Recommending Libraries

- LibraryRecommendJob

  The core migration target recommendation algorithm. It can recommend a list of migration targets given a query file, using the algorithm described in the paper.
  
  ```shell script
  bash -x ./run-woc.sh LibraryRecommendJob -q evaluation/test-lib-input.txt -o evaluation/test-recommend-output.csv
  ```
  
  ```shell script
  bash run-woc.sh LibraryRecommendJob -q evaluation/from-lib-confirmed.txt \
          -o evaluation/test-recommend-output-wocDepSeq3-all.csv \
          -r evaluation/test-recommend-output-wocDepSeq3-all-commits.csv \
          -e
  ```
  
- LibraryUpgradeRecommendJob
  
  ```shell script
  bash -x ./run-woc.sh LibraryUpgradeRecommendJob -l com.google.code.gson:gson -v 2.8.5
  bash -x ./run-woc.sh LibraryUpgradeRecommendJob -i evaluation/test-upgrade-input.csv
  ```
  
## Contributing

In this section, I will describe how to get comfortable and make new contributions to this project.

For each collaborator, please create a new branch with your name, and do your work in your own branch. After you have finished doing some work, please open a pull request to `master` and let someone review it. We do not set a `dev` branch because this project is still in very early stage with no need for such separation. 

The project currently uses GitHub Action for continuous integration. For each commit you pushed, it will be compiled and tested on the GitHub remote environment. You will see a green check mark if your commit can compile and pass some tests. However, only a limited number of tests can be run on GitHub Actions (e.g. we skipp all DB and World of Code related tests) Anyway, it will be useful to see whether your code is breaking things up or not.

### Preliminaries

To get handy on this project, you will need some preliminary knowledge. Here we list something you might need to know before you can understand every part of this project.

1. **Intermediate knowledge of Java and OOP**. You can learn this through tons of books, tutorials, courses or websites. Personally I learned Java from [Core Java](https://www.amazon.com/Core-Java-I-Fundamentals-11th-Horstmann/dp/0135166306), but there are really many good alternatives.

2. **Basic knowledge of Spring**, especially what is IoC and why we use IoC. Spring is most powerful when building web applications, but we are only using its IoC features (`@Service`, `@Autowired`, etc) and database facilities (`@Mapper` for MySQL and `@Repository` for MongoDB). To understand them, I personally read through tutorials [here](https://www.liaoxuefeng.com/) and first few chapters of [Spring In Action](https://www.amazon.com/Spring-Action-Covers-4/dp/161729120X) to have a basic understanding of them. Refer to the Spring documentation if you want to know more about the database facilities.

3. **Basic Knowledge of MongoDB**. MongoDB is very simple to learn. Going through the official [documentation](https://docs.mongodb.com/manual/introduction/) will suffice. MySQL is a little bit complex and its documentation is not very good for beginners. I recommend this [book](https://book.douban.com/subject/24250054/) which tells you the basics of all SQL-like databases. It is also very simple and have a very good coverage for everything you need to know.

4. **Basic Knowledge of Software Engineering Best Practices**. The importance of tests, code reviews, etc.

5. **(Optional) Knowledge about Java bytecode, Web and Code Analysis**, if you want to modify the Maven library analysis module.

6. **(Optional) Knowledge about Git and World of Code**, if you want to modify the repository analysis module. I've found a very useful WoC tutorial [here](https://github.com/woc-hack/tutorial).

### Repository Structure

All Java related code follows the best practices of a Java project, with `pom.xml` for dependency management, `src/main/java` for placing source code, `src/main/resources` for runtime files, `src/test/java` for test code, and `src/test/resources` for test related files. The `tool` and `doc` folder is not currently used. The `notebook` and `embedding` folder contains preliminary results which are not included in current paper. The `evaluation` folder contain several test input and test output files, along with ground truth data. Inside this, the `pic` folder contain scripts for plotting figures in the paper.

### Code Structure

All the source code are placed in the `src/main/java/edu/pku/migrationhelper` folder. In this folder, `data/` contains basic types used throughout this project. `config/` folder is basic Spring configurations for MySQL, MongoDB and thread pool. The `mapper` and `repository` folder places MySQL and MongoDB accessors, respectively. The `util` and `woc` folder contain code for accessing World of Code. The `service` folder contains classes that provide some functionalities (i.e. singleton components), and `job` folder contains command line runners that uses these components to accomplish certain tasks.

