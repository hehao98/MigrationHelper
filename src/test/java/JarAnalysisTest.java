import edu.pku.migrationhelper.data.MethodSignature;
import edu.pku.migrationhelper.service.JarAnalysisService;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

public class JarAnalysisTest {

    @Test
    void testAnalyzeJar() throws Exception {
        JarAnalysisService jas = new JarAnalysisService();
        List<MethodSignature> result = new LinkedList<>();
        String jarFilePath = getClass().getResource("jars/gson-2.8.6.jar").getPath();
        jas.analyzeJar(jarFilePath, result);
        // System.out.println(result);
    }
}
