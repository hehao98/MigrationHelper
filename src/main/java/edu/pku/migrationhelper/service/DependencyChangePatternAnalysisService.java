package edu.pku.migrationhelper.service;

import ca.pfv.spmf.algorithms.sequentialpatterns.spam.AlgoTKS;
import ca.pfv.spmf.algorithms.sequentialpatterns.spam.PatternTKS;
import ca.pfv.spmf.patterns.itemset_list_integers_without_support.Itemset;
import edu.pku.migrationhelper.data.RepositoryDepSeq;
import javafx.util.Pair;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

@Service
public class DependencyChangePatternAnalysisService {

    public static class DependencySequenceResult {
        public List<List<Long>> pattern;
        public int support;
        public double globalSupport;
        public double patternSupport;
        public double multipleSupport;
    }

    public List<DependencySequenceResult> analyzeDependencySequence(List<RepositoryDepSeq> depSeqList, int topK) throws Exception {
        File inputFile = File.createTempFile("dependency_change_pattern_input", ".txt");
        File outputFile = File.createTempFile("dependency_change_pattern_output", ".txt");
        inputFile.deleteOnExit();
        outputFile.deleteOnExit();
        FileWriter writer = new FileWriter(inputFile);

        Map<Long, Integer> lib2Id = new HashMap<>(100000);
        Map<Integer, Long> id2Lib = new HashMap<>(100000);
        int idGenerator = 1;

        for (RepositoryDepSeq depSeq : depSeqList) {
            if(depSeq.getDepSeqList() != null && !depSeq.getDepSeqList().isEmpty()) {
                for (Long lib : depSeq.getDepSeqList()) {
                    if(lib == 0) {
                        writer.write("-1 ");
                    } else {
                        Integer id = lib2Id.get(lib);
                        if(id == null) {
                            id = idGenerator++;
                            lib2Id.put(lib, id);
                            id2Lib.put(id, lib);
                        }
                        writer.write(id + " ");
                    }
                }
                writer.write("-2\n");
            }
        }

        writer.close();

        AlgoTKS algo = new AlgoTKS();
        algo.setMinimumPatternLength(3);
        algo.setMaximumPatternLength(3);
        PriorityQueue<PatternTKS> patterns = algo.runAlgorithm(inputFile.getPath(), outputFile.getPath(), topK);

        List<DependencySequenceResult> result = new ArrayList<>(patterns.size());
        for (PatternTKS pattern : patterns) {
            DependencySequenceResult res = new DependencySequenceResult();
            List<List<Long>> libIds = new LinkedList<>();
            for (Itemset itemset : pattern.prefix.getItemsets()) {
                List<Long> itemList = new ArrayList<>(10);
                for (Integer item : itemset.getItems()) {
                    Long lib = id2Lib.get(item);
                    if(lib == null) throw new RuntimeException("Unknown item: " + item);
                    itemList.add(lib);
                }
                libIds.add(itemList);
            }
            res.pattern = new ArrayList<>(libIds);
            res.support = pattern.support;
            result.add(res);
        }
        return result;
    }

    public static class LibraryMigrationCandidate {
        public long fromId;
        public long toId;
        public int patternSupport = 0;
        public int methodChangeSupport = 0;
        public double patternSupportPercent = 0;
        public double methodChangeSupportPercent = 0;
        public double positionSupportPercent = 0;
        public double multipleSupport = 0;
        public List<Pair<Integer, Integer>> positionList = new LinkedList<>();

        public LibraryMigrationCandidate(long fromId, long toId) {
            this.fromId = fromId;
            this.toId = toId;
        }
    }

    public static class LibraryMigrationPattern {
        public long fromId;
        public List<Long> toIdList; // order by timestamp desc

        public LibraryMigrationPattern(long fromId, List<Long> toIdList) {
            this.fromId = fromId;
            this.toIdList = toIdList;
        }
    }

