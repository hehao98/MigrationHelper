package repository;

import edu.pku.migrationhelper.config.MongoDbConfiguration;
import edu.pku.migrationhelper.data.ClassSignature;
import edu.pku.migrationhelper.repository.ClassSignatureRepository;
import edu.pku.migrationhelper.service.JarAnalysisService;
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

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@SpringBootTest(classes = { MongoDbConfiguration.class, JarAnalysisService.class })
public class ClassSignatureRepositoryTest {

    @Autowired
    ClassSignatureRepository repo;

    @Autowired
    JarAnalysisService jas;

    @Before
    public void init() {
        repo.deleteAll();
    }

    @Test
    public void testAddData() throws Exception {
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("jars/gson-2.8.6.jar")).getPath();
        List<ClassSignature> result = jas.analyzeJar(jarFilePath, true);
        int i = 0;
        for (ClassSignature cs : result) {
            System.out.println(cs);
            repo.save(cs.setId(i++));
        }
    }
}
