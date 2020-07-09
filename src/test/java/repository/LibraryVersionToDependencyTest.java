package repository;

import edu.pku.migrationhelper.config.DataSourceConfiguration;
import edu.pku.migrationhelper.config.MongoDbConfiguration;
import edu.pku.migrationhelper.data.lib.LibraryVersionToDependency;
import edu.pku.migrationhelper.repository.LibraryVersionToDependenciesRepository;
import edu.pku.migrationhelper.service.JarAnalysisService;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import edu.pku.migrationhelper.service.MongoDbUtilService;
import edu.pku.migrationhelper.service.PomAnalysisService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@SpringBootTest(classes = { MongoDbConfiguration.class, MongoDbUtilService.class,
        PomAnalysisService.class, LibraryIdentityService.class, JarAnalysisService.class,
        DataSourceConfiguration.class })
public class LibraryVersionToDependencyTest {

    @Autowired
    MongoDbUtilService utilService;

    @Autowired
    LibraryVersionToDependenciesRepository lv2dRepo;

    @Autowired
    PomAnalysisService pomAnalysisService;

    @Autowired
    LibraryIdentityService libraryIdentityService;

    @Before
    public void init() {
        assertTrue(utilService.getDbName().contains("test"));
        lv2dRepo.deleteAll();
        utilService.initMongoDb();
    }

    @Test
    public void testLibraryVersionToDependency() throws Exception {
        String groupId = "org.springframework";
        String artifactId = "spring-core";
        String version = "5.2.7.RELEASE";
        LibraryVersionToDependency lv2d = new LibraryVersionToDependency();
        List<PomAnalysisService.LibraryInfo> deps = libraryIdentityService.extractDependenciesFromMaven(groupId, artifactId, version);
        lv2d.setDependencies(deps).setGroupId(groupId).setArtifactId(artifactId).setVersion(version).setId(0);
        lv2dRepo.save(lv2d);
        assertTrue(lv2dRepo.findById(0L).isPresent());
        lv2d = lv2dRepo.findById(0L).get();
        assertEquals(1, lv2d.getDependencies().size());
        assertEquals(1, lv2dRepo.findByGroupIdAndArtifactId(groupId, artifactId).size());
        assertTrue(lv2dRepo.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version).isPresent());
    }

}
