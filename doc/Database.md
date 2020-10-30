# Documentation for MongoDB

This documentation describes the MongoDB database used in the SANER 2021 paper "A Multi-Metric Ranking Approach for Library Migration Recommendations."

## Access and Authentication

We have a deployed MongoDB database on World of Code. You can only access them on one of the World of Code servers (`da[1-4].eecs.utk.edu`), with authentication as in the following URL.

```
mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper?authSource=migration_helper
```

The database admin username is `myUserAdmin`. 

## Database Dump and Restore

If you do not have access to World of Code, you can download the database dumps and deploy your own MongoDB server.

Here is a utility script for dumping everything in the folder where the script is run.

```shell script
for collection in wocDepSeq3 libraryMigrationCandidate wocConfirmedMigration classSignature classToLibraryVersion libraryVersionToClass libraryGroupArtifact libraryVersion libraryVersionToDependency lioProject lioProjectDependency lioRepository lioRepositoryDependency wocCommit wocPomBlob wocRepository wocAPICount customSequences
do
    dumpfile=migration_helper.$collection.gz
    if [ -e $dumpfile ]
    then
        echo "Skipping $dumpfile because it already exists"
        echo "Delete the file to re-dump it"
    else
        echo "Beginning Dump for $collection"
        /da1_data/play/heh/mongodb/bin/mongodump --archive=$dumpfile --gzip \
            --collection=$collection \
            --uri=mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper?authSource=migration_helper
    fi
done
```

