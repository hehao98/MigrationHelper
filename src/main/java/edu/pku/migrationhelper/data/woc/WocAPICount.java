package edu.pku.migrationhelper.data.woc;

import org.springframework.data.annotation.Id;

public class WocAPICount {
    @Id
    private final String id;      // MongoDB auto-generated ID, has no use
    private final String fromLib; // groupId:artifactId
    private final String toLib;   // groupId:artifactId
    private final long count;

    public WocAPICount(String id, String fromLib, String toLib, long count) {
        this.id = id;
        this.fromLib = fromLib;
        this.toLib = toLib;
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public String getFromLib() {
        return fromLib;
    }

    public String getToLib() {
        return toLib;
    }

    public long getCount() {
        return count;
    }
}
