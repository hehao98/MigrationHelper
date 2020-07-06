package service;

import edu.pku.migrationhelper.data.ClassSignature;
import edu.pku.migrationhelper.data.MethodSignatureOld;
import edu.pku.migrationhelper.service.JarAnalysisService;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestJarAnalysisService {

    @Test
    public void testAnalyzeJar() throws Exception {
        JarAnalysisService jas = new JarAnalysisService();
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("jars/gson-2.8.6.jar")).getPath();
        List<ClassSignature> result = jas.analyzeJar(jarFilePath, true);
        Optional<ClassSignature> opt = result.stream()
                .filter(x -> x.getClassName().equals("com.google.gson.Gson")).findFirst();
        assertTrue(opt.isPresent());
        assertTrue(opt.get().isPublic());
        assertTrue(opt.get().isFinal());
        assertEquals("java.lang.Object", opt.get().getSuperClassName());
        System.out.println(result);
    }

    @Test
    public void testAnalyzeJarOld() throws Exception {
        JarAnalysisService jas = new JarAnalysisService();
        List<MethodSignatureOld> result = new LinkedList<>();
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("jars/gson-2.8.6.jar")).getPath();

        jas.analyzeJar(jarFilePath, result);
        assertTrue(result.size() > 0);

        List<String> packages = MethodSignatureOld.getPackages(result);
        System.out.println(packages);
        // System.out.println(result);
        assertTrue(packages.contains("com.google.gson"));
    }

    @Test
    public void testEOFExceptionWhenParsingSpecificJarOld() throws Exception {
        JarAnalysisService jas = new JarAnalysisService();
        List<MethodSignatureOld> result = new LinkedList<>();
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("jars/asm-all-6.0_ALPHA.jar")).getPath();
        jas.analyzeJar(jarFilePath, result);
        System.out.println(MethodSignatureOld.getClasses(result).subList(0, 10));
        assertTrue(result.size() > 0);
    }
}
