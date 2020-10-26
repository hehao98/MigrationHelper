# migration_helper.classSignature

This collections stores a compact representation of public API of Java classes. The classes are all extracted from JARs we download from Maven Central. This collection can be used to query API information given a class name or a library name (use with `libraryVersionToClass`).

You may want to refer to the following Java classes when using this collection.

```
edu.pku.migrationhelper.data.api.ClassSignature
edu.pku.migrationhelper.data.api.FieldOrMethod
edu.pku.migrationhelper.data.api.FieldSignature
edu.pku.migrationhelper.data.api.MethodSignature
edu.pku.migrationhelper.data.api.Annotation
```

## Indexes

This collection is indexed on **_id** and **className**. **_id** is unique but **className** is not unique, because the same class name may correspond to many different versions. 

## Formal Schema

```json
{
  "$jsonSchema": {
    "bsonType": "object",
    "title": "migration_helper.classSignature",
    "required": [
      "_id",
      "className",
      "flags",
      "superClassName",
      "superClassId",
      "interfaceNames",
      "interfaceIds",
      "methods",
      "fields"
    ],
    "properties": {
      "_id": {
        "bsonType": "string",
        "maxLength": 40,
        "minLength": 40
      },
      "className": {
        "bsonType": "string"
      },
      "flags": {
        "bsonType": "long"
      },
      "superClassName": {
        "bsonType": "string"
      },
      "superClassId": {
        "bsonType": "string",
        "maxLength": 40,
        "minLength": 40
      },
      "interfaceNames": {
        "bsonType": "array",
        "items": { "bsonType": "string" }
      },
      "interfaceIds": {
        "bsonType": "array",
        "items": {
          "bsonType": "string",
          "minLength": 40,
          "maxLength": 40
        }
      },
      "methods": {
        "bsonType": "object",
        "required": [
          "flag",
          "type",
          "name",
          "annotations",
          "parameters",
          "exceptions"
        ],
        "properties": {
          "flag": { "bsonType": "long" },
          "type": { "bsonType": "string" },
          "name": { "bsonType": "string" },
          "annotations": {
            "bsonType": "object",
            "required": ["className", "isRuntimeVisible", "valuePairs"],
            "properties": {
              "className": { "bsonType": "string" },
              "isRuntimeVisible": { "bsonType": "bool" },
              "valuePairs": {
                "bsonType": "array",
                "items": { "bsonType": "string" }
              }
            }
          },
          "parameters": {
            "bsonType": "array",
            "items": { "bsonType": "string" }
          },
          "exceptions": {
            "bsonType": "array",
            "items": { "bsonType": "string" }
          }
        }
      },
      "fields": {
        "bsonType": "object",
        "required": ["flag", "type", "name", "annotations"],
        "properties": {
          "flag": { "bsonType": "long" },
          "type": { "bsonType": "string" },
          "name": { "bsonType": "string" },
          "annotations": {
            "bsonType": "object",
            "required": ["className", "isRuntimeVisible", "valuePairs"],
            "properties": {
              "className": { "bsonType": "string" },
              "isRuntimeVisible": { "bsonType": "bool" },
              "valuePairs": {
                "bsonType": "array",
                "items": { "bsonType": "string" }
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

1. **_id**. 40 byte SHA1 hash to uniquely identify a class. This hash is computed from all properties in this collection. We expect a class signature object to be read only when using it. 
2. **className**. Full class name with package name, e.g. `edu.pku.migrationhelper.data.api.ClassSignature`.
3. **flags**. An integer to store all modifiers to this class, by the following definition.
```java
public static final long PUBLIC = 1;
public static final long PROTECTED = 1 << 1;
public static final long PACKAGE = 1 << 2;
public static final long PRIVATE = 1 << 3;
public static final long STATIC = 1 << 4;
public static final long FINAL = 1 << 5;
public static final long ABSTRACT = 1 << 6;
public static final long TRANSIENT = 1 << 7;
public static final long SYNCHRONIZED = 1 << 8;
public static final long VOLATILE = 1 << 9;
public static final long INTERFACE = 1 << 10;
public static final long ENUM = 1 << 11;
public static final long NESTED = 1 << 12;
public static final long ANONYMOUS = 1 << 13;
public static final long NATIVE = 1 << 14;
```
4. **superClassName**. The name of its super class.
5. **superClassId**. The 40 byte SHA1 hash of its super class. This reference is kept because a name may correspond to multiple class versions.
6. **interfaceNames**. The names of its implemented interfaces.
7. **interfaceIds**. The 40 byte SHA1 hashes of its implemented inferfaces.
8. **methods**. An array of the public methods that this class has.
  1. **methods[i].flag**. An integer to store all modifiers to this method, by the same definition as above. (some modifiders is not applicable to methods).
  2. **methods[i].type**. The return type of this method, in full class name.
  3. **methods[i].name**. The name of this method.
  4. **methods[i].annotations**. The annotations of this method.
    1. **methods[i].annotations[j].className**. The full class name of this annotation. 
    2. **methods[i].annotations[j].isRuntimeVisible**. Whether this annotation is runtime visible.
    3. **methods[i].annotations[j].valuePairs**. The value pairs of this annotation, denoted as "x=y".
  5. **methods[i].parameters**. The parameters of this method, each item is full class name (representing the type). We do not store parameter names.
  6. **methods[i].exceptions**. The exceptions of this method stored as full class name.
9. **fields**. An array of the public fields that this class has.
  1. **fields[i].flag**. An integer to store all modifiers to this field, by the same definition as above. (some modifiders is not applicable to fields).
  2. **fields[i].type**. The type of this field, in full class name.
  3. **fields[i].name**. The name of this field.
  4. **fields[i].annotations**. The annotations of this field.
    1. **fields[i].annotations[j].className**. The full class name of this annotation. 
    2. **fields[i].annotations[j].isRuntimeVisible**. Whether this annotation is runtime visible.
    3. **fields[i].annotations[j].valuePairs**. The value pairs of this annotation, denoted as "x=y".

## Additional Notes

Although during design I intentionally stored 40 byte SHA1 signature of classes in some places, I have found it impractical to store the signature everywhere. In fact, during code analysis you only need to use class name to resolve a class, because you can know which version it is by using the `libraryVerstionToClass` collection. (Suppose you already know which library version the code is using, by parsing the relevant `pom.xml` files.)