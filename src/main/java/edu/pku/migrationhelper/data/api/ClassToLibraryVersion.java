package edu.pku.migrationhelper.data.api;

import org.springframework.data.annotation.Id;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClassToLibraryVersion {

    @Id
    private String classId; // id field of class (40 byte SHA1)

    // The id in MongoDB libraryVersionToClassCollection, ONLY classes in JAR
    // Ideally, it should also can be used to query MySQL library_version table
    private Set<Long> versionIds;

    public String getClassId() {
        return classId;
    }

    public ClassToLibraryVersion setClassId(String classId) {
        this.classId = classId;
        return this;
    }

    public Set<Long> getVersionIds() {
        return Collections.unmodifiableSet(versionIds);
    }

    public boolean hasVersionId(long versionId) {
        return versionIds.contains(versionId);
    }

    public boolean addVersionId(long versionId) {
        return versionIds.add(versionId);
    }

    public boolean addVersionIds(Collection<Long> versionIds) {
        return this.versionIds.addAll(versionIds);
    }

    public ClassToLibraryVersion setVersionIds(Collection<Long> versionIds) {
        this.versionIds = new HashSet<>(versionIds);
        return this;
    }
}
