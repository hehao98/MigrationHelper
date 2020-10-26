# migration_helper.libraryVersionToDependency

This collection stores the dependency information for each Maven artifact that we retrieve and build from Maven Central

## Index

This collection has the same **_id** as the `migration_helper.libraryVersion` collection. It also has a unique compound index on (groupId, artifactId, version).

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.libraryVersionToDependency",
    "required": [
      "_id",
      "groupId",
      "artifactId",
      "version",
      "hasError",
      "dependencies"
    ],
    "properties": {
      "_id": { "bsonType": "long" },
      "groupId": { "bsonType": "string" },
      "artifactId": { "bsonType": "string" },
      "version": { "bsonType": "string" },
      "hasError": { "bsonType": "bool" },
      "dependencies": {
        "bsonType": "array",
        "items": {
          "bsonType": "object",
          "required": ["groupId", "artifactId", "version"],
          "properties": {
            "groupId": { "bsonType": "string" },
            "artifactId": { "bsonType": "string" },
            "version": { "bsonType": "string" }
          }
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
5. **hasError**. Whether any error occurred when extracting the dependencies of this Maven artifact, due to malformed pom.xml, missing data, network failtures, etc.
6. **dependencies**. The list of dependencies for this Maven artifact, including groupId, artifactId and version for each item. It may be empty if an artifact has no dependencies.