    public Map<Long, List<LibraryMigrationCandidate>> miningLibraryMigrationCandidate(
            Collection<List<Long>> depSeqCollection,
            Set<Long> fromIdLimit,
            Map<Long, Map<Long, Integer>> methodChangeSupportMap
    ) {
        Map<Long, Map<Long, LibraryMigrationCandidate>> candidateMap = new HashMap<>();
        for (List<Long> depSeq : depSeqCollection) {
            depSeq = simplifyLibIdList(depSeq);
            List<LibraryMigrationPattern> patternList = miningSingleDepSeq(depSeq, fromIdLimit);
            for (LibraryMigrationPattern pattern : patternList) {
                Map<Long, LibraryMigrationCandidate> toId2Candidate = candidateMap.computeIfAbsent(pattern.fromId, k -> new HashMap<>());
                int position = 1;
                for (Long toId : pattern.toIdList) {
                    LibraryMigrationCandidate candidate = toId2Candidate.computeIfAbsent(
                            toId, k -> new LibraryMigrationCandidate(pattern.fromId, toId));
                    candidate.patternSupport++;
                    candidate.positionList.add(new Pair<>(position++, pattern.toIdList.size()));
                }
            }
        }
        Map<Long, List<LibraryMigrationCandidate>> result = new HashMap<>();
        candidateMap.forEach((fromId, toIdCandidateMap) -> result.put(fromId, new ArrayList<>(toIdCandidateMap.values())));
        result.forEach((fromId, candidateList) -> {
            int totalPatternSupport = 0;
            int totalMCSupport = 0;
            for (LibraryMigrationCandidate candidate : candidateList) {
                if(methodChangeSupportMap.containsKey(fromId)) {
                    candidate.methodChangeSupport = methodChangeSupportMap.get(fromId)
                            .getOrDefault(candidate.toId, 0);
                }
                totalPatternSupport += candidate.patternSupport;
                totalMCSupport += candidate.methodChangeSupport;
            }
            for (LibraryMigrationCandidate candidate : candidateList) {
                double positionSupport = 0;
                for (Pair<Integer, Integer> position : candidate.positionList) {
                    positionSupport += Math.pow(1 / (double) position.getKey(), 0.4);
                }
                candidate.positionSupportPercent = positionSupport / candidate.positionList.size();
                if(totalPatternSupport != 0) {
                    candidate.patternSupportPercent = candidate.patternSupport / (double) totalPatternSupport;
                }
                if(totalMCSupport != 0) {
                    candidate.methodChangeSupportPercent = candidate.methodChangeSupport / (double) totalMCSupport;
                }
                candidate.multipleSupport =
                        Math.pow(candidate.patternSupportPercent, 1) *
                                Math.pow(candidate.methodChangeSupportPercent, 4) *
                                Math.pow(candidate.positionSupportPercent, 2);
            }
            candidateList.sort((a, b) -> Double.compare(b.multipleSupport, a.multipleSupport));
//            candidateList.sort((a, b) -> Double.compare(b.methodChangeSupportPercent, a.methodChangeSupportPercent));
//            candidateList.sort((a, b) -> Double.compare(b.patternSupportPercent, a.patternSupportPercent));
        });
        return result;
    }

    public List<LibraryMigrationPattern> miningSingleDepSeq(List<Long> depSeq, Set<Long> fromIdLimit) {
        Map<Long, LibraryMigrationPattern> patternMap = new HashMap<>();
        Set<Long> removeIds = new HashSet<>();
        int i = depSeq.size() - 1;
        while(i >= 0) {
            long currentId = depSeq.get(i--);
            if(currentId == 0) continue;
            if(currentId < 0) {
                if(fromIdLimit != null && !fromIdLimit.contains(-currentId)) {
                    continue;
                }
                removeIds.add(-currentId);
            } else {
                removeIds.remove(currentId);
                removeIds.forEach(removeId -> patternMap.computeIfAbsent(
                        removeId, k -> new LibraryMigrationPattern(k, new LinkedList<>()))
                        .toIdList.add(currentId));
            }
        }
        return new ArrayList<>(patternMap.values());
    }

    public List<Long> simplifyLibIdList(List<Long> libIds) {
        List<Long> result = new ArrayList<>(libIds.size());
        Set<Long> currentLibs = new HashSet<>();
        List<Long> currentList = new LinkedList<>();
        for (Long libId : libIds) {
            if(libId == 0) {
                if(currentList.isEmpty()) continue;
                result.addAll(currentList);
                result.add(0L);
                currentList.clear();
            } else {
                if(libId > 0) {
                    if(currentLibs.contains(libId)) continue;
                    currentLibs.add(libId);
                    currentList.add(libId);
                } else {
                    if(!currentLibs.contains(-libId)) continue;
                    currentLibs.remove(-libId);
                    currentList.add(libId);
                }
            }
        }
        return result;
    }
}
