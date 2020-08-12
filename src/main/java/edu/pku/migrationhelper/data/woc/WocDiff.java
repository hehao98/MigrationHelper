package edu.pku.migrationhelper.data.woc;

public class WocDiff {
    private final String oldBlob;
    private final String newBlob;
    private final String filename;

    public WocDiff(String oldBlob, String newBlob, String filename) {
        this.oldBlob = oldBlob;
        this.newBlob = newBlob;
        this.filename = filename;
    }

    public String getOldBlob() {
        return oldBlob;
    }

    public String getNewBlob() {
        return newBlob;
    }

    public String getFilename() {
        return filename;
    }
}
