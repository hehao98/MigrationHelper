package edu.pku.migrationhelper.data.woc;

import org.springframework.data.annotation.Id;

import java.util.List;

public class WocCommit {
    @Id
    private final String id; // 40 byte SHA1
    private final boolean error;
    private final String timestamp;
    private final String message;
    private final List<String> parents;
    private final List<WocDiff> diffs;

    public WocCommit(String id, boolean error, String timestamp, String message, List<String> parents, List<WocDiff> diffs) {
        this.id = id;
        this.error = error;
        this.timestamp = timestamp;
        this.message = message;
        this.parents = parents;
        this.diffs = diffs;
    }

    public String getId() {
        return id;
    }

    public boolean isError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<String> getParents() {
        return parents;
    }

    public List<WocDiff> getDiffs() {
        return diffs;
    }
}
