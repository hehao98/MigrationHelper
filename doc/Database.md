# Documentation for MongoDB

## Access and Authentication

We have a deployed MongoDB database on World of Code. You can only access them on one of the World of Code servers (`da[1-4].eecs.utk.edu`), with authentication as in the following URL.

```
mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper?authSource=migration_helper
```

## Database Dump

If you do not have access to World of Code, you can download the database dumps and deploy your own MongoDB server.

Here is a utility script for dumping everything in the folder where the script is run.

```shell script
for collection in wocDepSeq3 classSignature classToLibraryVersion libraryVersionToClass libraryGroupArtifact libraryVersion libraryVersionToDependency lioProject lioProjectDependency lioRepository lioRepositoryDependency wocCommit wocPomBlob wocRepository
do
    echo "Beginning Dump for $collection"
    /da1_data/play/heh/mongodb/bin/mongodump --archive=migration_helper.$collection.gz --gzip \
        --collection=$collection \
        --uri=mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper?authSource=migration_helper
done
```

## Database Scheme

### `migration_helper.wocDepSeq3`