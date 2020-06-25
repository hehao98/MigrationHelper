package service;

import edu.pku.migrationhelper.service.PomAnalysisService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PomAnalysisServiceTest {
    
    @Test
    public void testPomAnalysis() throws Exception {
        File pomFile = new File("pom.xml");
        FileInputStream fis = new FileInputStream(pomFile);
        byte[] content = new byte[(int) pomFile.length()];
        PomAnalysisService pomAnalysisService = new PomAnalysisService();

        int len = fis.read(content);
        assertEquals(len, content.length);
        List<PomAnalysisService.LibraryInfo> libraryInfoList = pomAnalysisService.analyzePom(new String(content));
        assertTrue(libraryInfoList.size() > 0);

        boolean hasJUnit = false;
        for (PomAnalysisService.LibraryInfo libraryInfo : libraryInfoList) {
            // System.out.printf("groupId = %s, artifactId = %s, version = %s",
            //        libraryInfo.groupId, libraryInfo.artifactId, libraryInfo.version);
            if (libraryInfo.groupId.equals("org.junit.jupiter")
                && libraryInfo.artifactId.equals("junit-jupiter-api")
                && libraryInfo.version.equals("5.6.2")) {
                hasJUnit = true;
                break;
            }
        }
        assertTrue(hasJUnit);
    }
}
