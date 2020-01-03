package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.LibraryVersion;
import edu.pku.migrationhelper.mapper.LibraryVersionMapper;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Created by xuyul on 2020/1/2.
 */
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "TestJob")
public class TestJob implements CommandLineRunner {

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private LibraryIdentityService libraryIdentityService;

    @Override
    public void run(String... args) throws Exception {
        libraryIdentityService.parseGroupArtifact("org.eclipse.jgit", "org.eclipse.jgit");
    }
}
