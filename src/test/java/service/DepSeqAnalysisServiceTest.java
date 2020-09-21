package service;

import edu.pku.migrationhelper.service.DepSeqAnalysisService;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class DepSeqAnalysisServiceTest {

    @Test
    public void testSplitGroupArtifact() {
        DepSeqAnalysisService service = new DepSeqAnalysisService();

        Set<String> result = service.splitGroupArtifact("org.apache.logging.log4j:log4j-slf4j-impl");
        System.out.println(result);
        assertTrue(result.contains("log4j"));
        assertTrue(result.contains("slf4j"));
        assertFalse(result.contains("org"));
        assertFalse(result.contains("apache"));
        assertFalse(result.contains("impl"));

        result = service.splitGroupArtifact("com.h2database:h2");
        System.out.println(result);
        assertTrue(result.contains("h2database"));
        assertTrue(result.contains("h2"));
        assertFalse(result.contains("com"));
    }

    @Test
    public void testIsPossibleMigration() {
        DepSeqAnalysisService service = new DepSeqAnalysisService();

        boolean result = service.isPossibleMigration(
                "org.xerial:sqlite-jdbc",
                "com.h2database:h2",
                "refactor(database): Use H2 instead of SQLite for tests",
                "refactor(database): Use H2 instead of SQLite for tests"
        );
        assertTrue(result);
    }

}
