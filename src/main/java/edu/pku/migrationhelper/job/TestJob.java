package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.LibraryVersion;
import edu.pku.migrationhelper.data.MethodSignature;
import edu.pku.migrationhelper.mapper.LibraryVersionMapper;
import edu.pku.migrationhelper.mapper.MethodSignatureMapper;
import edu.pku.migrationhelper.service.GitRepositoryAnalysisService;
import edu.pku.migrationhelper.service.JarAnalysisService;
import edu.pku.migrationhelper.service.JavaCodeAnalysisService;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tokyocabinet.HDB;

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

    @Autowired
    private GitRepositoryAnalysisService gitRepositoryAnalysisService;

    @Override
    public void run(String... args) throws Exception {
        libraryIdentityService.parseGroupArtifact("org.eclipse.jgit", "org.eclipse.jgit");
//        jarAnalysisService.analyzeJar("jar-download\\org\\eclipse\\jgit\\org.eclipse.jgit-1.2.0.201112221803-r.jar");
//        testJavaCodeAnalysis();
//        testAnalyzeBlob();
//        testTokyoCabinet();
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

    public void testAnalyzeBlob() throws Exception {
        gitRepositoryAnalysisService.analyzeRepositoryLibrary("jgit-cookbook");
    }

    public void testTokyoCabinet() throws Exception {
        // create the object
        HDB hdb = new HDB();

        // open the database
        if(!hdb.open("db/test_tc.tch", HDB.OWRITER | HDB.OCREAT)){
            int ecode = hdb.ecode();
            LOG.error("open error: " + hdb.errmsg(ecode));
        }

        // store records
        if(!hdb.put("foo", "hop") ||
                !hdb.put("bar", "step") ||
                !hdb.put("baz", "jump")){
            int ecode = hdb.ecode();
            LOG.error("put error: " + hdb.errmsg(ecode));
        }

        // retrieve records
        String value = hdb.get("foo");
        if(value != null){
            LOG.info(value);
        } else {
            int ecode = hdb.ecode();
            LOG.error("get error: " + hdb.errmsg(ecode));
        }

        // traverse records
        hdb.iterinit();
        String key;
        while((key = hdb.iternext2()) != null){
            value = hdb.get(key);
            if(value != null){
                LOG.info(key + ":" + value);
            }
        }

        // close the database
        if(!hdb.close()){
            int ecode = hdb.ecode();
            LOG.error("close error: " + hdb.errmsg(ecode));
        }

    }
}
