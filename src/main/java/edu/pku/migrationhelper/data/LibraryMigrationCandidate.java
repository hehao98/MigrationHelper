package edu.pku.migrationhelper.data;

import org.springframework.data.util.Pair;

import java.util.LinkedList;
import java.util.List;

public class LibraryMigrationCandidate {
    public long fromId;
    public long toId;
    public int ruleCount = 0;               // Number of times a rule (fromLib, toLib) occur in dependency sequence
    public int ruleCountSameCommit = 0;     // Number of times a rule (fromLib, toLib) occurs in one commit
    public int methodChangeCount = 0;       // Number of times API modifications occur in data
    public int libraryConcurrenceCount = 0; // Number of times l1 and l2 are used in same commit
    public int maxRuleCount = 0;            // For all (fromLib, *) candidates, max value of RuleCount
    public int maxRuleCountSameCommit = 0;  // For all (fromLib, *) candidates, max value of RuleCount in same commit
    // public int maxRuleCountToLibSameCommit = 0; // For all (*, toLib) candidates, max value of RuleCount in sameCommit
    public int maxMethodChangeCount = 0;    // For all (fromLib, *) candidates, max value of methodChangeCount
    public double ruleSupportByTotal = 0;
    public double ruleSupportByMax = 0;
    public double ruleSupportByMaxSameCommit = 0; // The Rule Support used in the Paper
    public double methodChangeSupportByTotal = 0;
    public double methodChangeSupportByMax = 0;   // The API Support used in the Paper
    public double libraryConcurrenceSupport = 0;
    public double commitDistanceSupport = 0;      // The Distance Support used in the Paper
    public double commitMessageSupport = 0;       // The Message Support used in the Paper
    public double confidence = 0;
    public double confidence2 = 0;
    public List<String[]> repoCommitList = new LinkedList<>();     // List<repoName, startCommitSHA, endCommitSHA, fileName>
    public List<Integer> commitDistanceList = new LinkedList<>();
    public List<String[]> possibleCommitList = new LinkedList<>(); // List<repoName, startCommitSHA, endCommitSHA, fileName>

    public LibraryMigrationCandidate() {}

    public LibraryMigrationCandidate(long fromId, long toId) {
        this.fromId = fromId;
        this.toId = toId;
    }
}
