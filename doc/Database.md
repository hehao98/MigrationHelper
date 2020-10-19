# Documentation for MongoDB

This documentation describes the MongoDB database used in the SANER 2021 paper "A Multi-Metric Ranking Approach for Library Migration Recommendations."

## Access and Authentication

We have a deployed MongoDB database on World of Code. You can only access them on one of the World of Code servers (`da[1-4].eecs.utk.edu`), with authentication as in the following URL.

```
mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper?authSource=migration_helper
```

The database admin username is `myUserAdmin`. 

## Database Dump and Deployment

If you do not have access to World of Code, you can download the database dumps and deploy your own MongoDB server.

Here is a utility script for dumping everything in the folder where the script is run.

```shell script
for collection in wocDepSeq3 libraryMigrationCandidate wocConfirmedMigration classSignature classToLibraryVersion libraryVersionToClass libraryGroupArtifact libraryVersion libraryVersionToDependency lioProject lioProjectDependency lioRepository lioRepositoryDependency wocCommit wocPomBlob wocRepository
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

For minimal deployment (only use precomputed recommendation result), you can deploy the following collections. (They are also used for the demo web service.)

```
migration_helper.wocDepSeq3.gz
migration_helper.wocConfirmedMigration.gz
migration_helper.lioRepository.gz
migration_helper.lioProject.gz
migration_helper.libraryMigrationCandidate.gz
migration_helper.libraryGroupArtifact.gz
```

For full deployment (need to run recommendation for other libraries), you should deploy every collection in this documentation and follow the general running instructions in this repository.

## Database Schema

We use [JSON Schema](https://json-schema.org) to formally describe the format of each collection in our MongoDB database in the `schemas/` folder. The schema file can be directly copy pasted into MongoDB for scema validation. We also enforce all the database schema in the deployed MongoDB database. However, you need to manually add the schemas if you want to deploy your database using our dump. You can also refer to the Java code (`migration_helper.data` package) to check how they will be loaded as Java classes.

## Collections

* [migration_helper.wocDepSeq3](markdowns/migration_helper.wocDepSeq3). The core dependency change sequence data used in the paper.

