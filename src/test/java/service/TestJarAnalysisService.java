package service;

import edu.pku.migrationhelper.data.MethodSignature;
import edu.pku.migrationhelper.service.JarAnalysisService;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJarAnalysisService {

    @Test
    public void testAnalyzeJar() throws Exception {
        JarAnalysisService jas = new JarAnalysisService();
        List<MethodSignature> result = new LinkedList<>();
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("jars/gson-2.8.6.jar")).getPath();

        jas.analyzeJar(jarFilePath, result);
        assertTrue(result.size() > 0);

        List<String> packages = MethodSignature.getPackages(result);
        System.out.println(packages);
        assertTrue(packages.contains("com.google.gson"));
    }

    @Test
    public void testEOFExceptionWhenParsingSpecificJar() throws Exception {
        JarAnalysisService jas = new JarAnalysisService();
        List<MethodSignature> result = new LinkedList<>();
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("jars/asm-all-6.0_ALPHA.jar")).getPath();
        jas.analyzeJar(jarFilePath, result);
        System.out.println(MethodSignature.getClasses(result).subList(0, 10));
        assertTrue(result.size() > 0);
    }
}
