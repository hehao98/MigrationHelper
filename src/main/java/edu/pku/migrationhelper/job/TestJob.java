package edu.pku.migrationhelper.job;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.pku.migrationhelper.data.*;
import edu.pku.migrationhelper.mapper.*;
import edu.pku.migrationhelper.service.*;
import edu.pku.migrationhelper.util.JsonUtils;
import edu.pku.migrationhelper.util.LZFUtils;
import edu.pku.migrationhelper.util.MathUtils;
import javafx.util.Pair;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.tomcat.util.buf.HexUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tokyocabinet.HDB;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by xuyul on 2020/1/2.
 */
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "TestJob")
public class TestJob implements CommandLineRunner {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private LibraryIdentityService libraryIdentityService;

    @Autowired
    private MethodSignatureMapper methodSignatureMapper;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private LibraryVersionToSignatureMapper libraryVersionToSignatureMapper;

    @Autowired
    private GitRepositoryAnalysisService gitRepositoryAnalysisService;

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
    private LibraryOverlapMapper libraryOverlapMapper;

    private Map<Long, LibraryGroupArtifact> groupArtifactCache = null;

    @Override
    public void run(String ...args) throws Exception {
        String methodName = args[0];

        java.lang.reflect.Method method;
        try {
            method = this.getClass().getMethod(methodName);
        } catch (NoSuchMethodException e) {
            LOG.error("Method {} does not exist in TestJob!", methodName);
            LOG.info("Please refer to source code to see available methods");
            System.exit(SpringApplication.exit(context));
            return;
        }

        LOG.info("Invoking method {}", methodName);
        try {
            method.invoke(this);
        } catch (IllegalArgumentException e) {
            LOG.error("Method {} has arguments, which is currently not supported, aborting", methodName);
            System.exit(SpringApplication.exit(context));
            return;
        }

        LOG.info("Running method {} finish", methodName);
        System.exit(SpringApplication.exit(context));
    }

    public void printLibraryDatabaseSummary() {
        List<LibraryGroupArtifact> libs = libraryGroupArtifactMapper.findAll();
        LOG.info("Number of group artifacts: {}", libs.size());
        LOG.info("Number of artifacts that version is extracted: {}",
                libs.stream().filter(LibraryGroupArtifact::isVersionExtracted).count());
        LOG.info("Number of parsed group artifacts: {}",
                libs.stream().filter(LibraryGroupArtifact::isParsed).count());
        LOG.info("Number of artifacts with parse errors: {}",
                libs.stream().filter(LibraryGroupArtifact::isParseError).count());

        List<LibraryVersion> versions = libs.stream()
                .map(lib -> libraryVersionMapper.findByGroupArtifactId(lib.getId()))
                .flatMap(List::stream).collect(Collectors.toList());
        long downloadCount = versions.stream().filter(LibraryVersion::isDownloaded).count();
        long parsedCount = versions.stream().filter(LibraryVersion::isParsed).count();
        long parseErrorCount = versions.stream().filter(LibraryVersion::isParseError).count();
        LOG.info("{} versions in total, downloadCount = {}, parsedCount = {}, parseErrorCount = {}",
                versions.size(), downloadCount, parsedCount, parseErrorCount);
    }

    public void printLibraryAPISummary(String groupId, String artifactId) {

    }

    public synchronized void buildGroupArtifactCache() {
        if(groupArtifactCache != null) return;
        List<LibraryGroupArtifact> list = libraryGroupArtifactMapper.findAll();
        Map<Long, LibraryGroupArtifact> map = new HashMap<>(list.size() * 2);
        for (LibraryGroupArtifact groupArtifact : list) {
            map.put(groupArtifact.getId(), groupArtifact);
        }
        groupArtifactCache = Collections.unmodifiableMap(map);
    }

