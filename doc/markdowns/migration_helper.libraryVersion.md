# migration_helper.libraryVersion

This is a collection that stores basic version information and some API parsing information. It is migrated from the MySQL database and kept for historical reasons.

## Indexes

This collection has a unique compound index on `groupArtifactId` and `version`.

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.libraryVersion",
    "required": [
      "_id",
      "groupArtifactId",
      "version",
      "downloaded",
      "parsed",
      "parseError"
    ],
    "properties": {
      "_id": { "bsonType": "long" },
      "groupArtifactId": { "bsonType": "long" },
      "version": { "bsonType": "string" },
      "downloaded": { "bsonType": "bool" },
      "parsed": { "bsonType": "bool" },
      "parseError": { "bsonType": "bool" }
    }
  }
}
```

## Property Description

1. **_id**. A 64-bit integer ID that uniquely stands for a version of a Maven artifact.
2. **groupArtifactId**. A 64-bit integer ID that is used in the **_id** field of `migration_helper.libraryGroupArtifact`.
3. **version**. The version string of this Maven artifact, which can be any string that may or may not adhere to the semantic versioning standard.
4. **downloaded**. Whether this version has been downloaded during our API analysis.
5. **parsed**. Whether this version has been parsed during our API analysis.
6. **parseError**. Whether any error occurred when extracting APIs of this version.

If `downloaded == True and parsed == True and parseError == False`, the APIs (`migration_helper.classSignature`) of this version is extracted without error.
