package data;

import edu.pku.migrationhelper.data.api.*;
import edu.pku.migrationhelper.service.JarAnalysisService;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class APIChangeTest {

    List<ClassSignature> getFromJar(String path) throws IOException {
        JarAnalysisService jas = new JarAnalysisService();
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource(path)).getPath();
        return jas.analyzeJar(jarFilePath, true, null);
    }


    @Test
    public void testAPIChanges() throws Exception {
        System.out.println("========== testAPIChanges ==========");
        List<ClassSignature> from = getFromJar("jars/gson-2.7.jar");
        List<ClassSignature> to = getFromJar("jars/gson-2.8.6.jar");
        APIChange change = new APIChange("com.google.code.gson", "gson", "2.7", "2.8.6", from, to);
        change.printAPIChange(System.out);
    }

    @Test
    public void testAPIChanges2() throws Exception {
        System.out.println("========== testAPIChanges2 ==========");
        List<ClassSignature> from = getFromJar("jars/gson-2.7.jar");
        List<ClassSignature> to = getFromJar("jars/gson-2.8.5.jar");
        APIChange change = new APIChange("com.google.code.gson", "gson", "2.7", "2.8.5", from, to);
        change.printAPIChange(System.out);
    }

    @Test
    public void testAPIChanges3() throws Exception {
        System.out.println("========== testAPIChanges3 ==========");
        List<ClassSignature> from = getFromJar("jars/gson-2.8.5.jar");
        List<ClassSignature> to = getFromJar("jars/gson-2.8.6.jar");
        APIChange change = new APIChange("com.google.code.gson", "gson", "2.8.5", "2.8.6", from, to);
        change.printAPIChange(System.out);
    }
}