    public void play() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader("db/rules-2014-artifactList.csv"));
        FileWriter writer = new FileWriter("db/r.csv");
        String line = reader.readLine();
        writer.write(line);
        writer.write("\n");
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            for (int i = 0; i < attrs.length; i++) {
                if(i != 1 && i != 3) continue;
                if(!"".equals(attrs[i])) {
                    String[] ids = attrs[i].split(";");
                    StringBuilder sb = new StringBuilder();
                    for (String id : ids) {
                        LibraryGroupArtifact ga = libraryGroupArtifactMapper.findById(Long.parseLong(id));
                        sb.append(ga.getGroupId()).append(":").append(ga.getArtifactId()).append(";");
                    }
                    sb.deleteCharAt(sb.length() - 1);
                    attrs[i+1] = sb.toString();
                } else {
                    attrs[i+1] = "";
                }
            }
            for (int i = 0; i < attrs.length; i++) {
                writer.write(attrs[i]);
                if(i == attrs.length - 1) {
                    writer.write("\n");
                }else {
                    writer.write(",");
                }
            }
        }
        writer.close();
    }
// TODO 仅保留包含truth的样例来作图，否则图像会被大量未知真假的点扭曲
    public void runRQ1() throws Exception {
        Map<Long, Set<Long>> groundTruthMap = buildGroundTruthMap("db/ground-truth-2014-manual.csv");
        List<List<Long>> rdsList = buildRepositoryDepSeq("db/RepositoryDepSeq-all.csv");
        Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result =
                dependencyChangePatternAnalysisService.miningLibraryMigrationCandidate(
                        rdsList, groundTruthMap.keySet(), new HashMap<>(), 0, DependencyChangePatternAnalysisService.DefaultMinMCSupportPercent, null, null);
        int repoTotal = rdsList.size();
        FileWriter output = new FileWriter("db/RQ0421/RQ1-pomOnly.csv");
        output.write("fromLib,toLib,isTruth,patternSupport,patternSupportP,occurCount,hot,hotRank\n");
        for (List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate> candidateList : result.values()) {
            for (DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate : candidateList) {
                output.write(candidate.fromId + "," + candidate.toId + "," +
                        groundTruthMap.get(candidate.fromId).contains(candidate.toId) + "," +
                        candidate.ruleCount + "," +candidate.ruleSupportByMax + "," +
                        candidate.libraryConcurrenceCount + "," + candidate.libraryConcurrenceSupport + "," +
                        (candidate.ruleSupportByMax * candidate.libraryConcurrenceSupport) + "\n"
                );
            }
        }
        output.close();
        LOG.info("Success");
    }

    public void runRQ2() throws Exception {
        Map<Long, Set<Long>> groundTruthMap = buildGroundTruthMap("db/ground-truth-2014-manual.csv");
        List<List<Long>> rdsList = buildRepositoryDepSeq("db/RepositoryDepSeq-all.csv");
        FileWriter truthPercent = new FileWriter("db/RQ0421/RQ2-truth-percent.csv");
        FileWriter truthPosition = new FileWriter("db/RQ0421/RQ2-truth-position.csv");
        truthPercent.write("fromId,truthCount,totalCount,percent\n");
        truthPosition.write("fromId,toId,isTruth,distance,total\n");
        for (List<Long> depSeq : rdsList) {
            depSeq = dependencyChangePatternAnalysisService.simplifyLibIdList(depSeq, null, null);
            List<DependencyChangePatternAnalysisService.LibraryMigrationPattern> patternList =
                    dependencyChangePatternAnalysisService.miningSingleDepSeq(depSeq, groundTruthMap.keySet(), null);
            for (DependencyChangePatternAnalysisService.LibraryMigrationPattern pattern : patternList) {
                int pos = 1;
                Set<Long> truth = groundTruthMap.get(pattern.fromId);
                int truthCount = 0;
                int totalCount = pattern.toIdList.size();
                for (Long toId : pattern.toIdList) {
                    boolean isTruth = false;
                    if(truth.contains(toId)) {
                        isTruth = true;
                        truthCount++;
                    }
                    truthPosition.write(pattern.fromId+","+toId+","+isTruth+","+pos+","+totalCount+"\n");
                    ++pos;
                }
                double p = truthCount / (double) totalCount;
                truthPercent.write(pattern.fromId+","+truthCount+","+totalCount+","+p+"\n");
            }
        }
        truthPercent.close();
        truthPosition.close();
        LOG.info("Success");
    }

    public void runRQ3() throws Exception {
        Map<Long, Map<Long, Integer>> methodChangeSupportMap = buildMethodChangeSupportMap("db/GAChangeInMethodChange-all.csv");
        List<List<Long>> rdsList = buildRepositoryDepSeq("db/RepositoryDepSeq-all.csv");
        Map<Long, Set<Long>> groundTruthMap = buildGroundTruthMap("db/ground-truth-2014-manual.csv");
        Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result =
                dependencyChangePatternAnalysisService.miningLibraryMigrationCandidate(
                        rdsList, groundTruthMap.keySet(), methodChangeSupportMap, DependencyChangePatternAnalysisService.DefaultMinPatternSupport, 0, null, null);
        FileWriter output = new FileWriter("db/RQ0421/RQ3.csv");
        output.write("fromId,toId,isTruth,APISupport,APIRank0,patternSupport\n");
        for (List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate> candidateList : result.values()) {
            boolean containsTruth = false;
            for (DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate : candidateList) {
                containsTruth = groundTruthMap.get(candidate.fromId).contains(candidate.toId);
                if(containsTruth) break;
            }
            if(!containsTruth) continue;
            for (DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate : candidateList) {
                boolean isTruth = groundTruthMap.get(candidate.fromId).contains(candidate.toId);
                output.write(candidate.fromId+","+candidate.toId+","+isTruth+","+candidate.methodChangeCount +","+candidate.methodChangeSupportByMax +","+candidate.ruleCount +"\n");
            }
        }
        output.close();
        LOG.info("Success");
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
        BufferedReader reader = new BufferedReader(new FileReader("db/MethodChangeDetail-all.csv"));
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
        FileWriter writer = new FileWriter("db/GAChangeInMethodChange-all.csv");
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

    public void calcGroundTruth2() throws Exception {
        FileWriter writer = new FileWriter("db/ground-truth-2014-manual.csv");
        BufferedReader raw = new BufferedReader(new FileReader("db/rules-2014-raw.csv"));
        BufferedReader artifactList = new BufferedReader(new FileReader("db/rules-2014-artifactList.csv"));
        Map<String, List<Long>> artifact2Ids = new HashMap<>();
        Map<String, List<Long>> artifact2IdsSimilar = new HashMap<>();
        String line = artifactList.readLine();
        while((line = artifactList.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            String name = attrs[0];
            String ids = attrs[1];
            if("".equals(ids)) {
                artifact2Ids.put(name, new ArrayList<>(0));
            } else {
                String[] idss = ids.split(";");
                List<Long> idList = new ArrayList<>(idss.length);
                for (String id : idss) {
                    idList.add(Long.parseLong(id));
                }
                artifact2Ids.put(name, idList);
            }
            ids = attrs[3];
            if("".equals(ids)) {
                artifact2IdsSimilar.put(name, new ArrayList<>(0));
            } else {
                String[] idss = ids.split(";");
                List<Long> idList = new ArrayList<>(idss.length);
                for (String id : idss) {
                    idList.add(Long.parseLong(id));
                }
                artifact2IdsSimilar.put(name, idList);
            }
        }
        line = raw.readLine();
        writer.write("fromLibrary;toLibrary;fromIds;toIds;score\n");
        while((line = raw.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            String fromLib = attrs[0];
            String toLib = attrs[1];
            String score = attrs[2];
            Set<Long> fromIds = new HashSet<>();
            Set<Long> toIds = new HashSet<>();
            fromIds.addAll(artifact2Ids.get(fromLib));
            toIds.addAll(artifact2Ids.get(fromLib));
            toIds.addAll(artifact2IdsSimilar.get(fromLib));
            toIds.addAll(artifact2Ids.get(toLib));
            toIds.addAll(artifact2IdsSimilar.get(toLib));
            String fromIdList = JsonUtils.writeObjectAsString(new ArrayList<>(fromIds));
            String toIdList = JsonUtils.writeObjectAsString(new ArrayList<>(toIds));
            writer.write(fromLib);
            writer.write(";");
            writer.write(toLib);
            writer.write(";");
            writer.write(fromIdList);
            writer.write(";");
            writer.write(toIdList);
            writer.write(";");
            writer.write(score);
            writer.write("\n");
        }
        writer.close();
        raw.close();
        artifactList.close();
    }

    public void calcGroundTruth() throws Exception {
        List<LibraryGroupArtifact> gaList = libraryGroupArtifactMapper.findAll();
        gaList.sort(Comparator.comparingLong(LibraryGroupArtifact::getId));
        FileWriter writer = new FileWriter("db/ground-truth-2014-equals-multi.csv");
        FileWriter raw = new FileWriter("db/rules-2014-raw.csv");
        FileWriter artifactList = new FileWriter("db/rules-2014-artifactList.csv");
        Map<String, String> libraryName2IdsMapFrom = new HashMap<>();
        Map<String, String> libraryName2IdsMapTo = new HashMap<>();
        writer.write("fromLibrary;toLibrary;fromIds;toIds;score\n");
        raw.write("fromLib,toLib,score\n");
        artifactList.write("libName,groupArtifactIds\n");
        Document document = Jsoup.parse(new File("db/rules-2014.html"), "UTF-8");
        Elements elements = document.select("tr");
        Set<String> nameSet = new HashSet<>();
        int i = 0;
        for (Element element : elements) {
            if(i++ == 0) continue;
            String fromLibrary = element.child(0).text();
            String toLibrary = element.child(1).text();
            String score = element.child(2).text();
            raw.write(fromLibrary + "," + toLibrary + "," + score + "\n");
            nameSet.add(fromLibrary);
            nameSet.add(toLibrary);
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
        for (String libName : nameSet) {
            artifactList.write(libName + ",\n");
        }
        writer.close();
        raw.close();
        artifactList.close();
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
//            String libIdString = attrs[5]; // pomWithCodeDel
//            String libIdString = attrs[6]; // pomWithCodeAdd
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

    public List<List<String>> buildDepSeqCommitList(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = reader.readLine();
        List<List<String>> result = new LinkedList<>();
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            if(attrs.length < 3) {
                System.out.println(line);
            }
            String libIdString = attrs[2]; // pomOnly
            if ("".equals(libIdString)) continue;
            String commitListString = attrs[7];
            int len = commitListString.length();
            int commitCount = len / 40;
            List<String> commitList = new ArrayList<>(commitCount);
            for (int i = 0; i < commitCount; i++) {
                commitList.add(commitListString.substring(i * 40, i * 40 + 40));
            }
            result.add(commitList);
        }
        return result;
    }

    public List<String> buildDepSeqRepoList(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = reader.readLine();
        List<String> result = new LinkedList<>();
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            if(attrs.length < 3) {
                System.out.println(line);
            }
            String libIdString = attrs[2]; // pomOnly
            if ("".equals(libIdString)) continue;
            result.add(attrs[1]);
        }
        return result;
    }

    public List<LibraryGroupArtifact> readLibraryFromArtifactIdFile(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        List<LibraryGroupArtifact> result = new LinkedList<>();
        while((line = reader.readLine()) != null) {
            result.addAll(libraryGroupArtifactMapper.findByArtifactId(line));
        }
        return result;
    }

    public void miningLibrariesSave2File() throws Exception {
        buildGroupArtifactCache();
        Map<Long, Map<Long, Integer>> methodChangeSupportMap = buildMethodChangeSupportMap("db/GAChangeInMethodChange-all.csv");
        List<List<Long>> rdsList = buildRepositoryDepSeq("db/RepositoryDepSeq-withCommit.csv");
        List<LibraryGroupArtifact> queryList = readLibraryFromArtifactIdFile("db/libs.txt");
        Set<Long> fromIdLimit = new HashSet<>();
        queryList.forEach(e -> fromIdLimit.add(e.getId()));
        Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result =
                dependencyChangePatternAnalysisService.miningLibraryMigrationCandidate(
                        rdsList, fromIdLimit, methodChangeSupportMap);
        FileWriter resultWriter = new FileWriter("db/libs-result.csv");
        result.forEach((fromId, candidateList) -> {
            LibraryGroupArtifact fromLib = groupArtifactCache.get(fromId);
            candidateList = candidateList.stream()
                    .filter(candidate -> {
                        LibraryGroupArtifact toLib = groupArtifactCache.get(candidate.toId);
                        return !Objects.equals(toLib.getGroupId(), fromLib.getGroupId());
                    }).collect(Collectors.toList());
            if(candidateList.isEmpty()) return;
            candidateList = candidateList.stream()
                    .limit(20)
                    .collect(Collectors.toList());
            try {
                resultWriter.write(fromLib.getGroupId());
                resultWriter.write(":");
                resultWriter.write(fromLib.getArtifactId());
                for (DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate : candidateList) {
                    LibraryGroupArtifact toLib = groupArtifactCache.get(candidate.toId);
                    resultWriter.write(",");
                    resultWriter.write(toLib.getGroupId());
                    resultWriter.write(":");
                    resultWriter.write(toLib.getArtifactId());
                }
                resultWriter.write("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        resultWriter.close();
        LOG.info("Success");
    }

    public void migrationRulesSave2File() throws Exception {
        buildGroupArtifactCache();
        Map<Long, Map<Long, Integer>> methodChangeSupportMap = buildMethodChangeSupportMap("db/GAChangeInMethodChange-all.csv");
        Map<Long, Set<Long>> groundTruthMap = buildGroundTruthMap("db/ground-truth-2014-manual.csv");
        List<List<Long>> rdsList = buildRepositoryDepSeq("db/RepositoryDepSeq-withCommit.csv");
        List<List<String>> commitList = buildDepSeqCommitList("db/RepositoryDepSeq-withCommit.csv");
        List<String> repoList = buildDepSeqRepoList("db/RepositoryDepSeq-withCommit.csv");
        Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result =
                dependencyChangePatternAnalysisService.miningLibraryMigrationCandidate(
                        rdsList, null, methodChangeSupportMap,
                        DependencyChangePatternAnalysisService.DefaultMinPatternSupport, DependencyChangePatternAnalysisService.DefaultMinMCSupportPercent,
                        repoList, commitList);
        FileWriter correct = new FileWriter("db/correct-library-migration.csv");
        FileWriter unknown = new FileWriter("db/unknown-library-migration.csv");
        result.forEach((fromId, candidateList) -> {
            LibraryGroupArtifact fromLib = groupArtifactCache.get(fromId);
            candidateList = candidateList.stream()
                    .filter(candidate -> {
                        LibraryGroupArtifact toLib = groupArtifactCache.get(candidate.toId);
                        return !Objects.equals(toLib.getGroupId(), fromLib.getGroupId());
                    }).collect(Collectors.toList());
            if(candidateList.isEmpty()) return;
            boolean isTruth;
            if(groundTruthMap.containsKey(fromId)) {
                isTruth = true;
                Set<Long> thisTruth = groundTruthMap.get(fromId);
                candidateList = candidateList.stream()
                        .filter(candidate -> thisTruth.contains(candidate.toId))
                        .limit(20)
                        .collect(Collectors.toList());
            } else {
                isTruth = false;
                candidateList = candidateList.stream()
                        .limit(20)
                        .collect(Collectors.toList());
            }
            if(candidateList.isEmpty()) return;
            FileWriter writer = isTruth ? correct : unknown;
            try {
                for (DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate : candidateList) {
                    writer.write(fromLib.getGroupId());
                    writer.write(":");
                    writer.write(fromLib.getArtifactId());
                    LibraryGroupArtifact toLib = groupArtifactCache.get(candidate.toId);
                    writer.write(",");
                    writer.write(toLib.getGroupId());
                    writer.write(":");
                    writer.write(toLib.getArtifactId());
                    for (String[] repoCommit : candidate.repoCommitList) {
                        writer.write(",");
                        writer.write(repoCommit[0]);
                        writer.write(";");
                        writer.write(repoCommit[1]);
                        writer.write(";");
                        writer.write(repoCommit[2]);
                    }
                    writer.write("\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        correct.close();
        unknown.close();
        LOG.info("Success");
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
