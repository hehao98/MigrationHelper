package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.LibraryVersion;
import edu.pku.migrationhelper.data.MethodSignature;
import edu.pku.migrationhelper.mapper.LibraryVersionMapper;
import edu.pku.migrationhelper.mapper.MethodSignatureMapper;
import edu.pku.migrationhelper.service.JarAnalysisService;
import edu.pku.migrationhelper.service.JavaCodeAnalysisService;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by xuyul on 2020/1/2.
 */
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "TestJob")
public class TestJob implements CommandLineRunner {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private LibraryIdentityService libraryIdentityService;

    @Autowired
    private MethodSignatureMapper methodSignatureMapper;

    @Autowired
    private JarAnalysisService jarAnalysisService;

    @Autowired
    private JavaCodeAnalysisService javaCodeAnalysisService;

    @Override
    public void run(String... args) throws Exception {
//        libraryIdentityService.parseGroupArtifact("org.eclipse.jgit", "org.eclipse.jgit");
//        jarAnalysisService.analyzeJar("jar-download\\org\\eclipse\\jgit\\org.eclipse.jgit-1.2.0.201112221803-r.jar");
        testJavaCodeAnalysis();
    }

    public void testJavaCodeAnalysis() throws Exception {
        String content = readFile("C:\\Users\\xuyul\\Documents\\实验室\\Library Migration\\jgit-cookbook\\src\\main\\java\\org\\dstadler\\jgit\\api\\ReadFileFromCommit.java");
        List<MethodSignature> msList = javaCodeAnalysisService.analyzeJavaCode(content);
        for (MethodSignature ms : msList) {
            Long id = methodSignatureMapper.findId(ms.getPackageName(), ms.getClassName(), ms.getMethodName(), ms.getParamList());
            LOG.info("id = {}, pn = {}, cn = {}, mn = {}, pl = {}", id,
                    ms.getPackageName(), ms.getClassName(), ms.getMethodName(), ms.getParamList());
        }
    }

    public String readFile(String filePath) throws Exception {
        File file = new File(filePath);
        FileInputStream fileInputStream = new FileInputStream(file);
        int len = (int) file.length();
        byte[] buf = new byte[len];
        int res = fileInputStream.read(buf, 0, len);
        if(res != len) {
            throw new RuntimeException("res != len");
        }
        return new String(buf);
    }
}
