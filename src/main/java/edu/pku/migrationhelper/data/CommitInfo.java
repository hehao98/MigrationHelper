package edu.pku.migrationhelper.data;

import java.util.List;

/**
 * Created by xuyul on 2020/2/4.
 */
public class CommitInfo {

    private byte[] commitId;

    private byte[] codeLibraryVersionIds;

    private byte[] codeGroupArtifactIds;

    private byte[] codeDeleteGroupArtifactIds;

    private byte[] codeAddGroupArtifactIds;

    private byte[] pomLibraryVersionIds;

    private byte[] pomGroupArtifactIds;

    private byte[] pomDeleteGroupArtifactIds;

    private byte[] pomAddGroupArtifactIds;

    private byte[] methodChangeIds;

    private String commitIdString;

    private List<Long> codeLibraryVersionIdList;

    private List<Long> codeGroupArtifactIdList;

    private List<Long> codeDeleteGroupArtifactIdList;

    private List<Long> codeAddGroupArtifactIdList;

    private List<Long> pomLibraryVersionIdList;

    private List<Long> pomGroupArtifactIdList;

    private List<Long> pomDeleteGroupArtifactIdList;

    private List<Long> pomAddGroupArtifactIdList;

    private List<Long> methodChangeIdList;

    public byte[] getCommitId() {
        return commitId;
    }

    public CommitInfo setCommitId(byte[] commitId) {
        GetSetHelper.hexByteSetter(commitId, e -> this.commitId = e, e -> this.commitIdString = e);
        return this;
    }

    public String getCommitIdString() {
        return commitIdString;
    }

    public CommitInfo setCommitIdString(String commitIdString) {
        GetSetHelper.hexStringSetter(commitIdString, e -> this.commitId = e, e -> this.commitIdString = e);
        return this;
    }

    public byte[] getCodeLibraryVersionIds() {
        return codeLibraryVersionIds;
    }

    public CommitInfo setCodeLibraryVersionIds(byte[] codeLibraryVersionIds) {
        GetSetHelper.berNumberByteSetter(codeLibraryVersionIds, e -> this.codeLibraryVersionIds = e, e -> this.codeLibraryVersionIdList = e);
        return this;
    }

    public List<Long> getCodeLibraryVersionIdList() {
        return codeLibraryVersionIdList;
    }

    public CommitInfo setCodeLibraryVersionIdList(List<Long> codeLibraryVersionIdList) {
        GetSetHelper.berNumberListSetter(codeLibraryVersionIdList, e -> this.codeLibraryVersionIds = e, e -> this.codeLibraryVersionIdList = e);
        return this;
    }


    public byte[] getCodeGroupArtifactIds() {
        return codeGroupArtifactIds;
    }

    public CommitInfo setCodeGroupArtifactIds(byte[] codeGroupArtifactIds) {
        GetSetHelper.berNumberByteSetter(codeGroupArtifactIds, e -> this.codeGroupArtifactIds = e, e -> this.codeGroupArtifactIdList = e);
        return this;
    }

    public List<Long> getCodeGroupArtifactIdList() {
        return codeGroupArtifactIdList;
    }

    public CommitInfo setCodeGroupArtifactIdList(List<Long> codeGroupArtifactIdList) {
        GetSetHelper.berNumberListSetter(codeGroupArtifactIdList, e -> this.codeGroupArtifactIds = e, e -> this.codeGroupArtifactIdList = e);
        return this;
    }


    public byte[] getCodeDeleteGroupArtifactIds() {
        return codeDeleteGroupArtifactIds;
    }

    public CommitInfo setCodeDeleteGroupArtifactIds(byte[] codeDeleteGroupArtifactIds) {
        GetSetHelper.berNumberByteSetter(codeDeleteGroupArtifactIds, e -> this.codeDeleteGroupArtifactIds = e, e -> this.codeDeleteGroupArtifactIdList = e);
        return this;
    }

    public List<Long> getCodeDeleteGroupArtifactIdList() {
        return codeDeleteGroupArtifactIdList;
    }

    public CommitInfo setCodeDeleteGroupArtifactIdList(List<Long> codeDeleteGroupArtifactIdList) {
        GetSetHelper.berNumberListSetter(codeDeleteGroupArtifactIdList, e -> this.codeDeleteGroupArtifactIds = e, e -> this.codeDeleteGroupArtifactIdList = e);
        return this;
    }


    public byte[] getCodeAddGroupArtifactIds() {
        return codeAddGroupArtifactIds;
    }

    public CommitInfo setCodeAddGroupArtifactIds(byte[] codeAddGroupArtifactIds) {
        GetSetHelper.berNumberByteSetter(codeAddGroupArtifactIds, e -> this.codeAddGroupArtifactIds = e, e -> this.codeAddGroupArtifactIdList = e);
        return this;
    }

