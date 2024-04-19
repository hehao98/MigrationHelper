#!/bin/bash

set -e

# if /dump exists, cd into it
[ -d /dump ] && cd /dump

# check if mongorestore is installed
if ! command -v mongorestore &> /dev/null
then
    echo "mongorestore could not be found"
    exit
fi

# find if the collections exist by finding if "No such file or directory" is in the output
if [ -z "$(ls migration_helper.* 2>/dev/null)" ]
then
    echo "No backup files found"
    exit
fi

for collection in wocDepSeq3 wocConfirmedMigration lioProject lioRepository libraryMigrationCandidate libraryGroupArtifact
do
    echo "Restoring $collection"
    [ -f migration_helper.$collection.gz ] && mongorestore . --gzip --archive=migration_helper.$collection.gz
done