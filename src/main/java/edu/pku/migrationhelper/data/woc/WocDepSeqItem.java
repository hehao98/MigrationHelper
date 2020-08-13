package edu.pku.migrationhelper.data.woc;

import java.util.List;

public class WocDepSeqItem {
    private final String commit;
    private final String oldBlob;
    private final String newBlob;
    private final List<String> changes;

    public WocDepSeqItem(String commit, String oldBlob, String newBlob, List<String> changes) {
        this.commit = commit;
        this.oldBlob = oldBlob;
        this.newBlob = newBlob;
        this.changes = changes;
    }

    public String getCommit() {
        return commit;
    }

    public String getOldBlob() {
        return oldBlob;
    }

    public String getNewBlob() {
        return newBlob;
    }

    public List<String> getChanges() {
        return changes;
    }
}
