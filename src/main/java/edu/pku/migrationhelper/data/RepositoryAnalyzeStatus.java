package edu.pku.migrationhelper.data;

import java.util.Date;

public class RepositoryAnalyzeStatus {

    public enum RepoType {
        WoC,
        Git,
    }

    public enum AnalyzeStatus {
        Analyzing,
        Success,
        Error,
    }

    public long id;

    public RepoType repoType;

    public String repoName;

    public Date startTime;

    public Date endTime;

    public AnalyzeStatus analyzeStatus;

    public long getId() {
        return id;
    }

    public RepositoryAnalyzeStatus setId(long id) {
        this.id = id;
        return this;
    }

    public RepoType getRepoType() {
        return repoType;
    }

    public RepositoryAnalyzeStatus setRepoType(RepoType repoType) {
        this.repoType = repoType;
        return this;
    }

    public String getRepoName() {
        return repoName;
    }

    public RepositoryAnalyzeStatus setRepoName(String repoName) {
        this.repoName = repoName;
        return this;
    }

    public Date getStartTime() {
        return startTime;
    }

    public RepositoryAnalyzeStatus setStartTime(Date startTime) {
        this.startTime = startTime;
        return this;
    }

    public Date getEndTime() {
        return endTime;
    }

    public RepositoryAnalyzeStatus setEndTime(Date endTime) {
        this.endTime = endTime;
        return this;
    }

    public AnalyzeStatus getAnalyzeStatus() {
        return analyzeStatus;
    }

    public RepositoryAnalyzeStatus setAnalyzeStatus(AnalyzeStatus analyzeStatus) {
        this.analyzeStatus = analyzeStatus;
        return this;
    }
}
