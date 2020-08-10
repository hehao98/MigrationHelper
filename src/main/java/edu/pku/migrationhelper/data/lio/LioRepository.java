package edu.pku.migrationhelper.data.lio;

import org.springframework.data.annotation.Id;
import org.springframework.util.Assert;

public class LioRepository {

    @Id
    private long id;
    private String hostType;
    private String nameWithOwner;
    private String description;
    private boolean fork;
    private String forkSourceNameWithOwner;
    private String createdTimestamp;
    private String updatedTimestamp;
    private String lastPushedTimestamp;
    private String homepageURL;
    private String mirrorURL;
    private long size;
    private String language;
    private long starsCount;
    private long forksCount;
    private long openIssuesCount;
    private long watchersCount;
    private long contributorsCount;
    private String readmeFilename;
    private String changeLogFilename;
    private String contributingGuidelinesFilename;
    private String licenseFilename;
    private String codeOfConductFilename;

    public String getName() {
        return nameWithOwner.split("/")[1];
    }

    public String getOwner() {
        return nameWithOwner.split("/")[0];
    }

    /**
     * Return WoC name for GitHub repositories
     *   Other sources are not supported but can be done later.
     * @return WoC repository name
     */
    public String getWoCName() {
        Assert.isTrue(hostType.equals("GitHub"), "Only conversion to GitHub repository is supported");
        return nameWithOwner.replace("/", "_");
    }

    public long getId() {
        return id;
    }

    public LioRepository setId(long id) {
        this.id = id;
        return this;
    }

    public String getHostType() {
        return hostType;
    }

    public LioRepository setHostType(String hostType) {
        this.hostType = hostType;
        return this;
    }

    public String getNameWithOwner() {
        return nameWithOwner;
    }

    public LioRepository setNameWithOwner(String nameWithOwner) {
        this.nameWithOwner = nameWithOwner;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public LioRepository setDescription(String description) {
        this.description = description;
        return this;
    }

    public boolean isFork() {
        return fork;
    }

    public LioRepository setFork(boolean fork) {
        this.fork = fork;
        return this;
    }

    public String getForkSourceNameWithOwner() {
        return forkSourceNameWithOwner;
    }

    public LioRepository setForkSourceNameWithOwner(String forkSourceNameWithOwner) {
        this.forkSourceNameWithOwner = forkSourceNameWithOwner;
        return this;
    }

    public String getCreatedTimestamp() {
        return createdTimestamp;
    }

    public LioRepository setCreatedTimestamp(String createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
        return this;
    }

    public String getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    public LioRepository setUpdatedTimestamp(String updatedTimestamp) {
        this.updatedTimestamp = updatedTimestamp;
        return this;
    }

    public String getLastPushedTimestamp() {
        return lastPushedTimestamp;
    }

    public LioRepository setLastPushedTimestamp(String lastPushedTimestamp) {
        this.lastPushedTimestamp = lastPushedTimestamp;
        return this;
    }

    public String getHomepageURL() {
        return homepageURL;
    }

    public LioRepository setHomepageURL(String homepageURL) {
        this.homepageURL = homepageURL;
        return this;
    }

    public String getMirrorURL() {
        return mirrorURL;
    }

    public LioRepository setMirrorURL(String mirrorURL) {
        this.mirrorURL = mirrorURL;
        return this;
    }

    public long getSize() {
        return size;
    }

    public LioRepository setSize(long size) {
        this.size = size;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public LioRepository setLanguage(String language) {
        this.language = language;
        return this;
    }

    public long getStarsCount() {
        return starsCount;
    }

    public LioRepository setStarsCount(long starsCount) {
        this.starsCount = starsCount;
        return this;
    }

    public long getForksCount() {
        return forksCount;
    }

    public LioRepository setForksCount(long forksCount) {
        this.forksCount = forksCount;
        return this;
    }

    public long getOpenIssuesCount() {
        return openIssuesCount;
    }

    public LioRepository setOpenIssuesCount(long openIssuesCount) {
        this.openIssuesCount = openIssuesCount;
        return this;
    }

    public long getWatchersCount() {
        return watchersCount;
    }

    public LioRepository setWatchersCount(long watchersCount) {
        this.watchersCount = watchersCount;
        return this;
    }

    public long getContributorsCount() {
        return contributorsCount;
    }

    public LioRepository setContributorsCount(long contributorsCount) {
        this.contributorsCount = contributorsCount;
        return this;
    }

    public String getReadmeFilename() {
        return readmeFilename;
    }

    public LioRepository setReadmeFilename(String readmeFilename) {
        this.readmeFilename = readmeFilename;
        return this;
    }

    public String getChangeLogFilename() {
        return changeLogFilename;
    }

    public LioRepository setChangeLogFilename(String changeLogFilename) {
        this.changeLogFilename = changeLogFilename;
        return this;
    }

    public String getContributingGuidelinesFilename() {
        return contributingGuidelinesFilename;
    }

    public LioRepository setContributingGuidelinesFilename(String contributingGuidelinesFilename) {
        this.contributingGuidelinesFilename = contributingGuidelinesFilename;
        return this;
    }

    public String getLicenseFilename() {
        return licenseFilename;
    }

    public LioRepository setLicenseFilename(String licenseFilename) {
        this.licenseFilename = licenseFilename;
        return this;
    }

    public String getCodeOfConductFilename() {
        return codeOfConductFilename;
    }

    public LioRepository setCodeOfConductFilename(String codeOfConductFilename) {
        this.codeOfConductFilename = codeOfConductFilename;
        return this;
    }
}
