package service;

import edu.pku.migrationhelper.config.DataSourceConfiguration;
import edu.pku.migrationhelper.config.MongoDbConfiguration;
import edu.pku.migrationhelper.data.api.ClassSignature;
import edu.pku.migrationhelper.data.api.ClassToLibraryVersion;
import edu.pku.migrationhelper.data.api.LibraryVersionToClass;
import edu.pku.migrationhelper.data.lib.LibraryGroupArtifact;
import edu.pku.migrationhelper.data.lib.LibraryVersion;
import edu.pku.migrationhelper.mapper.LibraryGroupArtifactMapper;
import edu.pku.migrationhelper.mapper.LibraryVersionMapper;
import edu.pku.migrationhelper.repository.ClassSignatureRepository;
import edu.pku.migrationhelper.repository.ClassToLibraryVersionRepository;
import edu.pku.migrationhelper.repository.LibraryVersionToClassRepository;
import edu.pku.migrationhelper.service.JarAnalysisService;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import edu.pku.migrationhelper.service.MongoDbUtilService;
import org.apache.tomcat.jni.Library;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@SpringBootTest(classes = {
        MongoDbConfiguration.class, MongoDbUtilService.class, JarAnalysisService.class,
        DataSourceConfiguration.class, LibraryIdentityService.class,
})
public class ParseGroupArtifactTest {

    @Value("${spring.datasource.dbcp2.url}")
    String mySqlDbUrl;

    @Autowired
    MongoDbUtilService utilService;

    @Autowired
    ClassSignatureRepository csRepo;

    @Autowired
    LibraryVersionToClassRepository lv2cRepo;

    @Autowired
    ClassToLibraryVersionRepository c2lvRepo;

    @Autowired
    LibraryGroupArtifactMapper lgaMapper;

    @Autowired
    LibraryVersionMapper lvMapper;

    @Autowired
    LibraryIdentityService libraryIdentityService;

    @Autowired
    JarAnalysisService jas;

    @Before
    public void init() {
        assertTrue(utilService.getDbName().contains("test"));
        assertTrue(mySqlDbUrl.contains("test"));
        csRepo.deleteAll();
        lv2cRepo.deleteAll();
        c2lvRepo.deleteAll();
        utilService.initMongoDb();
        lgaMapper.dropTable();
        lvMapper.dropTable();
        lgaMapper.createTable();
        lvMapper.createTable();
    }

    @Test
    public void testParseGroupArtifact() {
        libraryIdentityService.extractVersions("com.google.code.gson", "gson");
        assertEquals(1, lgaMapper.findAll().size());
        LibraryGroupArtifact lib = lgaMapper.findByGroupIdAndArtifactId("com.google.code.gson", "gson");
        assertNotEquals(null, lib);
        assertEquals("gson", lib.getArtifactId());
        assertTrue(lvMapper.findByGroupArtifactId(lib.getId()).size() > 5);

        libraryIdentityService.parseGroupArtifact("com.google.code.gson", "gson");
        lib = lgaMapper.findByGroupIdAndArtifactId("com.google.code.gson", "gson");
        assertTrue(lib.isVersionExtracted() && lib.isParsed() && !lib.isParseError());

        for (ClassSignature cs : csRepo.findAll()) {
            for (String id : cs.getSuperClassAndInterfaceIds()) {
                if (!id.equals(ClassSignature.ID_NULL))
                    assertTrue(csRepo.findById(id).isPresent());
            }
        }

        for (LibraryVersionToClass lv2c : lv2cRepo.findAll()) {
            for (String classId : lv2c.getClassIds()) {
                assertTrue(csRepo.findById(classId).isPresent());
            }
        }

        for (ClassToLibraryVersion c2lv : c2lvRepo.findAll()) {
            for (long versionId : c2lv.getVersionIds()) {
                LibraryVersion lv = lvMapper.findById(versionId);
                assertNotNull(lv);
            }
        }

        List<LibraryVersion> versions = lvMapper.findByGroupArtifactId(lib.getId());
        for (int i = 1; i < versions.size(); ++i) {
            LibraryVersion version = versions.get(i);
            LibraryVersion previous = versions.get(i - 1);

            assertTrue(lv2cRepo.findById(version.getId()).isPresent());
            assertTrue(lv2cRepo.findById(previous.getId()).isPresent());

            Set<ClassSignature> c1 = lv2cRepo.findById(version.getId()).get().getClassIds().stream()
                    .map(id -> csRepo.findById(id).get()).collect(Collectors.toSet());
            Set<ClassSignature> c2 = lv2cRepo.findById(previous.getId()).get().getClassIds().stream()
                    .map(id -> csRepo.findById(id).get()).collect(Collectors.toSet());
            System.out.printf("\nVersion %s: %s\n", version.getVersion(), c1);
            System.out.printf("Added Classes: %s\n", c1.stream()
                            .filter(x -> !c2.contains(x))
                            .sorted(Comparator.comparing(ClassSignature::toString))
                            .collect(Collectors.toList()));
            System.out.printf("Removed Classes: %s\n", c2.stream()
                    .filter(x -> !c1.contains(x))
                    .sorted(Comparator.comparing(ClassSignature::toString))
                    .collect(Collectors.toList()));

        }
    }

}
