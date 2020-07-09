package edu.pku.migrationhelper.data.lib;

import org.springframework.data.annotation.Id;

/**
 * Created by xuyul on 2020/1/3.
 */
public class LibraryGroupArtifact {

    @Id
    private long id;

    private String groupId;

    private String artifactId;

    private boolean versionExtracted;

    private boolean parsed;

    private boolean parseError = false;

    @Override
    public String toString() { return getGroupArtifactId(); }

    public long getId() {
        return id;
    }

    public LibraryGroupArtifact setId(long id) {
        this.id = id;
        return this;
    }

    public String getGroupId() {
        return groupId;
    }

    public LibraryGroupArtifact setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public LibraryGroupArtifact setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public String getGroupArtifactId() { return groupId + ":" + artifactId; }

    public boolean isVersionExtracted() {
        return versionExtracted;
    }

    public LibraryGroupArtifact setVersionExtracted(boolean versionExtracted) {
        this.versionExtracted = versionExtracted;
        return this;
    }

    public boolean isParsed() {
        return parsed;
    }

    public LibraryGroupArtifact setParsed(boolean parsed) {
        this.parsed = parsed;
        return this;
    }

    public boolean isParseError() {
        return parseError;
    }

    public LibraryGroupArtifact setParseError(boolean parseError) {
        this.parseError = parseError;
        return this;
    }
}
