# migration_helper.wocConfirmedMigration

This is the collection that stores the manually confirmed migration instances described in our paper. Please see our paper for how this data is manually generated. Note that we do not guarantee 100% correctness on the migration rules in here because of our limited expertise.

## Indexes

It has no index because it only have very few items and we assume that the whole collection should be loaded into memory for further use.

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.wocConfirmedMigration",
    "required": [
      "fromLib",
      "toLib",
      "repoName",
      "fileName",
      "startCommit",
      "endCommit",
      "startCommitChanges",
      "endCommitChanges",
      "startCommitMessage",
      "endCommitMessage",
      "startCommitTime",
      "endCommitTime"
    ],
    "properties": {
      "fromLib": {
        "bsonType": "string",
        "pattern": ".*:.*"
      },
      "toLib": {
        "bsonType": "string",
        "pattern": ".*:.*"
      },
      "repoName": {
        "bsonType": "string",
        "pattern": ".*_.*"
      },
      "fileName": {
        "bsonType": "string",
        "pattern": ".*pom.xml$"
      },
      "startCommit": {
        "bsonType": "string",
        "maxLength": 40,
        "minLength": 40
      },
      "endCommit": {
        "bsonType": "string",
        "maxLength": 40,
        "minLength": 40
      },
      "startCommitMessage": {
        "bsonType": ["string", "number"]
      },
      "endCommitMessage": {
        "bsonType": ["string", "number"]
      },
      "startCommitTime": {
        "bsonType": "date"
      },
      "endCommitTime": {
        "bsonType": "date"
      }
    }
  }
}
```

## Property Description

1. **fromLib**. The source library (i.e. pre-migration library), in groupId:artifactId format.
2. **toLib**. The target library (i.e. post-migration library), in groupId:artifactId format.
3. **repoName**. The repository where this migration happened, in World of Code naming convention (`/` are replaced with `_`).
4. **fileName**. The full pom.xml file path in the repository where this migration happened.
5. **startCommit**. 40-byte commit SHA1 hash where the migration starts.
6. **endCommit**. 40-byte commit SHA1 hash where the migration ends.
7. **startCommitMessage**. The commit message where the migration starts.
8. **endCommitMessage**. The commit message where the migration ends.
9. **startCommitTime**. The commit timestamp where the migration starts.
10. **endCommitTime**. The commit timestamp where the migration ends.

## Additional Notes

None.



