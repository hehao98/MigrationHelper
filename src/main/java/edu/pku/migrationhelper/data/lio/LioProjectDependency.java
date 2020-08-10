package edu.pku.migrationhelper.data.lio;

import org.springframework.data.annotation.Id;

public class LioProjectDependency {

    @Id
    private long id;
    private String platform;
    private String projectName;
    private long projectId;
    private String versionNumber;
    private long versionId;
    private String dependencyName;
    private String dependencyPlatform;
    private String dependencyKind;
    private boolean optionalDependency;
    private String dependencyRequirements;
    private long dependencyProjectId;

    public long getId() {
        return id;
    }

    public LioProjectDependency setId(long id) {
        this.id = id;
        return this;
    }

    public String getPlatform() {
        return platform;
    }

    public LioProjectDependency setPlatform(String platform) {
        this.platform = platform;
        return this;
    }

    public String getProjectName() {
        return projectName;
    }

    public LioProjectDependency setProjectName(String projectName) {
        this.projectName = projectName;
        return this;
    }

    public long getProjectId() {
        return projectId;
    }

    public LioProjectDependency setProjectId(long projectId) {
        this.projectId = projectId;
        return this;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public LioProjectDependency setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
        return this;
    }

    public long getVersionId() {
        return versionId;
    }

    public LioProjectDependency setVersionId(long versionId) {
        this.versionId = versionId;
        return this;
    }

    public String getDependencyName() {
        return dependencyName;
    }

    public LioProjectDependency setDependencyName(String dependencyName) {
        this.dependencyName = dependencyName;
        return this;
    }

    public String getDependencyPlatform() {
        return dependencyPlatform;
    }

    public LioProjectDependency setDependencyPlatform(String dependencyPlatform) {
        this.dependencyPlatform = dependencyPlatform;
        return this;
    }

    public String getDependencyKind() {
        return dependencyKind;
    }

    public LioProjectDependency setDependencyKind(String dependencyKind) {
        this.dependencyKind = dependencyKind;
        return this;
    }

    public boolean isOptionalDependency() {
        return optionalDependency;
    }

    public LioProjectDependency setOptionalDependency(boolean optionalDependency) {
        this.optionalDependency = optionalDependency;
        return this;
    }

    public String getDependencyRequirements() {
        return dependencyRequirements;
    }

    public LioProjectDependency setDependencyRequirements(String dependencyRequirements) {
        this.dependencyRequirements = dependencyRequirements;
        return this;
    }

    public long getDependencyProjectId() {
        return dependencyProjectId;
    }

    public LioProjectDependency setDependencyProjectId(long dependencyProjectId) {
        this.dependencyProjectId = dependencyProjectId;
        return this;
    }
}
