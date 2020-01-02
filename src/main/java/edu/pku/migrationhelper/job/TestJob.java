package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.LibraryVersion;
import edu.pku.migrationhelper.mapper.LibraryVersionMapper;
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

    @Override
    public void run(String... args) throws Exception {

        LibraryVersion lv1 = new LibraryVersion()
                .setGroupId("edu.pku")
                .setArtifactId("migration-helper")
                .setVersion("1.0.0");

        LibraryVersion lv2 = new LibraryVersion()
                .setGroupId("edu.pku")
                .setArtifactId("migration-helper")
                .setVersion("1.0.1");

        int r = libraryVersionMapper.insert(Arrays.asList(lv1, lv2));

        System.out.println(r);
        System.out.println(lv1.getId());
        System.out.println(lv2.getId());
    }
}
