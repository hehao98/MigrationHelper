package edu.pku.migrationhelper.job;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.pku.migrationhelper.data.LibraryGroupArtifact;
import edu.pku.migrationhelper.mapper.LibraryGroupArtifactMapper;
import edu.pku.migrationhelper.service.DependencyChangePatternAnalysisService;
import edu.pku.migrationhelper.util.JsonUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "LibraryRecommendJob")
@ConfigurationProperties(prefix = "migration-helper.library-recommend-job")
public class LibraryRecommendJob implements CommandLineRunner {

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private DependencyChangePatternAnalysisService dependencyChangePatternAnalysisService;

    final private Logger LOG = LoggerFactory.getLogger(getClass());

    private Map<Long, LibraryGroupArtifact> groupArtifactCache = null;

    private String apiSupportFile;

    private String dependencySeqFile;

    private String groundTruthFile;

    private String queryFile;

    private String outputFile;

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 2) {
            LOG.info("Usage: LibraryRecommendJob <Query File> <Output File>");
            System.exit(SpringApplication.exit(context, () -> -1));
        }

        this.queryFile = args[0];
        this.outputFile = args[1];
        LOG.info("Read libraries from {} and output results to {}", this.queryFile, this.outputFile);

        LOG.info("Building necessary data...");
        buildGroupArtifactCache();
        Map<Long, Map<Long, Integer>> methodChangeSupportMap = buildMethodChangeSupportMap(apiSupportFile);
        List<List<Long>> rdsList = buildRepositoryDepSeq(dependencySeqFile);

        List<LibraryGroupArtifact> queryList = readLibraryFromQueryFile(queryFile);
        Set<Long> fromIdLimit = new HashSet<>();
        queryList.forEach(e -> fromIdLimit.add(e.getId()));

        LOG.info("Generating recommendation result...");
        Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result =
                dependencyChangePatternAnalysisService.miningLibraryMigrationCandidate(
                        rdsList, fromIdLimit, methodChangeSupportMap);

        LOG.info("Writing results to csv...");
        outputCsv(queryList, result);

        LOG.info("Doing evaluation using ground truth from {}", groundTruthFile);
        Map<Long, Set<Long>> groundTruthMap = buildGroundTruthMap(groundTruthFile);
        evaluateResult(result, groundTruthMap);

        LOG.info("Success");
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private synchronized void buildGroupArtifactCache() {
        if(groupArtifactCache != null) return;
        List<LibraryGroupArtifact> list = libraryGroupArtifactMapper.findAll();
        Map<Long, LibraryGroupArtifact> map = new HashMap<>(list.size() * 2);
        for (LibraryGroupArtifact groupArtifact : list) {
            map.put(groupArtifact.getId(), groupArtifact);
        }
        groupArtifactCache = Collections.unmodifiableMap(map);
    }

    private List<LibraryGroupArtifact> readLibraryFromQueryFile(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line;
        List<LibraryGroupArtifact> result = new LinkedList<>();
        while((line = reader.readLine()) != null) {
            String[] ga = line.split(":");
            LibraryGroupArtifact groupArtifact = libraryGroupArtifactMapper.findByGroupIdAndArtifactId(ga[0], ga[1]);
            if(groupArtifact == null) {
                LOG.warn("groupArtifact not found: {}", line);
                continue;
            }
            result.add(groupArtifact);
        }
        reader.close();
        return result;
    }

    private static Map<Long, Map<Long, Integer>> buildMethodChangeSupportMap(String fileName) throws Exception {
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
        reader.close();
        return result;
    }

    private static List<List<Long>> buildRepositoryDepSeq(String fileName) throws Exception {
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
        reader.close();
        return result;
    }

    private static Map<Long, Set<Long>> buildGroundTruthMap(String fileName) throws Exception {
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

    public static List<List<String>> buildDepSeqCommitList(String fileName) throws Exception {
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
        reader.close();
        return result;
    }

    public static List<String> buildDepSeqRepoList(String fileName) throws Exception {
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
        reader.close();
        return result;
    }

    private void outputCsv(List<LibraryGroupArtifact> queryList, Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result) {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputFile), CSVFormat.EXCEL)) {
            printer.printRecord("fromId", "toId", "fromGroupArtifact", "toGroupArtifact", "confidence",
                    "ruleFreq", "relativeRuleFreq", "concurrence", "concurrenceAdjustment", "commitDistance", "apiSupport");
            for (LibraryGroupArtifact fromLib : queryList) {
                Long fromId = fromLib.getId();
                List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>
                        candidateList = result.get(fromId);

                candidateList = candidateList.stream()
                        .filter(candidate -> {
                            LibraryGroupArtifact toLib = groupArtifactCache.get(candidate.toId);
                            return !Objects.equals(toLib.getGroupId(), fromLib.getGroupId());
                        }).collect(Collectors.toList());

                if(candidateList.isEmpty()) continue;
                candidateList = candidateList.stream()
                        .limit(20)
                        .collect(Collectors.toList());

                for (DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate : candidateList) {
                    LibraryGroupArtifact toLib =  groupArtifactCache.get(candidate.toId);
                    printer.printRecord(fromId, candidate.toId, fromLib.getGroupArtifactId(),
                            toLib.getGroupArtifactId(), candidate.confidence, candidate.ruleCount,
                            candidate.ruleSupportByMax, candidate.libraryConcurrenceCount, candidate.libraryConcurrenceSupport,
                            candidate.commitDistance, candidate.methodChangeSupportByMax);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void evaluateResult(
            Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result,
            Map<Long, Set<Long>> groundTruthMap
    ) {
        int maxK = 10;
        Map<Long, double[]> precisionMap = new HashMap<>();
        Map<Long, double[]> recallMap = new HashMap<>();
        MutableInt ruleCounter = new MutableInt(0);
        result.entrySet().stream()
                .sorted(Comparator.comparingLong(Map.Entry::getKey))
                .forEach(entry -> {
                    long fromId = entry.getKey();
                    LibraryGroupArtifact fromLib = groupArtifactCache.get(fromId);
                    List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate> candidateList = entry.getValue();
                    candidateList = candidateList.stream()
//                            .filter(candidate -> candidate.patternSupport >= 10)
//                            .filter(candidate -> candidate.multipleSupport > 0)
                            .filter(candidate -> {
                                LibraryGroupArtifact toLib = groupArtifactCache.get(candidate.toId);
                                return !Objects.equals(toLib.getGroupId(), fromLib.getGroupId());
                            }).collect(Collectors.toList());
                    if(candidateList.isEmpty()) return;
                    Set<Long> groundTruth = groundTruthMap.get(fromId);
                    if(groundTruth == null) return;
//                    if(groundTruth != null) return; groundTruth = new HashSet<>();
                    ruleCounter.add(candidateList.size());
                    Set<Long> thisTruth = new HashSet<>();
                    for (DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate : candidateList) {
                        if(groundTruth.contains(candidate.toId)) {
                            thisTruth.add(candidate.toId);
                        }
                    }
                    if(thisTruth.isEmpty()) return;
                    //System.out.println("fromId: " + fromId + " groundTruth.size: " + groundTruth.size() + " thisTruth.size: " + thisTruth.size());
                    //System.out.println(fromLib.getId() + ":" + fromLib.getGroupId() + ":" + fromLib.getArtifactId());
                    int correct = 0;
                    double[] precision = new double[maxK];
                    double[] recall = new double[maxK];
                    for (int k = 1; k <= maxK; ++k) {
                        if(candidateList.size() < k) {
                            precision[k-1] = precision[k-2];
                            recall[k-1] = recall[k-2];
                        } else {
                            DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate = candidateList.get(k - 1);
                            boolean thisCorrect = false;
                            if(groundTruth.contains(candidate.toId)) {
                                thisCorrect = true;
                                correct++;
                            }
                            precision[k-1] = correct / (double) k;
                            recall[k-1] = correct / (double) thisTruth.size();
//                            recall[k-1] = correct / (double) groundTruth.size();
                            LibraryGroupArtifact toLib = groupArtifactCache.get(candidate.toId);
                            /*System.out.println(" Top" + k + ": " + correct + "," + thisCorrect + ", " + toLib.getId() + ":" + toLib.getGroupId() + ":" + toLib.getArtifactId() +
                                    ", RuleFreq: " + candidate.ruleCount + ", RelativeRuleFreq: " + candidate.ruleSupportByMax +
                                    ", CoOccurrence: " + candidate.libraryConcurrenceCount + ", CA: " + candidate.libraryConcurrenceSupport + ", PR: " + (candidate.libraryConcurrenceSupport *candidate.ruleSupportByMax) +
                                    ", CommitDistance: " + candidate.commitDistance + ", APISupport: " + candidate.methodChangeSupportByMax + ", FinalConfidenceValue: " + candidate.confidence
                            );*/
                        }
                    }
                    // System.out.println();
//                    for (int i = 0; i < 10; i++) {
//                        if(candidateList.size() <= i) break;
//                        if((i == 0 && precision[i] == 0) || (i > 0 && precision[i] <= precision[i-1])) {
//                            LibraryGroupArtifact toLib = libraryGroupArtifactMapper.findById(candidateList.get(i).toId);
//                            System.out.println(fromLib.getId() + ":" + fromLib.getGroupId() + ":" + fromLib.getArtifactId() +
//                                    " -> " + toLib.getId() + ":" + toLib.getGroupId() + ":" + toLib.getArtifactId());
//                        }
//                    }
                    precisionMap.put(fromId, precision);
                    recallMap.put(fromId, recall);
                });
        System.out.println("Rule Count: " + ruleCounter.intValue());
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
            double f = 2 * p * r / (p + r);
            System.out.println("Top" + k + ": Precision: " + p + " Recall: " + r + " F-measure:" + f);
        }

        try (FileWriter output = new FileWriter("test_data/evaluation.csv")) {
            int outputK = 10;
            for (int i = 1; i <= outputK; i++) {
                output.write(",Top " + i);
            }
            output.write("\nPrecision");
            for (int i = 0; i < outputK; i++) {
                double p = totalPrecision[i] / precisionMap.size();
                output.write("," + p);
            }
            output.write("\nRecall");
            for (int i = 0; i < outputK; i++) {
                double r = totalRecall[i] / recallMap.size();
                output.write("," + r);
            }
            output.write("\nF-measure");
            for (int i = 0; i < outputK; i++) {
                double p = totalPrecision[i] / precisionMap.size();
                double r = totalRecall[i] / recallMap.size();
                double f = 2 * p * r / (p + r);
                output.write("," + f);
            }
            output.write("\n");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /* configuration getter and setter */

    public String getApiSupportFile() {
        return apiSupportFile;
    }

    public LibraryRecommendJob setApiSupportFile(String apiSupportFile) {
        this.apiSupportFile = apiSupportFile;
        return this;
    }

    public String getDependencySeqFile() {
        return dependencySeqFile;
    }

    public LibraryRecommendJob setDependencySeqFile(String dependencySeqFile) {
        this.dependencySeqFile = dependencySeqFile;
        return this;
    }

    public String getGroundTruthFile() {
        return groundTruthFile;
    }

    public void setGroundTruthFile(String groundTruthFile) {
        this.groundTruthFile = groundTruthFile;
    }


    public String getQueryFile() {
        return queryFile;
    }

    public LibraryRecommendJob setQueryFile(String queryFile) {
        this.queryFile = queryFile;
        return this;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public LibraryRecommendJob setOutputFile(String outputFile) {
        this.outputFile = outputFile;
        return this;
    }
}
