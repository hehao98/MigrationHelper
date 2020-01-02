package edu.pku.migrationhelper.data;

/**
 * Created by xuyul on 2020/1/2.
 */
public class LibraryVersion {

    private long id;

    private String groupId;

    private String artifactId;

    private String version;

    public long getId() {
        return id;
    }

    public LibraryVersion setId(long id) {
        this.id = id;
        return this;
    }

    public String getGroupId() {
        return groupId;
    }

    public LibraryVersion setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public LibraryVersion setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public LibraryVersion setVersion(String version) {
        this.version = version;
        return this;
    }
}
