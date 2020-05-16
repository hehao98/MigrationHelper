package edu.pku.migrationhelper.data;

import java.util.List;

public class RepositoryDepSeq {

    private long id;

    private String repoName;

    private byte[] pomOnly;

    private List<Long> pomOnlyList;

    private byte[] pomOnlyCommits;

    private byte[] codeWithDup;

    private List<Long> codeWithDupList;

    private byte[] codeWithoutDup;

    private List<Long> codeWithoutDupList;

    private byte[] pomWithCodeDel;

    private List<Long> pomWithCodeDelList;

    private byte[] pomWithCodeAdd;

    private List<Long> pomWithCodeAddList;

    public long getId() {
        return id;
    }

    public RepositoryDepSeq setId(long id) {
        this.id = id;
        return this;
    }

    public String getRepoName() {
        return repoName;
    }

    public RepositoryDepSeq setRepoName(String repoName) {
        this.repoName = repoName;
        return this;
    }

    public byte[] getPomOnly() {
        return pomOnly;
    }

    public RepositoryDepSeq setPomOnly(byte[] pomOnly) {
        GetSetHelper.berNumberByteSetter(pomOnly, e -> this.pomOnly = e, e -> this.pomOnlyList = e);
        return this;
    }

    public List<Long> getPomOnlyList() {
        return pomOnlyList;
    }

    public RepositoryDepSeq setPomOnlyList(List<Long> pomOnlyList) {
        GetSetHelper.berNumberListSetter(pomOnlyList, e -> this.pomOnly = e, e -> this.pomOnlyList = e);
        return this;
    }

    public byte[] getPomOnlyCommits() {
        return pomOnlyCommits;
    }

    public RepositoryDepSeq setPomOnlyCommits(byte[] pomOnlyCommits) {
        this.pomOnlyCommits = pomOnlyCommits;
        return this;
    }

    public byte[] getCodeWithDup() {
        return codeWithDup;
    }

    public RepositoryDepSeq setCodeWithDup(byte[] codeWithDup) {
        GetSetHelper.berNumberByteSetter(codeWithDup, e -> this.codeWithDup = e, e -> this.codeWithDupList = e);
        return this;
    }

    public List<Long> getCodeWithDupList() {
        return codeWithDupList;
    }

    public RepositoryDepSeq setCodeWithDupList(List<Long> codeWithDupList) {
        GetSetHelper.berNumberListSetter(codeWithDupList, e -> this.codeWithDup = e, e -> this.codeWithDupList = e);
        return this;
    }

    public byte[] getCodeWithoutDup() {
        return codeWithoutDup;
    }

    public RepositoryDepSeq setCodeWithoutDup(byte[] codeWithoutDup) {
        GetSetHelper.berNumberByteSetter(codeWithoutDup, e -> this.codeWithoutDup = e, e -> this.codeWithoutDupList = e);
        return this;
    }

    public List<Long> getCodeWithoutDupList() {
        return codeWithoutDupList;
    }

    public RepositoryDepSeq setCodeWithoutDupList(List<Long> codeWithoutDupList) {
        GetSetHelper.berNumberListSetter(codeWithoutDupList, e -> this.codeWithoutDup = e, e -> this.codeWithoutDupList = e);
        return this;
    }

    public byte[] getPomWithCodeDel() {
        return pomWithCodeDel;
    }

    public RepositoryDepSeq setPomWithCodeDel(byte[] pomWithCodeDel) {
        GetSetHelper.berNumberByteSetter(pomWithCodeDel, e -> this.pomWithCodeDel = e, e -> this.pomWithCodeDelList = e);
        return this;
    }

    public List<Long> getPomWithCodeDelList() {
        return pomWithCodeDelList;
    }

    public RepositoryDepSeq setPomWithCodeDelList(List<Long> pomWithCodeDelList) {
        GetSetHelper.berNumberListSetter(pomWithCodeDelList, e -> this.pomWithCodeDel = e, e -> this.pomWithCodeDelList = e);
        return this;
    }

    public byte[] getPomWithCodeAdd() {
        return pomWithCodeAdd;
    }

    public RepositoryDepSeq setPomWithCodeAdd(byte[] pomWithCodeAdd) {
        GetSetHelper.berNumberByteSetter(pomWithCodeAdd, e -> this.pomWithCodeAdd = e, e -> this.pomWithCodeAddList = e);
        return this;
    }

    public List<Long> getPomWithCodeAddList() {
        return pomWithCodeAddList;
    }

    public RepositoryDepSeq setPomWithCodeAddList(List<Long> pomWithCodeAddList) {
        GetSetHelper.berNumberListSetter(pomWithCodeAddList, e -> this.pomWithCodeAdd = e, e -> this.pomWithCodeAddList = e);
        return this;
    }
}
