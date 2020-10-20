# migration_helper.wocDepSeq3

This is the collection that stores the dependency change sequences described in the paper. Besides the abstract mathematical models, it also stores other auxiliary information for ease of use.

## Indexes

It has a unique compound index on `repoName` and `fileName`, as one pom.xml file for one repository should only have one dependency change sequence.

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
        "bsonType": "string",
        "pattern": ".*_.*"
      },
      "fileName": {
        "bsonType": "string",
        "pattern": ".*pom.xml$"
      },
      "seq": {
        "bsonType": "array",
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
              "bsonType": "string",
              "maxLength": 40,
              "minLength": 0
            },
            "newBlob": {
              "bsonType": "string",
              "maxLength": 40,
              "minLength": 0
            },
            "commit": {
              "bsonType": "string",
              "maxLength": 40,
              "minLength": 40
            },
            "changes": {
              "bsonType": "array",
              "minItems": 0,
              "items": {
                "bsonType": "string",
                "pattern": "^[+-].*:.*$"
              }
            },
            "versionChanges": {
              "bsonType": "array",
              "minItems": 0,
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

2. **fileName**. The full pom.xml file path in the repository where this dependency change sequence is constructed from.

3. **seq**. The dependency change sequence.

   1. **seq[i].oldBlob**. 40 byte SHA1 hash as a unique blob identifier. The string will be empty (i.e `""`) if the blob is newly added.

   2. **seq[i].newBlob**. 40 byte SHA1 hash as a unique blob identifier. The string will be empty (i.e `""`) if the blob is deleted.

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

This dependency sequence construction is at commit granularity. If you want a more coarse granularity (e.g per-release), you may need to load additional data to link commit to releases, and merge this sequence into per-release dependency changes.



