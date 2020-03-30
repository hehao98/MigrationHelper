package edu.pku.migrationhelper.job;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.pku.migrationhelper.data.*;
import edu.pku.migrationhelper.mapper.*;
import edu.pku.migrationhelper.service.*;
import edu.pku.migrationhelper.util.JsonUtils;
import edu.pku.migrationhelper.util.LZFUtils;
import edu.pku.migrationhelper.util.MathUtils;
import javafx.util.Pair;
import org.apache.tomcat.util.buf.HexUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tokyocabinet.HDB;

import java.io.*;
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
    private GitObjectStorageService gitObjectStorageService;

    @Autowired
    private DependencyChangePatternAnalysisService dependencyChangePatternAnalysisService;

    @Autowired
    private BlobInfoMapper blobInfoMapper;

    @Autowired
    private CommitInfoMapper commitInfoMapper;

    @Autowired
    private TestMapper testMapper;

    @Autowired
    private MethodChangeMapper methodChangeMapper;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private LibraryOverlapMapper libraryOverlapMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void run() throws Exception {
//        testDatabase();
//        testBin2List();
//        testDatabaseSize();
//        testLZF();
//        testPomAnalysis();
//        libraryIdentityService.parseGroupArtifact("org.eclipse.jgit", "org.eclipse.jgit", false);
//        libraryIdentityService.parseGroupArtifact("com.liferay.portal", "com.liferay.portal.impl", false);
//        jarAnalysisService.analyzeJar("jar-download\\org\\eclipse\\jgit\\org.eclipse.jgit-1.2.0.201112221803-r.jar");
//        testJavaCodeAnalysis();
//        createTable();
//        testAnalyzeBlob();
//        testTokyoCabinet();
//        testBlobCommitMapper();
//        genBerIdsCode();
//        testCreateTable();
//        commitInfoCommandLine();
//        diffCommandLine();
//        alterTableJob();
//        testRepositoryDepSeq();
//        testAnalyzeDepSeq();
//        insertGroupArtifact();
//        insertLibraryOverlap();
//        calcGroundTruth();
//        calcGAChangeInMethodChange();
        testMiningMigration();
//        testTruthPosition();
    }

    public void testTruthPosition() throws Exception {
        Map<Long, Map<Long, Integer>> methodChangeSupportMap = buildMethodChangeSupportMap("db/GAChangeInMethodChange.csv");
        Map<Long, Set<Long>> groundTruthMap = buildGroundTruthMap("db/ground-truth-2014-equals.csv");
        List<List<Long>> rdsList = buildRepositoryDepSeq("db/RepositoryDepSeq.csv");
        Map<Double, Integer> percentCounter = new HashMap<>();
        Map<Integer, Integer> positionCounter = new HashMap<>();
        for (List<Long> depSeq : rdsList) {
            depSeq = dependencyChangePatternAnalysisService.simplifyLibIdList(depSeq);
            List<DependencyChangePatternAnalysisService.LibraryMigrationPattern> patternList =
                    dependencyChangePatternAnalysisService.miningSingleDepSeq(depSeq, groundTruthMap.keySet());
            for (DependencyChangePatternAnalysisService.LibraryMigrationPattern pattern : patternList) {
                int pos = 0;
                Set<Long> truth = groundTruthMap.get(pattern.fromId);
                for (Long toId : pattern.toIdList) {
                    if(truth.contains(toId)) {
                        positionCounter.put(pos, positionCounter.getOrDefault(pos, 0) + 1);
                        double percent = (pos) / (double) pattern.toIdList.size();

                        percentCounter.put(percent, percentCounter.getOrDefault(percent, 0) + 1);
                    }
                    ++pos;
                }
            }
        }
        positionCounter.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
        percentCounter.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
    }

    public Map<Long, Map<Long, Integer>> buildMethodChangeSupportMap(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = reader.readLine();
        Map<Long, Map<Long, Integer>> result = new HashMap<>(100000);
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",");
            Long fromId = Long.parseLong(attrs[0]);
            Long toId = Long.parseLong(attrs[1]);
            Integer counter = Integer.parseInt(attrs[2]);
            result.computeIfAbsent(fromId, k -> new HashMap<>()).put(toId, counter);
        }
        return result;
    }

    public void calcGAChangeInMethodChange() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader("db/MethodChangeDetail.csv"));
        String line = reader.readLine();
        Map<Long, Map<Long, Integer>> result = new HashMap<>(100000);
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",");
            if("".equals(attrs[3]) || "".equals(attrs[4])) {
                continue;
            }
            String[] delGA = attrs[3].split(";");
            String[] addGA = attrs[4].split(";");
            int counter = Integer.parseInt(attrs[5]);
            Set<Long> delGASet = new HashSet<>(delGA.length * 2);
            Set<Long> addGASet = new HashSet<>(addGA.length * 2);
            for (int i = 0; i < delGA.length; i++) {
                delGASet.add(Long.parseLong(delGA[i]));
            }
            for (int i = 0; i < addGA.length; i++) {
                addGASet.add(Long.parseLong(addGA[i]));
            }
            Set<Long> delGANoDup = new HashSet<>(delGASet);
            delGANoDup.removeAll(addGASet);
            if(delGANoDup.isEmpty()) continue;
            Set<Long> addGaNoDup = new HashSet<>(addGASet);
            addGaNoDup.removeAll(delGASet);
            if(addGaNoDup.isEmpty()) continue;
            for (Long del : delGANoDup) {
                Map<Long, Integer> candidateMap = result.computeIfAbsent(del, k -> new HashMap<>());
                for (Long add : addGaNoDup) {
                    candidateMap.put(add, candidateMap.getOrDefault(add, 0) + counter);
                }
            }
        }
        reader.close();
        FileWriter writer = new FileWriter("db/GAChangeInMethodChange.csv");
        writer.write("fromId,toId,counter\n");
        List<Pair<Long, List<Pair<Long, Integer>>>> outputLines = new ArrayList<>(result.size());
        result.forEach((fromId, candidateMap) -> {
            List<Pair<Long, Integer>> candidateList = new ArrayList<>(candidateMap.size());
            candidateMap.forEach((toId, counter) -> candidateList.add(new Pair<>(toId, counter)));
            candidateList.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            outputLines.add(new Pair<>(fromId, candidateList));
        });
        outputLines.sort(Comparator.comparingLong(Pair::getKey));
        for (Pair<Long, List<Pair<Long, Integer>>> outputLine : outputLines) {
            long fromId = outputLine.getKey();
            for (Pair<Long, Integer> subLine : outputLine.getValue()) {
                writer.write(fromId + "," + subLine.getKey() + "," + subLine.getValue() + "\n");
            }
        }
        writer.close();
        LOG.info("Success");
    }

    public void calcGroundTruth() throws Exception {
        List<LibraryGroupArtifact> gaList = libraryGroupArtifactMapper.findAll();
        gaList.sort(Comparator.comparingLong(LibraryGroupArtifact::getId));
        FileWriter writer = new FileWriter("db/ground-truth-2014-equals-multi.csv");
        Map<String, String> libraryName2IdsMapFrom = new HashMap<>();
        Map<String, String> libraryName2IdsMapTo = new HashMap<>();
        writer.write("fromLibrary;toLibrary;fromIds;toIds;score\n");
        Document document = Jsoup.parse(new File("db/rules-2014.html"), "UTF-8");
        Elements elements = document.select("tr");
        int i = 0;
        for (Element element : elements) {
            if(i++ == 0) continue;
            String fromLibrary = element.child(0).text();
            String toLibrary = element.child(1).text();
            String score = element.child(2).text();
            LOG.info("from = {}, to = {}, score = {}", fromLibrary, toLibrary, score);
            String fromIds = calcLibraryIdsFromNameEquals(fromLibrary, gaList, libraryName2IdsMapFrom);
            String toIds = calcLibraryIdsFromNameEquals(toLibrary, gaList, libraryName2IdsMapTo);
            writer.write(fromLibrary);
            writer.write(";");
            writer.write(toLibrary);
            writer.write(";");
            writer.write(fromIds);
            writer.write(";");
            writer.write(toIds);
            writer.write(";");
            writer.write(score);
            writer.write("\n");
        }
        writer.close();
    }

    private String calcLibraryIdsFromNameEquals(String libraryName, List<LibraryGroupArtifact> gaList, Map<String, String> libraryName2IdsMap) {
        String res = libraryName2IdsMap.get(libraryName);
        if(res!= null) return res;
        List<Long> result = new LinkedList<>();
        for (LibraryGroupArtifact ga : gaList) {
            if(ga.getArtifactId().equals(libraryName)) {
                result.add(ga.getId());
//                break;
            }
        }
        if(result.isEmpty()) {
            for (LibraryGroupArtifact ga : gaList) {
                if(ga.getArtifactId().contains(libraryName)) {
                    result.add(ga.getId());
//                    break;
                }
            }
        }
        res = JsonUtils.writeObjectAsString(result);
        libraryName2IdsMap.put(libraryName, res);
        return res;
    }

    private String calcLibraryIdsFromNameContains(String libraryName, List<LibraryGroupArtifact> gaList, Map<String, String> libraryName2IdsMap) {
        String res = libraryName2IdsMap.get(libraryName);
        if(res!= null) return res;
        List<Long> result = new LinkedList<>();
        for (LibraryGroupArtifact ga : gaList) {
            if(ga.getArtifactId().contains(libraryName)) {
                result.add(ga.getId());
            }
        }
        res = JsonUtils.writeObjectAsString(result);
        libraryName2IdsMap.put(libraryName, res);
        return res;
    }

    private Map<Long, Set<Long>> buildGroundTruthMap(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = reader.readLine();
        Map<Long, Set<Long>> result = new HashMap<>();
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(";");
            List<Long> fromIds = JsonUtils.readStringAsObject(attrs[2], new TypeReference<List<Long>>() {});
            List<Long> toIds = JsonUtils.readStringAsObject(attrs[3], new TypeReference<List<Long>>() {});
            for (Long fromId : fromIds) {
                result.computeIfAbsent(fromId, k -> new HashSet<>()).addAll(toIds);
            }
        }
        reader.close();
        return result;
    }

    public void insertLibraryOverlap() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader("db/LibraryOverlap.csv"));
        String line = reader.readLine();
        int limit = 1000;
        List<LibraryOverlap> list = new ArrayList<>(limit);
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",");
            list.add(new LibraryOverlap()
                    .setGroupArtifactId1(Long.parseLong(attrs[0]))
                    .setGroupArtifactId2(Long.parseLong(attrs[1]))
                    .setSignatureCount(Integer.parseInt(attrs[2])));
            if(list.size() >= limit) {
                libraryOverlapMapper.insert(list);
                list.clear();
            }
        }
        if(!list.isEmpty()) {
            libraryOverlapMapper.insert(list);
        }
    }

    public void insertGroupArtifact() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader("db/LibraryGroupArtifact.csv"));
        String line = reader.readLine();
        int limit = 1000;
        List<LibraryGroupArtifact> list = new ArrayList<>(limit);
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",");
            list.add(new LibraryGroupArtifact()
                    .setId(Long.parseLong(attrs[0]))
                    .setGroupId(attrs[1])
                    .setArtifactId(attrs[2]));
            if(list.size() >= limit) {
                libraryGroupArtifactMapper.insertWithId(list);
                list.clear();
            }
        }
        if(!list.isEmpty()) {
            libraryGroupArtifactMapper.insertWithId(list);
        }
    }

    public List<List<Long>> buildRepositoryDepSeq(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = reader.readLine();
        List<List<Long>> result = new LinkedList<>();
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            if(attrs.length < 3) {
                System.out.println(line);
            }
            String libIdString = attrs[2]; // pomOnly
//            String libIdString = attrs[3]; // codeWithDup
//            String libIdString = attrs[4]; // codeWithoutDup
            if ("".equals(libIdString)) continue;
            String[] libIds = libIdString.split(";");
            List<Long> libIdList = new ArrayList<>(libIds.length);
            for (String libId : libIds) {
                libIdList.add(Long.parseLong(libId));
            }
            result.add(libIdList);
        }
        return result;
    }

    public void testMiningMigration() throws Exception {
        Map<Long, Map<Long, Integer>> methodChangeSupportMap = buildMethodChangeSupportMap("db/GAChangeInMethodChange.csv");
        Map<Long, Set<Long>> groundTruthMap = buildGroundTruthMap("db/ground-truth-2014-equals-multi.csv");
        List<List<Long>> rdsList = buildRepositoryDepSeq("db/RepositoryDepSeq.csv");
        Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result =
                dependencyChangePatternAnalysisService.miningLibraryMigrationCandidate(
                        rdsList, groundTruthMap.keySet(), methodChangeSupportMap);
        int maxK = 30;
        Map<Long, double[]> precisionMap = new HashMap<>();
        Map<Long, double[]> recallMap = new HashMap<>();
        result.forEach((fromId, candidateList) -> {
            Set<Long> groundTruth = groundTruthMap.get(fromId);
            if(groundTruth == null) return;
            Set<Long> thisTruth = new HashSet<>();
            for (DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate : candidateList) {
                if(groundTruth.contains(candidate.toId)) {
                    thisTruth.add(candidate.toId);
                }
            }
            if(thisTruth.isEmpty()) return;
            System.out.print("fromId: " + fromId + " groundTruth.size: " + groundTruth.size() + " thisTruth.size: " + thisTruth.size());
            int correct = 0;
            double[] precision = new double[maxK];
            double[] recall = new double[maxK];
            for (int k = 1; k <= maxK; ++k) {
                if(candidateList.size() < k) {
                    precision[k-1] = precision[k-2];
                    recall[k-1] = recall[k-2];
                } else {
                    DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate = candidateList.get(k - 1);
                    if(groundTruth.contains(candidate.toId)) {
                        correct++;
                    }
                    precision[k-1] = correct / (double) k;
                    recall[k-1] = correct / (double) thisTruth.size();
                    System.out.print(" Top" + k + ": " + correct);
                }
            }
            System.out.println();
            LibraryGroupArtifact fromLib = libraryGroupArtifactMapper.findById(fromId);
            for (int i = 0; i < 3; i++) {
                if(candidateList.size() <= i) break;
                if(precision[i] == 0) {
                    LibraryGroupArtifact toLib = libraryGroupArtifactMapper.findById(candidateList.get(i).toId);
                    System.out.println(fromLib.getGroupId() + ":" + fromLib.getArtifactId() + " -> " + toLib.getGroupId() + ":" + toLib.getArtifactId());
                }
            }
            precisionMap.put(fromId, precision);
            recallMap.put(fromId, recall);
        });
        double[] totalPrecision = new double[maxK];
        for (double[] value : precisionMap.values()) {
            for (int i = 0; i < maxK; i++) {
                totalPrecision[i] += value[i];
            }
        }
        double[] totalRecall = new double[maxK];
        for (double[] value : recallMap.values()) {
            for (int i = 0; i < maxK; i++) {
                totalRecall[i] += value[i];
            }
        }
        for (int k = 1; k <= maxK; k++) {
            double p = totalPrecision[k-1] / precisionMap.size();
            double r = totalRecall[k-1] / recallMap.size();
            double m = p * r;
            System.out.println("Top" + k + ": Precision: " + p + " Recall: " + r + " M:" + m);
        }
    }

    public void testRepositoryDepSeq() throws Exception {
        RepositoryDepSeq depSeq = gitRepositoryAnalysisService.getRepositoryDepSeq("jgit-cookbook");
        System.out.println(depSeq.getPomOnlyList());
        System.out.println(depSeq.getCodeWithDupList());
        System.out.println(depSeq.getCodeWithoutDupList());
    }

    public void alterTableJob() throws Exception {
        for (int i = 0; i < BlobInfoMapper.MAX_TABLE_COUNT; i++) {
            blobInfoMapper.alterTable(i);
        }

        for (int i = 0; i < CommitInfoMapper.MAX_TABLE_COUNT; i++) {
            commitInfoMapper.alterTable(i);
        }

        for (int i = 0; i < MethodChangeMapper.MAX_TABLE_COUNT; i++) {
            long ii = (long) i;
            long ai = ii << MethodChangeMapper.MAX_ID_BIT;
            methodChangeMapper.createTable(i);
            methodChangeMapper.setAutoIncrement(i, ai);
        }
    }

    // blob 862dbe191db587c33adaf7ea0596c0a02c6cccd2 5a043332e3a22a4d17ed03240fffed222ec74fd1
    // blob b703f00d138039eb44e986e8e90abafc0588464a 9aed2812ec9bc716740da6b5332c95181263de8c
    public void diffCommandLine() throws Exception {
        Scanner sc = new Scanner(System.in);
        RepositoryAnalysisService.AbstractRepository repository = gitRepositoryAnalysisService.openRepository("jgit-cookbook");
        while(true) {
            String cmd = sc.next().toLowerCase();
            String parent = sc.next();
            String revision = sc.next();
            switch (cmd) {
                case "commit": {
                    List<RepositoryAnalysisService.BlobInCommit[]> result = gitRepositoryAnalysisService.getCommitBlobDiff(repository,
                            gitRepositoryAnalysisService.getCommitInfo(repository, revision),
                            gitRepositoryAnalysisService.getCommitInfo(repository, parent));
                    result.forEach(pr -> {
                        System.out.println("---Chunk---");
                        System.out.println("Delete Blob: " + (pr[0] == null ? "null" : pr[0].blobId + " " + pr[0].fileName));
                        System.out.println("Add Blob: " + (pr[1] == null ? "null" : pr[1].blobId + " " + pr[1].fileName));
                    });
                    break;
                }
                case "blob": {
                    RepositoryAnalysisService.BlobInCommit p = new RepositoryAnalysisService.BlobInCommit();
                    p.blobId = parent;
                    p.fileName = "xxx.java";
                    RepositoryAnalysisService.BlobInCommit r = new RepositoryAnalysisService.BlobInCommit();
                    r.blobId = revision;
                    r.fileName = "yyy.java";
                    List<Set<Long>[]> result = gitRepositoryAnalysisService.analyzeBlobDiff(repository, p, r);
                    result.forEach(da -> {
                        System.out.println("---Chunk---");
                        System.out.println("Delete Signature: ");
                        da[0].forEach(this::showMethodSignature);
                        System.out.println("Add Signature: ");
                        da[1].forEach(this::showMethodSignature);
                    });
                    System.out.println("---Blob Diff End---");
                    break;
                }
                default:
                    continue;
            }
        }
    }

    public void commitInfoCommandLine() throws Exception {
        Scanner sc = new Scanner(System.in);
        while(true) {
            String line = sc.nextLine();
            if("".equals(line.trim())) {
                continue;
            }
            CommitInfo commitInfo = gitObjectStorageService.getCommitById(line);
            if(commitInfo == null) {
                System.out.println("Commit: " + line + " = null");
                continue;
            }
            System.out.println("CommitId: " + commitInfo.getCommitIdString());
            System.out.println("CodeGA: " + commitInfo.getCodeGroupArtifactIdList());
            System.out.println("POMGA: " + commitInfo.getPomGroupArtifactIdList());
            System.out.println("MethodChange: " + commitInfo.getMethodChangeIdList());
            int i = 0;
            for (Long methodChangeId : commitInfo.getMethodChangeIdList()) {
                i++;
                if(i % 2 == 0) continue;
                int methodChangeSlice = RepositoryAnalysisService.getMethodChangeSliceKey(methodChangeId);
                MethodChange methodChange = methodChangeMapper.findById(methodChangeSlice, methodChangeId);
                System.out.println("MethodChangeId: " + methodChange.getId());
                System.out.println("Counter: " + methodChange.getCounter());
                System.out.println("DelSig: " + methodChange.getDeleteSignatureIdList());
                methodChange.getDeleteSignatureIdList().forEach(this::showMethodSignature);
                System.out.println("AddSig: " + methodChange.getAddSignatureIdList());
                methodChange.getAddSignatureIdList().forEach(this::showMethodSignature);
                System.out.println("DelGA: " + methodChange.getDeleteGroupArtifactIdList());
                System.out.println("AddGA: " + methodChange.getAddGroupArtifactIdList());
            }
        }
    }

    public void showMethodSignature(long signatureId) {
        int slice = libraryIdentityService.getMethodSignatureSliceKey(signatureId);
        MethodSignature methodSignature = methodSignatureMapper.findById(slice, signatureId);
        if(methodSignature == null) {
            System.out.println("Signature: Id = " + signatureId + " null");
        } else {
            System.out.println("Signature: Id = " + signatureId + ", " +
                    methodSignature.getPackageName() + "." + methodSignature.getClassName() + "::" +
                    methodSignature.getMethodName() + "(" + methodSignature.getParamList() + ")");
        }
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
        String content = readFile("..\\jgit-cookbook\\src\\main\\java\\org\\dstadler\\jgit\\porcelain\\ListTags.java");
        List<MethodSignature> msList = javaCodeAnalysisService.analyzeJavaCode(content);
        for (MethodSignature ms : msList) {
            MethodSignature dbms = libraryIdentityService.getMethodSignature(ms, null);
            Long id = dbms == null ? null : dbms.getId();
            LOG.info("id = {}, pn = {}, cn = {}, mn = {}, pl = {}, ss = {}, se = {}", id,
                    ms.getPackageName(), ms.getClassName(), ms.getMethodName(), ms.getParamList(), ms.getStartLine(), ms.getEndLine());
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

    public void createTable() {
        for (int i = 0; i < BlobInfoMapper.MAX_TABLE_COUNT; i++) {
            blobInfoMapper.createTable(i);
        }

        for (int i = 0; i < CommitInfoMapper.MAX_TABLE_COUNT; i++) {
            commitInfoMapper.createTable(i);
        }
        for (int i = 0; i < MethodChangeMapper.MAX_TABLE_COUNT; i++) {
            long ii = (long) i;
            long ai = ii << MethodChangeMapper.MAX_ID_BIT;
            methodChangeMapper.createTable(i);
            methodChangeMapper.setAutoIncrement(i, ai);
        }
    }
}
