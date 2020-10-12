package edu.pku.migrationhelper.data.woc;

import org.springframework.data.annotation.Id;

import java.util.Date;

public class WocConfirmedMigration {
    @Id
    private final String id;                  // MongoDB auto-generated ID, has no use
    private final String fromLib;             // groupId:artifactId
    private final String toLib;               // groupId:artifactId
    private final String repoName;            // Repository name as in World of Code
    private final String fileName;            // Path to the changed pom.xml
    private final String startCommit;         // 40-byte commit SHA
    private final String endCommit;           // 40-byte commit SHA
    private final String startCommitMessage;  // Full commit message
    private final String endCommitMessage;    // Full commit message
    private final Date startCommitTime;       // Committed-at timestamp, not authored-at
    private final Date endCommitTime;         // Committed-at timestamp, not authored-at

    public WocConfirmedMigration(
            String id, String fromLib, String toLib,
            String repoName, String fileName, String startCommit, String endCommit,
            String startCommitMessage, String endCommitMessage,
            Date startCommitTime, Date endCommitTime
    ) {
        this.id = id;
        this.fromLib = fromLib;
        this.toLib = toLib;
        this.repoName = repoName;
        this.fileName = fileName;
        this.startCommit = startCommit;
        this.endCommit = endCommit;
        this.startCommitMessage = startCommitMessage;
        this.endCommitMessage = endCommitMessage;
        this.startCommitTime = startCommitTime;
        this.endCommitTime = endCommitTime;
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

    public String getRepoName() {
        return repoName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStartCommit() {
        return startCommit;
    }

    public String getEndCommit() {
        return endCommit;
    }

    public String getStartCommitMessage() {
        return startCommitMessage;
    }

    public String getEndCommitMessage() {
        return endCommitMessage;
    }

    public Date getStartCommitTime() {
        return startCommitTime;
    }

    public Date getEndCommitTime() {
        return endCommitTime;
    }
}
