package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.*;
import edu.pku.migrationhelper.mapper.*;
import edu.pku.migrationhelper.service.*;
import edu.pku.migrationhelper.util.JsonUtils;
import edu.pku.migrationhelper.util.LZFUtils;
import edu.pku.migrationhelper.util.MathUtils;
import org.apache.tomcat.util.buf.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tokyocabinet.HDB;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.*;

/**
 * Created by xuyul on 2020/1/2.
 */
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "TestJob")
public class TestJob {

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

    @Autowired
    private PomAnalysisService pomAnalysisService;

    @Autowired
    private BlobInfoMapper blobInfoMapper;

    @Autowired
    private CommitInfoMapper commitInfoMapper;

    @Autowired
    private TestMapper testMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void run() throws Exception {
//        testDatabase();
//        testBin2List();
//        testDatabaseSize();
//        testLZF();
//        testPomAnalysis();
        libraryIdentityService.parseGroupArtifact("org.eclipse.jgit", "org.eclipse.jgit", false);
//        libraryIdentityService.parseGroupArtifact("com.liferay.portal", "com.liferay.portal.impl", false);
//        jarAnalysisService.analyzeJar("jar-download\\org\\eclipse\\jgit\\org.eclipse.jgit-1.2.0.201112221803-r.jar");
//        testJavaCodeAnalysis();
//        testAnalyzeBlob();
//        testTokyoCabinet();
//        testBlobCommitMapper();
//        genBerIdsCode();
//        testCreateTable();
    }

    public void testDatabase() throws Exception {
        MethodSignature ms = new MethodSignature()
                .setPackageName("org.eclipse.jgit.api")
                .setClassName("GarbageCollectCommand")
                .setMethodName("wait")
                .setParamList("");
        MethodSignature mss = libraryIdentityService.getMethodSignature(ms, null);
        System.out.println(mss.getId());
        List<MethodSignature> msList = libraryIdentityService.getMethodSignatureList(ms.getPackageName(), ms.getClassName(), ms.getMethodName());
        for (MethodSignature msss : msList) {
            System.out.println(msss.getId());
        }

        LibrarySignatureToVersion s2v = libraryIdentityService.getSignatureToVersion(ms.getId());
        System.out.println(s2v.getVersionIdList());
        System.out.println(s2v.getGroupArtifactIdList());

        System.out.println(libraryIdentityService.getVersionToSignature(1).getSignatureIdList());
    }

    public void testCreateTable() throws Exception {
        for (long i = 0; i < 128; i++) {
            long ai = i << 35;
            methodSignatureMapper.createTable((int)i);
            methodSignatureMapper.setAutoIncrement((int)i, ai);
        }
    }

