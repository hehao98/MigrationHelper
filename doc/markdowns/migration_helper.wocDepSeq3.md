# migration_helper.wocDepSeq3

This is the collection that stores the dependency change sequences described in the paper. Besides the abstract mathematical models, it also stores other auxiliary information for ease of use.

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.wocDepSeq3",
    "required": [
      "repoName",
      "fileName",
      "seq"
    ],
    "properties": {
      "repoName": {
        "bsonType": "string"
      },
      "fileName": {
        "bsonType": "string"
      },
      "seq": {
        "bsonType": "array",
        "description": "The dependency change sequence.",
        "minItems": 0,
        "uniqueItems": true,
        "items": {
          "bsonType": "object",
          "required": [
            "oldBlob",
            "newBlob",
            "commit",
            "changes",
            "versionChanges"
          ],
          "properties": {
            "oldBlob": {
              "bsonType": "string"
            },
            "newBlob": {
              "bsonType": "string"
            },
            "commit": {
              "bsonType": "string"
            },
            "changes": {
              "bsonType": "array",
              "minItems": 0,
              "uniqueItems": true,
              "items": {
                "bsonType": "string"
              }
            },
            "versionChanges": {
              "bsonType": "array",
              "minItems": 0,
              "uniqueItems": true,
              "items": {
                "bsonType": "string"
              }
            }
          }
        }
      }
    }
  }
}
```

## Property Description

1. **repoName**. The repository where this dependency change sequence is constructed from, in World of Code naming convention (`/` are replaced with `_`).

2. **fileName**. The pom.xml filename in the repository where this dependency change sequence is constructed from.

3. **seq**. The dependency change sequence.

   1. **seq[i].oldBlob**. 40 byte SHA1 hash as a unique blob identifier.

   2. **seq[i].newBlob**. 40 byte SHA1 hash as a unique blob identifier.

   3. **seq[i].commit**. 40 byte SHA1 hash as a unique commit identifier.

   4. **seq[i].changes**. A sequence of dependency changes that does not take version into consideration, in the following formats:
      * Add library: `"+{}:{}".format(groupId, artifactId)`
      * Remove library: `"-{}:{}".format(groupId, artifactId)`

   5. **seq[i].versionChanges**. A sequence of dependency changes that takes version into consideration, in the following formats
      * Add library: `"+{}:{} {}".format(groupId, artifactId, version)`
      * Remove library: `"-{}:{} {}".format(groupId, artifactId, version)`
      * Update library: `" {}:{} {} -> {}".format(groupId, artifactId, oldVer, newVer)`

## Additional Notes

For each item in the dependency change sequence, we intentionally keep its reference to related blobs and commits, which can be used in a variety of ways (e.g. retrieve more info from GitHub using the SHA1 and repository name, etc).



