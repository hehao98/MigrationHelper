package edu.pku.migrationhelper.data.woc;

import org.springframework.data.annotation.Id;

import java.util.List;

public class WocRepository {
    @Id
    private final long id;              // Same id as in LioRepository
    private final String name;          // WoC Name, which is different from LibrariesIO name
    private final List<String> commits; // List of SHAs

    public WocRepository(long id, String name, List<String> commits) {
        this.id = id;
        this.name = name;
        this.commits = commits;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getCommits() {
        return commits;
    }
}
