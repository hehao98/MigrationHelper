# RESTful API Documentation

Currently, I do not have time to document the behavior of each API in detail. However, you can quickly checkout what is returned using the following example links.

## Error Message Example

```
http://migration-helper.net:22568/recommend?fromLib=something:non-existent&pageNum=0&pageSize=20
```
```json
{
  "timestamp": "2020-10-23T02:28:34.015+0000",
  "status": 404,
  "error": "404 Not Found",
  "message": "fromLib something:non-existent does not exist",
  "path": "/recommend",
  "url": "http://migration-helper.net:22568/recommend?fromLib=something:non-existent&page=0&size=20"
}
```


## Query Migration Recommendations for a Library

```
http://migration-helper.net:22568/recommend?fromLib=org.json:json&page=0&size=20
```

## Query Related Repositories and Commits for a Source/Target Library Pair

```
http://migration-helper.net:22568/recommend-one?fromLib=org.json:json&toLib=com.google.code.gson:gson
```

## Query Libraries.io Information of a Library

```
http://migration-helper.net:22568/library?lib=org.json:json
```

## Query Top-20 Libraries with Prefix

```
http://www.migration-helper.net:22568/libraries-with-prefix?prefix=org.jso
```

## Query Top-20 Libraries with Similar Names (Edit Distance)

```
http://www.migration-helper.net:22568/libraries-similar?lib=org.gson
```

