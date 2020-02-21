package edu.pku.migrationhelper.data;

/**
 * Created by xuyul on 2020/1/2.
 */
public class LibraryVersion {

    private long id;

    private long groupArtifactId;

    private String version;

    private boolean downloaded;

    private boolean parsed;

    private boolean parseError = false;

    public long getId() {
        return id;
    }

    public LibraryVersion setId(long id) {
        this.id = id;
        return this;
    }

    public long getGroupArtifactId() {
        return groupArtifactId;
    }

    public LibraryVersion setGroupArtifactId(long groupArtifactId) {
        this.groupArtifactId = groupArtifactId;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public LibraryVersion setVersion(String version) {
        this.version = version;
        return this;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public LibraryVersion setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
        return this;
    }

    public boolean isParsed() {
        return parsed;
    }

    public LibraryVersion setParsed(boolean parsed) {
        this.parsed = parsed;
        return this;
    }

    public boolean isParseError() {
        return parseError;
    }

    public LibraryVersion setParseError(boolean parseError) {
        this.parseError = parseError;
        return this;
    }
}
