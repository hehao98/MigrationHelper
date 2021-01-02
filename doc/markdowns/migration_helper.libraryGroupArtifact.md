# migration_helper.libraryGroupArtifact

## Indexes

The collection is indexed on an Index `_id`, and a Compund Index (`groupId` and `artifactId`). Both are guaranteed to be unique. Some part of code use `_id` to refer to a `libraryGroupArtifact`, mostly for historical reasons. Generally, we strongly advise storing only group ID and artifact ID as a property.

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.libraryGroupArtifact",
    "required": [
      "_id",
      "groupId",
      "artifactId",
      "versionExtracted",
      "parsed",
      "parseError"
    ],
    "properties": {
      "_id": {
        "bsonType": "long"
      },
      "groupId": {
        "bsonType": "string"
      },
      "artifactId": {
        "bsonType": "string"
      },
      "versionExtracted": {
        "bsonType": "bool"
      },
      "parsed": {
        "bsonType": "bool"
      },
      "parseError": {
        "bsonType": "bool"
      }
    }
  }
}
```

## Property Description

1. **_id**. A unique long ID which can be used to refer to one library.
2. **groupId**. The group ID of this library on Maven.
3. **artifactId**. The artifact ID of this library on Maven.
4. **versionExtracted**. Whether the version information is extracted for this library. If false, it is likely because of network error or missing data in Maven Central.
5. **parsed**. Whether the JARs of this library is parsed. If false, it is because an error occurred or the library cannot be downloaded.
6. **parseError**. Whether an error occurred when downloading and parsing the JARs of this library. If any of the JAR triggered parse errors when extracting API, it will be marked as false. However, you can still use part of the extracted APIs in this database.
