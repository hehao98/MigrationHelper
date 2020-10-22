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
