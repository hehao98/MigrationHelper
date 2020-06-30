package service;

import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LibraryIdentityServiceTest {

    @Test // Well, this test is a little bit slow...
    void testDownloadLibraryFromMaven() throws Exception {
        LibraryIdentityService lis = new LibraryIdentityService().setMavenUrlBase("https://repo1.maven.org/maven2/");
        OutputStream output = new FileOutputStream("target/test.jar");
        lis.downloadLibraryFromMaven("junit", "junit", "4.13-rc-2", output);
        assertTrue(new File("target/test.jar").exists());
    }

    @Test
    void testDownloadNonExistentLibraryFromMaven() throws Exception {
        LibraryIdentityService lis = new LibraryIdentityService().setMavenUrlBase("https://repo1.maven.org/maven2/");
        OutputStream output = new FileOutputStream("target/test-non-existent.jar");
        IOException ex = assertThrows(IOException.class, () -> {
            lis.downloadLibraryFromMaven("org.tensorflow", "parentpom", "1.1.0", output);
            assertFalse(new File("target/test-non-existent.jar").exists());
        });
        assertTrue(ex.getMessage().contains("http status code 404"));
    }

    @Test
    void testExtractVersionInformationFromMaven() throws Exception {
        LibraryIdentityService lis = new LibraryIdentityService().setMavenUrlBase("https://repo1.maven.org/maven2/");
        List<String> result = lis.extractAllVersionsFromMaven("junit", "junit");
        System.out.println(result);
        assertTrue(result.contains("4.13-rc-2"));
    }

    @Test
    void testExtractAvailableFilesFromMaven() throws Exception {
        LibraryIdentityService lis = new LibraryIdentityService().setMavenUrlBase("https://repo1.maven.org/maven2/");
        List<String> result = lis.extractAvailableFilesFromMaven("org.tensorflow", "parentpom", "1.15.0");
        System.out.println(result);
        assertTrue(result.contains("parentpom-1.15.0.pom"));
        result = lis.extractAvailableFilesFromMaven("org.apache.cxf", "cxf-rt-frontend-jaxrs", "2.1.10");
        assertTrue(result.contains("cxf-rt-frontend-jaxrs-2.1.10.jar"));
    }
}
