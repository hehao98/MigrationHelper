# migration_helper.lioProject

## Libraries.io Description

A Libraries.io project is the definition of a package available from one of the 37 Package Managers that it supports.

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.lioProject",
    "required": [
      "_id",
      "platform",
      "language",
      "name",
      "homepageUrl",
      "description",
      "keywords",
      "repositoryUrl",
      "repositoryId",
      "repositoryDescription",
      "sourceRank",
      "repositoryStarCount",
      "repositoryForkCount",
      "repositoryWatchersCount",
      "repositorySourceRank",
      "dependentProjectsCount",
      "dependentRepositoriesCount"
    ],
    "properties": {
      "_id": {
        "bsonType": "long"
      },
      "platform": {
        "bsonType": "string"
      },
      "language": {
        "bsonType": "string"
      },
      "name": {
        "bsonType": "string"
      },
      "homepageUrl": {
        "bsonType": "string"
      },
      "description": {
        "bsonType": "string"
      },
      "keywords": {
        "bsonType": "string"
      },
      "repositoryUrl": {
        "bsonType": "string"
      },
      "repositoryId": {
        "bsonType": "long"
      },
      "repositoryDescription": {
        "bsonType": "string"
      },
      "sourceRank": {
        "bsonType": "int"
      },
      "repositoryStarCount": {
        "bsonType": "int"
      },
      "repositoryForkCount": {
        "bsonType": "int"
      },
      "repositoryWatchersCount": {
        "bsonType": "int"
      },
      "repositorySourceRank": {
        "bsonType": "int"
      },
      "dependentProjectsCount": {
        "bsonType": "int"
      },
      "dependentRepositoriesCount": {
        "bsonType": "int"
      }
    }
  }
}
```

## Property Description

See the [Libraries.io Documentation](https://libraries.io/data) for the meaning of each field.
