package repository;

import edu.pku.migrationhelper.config.MongoDbConfiguration;
import edu.pku.migrationhelper.data.api.ClassSignature;
import edu.pku.migrationhelper.repository.ClassSignatureRepository;
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@SpringBootTest(classes = { MongoDbConfiguration.class, MongoDbUtilService.class, JarAnalysisService.class })
public class ClassSignatureRepositoryTest {

    @Autowired
    MongoDbUtilService utilService;

    @Autowired
    ClassSignatureRepository csRepo;

    @Autowired
    JarAnalysisService jas;

    @Before
    public void init() {
        assertTrue(utilService.getDbName().contains("test"));
        csRepo.deleteAll();
        utilService.initMongoDb();
    }

    @Test
    public void testClassSignatureDb() throws Exception {
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("jars/gson-2.8.6.jar")).getPath();
        List<ClassSignature> testSignatures = jas.analyzeJar(jarFilePath, true, null);
        System.out.println(testSignatures);

        for (ClassSignature cs : testSignatures) {
            csRepo.save(cs);
        }
        assertEquals(csRepo.count(), testSignatures.size());

        List<ClassSignature> queryResult = csRepo.findByClassName("com.google.gson.Gson");
        System.out.println(queryResult);
        assertTrue(queryResult.size() > 0);

        ClassSignature cs = testSignatures.get(0);
        String id = cs.getId();
        cs.setClassName("test.ClassName");
        String id2 = cs.getId();
        assertNotEquals(id, id2);
        csRepo.save(cs);
        queryResult = csRepo.findByClassName("test.ClassName");
        assertTrue(queryResult.size() > 0);
        Optional<ClassSignature> opt = csRepo.findById(id2);
        assertTrue(opt.isPresent());
        assertEquals(opt.get().getId(), id2);

        queryResult = csRepo.findByClassNameStartingWith("com.google.gson");
        System.out.println("Classes that begin with com.google.gson: ");
        System.out.println(queryResult);
        assertTrue(queryResult.size() > 0);
        for (ClassSignature x : queryResult) {
            assertTrue(x.getClassName().startsWith("com.google.gson"));
        }
    }
}
