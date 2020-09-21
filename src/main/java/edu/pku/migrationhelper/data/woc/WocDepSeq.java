package edu.pku.migrationhelper.data.woc;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "wocDepSeq3")
public class WocDepSeq {
    @Id
    private final String id; // MongoDB auto-generated ID, has no use
    private final String repoName;
    private final String fileName;
    private final List<WocDepSeqItem> seq;

    public WocDepSeq(String id, String repoName, String fileName, List<WocDepSeqItem> seq) {
        this.id = id;
        this.repoName = repoName;
        this.fileName = fileName;
        this.seq = seq;
    }

    public String getId() {
        return id;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getFileName() {
        return fileName;
    }

    public List<WocDepSeqItem> getSeq() {
        return seq;
    }
}
