# migration_helper.wocRepository

This collections stores for each repository, all the commits we can retrieve in World of Code. In typical use cases, a program often first query all commits in a repository, then retrieve all commits, then retrieve related diffs, etc.

## Indexes

This collection has a unique index on `name`, which should be used to query this collection.

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.wocRepository",
    "required": ["_id", "name", "commits"],
    "properties": {
      "_id": {
        "bsonType": "long"
      },
      "name": {
        "bsonType": "string"
      },
      "commits": {
        "bsonType": "array",
        "items": {
          "bsonType": "string",
          "maxLength": 40,
          "minLength": 40
        }
      }
    }
  }
}
```

## Property Description

1. **_id**. Same as the **_id** field in `lioRepository` collection.
2. **name**. Repository name as in World of Code format (replace "/" with "_").
3. **commits**. An array of 40-Byte commit SHA1s. Each SHA1 can be used to query commit information in other collections or from GitHub APIs.
