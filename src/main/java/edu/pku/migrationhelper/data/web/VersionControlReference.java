package edu.pku.migrationhelper.data.web;

import java.util.Objects;

public class VersionControlReference {
    private final boolean isConfirmed;
    private final boolean isPossible;
    private final String repoName;
    private final String startCommit;
    private final String endCommit;
    private final String fileName;

    public VersionControlReference(
            boolean isConfirmed,
            boolean isPossible,
            String repoName,
            String startCommit,
            String endCommit,
            String fileName
    ) {
        this.isConfirmed = isConfirmed;
        this.isPossible = isPossible;
        this.repoName = repoName;
        this.startCommit = startCommit;
        this.endCommit = endCommit;
        this.fileName = fileName;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public boolean isPossible() {
        return isPossible;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getStartCommit() {
        return startCommit;
    }

    public String getEndCommit() {
        return endCommit;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionControlReference that = (VersionControlReference) o;
        return isConfirmed == that.isConfirmed &&
                isPossible == that.isPossible &&
                repoName.equals(that.repoName) &&
                startCommit.equals(that.startCommit) &&
                endCommit.equals(that.endCommit) &&
                fileName.equals(that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isConfirmed, isPossible, repoName, startCommit, endCommit, fileName);
    }
}
