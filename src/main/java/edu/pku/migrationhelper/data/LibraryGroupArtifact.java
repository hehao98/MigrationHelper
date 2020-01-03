package edu.pku.migrationhelper.data;

/**
 * Created by xuyul on 2020/1/3.
 */
public class LibraryGroupArtifact {

    private long id;

    private String groupId;

    private String artifactId;

    private boolean versionExtracted;

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

    public boolean isVersionExtracted() {
        return versionExtracted;
    }

    public LibraryGroupArtifact setVersionExtracted(boolean versionExtracted) {
        this.versionExtracted = versionExtracted;
        return this;
    }
}
