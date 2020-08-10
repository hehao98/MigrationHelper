package repository;

import edu.pku.migrationhelper.config.MongoDbConfiguration;
import edu.pku.migrationhelper.data.api.ClassSignature;
import edu.pku.migrationhelper.data.api.ClassToLibraryVersion;
import edu.pku.migrationhelper.data.lib.LibraryVersionToClass;
import edu.pku.migrationhelper.repository.ClassSignatureRepository;
import edu.pku.migrationhelper.repository.ClassToLibraryVersionRepository;
import edu.pku.migrationhelper.repository.LibraryVersionToClassRepository;
import edu.pku.migrationhelper.service.JarAnalysisService;
import edu.pku.migrationhelper.service.MongoDbUtilService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@SpringBootTest(classes = { MongoDbConfiguration.class, MongoDbUtilService.class, JarAnalysisService.class })
public class LibraryVersionAndClassMappingTest {

    @Autowired
    MongoDbUtilService utilService;

    @Autowired
    ClassSignatureRepository csRepo;

    @Autowired
    LibraryVersionToClassRepository lv2cRepo;

    @Autowired
    ClassToLibraryVersionRepository c2lvRepo;

    @Autowired
    JarAnalysisService jas;

    @Before
    public void init() {
        assumeTrue(utilService.isDbRunning());
        assertTrue(utilService.getDbName().contains("test"));
        csRepo.deleteAll();
        lv2cRepo.deleteAll();
        c2lvRepo.deleteAll();
        utilService.initMongoDb();
    }

    @Test
    public void testLibraryVersionAndClassMapping() throws Exception {
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("jars/gson-2.8.6.jar")).getPath();
        List<ClassSignature> testSignatures = jas.analyzeJar(jarFilePath, true, null);
        List<String> testClassIds = testSignatures.stream().map(ClassSignature::getId).collect(Collectors.toList());
        csRepo.saveAll(testSignatures);
        LibraryVersionToClass lv2c = new LibraryVersionToClass()
                .setClassIds(testClassIds)
                .setId(0L)
                .setGroupId("com.google.code")
                .setArtifactId("gson")
                .setVersion("2.8.6");
        lv2cRepo.save(lv2c);
        List<ClassToLibraryVersion> c2lvList = testSignatures.stream()
                .map(cs -> {
                    List<Long> l = new ArrayList<>();
                    l.add(0L);
                    return new ClassToLibraryVersion().setClassId(cs.getId()).setVersionIds(l);
                })
                .collect(Collectors.toList());
        c2lvRepo.saveAll(c2lvList);
        assertTrue(lv2cRepo.findById(0L).isPresent());
        for (String id : testClassIds) {
            assertTrue(c2lvRepo.findById(id).isPresent());
            assertTrue(c2lvRepo.findById(id).get().getVersionIds().contains(0L));
        }
        assertTrue(lv2cRepo.findByGroupIdAndArtifactIdAndVersion(
                "com.google.code", "gson", "2.8.6").isPresent());
        assertEquals(1, lv2cRepo.findByGroupIdAndArtifactId("com.google.code", "gson").size());
    }

}