    public List<Long> getCodeAddGroupArtifactIdList() {
        return codeAddGroupArtifactIdList;
    }

    public CommitInfo setCodeAddGroupArtifactIdList(List<Long> codeAddGroupArtifactIdList) {
        GetSetHelper.berNumberListSetter(codeAddGroupArtifactIdList, e -> this.codeAddGroupArtifactIds = e, e -> this.codeAddGroupArtifactIdList = e);
        return this;
    }


    public byte[] getPomLibraryVersionIds() {
        return pomLibraryVersionIds;
    }

    public CommitInfo setPomLibraryVersionIds(byte[] pomLibraryVersionIds) {
        GetSetHelper.berNumberByteSetter(pomLibraryVersionIds, e -> this.pomLibraryVersionIds = e, e -> this.pomLibraryVersionIdList = e);
        return this;
    }

    public List<Long> getPomLibraryVersionIdList() {
        return pomLibraryVersionIdList;
    }

    public CommitInfo setPomLibraryVersionIdList(List<Long> pomLibraryVersionIdList) {
        GetSetHelper.berNumberListSetter(pomLibraryVersionIdList, e -> this.pomLibraryVersionIds = e, e -> this.pomLibraryVersionIdList = e);
        return this;
    }


    public byte[] getPomGroupArtifactIds() {
        return pomGroupArtifactIds;
    }

    public CommitInfo setPomGroupArtifactIds(byte[] pomGroupArtifactIds) {
        GetSetHelper.berNumberByteSetter(pomGroupArtifactIds, e -> this.pomGroupArtifactIds = e, e -> this.pomGroupArtifactIdList = e);
        return this;
    }

    public List<Long> getPomGroupArtifactIdList() {
        return pomGroupArtifactIdList;
    }

    public CommitInfo setPomGroupArtifactIdList(List<Long> pomGroupArtifactIdList) {
        GetSetHelper.berNumberListSetter(pomGroupArtifactIdList, e -> this.pomGroupArtifactIds = e, e -> this.pomGroupArtifactIdList = e);
        return this;
    }


    public byte[] getPomDeleteGroupArtifactIds() {
        return pomDeleteGroupArtifactIds;
    }

    public CommitInfo setPomDeleteGroupArtifactIds(byte[] pomDeleteGroupArtifactIds) {
        GetSetHelper.berNumberByteSetter(pomDeleteGroupArtifactIds, e -> this.pomDeleteGroupArtifactIds = e, e -> this.pomDeleteGroupArtifactIdList = e);
        return this;
    }

    public List<Long> getPomDeleteGroupArtifactIdList() {
        return pomDeleteGroupArtifactIdList;
    }

    public CommitInfo setPomDeleteGroupArtifactIdList(List<Long> pomDeleteGroupArtifactIdList) {
        GetSetHelper.berNumberListSetter(pomDeleteGroupArtifactIdList, e -> this.pomDeleteGroupArtifactIds = e, e -> this.pomDeleteGroupArtifactIdList = e);
        return this;
    }


    public byte[] getPomAddGroupArtifactIds() {
        return pomAddGroupArtifactIds;
    }

    public CommitInfo setPomAddGroupArtifactIds(byte[] pomAddGroupArtifactIds) {
        GetSetHelper.berNumberByteSetter(pomAddGroupArtifactIds, e -> this.pomAddGroupArtifactIds = e, e -> this.pomAddGroupArtifactIdList = e);
        return this;
    }

    public List<Long> getPomAddGroupArtifactIdList() {
        return pomAddGroupArtifactIdList;
    }

    public CommitInfo setPomAddGroupArtifactIdList(List<Long> pomAddGroupArtifactIdList) {
        GetSetHelper.berNumberListSetter(pomAddGroupArtifactIdList, e -> this.pomAddGroupArtifactIds = e, e -> this.pomAddGroupArtifactIdList = e);
        return this;
    }


    public byte[] getMethodChangeIds() {
        return methodChangeIds;
    }

    public CommitInfo setMethodChangeIds(byte[] methodChangeIds) {
        GetSetHelper.berNumberByteSetter(methodChangeIds, e -> this.methodChangeIds = e, e -> this.methodChangeIdList = e);
        return this;
    }

    public List<Long> getMethodChangeIdList() {
        return methodChangeIdList;
    }

    public CommitInfo setMethodChangeIdList(List<Long> methodChangeIdList) {
        GetSetHelper.berNumberListSetter(methodChangeIdList, e -> this.methodChangeIds = e, e -> this.methodChangeIdList = e);
        return this;
    }
}
