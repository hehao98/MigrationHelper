package edu.pku.migrationhelper.data.lio;

import org.springframework.data.annotation.Id;

public class LioRepositoryDependency {
    
    @Id
    private long id;
    private String hostType;
    private String repositoryNameWithOwner;
    private String repositoryId;
    private String manifestPlatform;
    private String manifestFilePath;
    private String gitBranch;
    private String manifestKind;
    private boolean optional;
    private long dependencyProjectId;
    private String dependencyProjectName;
    private String dependencyRequirements;
    private String dependencyKind;

    public long getId() {
        return id;
    }

    public LioRepositoryDependency setId(long id) {
        this.id = id;
        return this;
    }

    public String getHostType() {
        return hostType;
    }

    public LioRepositoryDependency setHostType(String hostType) {
        this.hostType = hostType;
        return this;
    }

    public String getRepositoryNameWithOwner() {
        return repositoryNameWithOwner;
    }

    public LioRepositoryDependency setRepositoryNameWithOwner(String repositoryNameWithOwner) {
        this.repositoryNameWithOwner = repositoryNameWithOwner;
        return this;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public LioRepositoryDependency setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
        return this;
    }

    public String getManifestPlatform() {
        return manifestPlatform;
    }

    public LioRepositoryDependency setManifestPlatform(String manifestPlatform) {
        this.manifestPlatform = manifestPlatform;
        return this;
    }

    public String getManifestFilePath() {
        return manifestFilePath;
    }

    public LioRepositoryDependency setManifestFilePath(String manifestFilePath) {
        this.manifestFilePath = manifestFilePath;
        return this;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public LioRepositoryDependency setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
        return this;
    }

    public String getManifestKind() {
        return manifestKind;
    }

    public LioRepositoryDependency setManifestKind(String manifestKind) {
        this.manifestKind = manifestKind;
        return this;
    }

    public boolean isOptional() {
        return optional;
    }

    public LioRepositoryDependency setOptional(boolean optional) {
        this.optional = optional;
        return this;
    }

    public long getDependencyProjectId() {
        return dependencyProjectId;
    }

    public LioRepositoryDependency setDependencyProjectId(long dependencyProjectId) {
        this.dependencyProjectId = dependencyProjectId;
        return this;
    }

    public String getDependencyProjectName() {
        return dependencyProjectName;
    }

    public LioRepositoryDependency setDependencyProjectName(String dependencyProjectName) {
        this.dependencyProjectName = dependencyProjectName;
        return this;
    }

    public String getDependencyRequirements() {
        return dependencyRequirements;
    }

    public LioRepositoryDependency setDependencyRequirements(String dependencyRequirements) {
        this.dependencyRequirements = dependencyRequirements;
        return this;
    }

    public String getDependencyKind() {
        return dependencyKind;
    }

    public LioRepositoryDependency setDependencyKind(String dependencyKind) {
        this.dependencyKind = dependencyKind;
        return this;
    }
}
