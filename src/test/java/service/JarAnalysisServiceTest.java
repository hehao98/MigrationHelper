package service;

import edu.pku.migrationhelper.data.api.ClassSignature;
import edu.pku.migrationhelper.data.api.MethodSignatureOld;
import edu.pku.migrationhelper.service.JarAnalysisService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class JarAnalysisServiceTest {

    @Test
    public void testAnalyzeJar() throws IOException {
        JarAnalysisService jas = new JarAnalysisService();
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("jars/gson-2.8.6.jar")).getPath();

        List<ClassSignature> result = jas.analyzeJar(jarFilePath, true, null);
        Optional<ClassSignature> opt = result.stream()
                .filter(x -> x.getClassName().equals("com.google.gson.Gson")).findFirst();
        assertTrue(opt.isPresent());
        assertTrue(opt.get().isPublic());
        assertTrue(opt.get().isFinal());
        assertEquals("java.lang.Object", opt.get().getSuperClassName());

        // Test whether all the class SHAs are consistent
        Set<String> keys = result.stream().map(ClassSignature::getId).collect(Collectors.toSet());
        for (ClassSignature cs : result) {
            List<String> ids = new ArrayList<>(cs.getInterfaceIds());
            ids.add(cs.getSuperClassId());
            for (String id : ids) {
                if (!id.equals(ClassSignature.ID_NULL)) {
                    assertTrue(keys.contains(id));
                }
            }
        }
        System.out.println(result);
    }

    @Test
    public void testClassNamesInJar() throws IOException {
        JarAnalysisService jas = new JarAnalysisService();
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("jars/gson-2.8.6.jar")).getPath();
        List<String> classNamesInJar = new ArrayList<>();
        List<ClassSignature> result = jas.analyzeJar(jarFilePath, true, classNamesInJar);
        assertTrue(classNamesInJar.contains("com.google.gson.Gson"));
        assertFalse(classNamesInJar.contains("java.lang.Object"));
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
