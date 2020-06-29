package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.LibraryGroupArtifact;
import edu.pku.migrationhelper.mapper.LibraryGroupArtifactMapper;
import edu.pku.migrationhelper.mapper.LioProjectWithRepositoryMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that provide evaluation facilities such as
 *   1. loading and querying ground truth
 *   2. select libraries by different criteria
 */
@Service
public class EvaluationService {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private static class GroundTruth {
        String fromLib;
        String toLib;
        List<String> fromGroupArtifacts;
        List<String> toGroupArtifacts;
        List<Long> fromGroupArtifactIds;
        List<Long> toGroupArtifactIds;
    }

    @Value("${migration-helper.evaluation.ground-truth-file}")
    private String groundTruthFile;

    @Autowired
    private LioProjectWithRepositoryMapper lioProjectWithRepositoryMapper;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    private List<GroundTruth> groundTruths;

    private Map<Long, Set<Long>> groundTruthMap;

    private Map<Long, LibraryGroupArtifact> groupArtifactCache;

    private long getGroupArtifactId(String name) {
        String[] ga = name.split(":");
        LibraryGroupArtifact lib = libraryGroupArtifactMapper.findByGroupIdAndArtifactId(ga[0], ga[1]);
        if (lib != null) {
            return lib.getId();
        } else {
            LOG.warn("{} does not exist in database", name);
            return -1;
        }
    }

    @PostConstruct
    public void initializeGroundTruth() throws IOException {
        groundTruths = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new FileReader(groundTruthFile))) {
            for (CSVRecord record : parser) {
                GroundTruth gt = new GroundTruth();
                gt.fromLib = record.get("fromLibrary");
                gt.toLib = record.get("toLibrary");
                if (!record.get("fromGroupArtifacts").equals(""))
                    gt.fromGroupArtifacts = Arrays.asList(record.get("fromGroupArtifacts").split(";"));
                else
                    gt.fromGroupArtifacts = new ArrayList<>();
                if (!record.get("toGroupArtifacts").equals(""))
                    gt.toGroupArtifacts = Arrays.asList(record.get("toGroupArtifacts").split(";"));
                else
                    gt.fromGroupArtifacts = new ArrayList<>();
                gt.fromGroupArtifactIds = gt.fromGroupArtifacts.stream()
                        .map(this::getGroupArtifactId).collect(Collectors.toList());
                gt.toGroupArtifactIds = gt.toGroupArtifacts.stream()
                        .map(this::getGroupArtifactId).collect(Collectors.toList());
                groundTruths.add(gt);
            }
        }
        groundTruthMap = new HashMap<>();
        for (GroundTruth gt : groundTruths) {
            for (Long fromId : gt.fromGroupArtifactIds) {
                groundTruthMap.computeIfAbsent(fromId, k -> new HashSet<>()).addAll(gt.toGroupArtifactIds);
            }
        }
    }

    @PostConstruct
    public synchronized void initializeGroupArtifactCache() {
        List<LibraryGroupArtifact> list = libraryGroupArtifactMapper.findAll();
        Map<Long, LibraryGroupArtifact> map = new HashMap<>(list.size() * 2);
        for (LibraryGroupArtifact groupArtifact : list) {
            map.put(groupArtifact.getId(), groupArtifact);
        }
        groupArtifactCache = Collections.unmodifiableMap(map);
    }

    public void evaluate(Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result) {
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

    public List<Long> getLioProjectIdsInGroundTruth() {
        Set<Long> result = new HashSet<>();
        for (GroundTruth gt : groundTruths) {
            result.addAll(gt.fromGroupArtifacts.stream()
                    .map(s -> lioProjectWithRepositoryMapper.findByName(s).getId())
                    .collect(Collectors.toList()));
            result.addAll(gt.toGroupArtifacts.stream()
                    .map(s -> lioProjectWithRepositoryMapper.findByName(s).getId())
                    .collect(Collectors.toList()));
        }
        return new ArrayList<>(result);
    }

    public List<Long> getLioProjectIdsByCombinedPopularity(int limitCount) {
        LOG.info("Get libraries by combining results from different popularity measure, limit = {}", limitCount);

        Set<Long> idSet = new HashSet<>();
        List<Long> needParseIds = new LinkedList<>();
        Iterator<Long>[] idsArray = new Iterator[7];
        idsArray[0] = lioProjectWithRepositoryMapper.selectIdOrderByDependentProjectsCountLimit(limitCount).iterator();
        idsArray[1] = lioProjectWithRepositoryMapper.selectIdOrderByDependentRepositoriesCountLimit(limitCount).iterator();
        idsArray[2] = lioProjectWithRepositoryMapper.selectIdOrderByRepositoryForkCountLimit(limitCount).iterator();
        idsArray[3] = lioProjectWithRepositoryMapper.selectIdOrderByRepositoryStarCountLimit(limitCount).iterator();
        idsArray[4] = lioProjectWithRepositoryMapper.selectIdOrderByRepositoryWatchersCountLimit(limitCount).iterator();
        idsArray[5] = lioProjectWithRepositoryMapper.selectIdOrderBySourceRankLimit(limitCount).iterator();
        idsArray[6] = lioProjectWithRepositoryMapper.selectIdOrderByRepositorySourceRankLimit(limitCount).iterator();
        while (true) {
            boolean remain = false;
            for (Iterator<Long> longIterator : idsArray) {
                if (longIterator.hasNext()) {
                    remain = true;
                    long id = longIterator.next();
                    if (!idSet.contains(id)) {
                        needParseIds.add(id);
                        idSet.add(id);
                    }
                }
            }
            if(!remain) break;
        }
        return needParseIds;
    }
}