    public void testBin2List() throws Exception {
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                testBin2List(i);
            }
        }
        LOG.info("testBin2List success");
    }

    public void testBin2List(int size) throws Exception {
        Random random = new Random();
        List<Long> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(random.nextLong());
        }
        byte[] content = MathUtils.berNumberList(list);
        List<Long> result = MathUtils.unberNumberList(content);
        Iterator<Long> listIt = list.iterator();
        Iterator<Long> resultIt = result.iterator();
        while(listIt.hasNext() && resultIt.hasNext()) {
            if(!listIt.next().equals(resultIt.next())) {
                throw new RuntimeException("testBin2List fail: " + list);
            }
        }
        if(listIt.hasNext() != resultIt.hasNext()) {
            throw new RuntimeException("testBin2List fail: " + list);
        }
    }

    public void testDatabaseSize() throws Exception {
        List<Long> signatureIds = testMapper.findAllSignatureIds();
        Map<Long, List<Long>> v2s = new HashMap<>();
        for (long signatureId : signatureIds) {
            List<Long> versionIds = testMapper.findVersionIdsBySignatureId(signatureId);
            for (Long versionId : versionIds) {
                v2s.computeIfAbsent(versionId, k -> new LinkedList<>()).add(signatureId);
            }
            testMapper.insertS2VJ(signatureId, JsonUtils.writeObjectAsString(versionIds));
            testMapper.insertS2VB(signatureId, MathUtils.berNumberList(versionIds));
        }
        v2s.forEach((vId, sIds) -> {
            testMapper.insertV2SB(vId, MathUtils.berNumberList(sIds));
            testMapper.insertV2SJ(vId, JsonUtils.writeObjectAsString(sIds));
        });
    }

    public void testJavaCodeAnalysis() throws Exception {
        String content = readFile("C:\\Users\\xuyul\\Documents\\实验室\\Library Migration\\jgit-cookbook\\src\\main\\java\\org\\dstadler\\jgit\\api\\ReadFileFromCommit.java");
        List<MethodSignature> msList = javaCodeAnalysisService.analyzeJavaCode(content);
        for (MethodSignature ms : msList) {
            MethodSignature dbms = libraryIdentityService.getMethodSignature(ms, null);
            Long id = dbms == null ? null : dbms.getId();
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
        if (res != len) {
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
        if (!hdb.open("db/test_tc.tch", HDB.OWRITER | HDB.OCREAT)) {
            int ecode = hdb.ecode();
            LOG.error("open error: " + hdb.errmsg(ecode));
        }

        // store records
        if (!hdb.put("foo", "hop") ||
                !hdb.put("bar", "step") ||
                !hdb.put("baz", "jump")) {
            int ecode = hdb.ecode();
            LOG.error("put error: " + hdb.errmsg(ecode));
        }

        // retrieve records
        String value = hdb.get("foo");
        if (value != null) {
            LOG.info(value);
        } else {
            int ecode = hdb.ecode();
            LOG.error("get error: " + hdb.errmsg(ecode));
        }

        // traverse records
        hdb.iterinit();
        String key;
        while ((key = hdb.iternext2()) != null) {
            value = hdb.get(key);
            if (value != null) {
                LOG.info(key + ":" + value);
            }
        }

        // close the database
        if (!hdb.close()) {
            int ecode = hdb.ecode();
            LOG.error("close error: " + hdb.errmsg(ecode));
        }

    }

    public void testPomAnalysis() throws Exception {
        File pomFile = new File("pom.xml");
        FileInputStream fis = new FileInputStream(pomFile);
        byte[] content = new byte[(int) pomFile.length()];
        fis.read(content);
        List<PomAnalysisService.LibraryInfo> libraryInfoList = pomAnalysisService.analyzePom(new String(content));
        for (PomAnalysisService.LibraryInfo libraryInfo : libraryInfoList) {
            LOG.info("groupId = {}, artifactId = {}, version = {}",
                    libraryInfo.groupId, libraryInfo.artifactId, libraryInfo.version);
        }
    }

    public void testLZF() throws Exception {
        printFirstNByte("lzf_test/1.bin", 50);
        printFirstNByte("lzf_test/2.bin", 50);
        printFirstNByte("lzf_test/3.bin", 50);
        printFirstNByte("lzf_test/4.bin", 50);
        testLZF0("lzf_test/1.bin");
        testLZF0("lzf_test/2.bin");
        testLZF0("lzf_test/3.bin");
        testLZF0("lzf_test/4.bin");
    }

    public void testLZF0(String fileName) throws Exception {
        System.out.println(fileName);
        File file = new File(fileName);
        int length = (int) file.length();
        byte[] content = new byte[length];
        FileInputStream fis = new FileInputStream(file);
        fis.read(content);
        fis.close();
        content = LZFUtils.lzfDecompressFromPerl(content);
        System.out.println(new String(content));
    }

    public void printFirstNByte(String fileName, int n) throws Exception {
        System.out.println(fileName);
        byte[] content = new byte[n];
        FileInputStream fis = new FileInputStream(fileName);
        int len = fis.read(content);
        fis.close();
        for (int i = 0; i < len; i++) {
            System.out.print("0x");
            System.out.print(HexUtils.toHexString(new byte[]{content[i]}));
            System.out.print(',');
        }
        System.out.println("");
    }

    public void genBerIdsCode() throws Exception {
        String className = "BlobInfo";
        BufferedReader reader = new BufferedReader(new FileReader("db/test.txt"));
        String line;
        while((line = reader.readLine()) != null) {
            line = line.trim();
            if("".equals(line)) continue;
            String[] attrs = line.split(" ");
            String fieldName = attrs[2];
            fieldName = fieldName.substring(0, fieldName.length() - 1);
            String FieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            String listFieldName = fieldName.substring(0, fieldName.length() - 1) + "List";
            String ListFieldName = FieldName.substring(0, FieldName.length() - 1) + "List";
            System.out.println(
                    "    public byte[] get"+FieldName+"() {\n" +
                            "        return "+fieldName+";\n" +
                            "    }\n" +
                            "\n" +
                            "    public "+className+" set"+FieldName+"(byte[] "+fieldName+") {\n" +
                            "        GetSetHelper.berNumberByteSetter("+fieldName+", e -> this."+fieldName+" = e, e -> this."+listFieldName+" = e);\n" +
                            "        return this;\n" +
                            "    }\n" +
                            "\n" +
                            "    public List<Long> get"+ListFieldName+"() {\n" +
                            "        return "+listFieldName+";\n" +
                            "    }\n" +
                            "\n" +
                            "    public "+className+" set"+ListFieldName+"(byte[] "+listFieldName+") {\n" +
                            "        GetSetHelper.berNumberByteSetter("+listFieldName+", e -> this."+fieldName+" = e, e -> this."+listFieldName+" = e);\n" +
                            "        return this;\n" +
                            "    }\n" +
                            "\n"
            );
        }
    }
}
