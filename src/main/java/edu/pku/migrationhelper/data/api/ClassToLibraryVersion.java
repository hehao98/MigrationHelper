package edu.pku.migrationhelper.data.api;

import org.springframework.data.annotation.Id;
import java.util.List;

public class ClassToLibraryVersion {

    @Id
    private String classId; // id field of class (40 byte SHA1)

    // The id in MongoDB libraryVersionToClassCollection
    // Ideally, it should also can be used to query MySQL library_version table
    private List<Long> versionIds;

    public String getClassId() {
        return classId;
    }

    public ClassToLibraryVersion setClassId(String classId) {
        this.classId = classId;
        return this;
    }

    public List<Long> getVersionIds() {
        return versionIds;
    }

    public ClassToLibraryVersion setVersionIds(List<Long> versionIds) {
        this.versionIds = versionIds;
        return this;
    }
}
