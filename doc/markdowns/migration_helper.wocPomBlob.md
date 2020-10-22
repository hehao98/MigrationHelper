# migration_helper.wocPomBlob

Information abut all pom.xml blobs (i.e. full change history) used in our study, retrieved from World of Code.

## Index

This collection is indexed on the 40 Byte SHA1 hash (**_id** field) as used by git to index a blob.

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.wocPomBlob",
    "required": ["_id", "dependencies", "error"],
    "properties": {
      "_id": {
        "bsonType": "string",
        "minLength": 40,
        "maxLength": 40
      },
      "dependencies": {
        "bsonType": "array",
        "uniqueItems": true,
        "items": {
          "bsonType": "object",
          "required": ["groupId", "artifactId", "version"],
          "properties": {
            "groupId": {
              "bsonType": "string"
            },
            "artifactId": {
              "bsonType": "string"
            },
            "version": {
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

1. **_id**. The 40 Byte SHA1 hash (**_id** field) as used by git to uniquely identify a blob.
2. **dependencies**. The dependnencies declared in the `<dependency></dependency>` section/.
  1. **dependencies[i].groupId**. The group ID used in Maven.
  2. **dependencies[i].artifactId**. The artifact ID used in Maven.
  3. **dependencies[i].version**. The version number used in Maven. It is a string and may or may not adhere to the semantic versioning standard.
3. **error**. Whether this pom.xml blob contain error during parsing, either due to malformed XML, or because the blob data is missing or corrupted in World of Code.
