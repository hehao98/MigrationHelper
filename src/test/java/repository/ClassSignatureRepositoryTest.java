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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        List<ClassSignature> result = jas.analyzeJar(jarFilePath, true);
        System.out.println(result);

        for (ClassSignature cs : result) {
            csRepo.save(cs);
        }
        assertEquals(csRepo.count(), result.size());

        List<ClassSignature> queryResult = csRepo.findByClassName("com.google.gson.Gson");
        System.out.println(queryResult);
        assertTrue(queryResult.size() > 0);
    }
}
