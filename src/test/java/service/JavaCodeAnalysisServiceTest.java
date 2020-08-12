package service;

import edu.pku.migrationhelper.data.api.MethodSignatureOld;
import edu.pku.migrationhelper.service.JavaCodeAnalysisService;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JavaCodeAnalysisServiceTest {

    public String readFile(String filePath) throws Exception {
        File file = new File(getClass().getClassLoader().getResource(filePath).getFile());
        FileInputStream fileInputStream = new FileInputStream(file);
        int len = (int) file.length();
        byte[] buf = new byte[len];
        int res = fileInputStream.read(buf, 0, len);
        if (res != len) {
            throw new RuntimeException("res != len");
        }
        return new String(buf);
    }

    @Deprecated
    @Ignore
    @Test
    public void testJavaCodeAnalysis() throws Exception {
        String content = readFile("JavaCodeAnalysisServiceTest.java");
        JavaCodeAnalysisService jcas = new JavaCodeAnalysisService();
        List<MethodSignatureOld> msList = jcas.analyzeJavaCode(content);
        assertTrue(msList.size() >= 2);
        for (MethodSignatureOld ms : msList) {
            System.out.printf("%s, ss = %d, se = %d\n", ms.toString(), ms.getStartLine(), ms.getEndLine());
        }
    }
}
