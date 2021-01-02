# migration_helper.classToLibraryVersion

This collection maintains the mapping between a class and the library versions that the class has occurred.

## Indexes

This collection is indexed on 40 Byte SHA1 hash which uniquely represents a class by our design.

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.classToLibraryVersion",
    "required": ["_id", "versionIds"],
    "properties": {
      "_id": {
        "bsonType": "long"
      },
      "versionIds": {
        "bsonType": "array",
        "minItems": 1,
        "items": {
          "bsonType": "long"
        }
      }
    }
  }
}
```

## Properties

1. **_id**. 40 byte SHA1 hash that uniquely represents a class, also used as the **_id** field in `migration_helper.classSignature`.
2. **versionIds**. An array of 64-bit integers as the version mapping. Each integer is the **_id**. from the `migration_helper.libraryVersion` and uniquely represents a library version.
