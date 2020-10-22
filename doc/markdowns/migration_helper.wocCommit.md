# migration_helper.wocCommit.md

This collection store all related commits we retrieve from World of Code, including commit message, timestamp and diff blobs retrieved from World of Code.

## Indexes

This collection is indexed on 40 Byte SHA1 hash (**_id**) used by git. You should use SHA1 to query this collection.

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.wocCommit",
    "required": ["_id", "timestamp", "message", "parents", "diffs", "error"],
    "properties": {
      "_id": {
        "bsonType": "string",
        "minLength": 40,
        "maxLength": 40
      },
      "timestamp": {
        "bsonType": "date"
      },
      "message": {
        "bsonType": "string"
      },
      "parents": {
        "bsonType": "array",
        "maxItems": 2,
        "uniqueItems": true,
        "items": {
          "bsonType": "string",
          "minLength": 40,
          "maxLength": 40
        }
      },
      "diffs": {
        "bsonType": "array",
        "uniqueItems": true,
        "items": {
          "bsonType": "object",
          "required": ["newBlob", "oldBlob", "filename"],
          "properties": {
            "newBlob": {
              "bsonType": "string",
              "minLength": 0,
              "maxLength": 40
            },
            "oldBlob": {
              "bsonType": "string",
              "minLength": 0,
              "maxLength": 40
            },
            "filename": {
              "bsonType": "string"
            }
          }
        }
      },
      "error": {
        "bsonType": "bool"
      }
    }
  }
}
```

## Property Description

1. **_id**. 40 Byte SHA1 hash that uniquely identifies a commit.
2. **timestamp**. The time when this commit is committed.
3. **message**. The full commit message.
4. **parents**. The parent commit. A commit have 0, 1, or 2 parents.
5. **diffs**. The diffs (i.e. file changes) of this commit. We only store pom.xml and .java diffs, and this array is capped at length 1024 to avoid DocumentTooLargeError. Therefore, in extreme cases the diff pairs may be incomplete. *We do not consider renaming during diff computation.*
  1. **diffs[i].filename**. The full path of this file change.
  2. **diffs[i].oldBlob**. The 40 Byte SHA1 hash of old file blob. Will be `""` if this file is newly created.
  3. **diffs[i].newBlob**. The 40 Byte SHA1 hash of new file blob. Will be `""` if this file is deleted.
9. **error**. Will be `true` if this commit object contains error in the World of Code database.
