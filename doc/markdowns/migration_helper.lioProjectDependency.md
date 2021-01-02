# migration_helper.lioProjectDependency

This collection stores the dependencies of all Maven artifacts indexed by Libraries.io Dataset. It is called "project dependency" because Libraries.io call a library as a "project".

## Libraries.io Description

Libraries.io dependencies belong to versions of a project, each version can have different sets of dependencies with different versions. Dependencies point at a specific version or range of versions of other projects, the resolution of that project version change over time as new versions are published and dependent on the specifics of the platform.

Almost all package managers dependencies will be from the same package manager, the only exception is Atom, which pulls its dependencies from the NPM package manager, hence the extra `Dependency Platform` field.

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.lioProjectDependency",
    "required": [
      "_id",
      "platform",
      "projectName",
      "projectId",
      "versionNumber",
      "versionId",
      "dependencyName",
      "dependencyPlatform",
      "dependencyKind",
      "optionalDependency",
      "dependencyRequirements",
      "dependencyProjectId"
    ],
    "properties": {
      "_id": {
        "bsonType": "long"
      },
      "platform": {
        "bsonType": "string"
      },
      "projectName": {
        "bsonType": "string"
      },
      "projectId": {
        "bsonType": "long"
      },
      "versionNumber": {
        "bsonType": "string"
      },
      "versionId": {
        "bsonType": "long"
      },
      "dependencyName": {
        "bsonType": "string"
      },
      "dependencyPlatform": {
        "bsonType": "string"
      },
      "dependencyKind": {
        "bsonType": "string"
      },
      "dependencyRequirements": {
        "bsonType": "string"
      },
      "dependencyProjectId": {
        "bsonType": "long"
      }
    }
  }
}
```

## Property Description

See the [Libraries.io Documentation](https://libraries.io/data) for the meaning of each field.