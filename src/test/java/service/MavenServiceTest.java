package service;

import edu.pku.migrationhelper.data.lib.LibraryInfo;
import edu.pku.migrationhelper.service.MavenService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class MavenServiceTest {

    @Test
    public void testResolveProperties() {
        MavenService mavenService = new MavenService();
        Properties properties = new Properties();
        properties.setProperty("abc", "def");
        properties.setProperty("asdef", "sss");
        properties.setProperty("ccc", "ddd");
        String res = mavenService.resolveProperties("${abc}", properties);
        assertEquals("def", res);
        res = mavenService.resolveProperties("${abc}${ccc}", properties);
        assertEquals("defddd", res);
    }

    
    @Test
    public void testPomAnalysis() throws Exception {
        File pomFile = new File("pom.xml");
        FileInputStream fis = new FileInputStream(pomFile);
        byte[] content = new byte[(int) pomFile.length()];
        MavenService mavenService = new MavenService();

        int len = fis.read(content);
        assertEquals(len, content.length);
        List<LibraryInfo> libraryInfoList = mavenService.analyzePom(new String(content));
        assertTrue(libraryInfoList.size() > 0);

        boolean hasJUnit = false;
        for (LibraryInfo libraryInfo : libraryInfoList) {
            System.out.printf("groupId = %s, artifactId = %s, version = %s\n",
                    libraryInfo.groupId, libraryInfo.artifactId, libraryInfo.version);
            if (libraryInfo.groupId.equals("org.junit.jupiter")
                && libraryInfo.artifactId.equals("junit-jupiter-api")
                && libraryInfo.version.equals("5.6.2")) {
                hasJUnit = true;
            }
        }
        assertTrue(hasJUnit);
    }
}
