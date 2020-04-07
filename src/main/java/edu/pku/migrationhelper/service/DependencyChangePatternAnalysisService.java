package edu.pku.migrationhelper.service;

import javafx.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DependencyChangePatternAnalysisService {

    public static class LibraryMigrationCandidate {
        public long fromId;
        public long toId;
        public int patternSupport = 0;
        public int methodChangeSupport = 0;
        public int occurCount = 0;
        public double patternSupportPercent = 0;
        public double methodChangeSupportPercent = 0;
        public double occurSupportPercent = 0;
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
        Map<Long, Map<Long, Integer>> occurCounter = new HashMap<>();
        Map<Long, Map<Long, LibraryMigrationCandidate>> candidateMap = new HashMap<>();
        for (List<Long> depSeq : depSeqCollection) {
            depSeq = simplifyLibIdList(depSeq);
            calcOccurCounter(depSeq, occurCounter);
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
        candidateMap.forEach((fromId, toIdCandidateMap) -> {
            List<LibraryMigrationCandidate> candidateList = new ArrayList<>(toIdCandidateMap.values());
//            candidateList = candidateList.stream()
//                    .filter(candidate -> candidate.patternSupport >= 10)
//                    .collect(Collectors.toList());
            result.put(fromId, candidateList);
        });
        result.forEach((fromId, candidateList) -> {
            int totalPatternSupport = 0;
            int totalMCSupport = 0;
            for (LibraryMigrationCandidate candidate : candidateList) {
                if(methodChangeSupportMap.containsKey(fromId)) {
                    candidate.methodChangeSupport = methodChangeSupportMap.get(fromId)
                            .getOrDefault(candidate.toId, 0);
                }
                long lib1 = fromId;
                long lib2 = candidate.toId;
                if(lib1 > lib2) {
                    lib1 = candidate.toId;
                    lib2 = fromId;
                }
                Map<Long, Integer> lib2Count = occurCounter.get(lib1);
                if(lib2Count != null) {
                    candidate.occurCount = lib2Count.getOrDefault(lib2, 0);
                }
                totalPatternSupport += candidate.patternSupport;
                totalMCSupport += candidate.methodChangeSupport;
            }
            for (LibraryMigrationCandidate candidate : candidateList) {
                double positionSupport = 0;
                for (Pair<Integer, Integer> position : candidate.positionList) {
                    positionSupport += Math.pow(1 / ((double) position.getKey() + 5), 0.4);
                }
                candidate.positionSupportPercent = positionSupport / candidate.positionList.size();
                if(totalPatternSupport != 0) {
                    candidate.patternSupportPercent = candidate.patternSupport / (double) totalPatternSupport;
                }
                if(totalMCSupport != 0) {
                    candidate.methodChangeSupportPercent = candidate.methodChangeSupport / (double) totalMCSupport;
                }
                if(candidate.occurCount != 0) {
                    candidate.occurSupportPercent = candidate.patternSupport / (double) candidate.occurCount;
                }
                candidate.multipleSupport =
                        Math.pow(candidate.patternSupportPercent, 1) *
                                Math.pow(candidate.occurSupportPercent, 0) *
                                Math.pow(candidate.methodChangeSupportPercent, 0) *
                                Math.pow(candidate.positionSupportPercent, 0);
            }
            candidateList.sort((a, b) -> Double.compare(b.multipleSupport, a.multipleSupport));
//            candidateList.sort((a, b) -> Double.compare(b.methodChangeSupportPercent, a.methodChangeSupportPercent));
//            candidateList.sort((a, b) -> Double.compare(b.patternSupportPercent, a.patternSupportPercent));
        });
        return result;
    }

    public void calcOccurCounter(List<Long> depSeq, Map<Long, Map<Long, Integer>> occurCounter) {
        Set<Long> occurLib = new HashSet<>();
        for (Long lib : depSeq) {
            if(lib > 0) {
                occurLib.add(lib);
            }
        }
        List<Long> occurList = new ArrayList<>(occurLib);
        occurList.sort(Long::compare);
        int len = occurList.size();
        for (int i = 0; i < len; i++) {
            for (int j = i+1; j < len; j++) {
                long lib1 = occurList.get(i);
                long lib2 = occurList.get(j);
                Map<Long, Integer> counter = occurCounter.computeIfAbsent(lib1, k -> new TreeMap<>());
                Integer old = counter.get(lib2);
                if(old == null) {
                    counter.put(lib2, 1);
                } else {
                    counter.replace(lib2, old + 1);
                }
            }
        }
    }

    public List<LibraryMigrationPattern> miningSingleDepSeq(List<Long> depSeq, Set<Long> fromIdLimit) {
        Map<Long, LibraryMigrationPattern> patternMap = new HashMap<>();
        Set<Long> removeIds = new HashSet<>();
        int i = depSeq.size() - 1;
        while(i > 0) {
            long currentId = depSeq.get(i--);
            List<Long> addIds = new LinkedList<>();
            while(i >= 0 && (currentId = depSeq.get(i--)) != 0L) {
                if(currentId > 0) {
                    addIds.add(currentId);
                } else {
                    if(fromIdLimit != null && !fromIdLimit.contains(-currentId)) {
                        continue;
                    }
                    removeIds.add(-currentId);
                }
            }
            ++i;
            for (Long addId : addIds) {
                removeIds.remove(addId);
            }
            for (Long addId : addIds) {
                removeIds.forEach(removeId -> patternMap.computeIfAbsent(
                        removeId, k -> new LibraryMigrationPattern(k, new LinkedList<>()))
                        .toIdList.add(addId));
            }
        }
        for (LibraryMigrationPattern pattern : patternMap.values()) {
            List<Long> toIdList = new ArrayList<>(pattern.toIdList);
            List<Long> tmp = new ArrayList<>(toIdList.size());
            Set<Long> idSet = new HashSet<>();
            int len = toIdList.size();
            for (int index = len - 1; index >= 0; index--) {
                long id = toIdList.get(index);
                if(!idSet.contains(id)) {
                    idSet.add(id);
                    tmp.add(id);
                }
            }
            len = tmp.size();
            pattern.toIdList.clear();
            for (int index = len - 1; index >= 0; index--) {
                pattern.toIdList.add(tmp.get(index));
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
