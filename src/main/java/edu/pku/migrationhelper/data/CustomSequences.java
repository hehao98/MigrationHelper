package edu.pku.migrationhelper.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "customSequences")
public class CustomSequences {
    @Id
    private String id;

    private long seq;

    public String getId() {
        return id;
    }

    public long getSeq() {
        return seq;
    }

    public CustomSequences setSeq(long seq) {
        this.seq = seq;
        return this;
    }
}