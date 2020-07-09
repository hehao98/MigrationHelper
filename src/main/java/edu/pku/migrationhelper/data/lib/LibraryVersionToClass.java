package edu.pku.migrationhelper.data.lib;

import org.springframework.data.annotation.Id;

import java.util.*;

public class LibraryVersionToClass {

    @Id
    private long id; // The id in MySQL library_version table, should be unique

    private String groupId;

    private String artifactId;

    private String version;

    private Set<String> classIds; // ONLY class ids included in this JAR

    public long getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public LibraryVersionToClass setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public LibraryVersionToClass setId(long id) {
        this.id = id;
        return this;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public LibraryVersionToClass setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public LibraryVersionToClass setVersion(String version) {
        this.version = version;
        return this;
    }

    public Set<String> getClassIds() {
        return Collections.unmodifiableSet(classIds);
    }

    public LibraryVersionToClass setClassIds(Collection<String> classIds) {
        this.classIds = new HashSet<>(classIds);
        return this;
    }

    public boolean addClassIds(Collection<String> classIds) {
        return this.classIds.addAll(classIds);
    }
}
