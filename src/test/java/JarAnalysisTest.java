import edu.pku.migrationhelper.data.LibraryGroupArtifact;
import edu.pku.migrationhelper.data.MethodSignature;
import edu.pku.migrationhelper.service.JarAnalysisService;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JarAnalysisTest {

    @Test
    void testAnalyzeJar() throws Exception {
        JarAnalysisService jas = new JarAnalysisService();
        List<MethodSignature> result = new LinkedList<>();
        String jarFilePath = getClass().getResource("jars/gson-2.8.6.jar").getPath();
        jas.analyzeJar(jarFilePath, result);
        // System.out.println(result);
        assertTrue(result.size() > 0);
    }

    @Test // Well, the test is a little bit slow...
    void testDownloadLibraryFromMaven() throws Exception {
        List<String> mavenUrlBase = new ArrayList<>();
        mavenUrlBase.add("https://repo1.maven.org/maven2/");
        LibraryIdentityService lis = new LibraryIdentityService().setMavenUrlBase(mavenUrlBase);
        OutputStream output = new FileOutputStream("target/test.jar");
        lis.downloadLibraryFromMaven("junit", "junit", "4.13-rc-2", output);
        assertTrue(new File("target/test.jar").exists());
    }

}
