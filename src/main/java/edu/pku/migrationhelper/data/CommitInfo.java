package edu.pku.migrationhelper.data;

/**
 * Created by xuyul on 2020/2/4.
 */
public class CommitInfo {

    private String commitId;

    private String codeLibraryVersionIds = "";

    private String codeGroupArtifactIds = "";

    private String codeDeleteGroupArtifactIds = "";

    private String codeAddGroupArtifactIds = "";

    private String pomLibraryVersionIds = "";

    private String pomGroupArtifactIds = "";

    private String pomDeleteGroupArtifactIds = "";

    private String pomAddGroupArtifactIds = "";

    private String methodChangeIds = "";

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

    public String getCodeGroupArtifactIds() {
        return codeGroupArtifactIds;
    }

    public CommitInfo setCodeGroupArtifactIds(String codeGroupArtifactIds) {
        this.codeGroupArtifactIds = codeGroupArtifactIds;
        return this;
    }

    public String getCodeDeleteGroupArtifactIds() {
        return codeDeleteGroupArtifactIds;
    }

    public CommitInfo setCodeDeleteGroupArtifactIds(String codeDeleteGroupArtifactIds) {
        this.codeDeleteGroupArtifactIds = codeDeleteGroupArtifactIds;
        return this;
    }

    public String getCodeAddGroupArtifactIds() {
        return codeAddGroupArtifactIds;
    }

    public CommitInfo setCodeAddGroupArtifactIds(String codeAddGroupArtifactIds) {
        this.codeAddGroupArtifactIds = codeAddGroupArtifactIds;
        return this;
    }

    public String getPomLibraryVersionIds() {
        return pomLibraryVersionIds;
    }

    public CommitInfo setPomLibraryVersionIds(String pomLibraryVersionIds) {
        this.pomLibraryVersionIds = pomLibraryVersionIds;
        return this;
    }

    public String getPomGroupArtifactIds() {
        return pomGroupArtifactIds;
    }

    public CommitInfo setPomGroupArtifactIds(String pomGroupArtifactIds) {
        this.pomGroupArtifactIds = pomGroupArtifactIds;
        return this;
    }

    public String getPomDeleteGroupArtifactIds() {
        return pomDeleteGroupArtifactIds;
    }

    public CommitInfo setPomDeleteGroupArtifactIds(String pomDeleteGroupArtifactIds) {
        this.pomDeleteGroupArtifactIds = pomDeleteGroupArtifactIds;
        return this;
    }

    public String getPomAddGroupArtifactIds() {
        return pomAddGroupArtifactIds;
    }

    public CommitInfo setPomAddGroupArtifactIds(String pomAddGroupArtifactIds) {
        this.pomAddGroupArtifactIds = pomAddGroupArtifactIds;
        return this;
    }

    public String getMethodChangeIds() {
        return methodChangeIds;
    }

    public CommitInfo setMethodChangeIds(String methodChangeIds) {
        this.methodChangeIds = methodChangeIds;
        return this;
    }
}
