package edu.pku.migrationhelper.data.woc;

import edu.pku.migrationhelper.data.lib.LibraryInfo;
import org.springframework.data.annotation.Id;

import java.util.List;

public class WocPomBlob {

    @Id
    private final String id; // 40 byte blob SHA1

    private final boolean error;

    private final List<LibraryInfo> dependencies;

    public WocPomBlob(String id, boolean error, List<LibraryInfo> dependencies) {
        this.id = id;
        this.error = error;
        this.dependencies = dependencies;
    }

    public String getId() {
        return id;
    }

    public boolean isError() {
        return error;
    }

    public List<LibraryInfo> getDependencies() {
        return dependencies;
    }
}