For minimal deployment (only use precomputed recommendation result), you can deploy the following collections. You may need to modify the Java code a little bit to avoid missing data crashes. (I didn't have time to test about this)

```
migration_helper.wocDepSeq3.gz
migration_helper.wocConfirmedMigration.gz
migration_helper.lioRepository.gz
migration_helper.lioProject.gz
migration_helper.libraryMigrationCandidate.gz
migration_helper.libraryGroupArtifact.gz
```

For full deployment (need to run recommendation for other libraries, need to use library knowledge graph, repository data, etc), you should deploy every collection in this documentation and follow the general running instructions in this repository.

Since all collections are dumped individually (to avoid too large files), you should restore them one by one using a command like this in the folder with all the dump files.

```shell script
for collection in wocDepSeq3 libraryMigrationCandidate wocConfirmedMigration classSignature classToLibraryVersion libraryVersionToClass libraryGroupArtifact libraryVersion libraryVersionToDependency lioProject lioProjectDependency lioRepository lioRepositoryDependency wocCommit wocPomBlob wocRepository wocAPICount customSequences
do
    mongorestore . --gzip --archive=migration_helper.$collection.gz
done
```

## Database Schema

We use [JSON Schema](https://json-schema.org) to formally describe the format of each collection in our MongoDB database in the `schemas/` folder. The schema file can be directly copy pasted into MongoDB for schema validation. We also enforce all the database schema in the deployed MongoDB database. However, you need to manually add the schemas if you want to deploy your database using our dump. 

When reading this documentation, we strongly advise going through several examples using MongoDB Compass to understand the database schema. You can also refer to the Java code (`migration_helper.data` package) to check how they will be loaded as Java classes.

## Core Collections

* [migration_helper.wocDepSeq3](markdowns/migration_helper.wocDepSeq3.md). The core dependency change sequence data used in the paper.
* [migration_helper.wocConfirmedMigration](markdowns/migration_helper.wocConfirmedMigration.md). Manually confirmed ground truth migrations, including libraries, commits, and file changes.
* [migration_helper.libraryMigrationCandidate](markdowns/migration_helper.libraryMigrationCandidate.md). Precomputed migration recommendation results for all Maven artifacts.
* [migration_helper.lioRepository](markdowns/migration_helper.lioRepository.md). The full repository list from Libraries.io dataset 1.6.0 (released in 2020-01-12).
* [migration_helper.lioProject](markdowns/migration_helper.lioProject.md). The Maven artifact list (unique group ID and artifact ID) from Libraries.io dataset 1.6.0 (released in 2020-01-12).
* [migration_helper.libraryGroupArtifact](markdowns/migration_helper.libraryGroupArtifact.md). The Maven group ID and artifact ID list imported from Libraries.io. We use this collection, especially the `_id` field for historical reasons.

## Additional Collections

* [migration_helper.lioRepositoryDependency](markdowns/migration_helper.lioRepositoryDependency.md). The latest pom.xml dependencies information recored for each repository provided by the Libraries.io dataset.
* [migration_helper.lioProjectDependency](markdowns/migration_helper.lioProjectDependency.md). The dependencies for each library version provided by the Libraries.io dataset. 
* [migration_helper.libraryVersion](markdowns/migration_helper.libraryVersion.md). For each Maven artifact, the version information we retreive from Maven Central.
* [migration_helper.libraryVersionToDependency](markdowns/migration_helper.libraryVersionToDependency.md). The dependency info for each artifact version that we retrieve from Maven Central. 
* [migration_helper.classSignature](markdowns/migration_helper.classSignature.md). The collection that stores a compact representation of Java class APIs in the JARs download from Maven Central,
* [migration_helper.classToLibraryVersion](markdowns/migration_helper.classToLibraryVersion.md). Maintains the mapping between a class and the library versions that the class has occurred.
* [migration_helper.libraryVersionToClass](markdowns/migration_helper.libraryVersionToClass.md). Maintains the mapping between the classes that a library version has. 
* [migration_helper.wocRepository](markdowns/migration_helper.wocRepository.md). Repository to commit mapping retrieved from World of Code. 
* [migration_helper.wocCommit](markdowns/migration_helper.wocCommit.md). Information about all commits used in our study, including commit message, timestamp and diff blobs retrieved from World of Code.
* [migration_helper.wocPomBlob](markdowns/migration_helper.wocPomBlob.md). Information abut all pom.xml blobs (i.e. full change history) used in our study, retrieved from World of Code.

## Extract Relevant Code Changes for a Migration

In this section, I will describe how to extract relevant code changes for a specific library migration in our data. For example, suppose we want to extract relevant code changes for the migration from `org.json:json` to `com.google.code.gson:gson`, to get changes like the following (copied from [here](https://github.com/vmi/selenese-runner-java/commit/641ab94e7d014cdf4fd6a83554dcff57130143d3)).

```java
-import org.json.JSONObject;
+import com.google.gson.Gson;
 import com.thoughtworks.selenium.SeleniumException;
......
             bindings.put("rule", ((JSMap<?, ?>) rule).unwrap());
         else
             bindings.put("rule", rule);
-        String args = new JSONObject(rollupArgs).toString();
+        String args = new Gson().toJson(rollupArgs);
```

Note that, even if a commit is marked as migration commit, it is **because of the pom.xml changes and the commit messages, not because there are migration code changes**. We make this design choice because it is very hard to trace migration code changes and maybe for some migrations there is no code change at all. As a result, the migration commits in our data may not necessarily contain migration code changes, and you have to filter out the commits that do not have any migration code changes.

We suggest using the following procedure to extract relevant code changes for a migration from library A (fromLib=A) to library (toLib=B).

1. Query [migration_helper.wocConfirmedMigration](markdowns/migration_helper.wocConfirmedMigration.md) to retrieve all related commits that records a migration from library A to library B. If there is none, you can find related commits using [migration_helper.libraryMigrationCandidate](markdowns/migration_helper.libraryMigrationCandidate.md), although the results there are not guaranteed to be correct.
2. (Optional) extend the related commits using [migration_helper.wocCommit](markdowns/migration_helper.wocCommit.md) and [migration_helper.wocRepository](markdowns/migration_helper.wocRepository.md). For each (repository, startCommit, endCommit), you can first retrieve all commits of the repository, and get all commits in between startCommit and endCommit by timestamp. You can also retrieve other commits in a given time interval (e.g. startCommit - one month, endCommit + one month), in case the code changes are performed in the commits before the startCommit or after the endCommit.
3. For each commit retrieved above, you can get the commit diff and blob content using GitHub API, **because this database does not store any Java blob data**. For each of the Java file diffs, you can check whether the file uses any package from library A or library B using [migration_helper.libraryVersionToClass](markdowns/migration_helper.libraryVersionToClass.md) and [migration_helper.classSignature](markdowns/migration_helper.classSignature.md). If the old version of this Java file uses APIs from A and the new version uses APIs from B, you can then identify diff hunks that conduct the migration code changes, by mapping the references of APIs to line numbers, and choose the diff hunks that both the addition of B's API and removal of A's API are mapped to this hunk.