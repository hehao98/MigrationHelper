package edu.pku.migrationhelper.data.lib;

/**
 * Created by xuyul on 2020/2/16.
 */
public class LioProjectWithRepository {

    private long id;

    private String platform;

    private String language;

    private String name;

    private String repositoryUrl;

    private long repositoryId;

    private int sourceRank;

    private int repositoryStarCount;

    private int repositoryForkCount;

    private int repositoryWatchersCount;

    private int repositorySourceRank;

    private int dependentProjectsCount;

    private int dependentRepositoriesCount;

    public long getId() {
        return id;
    }

    public LioProjectWithRepository setId(long id) {
        this.id = id;
        return this;
    }

    public String getPlatform() {
        return platform;
    }

    public LioProjectWithRepository setPlatform(String platform) {
        this.platform = platform;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public LioProjectWithRepository setLanguage(String language) {
        this.language = language;
        return this;
    }

    public String getName() {
        return name;
    }

    public LioProjectWithRepository setName(String name) {
        this.name = name;
        return this;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public LioProjectWithRepository setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
        return this;
    }

    public long getRepositoryId() {
        return repositoryId;
    }

    public LioProjectWithRepository setRepositoryId(long repositoryId) {
        this.repositoryId = repositoryId;
        return this;
    }

    public int getSourceRank() {
        return sourceRank;
    }

    public LioProjectWithRepository setSourceRank(int sourceRank) {
        this.sourceRank = sourceRank;
        return this;
    }

    public int getRepositoryStarCount() {
        return repositoryStarCount;
    }

    public LioProjectWithRepository setRepositoryStarCount(int repositoryStarCount) {
        this.repositoryStarCount = repositoryStarCount;
        return this;
    }

    public int getRepositoryForkCount() {
        return repositoryForkCount;
    }

    public LioProjectWithRepository setRepositoryForkCount(int repositoryForkCount) {
        this.repositoryForkCount = repositoryForkCount;
        return this;
    }

    public int getRepositoryWatchersCount() {
        return repositoryWatchersCount;
    }

    public LioProjectWithRepository setRepositoryWatchersCount(int repositoryWatchersCount) {
        this.repositoryWatchersCount = repositoryWatchersCount;
        return this;
    }

    public int getRepositorySourceRank() {
        return repositorySourceRank;
    }

    public LioProjectWithRepository setRepositorySourceRank(int repositorySourceRank) {
        this.repositorySourceRank = repositorySourceRank;
        return this;
    }

    public int getDependentProjectsCount() {
        return dependentProjectsCount;
    }

    public LioProjectWithRepository setDependentProjectsCount(int dependentProjectsCount) {
        this.dependentProjectsCount = dependentProjectsCount;
        return this;
    }

    public int getDependentRepositoriesCount() {
        return dependentRepositoriesCount;
    }

    public LioProjectWithRepository setDependentRepositoriesCount(int dependentRepositoriesCount) {
        this.dependentRepositoriesCount = dependentRepositoriesCount;
        return this;
    }
}
