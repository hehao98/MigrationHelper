package edu.pku.migrationhelper.data.lio;

import org.springframework.data.annotation.Id;

/**
 * Created by xuyul on 2020/2/16.
 */
public class LioProject {

    @Id
    private long id;

    private String platform;

    private String language;

    private String name;

    private String homepageUrl;

    private String description;

    private String keywords;

    private String repositoryUrl;

    private long repositoryId;

    private String repositoryDescription;

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

    public LioProject setId(long id) {
        this.id = id;
        return this;
    }

    public String getPlatform() {
        return platform;
    }

    public LioProject setPlatform(String platform) {
        this.platform = platform;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public LioProject setLanguage(String language) {
        this.language = language;
        return this;
    }

    public String getName() {
        return name;
    }

    public LioProject setName(String name) {
        this.name = name;
        return this;
    }

    public String getHomepageUrl() {
        return homepageUrl;
    }

    public void setHomepageUrl(String homepageUrl) {
        this.homepageUrl = homepageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public LioProject setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
        return this;
    }

    public long getRepositoryId() {
        return repositoryId;
    }

    public LioProject setRepositoryId(long repositoryId) {
        this.repositoryId = repositoryId;
        return this;
    }

    public String getRepositoryDescription() {
        return repositoryDescription;
    }

    public void setRepositoryDescription(String repositoryDescription) {
        this.repositoryDescription = repositoryDescription;
    }

    public int getSourceRank() {
        return sourceRank;
    }

    public LioProject setSourceRank(int sourceRank) {
        this.sourceRank = sourceRank;
        return this;
    }

    public int getRepositoryStarCount() {
        return repositoryStarCount;
    }

    public LioProject setRepositoryStarCount(int repositoryStarCount) {
        this.repositoryStarCount = repositoryStarCount;
        return this;
    }

    public int getRepositoryForkCount() {
        return repositoryForkCount;
    }

    public LioProject setRepositoryForkCount(int repositoryForkCount) {
        this.repositoryForkCount = repositoryForkCount;
        return this;
    }

    public int getRepositoryWatchersCount() {
        return repositoryWatchersCount;
    }

    public LioProject setRepositoryWatchersCount(int repositoryWatchersCount) {
        this.repositoryWatchersCount = repositoryWatchersCount;
        return this;
    }

    public int getRepositorySourceRank() {
        return repositorySourceRank;
    }

    public LioProject setRepositorySourceRank(int repositorySourceRank) {
        this.repositorySourceRank = repositorySourceRank;
        return this;
    }

    public int getDependentProjectsCount() {
        return dependentProjectsCount;
    }

    public LioProject setDependentProjectsCount(int dependentProjectsCount) {
        this.dependentProjectsCount = dependentProjectsCount;
        return this;
    }

    public int getDependentRepositoriesCount() {
        return dependentRepositoriesCount;
    }

    public LioProject setDependentRepositoriesCount(int dependentRepositoriesCount) {
        this.dependentRepositoriesCount = dependentRepositoriesCount;
        return this;
    }
}
