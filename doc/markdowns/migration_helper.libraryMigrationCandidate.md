# migration_helper.libraryMigrationCandiate

This collection contains precomputed recommendation results for all Maven artifacts, including all the metrics described in the paper, intermediate data to compute the metrics, and some other fields that are not used in the current algorithm. When deploying a recommendation system, the most straight forward way of using this collection is to query by fromId, sort by confidence, and return as recommendation result.

Here is the mapping between metrics used in the paper and property names in this collection.
1. Confidence: `confidence`.
2. Rule Support: `ruleSupportByMaxSameCommit`.
3. Message Support: `commitMessageSupport`.
4. API Support: `methodChangeSupportByMax`.
5. Distance Support: `commitDistanceSupport`.

## Indexes

This collection has a unique compound index of (fromId, toId, confidence).

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.libraryMigrationCandidate",
    "required": [
      "fromId",
      "toId",
      "ruleCount",
      "ruleCountSameCommit",
      "methodChangeCount",
      "libraryConcurrenceCount",
      "maxRuleCount",
      "maxRuleCountSameCommit",
      "maxMethodChangeCount",
      "ruleSupportByTotal",
      "ruleSupportByMax",
      "ruleSupportByMaxSameCommit",
      "methodChangeSupportByTotal",
      "methodChangeSupportByMax",
      "libraryConcurrenceSupport",
      "commitDistanceSupport",
      "commitMessageSupport",
      "confidence",
      "repoCommitList",
      "commitDistanceList",
      "possibleCommitList"
    ],
    "properties": {
      "fromId": {
        "bsonType": "long"
      },
      "toId": {
        "bsonType": "long"
      },
      "ruleCount": {
        "bsonType": "int"
      },
      "ruleCountSameCommit": {
        "bsonType": "int"
      },
      "methodChangeCount": {
        "bsonType": "int"
      },
      "libraryConcurrenceCount": {
        "bsonType": "int"
      },
      "maxRuleCount": {
        "bsonType": "int"
      },
      "maxRuleCountSameCommit": {
        "bsonType": "int"
      },
      "maxMethodChangeCount": {
        "bsonType": "int"
      },
      "ruleSupportByTotal": {
        "bsonType": "double"
      },
      "ruleSupportByMax": {
        "bsonType": "double"
      },
      "ruleSupportByMaxSameCommit": {
        "bsonType": "double"
      },
      "methodChangeSupportByTotal": {
        "bsonType": "double"
      },
      "methodChangeSupportByMax": {
        "bsonType": "double"
      },
      "libraryConcurrenceSupport": {
        "bsonType": "double"
      },
      "commitDistanceSupport": {
        "bsonType": "double"
      },
      "commitMessageSupport": {
        "bsonType": "double"
      },
      "confidence": {
        "bsonType": "double"
      },
      "repoCommitList": {
        "bsonType": "array",
        "items": {
          "bsonType": "array",
          "minItems": 4,
          "maxItems": 4,
          "items": {
            "bsonType": "string"
          }
        }
      },
      "commitDistanceList": {
        "bsonType": "array",
        "items": {
          "bsonType": "int"
        }
      },
      "possibleCommitList": {
        "bsonType": "array",
        "items": {
          "bsonType": "array",
          "minItems": 4,
          "maxItems": 4,
          "items": {
            "bsonType": "string"
          }
        }
      }
    }
  }
}
```

## Property Description

1. **fromId**. The library ID used in `libraryGroupArtifact` collection, which points to the source library (being migrated from).
2. **toId**. The library ID used in `libraryGroupArtifact` collection, which points to the target library (being migrated to).
3. **ruleCount**. The number of dependency change sequences the source library is removed and the target library is added.
4. **ruleCountSameCommit**. The number of commits the source library is removed and the target library is added.
5. **methodChangeCount**. Number of Java code hunks where APIs of the source library is removed and APIs of the target library is added.
6. **libraryConcurrenceCount**. Number of dependency change sequences where both source library and target library is added.
7. **maxRuleCount**. The max `ruleCount` value for all (fromId, *) in collection.
8. **maxRuleCountSameCommit**. The max `ruleCountSameCommit` value for all (fromId, *) in collection.
9. **maxMethodChangeCount**. The max `methodChangeCount` value for all (fromId, *) in collection.
10. **ruleSupportByTotal**. Not used.
11. **ruleSupportByMax**. ruleCount / maxRuleCount.
12. **ruleSupportByMaxSameCommit**. ruleCountSameCommit / maxRuleCountSameCommit. The Rule Support metric in the paper.
13. **methodChangeSupportByTotal**. Not used.
14. **methodChangeSupportByMax**. methodChangeCount / maxMethodChangeCount.
15. **libraryConcurrenceSupport**. Not used.
16. **commitMessageSupport**. The Message Support in the paper.
17. **commitDistanceSupport**. The Distance Support in the paper.
18. **confidence**. The confidence used in the paper.
19. **repoCommitList**. List of related commits in the following tuple format (repoName, startCommit, endCommit, fileName).
20. **commitDistanceList**. For each of the commit pair above, number of commits in between.
21. **possibleCommitList**. A subset of related commits that our message matching algorithm thinks they are very likely to be a migration commit.
