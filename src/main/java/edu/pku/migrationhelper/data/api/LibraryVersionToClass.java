package edu.pku.migrationhelper.data.api;

import org.springframework.data.annotation.Id;
import scala.collection.immutable.List;

public class LibraryVersionToClass {

    @Id
    private long id; // The id in MySQL library_version table, should be unique

    private List<String> classIds;

    public long getId() {
        return id;
    }

    public LibraryVersionToClass setId(long id) {
        this.id = id;
        return this;
    }

    public List<String> getClassIds() {
        return classIds;
    }

    public LibraryVersionToClass setClassIds(List<String> classIds) {
        this.classIds = classIds;
        return this;
    }
}
