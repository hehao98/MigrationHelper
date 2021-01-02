# migration_helper.libraryVersionToClass

This collection maintains the mapping between the classes that a library version has. 

## Indexes

This collection has a unique compound index on (groupId, artifactId, version).

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.libraryVersionToClass",
    "required": ["_id", "groupId", "artifactId", "version", "classIds"],
    "properties": {
      "_id": { "bsonType": "long" },
      "groupId": { "bsonType": "string" },
      "artifactId": { "bsonType": "string" },
      "version": { "bsonType": "string" },
      "classIds": {
        "bsonType": "array",
        "uniqueItems": true,
        "minItems": 1,
        "items": {
          "bsonType": "string",
          "minLength": 40,
          "maxLength": 40
        }
      }
    }
  }
}
```

## Property Description

1. **_id**. An 64-bit integer ID which is same as the **_id** in `migration_helper.libraryVersion`, for each Maven artifact.
2. **groupId**. The group ID of this Maven artifact.
3. **artifactId**. The artifact ID of this Maven artifact.
4. **version**. The version string of this Maven artifact, which can be any string that may or may not adhere to the semantic versioning standard. It can also be empty `""` if the version string is not specified in some Maven artifacts.
5. **classIds**. An array of 40 Byte SHA1 hashes. Each hash uniquely represents a class and can be used to query class API information from the `classSignature` collection.
