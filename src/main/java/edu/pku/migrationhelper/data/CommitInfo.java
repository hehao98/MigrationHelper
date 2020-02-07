package edu.pku.migrationhelper.data;

/**
 * Created by xuyul on 2020/2/4.
 */
public class CommitInfo {

    private String commitId;

    private String codeLibraryVersionIds;

    private String pomLibraryVersionIds;

    public String getCommitId() {
        return commitId;
    }

    public CommitInfo setCommitId(String commitId) {
        this.commitId = commitId;
        return this;
    }

    public String getCodeLibraryVersionIds() {
        return codeLibraryVersionIds;
    }

    public CommitInfo setCodeLibraryVersionIds(String codeLibraryVersionIds) {
        this.codeLibraryVersionIds = codeLibraryVersionIds;
        return this;
    }

    public String getPomLibraryVersionIds() {
        return pomLibraryVersionIds;
    }

    public CommitInfo setPomLibraryVersionIds(String pomLibraryVersionIds) {
        this.pomLibraryVersionIds = pomLibraryVersionIds;
        return this;
    }
}
