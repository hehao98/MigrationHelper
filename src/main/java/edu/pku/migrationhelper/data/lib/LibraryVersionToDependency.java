package edu.pku.migrationhelper.data.lib;

import edu.pku.migrationhelper.service.PomAnalysisService;
import org.springframework.data.annotation.Id;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LibraryVersionToDependency {

    @Id
    private long id;

    private String groupId;

    private String artifactId;

    private String version;

    private boolean hasError;

    private Set<PomAnalysisService.LibraryInfo> dependencies = new HashSet<>();

    public long getId() {
        return id;
    }

    public LibraryVersionToDependency setId(long id) {
        this.id = id;
        return this;
    }

    public String getGroupId() {
        return groupId;
    }

    public LibraryVersionToDependency setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public LibraryVersionToDependency setArtifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public LibraryVersionToDependency setVersion(String version) {
        this.version = version;
        return this;
    }

    public boolean isHasError() {
        return hasError;
    }

    public LibraryVersionToDependency setHasError(boolean hasError) {
        this.hasError = hasError;
        return this;
    }

    public Set<PomAnalysisService.LibraryInfo> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    public LibraryVersionToDependency setDependencies(Collection<PomAnalysisService.LibraryInfo> dependencies) {
        this.dependencies = new HashSet<>(dependencies);
        return this;
    }

    public boolean addDependency(PomAnalysisService.LibraryInfo dependency) {
        return this.dependencies.add(dependency);
    }
}
