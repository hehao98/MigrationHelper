package edu.pku.migrationhelper.service;

import javafx.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DependencyChangePatternAnalysisService {

    public static final int DefaultMinPatternSupport = 8;

    public static final double DefaultMinMCSupportPercent = 0.6;

    public static class LibraryMigrationCandidate {
        public long fromId;
        public long toId;
        public int ruleCount = 0;               // Number of times a rule occur in dependency sequence
        public int methodChangeCount = 0;       // Number of times API modifications occur in data
        public int libraryConcurrenceCount = 0; // Number of times l1 and l2 are used in same commit
        public int maxRuleCount = 0;            // For all candidates, max value of RuleCount
        public int maxMethodChangeCount = 0;    // For all candidates, max value of methodChangeCount
        public double ruleSupportByTotal = 0;
        public double ruleSupportByMax = 0;
        public double methodChangeSupportByTotal = 0;
        public double methodChangeSupportByMax = 0;
        public double libraryConcurrenceSupport = 0;
        public double commitDistance = 0;
        public double confidence = 0;
        public double confidence2 = 0;
        public List<Pair<Integer, Integer>> positionList = new LinkedList<>();
        public List<String[]> repoCommitList = new LinkedList<>();

        public LibraryMigrationCandidate(long fromId, long toId) {
            this.fromId = fromId;
            this.toId = toId;
        }
    }

    public static class LibraryMigrationPattern {
        public long fromId;
        public List<Long> toIdList; // order by timestamp desc
        public List<String[]> startEndCommitList;

        public LibraryMigrationPattern(long fromId, List<Long> toIdList, List<String[]> startEndCommitList) {
            this.fromId = fromId;
            this.toIdList = toIdList;
            this.startEndCommitList = startEndCommitList;
        }
    }

    public Map<Long, List<LibraryMigrationCandidate>> miningLibraryMigrationCandidate(
            List<List<Long>> depSeqCollection,
            Set<Long> fromIdLimit,
            Map<Long, Map<Long, Integer>> methodChangeSupportMap
    ) {
        return miningLibraryMigrationCandidate(depSeqCollection, fromIdLimit, methodChangeSupportMap, DefaultMinPatternSupport, DefaultMinMCSupportPercent,null, null);
    }

    /**
     * 依赖变更序列挖掘算法，从依赖变更序列中，挖掘库迁移规则
     * @param depSeqCollection 依赖变更序列的列表，每一个列表项是一个依赖变更序列，序列中正数表示添加该库，负数表示删除该库，0表示分隔两个项集（即两个0之间的添加删除都是在一个Commit内完成的）
     * @param fromIdLimit 挖掘出来的库迁移规则的原库限定范围，null表示不限定
     * @param methodChangeSupportMap APISupport的映射关系，fromId -> toId -> counter
     * @param minPatternSupport 最小支持度值
     * @param mcSupportLowerBound APISupport指标值中的最小值，范围[0,1]
     * @param repoNameCollection 项目名称列表，长度和顺序与depSeqCollection参数一致，可以为null，填写后可以溯源库迁移规则
     * @param depSeqCommitsCollection Commit来源列表，长度和顺序与depSeqCollection参数一致，且内部列表的长度和顺序与depSeqCollection的内部列表一致，可以为null，填写后可以溯源库迁移规则
     * @return 挖掘出来的库迁移规则，Key是原库Id，Value是该原库拥有的所有库迁移规则列表，以推荐顺序排序
     */
    public Map<Long, List<LibraryMigrationCandidate>> miningLibraryMigrationCandidate(
            List<List<Long>> depSeqCollection,
            Set<Long> fromIdLimit,
            Map<Long, Map<Long, Integer>> methodChangeSupportMap,
            int minPatternSupport,
            double mcSupportLowerBound,
            List<String> repoNameCollection,
            List<List<String>> depSeqCommitsCollection
    ) {
        Map<Long, Map<Long, Integer>> occurCounter = new HashMap<>();
        Map<Long, Map<Long, LibraryMigrationCandidate>> candidateMap = new HashMap<>();
        Iterator<String> repoNameIt = repoNameCollection == null ? null : repoNameCollection.iterator();
        Iterator<List<String>> commitListIt = depSeqCommitsCollection == null ? null : depSeqCommitsCollection.iterator();
        for (List<Long> depSeq : depSeqCollection) {
            String repoName = repoNameIt == null ? null : repoNameIt.next();
//            System.out.println(repoName);
            List<String> commitList0 = commitListIt == null ? null : commitListIt.next();
            List<String> commitList = commitList0 == null ? null : new ArrayList<>(commitList0.size());
            depSeq = simplifyLibIdList(depSeq, commitList0, commitList);
            calcOccurCounter(depSeq, occurCounter);
            List<LibraryMigrationPattern> patternList = miningSingleDepSeq(depSeq, fromIdLimit, commitList);
            for (LibraryMigrationPattern pattern : patternList) {
                Map<Long, LibraryMigrationCandidate> toId2Candidate = candidateMap.computeIfAbsent(pattern.fromId, k -> new HashMap<>());
                int position = 1;
                Iterator<String[]> startEndCommitIt = pattern.startEndCommitList.iterator();
                for (Long toId : pattern.toIdList) {
                    String[] startEndCommit = startEndCommitIt.next();
                    LibraryMigrationCandidate candidate = toId2Candidate.computeIfAbsent(
                            toId, k -> new LibraryMigrationCandidate(pattern.fromId, toId));
                    candidate.ruleCount++;
                    candidate.positionList.add(new Pair<>(position++, pattern.toIdList.size()));
                    candidate.repoCommitList.add(new String[]{repoName, startEndCommit[0], startEndCommit[1]});
                }
            }
//            System.out.println(repoName);
        }
        Map<Long, List<LibraryMigrationCandidate>> result = new HashMap<>();
        candidateMap.forEach((fromId, toIdCandidateMap) -> {
            List<LibraryMigrationCandidate> candidateList = new ArrayList<>(toIdCandidateMap.values());
            candidateList = candidateList.stream()
                    .filter(candidate -> candidate.ruleCount >= minPatternSupport)
                    .collect(Collectors.toList());
            result.put(fromId, candidateList);
        });
        result.forEach((fromId, candidateList) -> {
            int totalPatternSupport = 0;
            int totalMCSupport = 0;
            int maxPatternSupport = 0;
            int maxMCSupport = 0;
            for (LibraryMigrationCandidate candidate : candidateList) {
                if(methodChangeSupportMap.containsKey(fromId)) {
                    candidate.methodChangeCount = methodChangeSupportMap.get(fromId)
                            .getOrDefault(candidate.toId, 0);
                }
                maxPatternSupport = Math.max(maxPatternSupport, candidate.ruleCount);
                maxMCSupport = Math.max(maxMCSupport, candidate.methodChangeCount);
                long lib1 = fromId;
                long lib2 = candidate.toId;
                if(lib1 > lib2) {
                    lib1 = candidate.toId;
                    lib2 = fromId;
                }
                Map<Long, Integer> lib2Count = occurCounter.get(lib1);
                if(lib2Count != null) {
                    candidate.libraryConcurrenceCount = lib2Count.getOrDefault(lib2, 0);
                }
                totalPatternSupport += candidate.ruleCount;
                totalMCSupport += candidate.methodChangeCount;
            }
            for (LibraryMigrationCandidate candidate : candidateList) {
                double positionSupport = 0;
                double positionA = 1;
                int positionB = 5;
                for (Pair<Integer, Integer> position : candidate.positionList) {
                    positionSupport += Math.pow((positionB + 1) / (double)(position.getKey() + positionB), positionA);
                }
                candidate.commitDistance = positionSupport / candidate.positionList.size();
                if(totalPatternSupport != 0) {
                    candidate.ruleSupportByTotal = candidate.ruleCount / (double) totalPatternSupport;
                }
                if(maxPatternSupport != 0) {
                    candidate.maxRuleCount = maxPatternSupport;
                    candidate.ruleSupportByMax = candidate.ruleCount / (double) maxPatternSupport;
                }
                if(totalMCSupport != 0) {
                    candidate.methodChangeSupportByTotal = candidate.methodChangeCount / (double) totalMCSupport;
                }
                if(maxMCSupport != 0) {
                    candidate.maxMethodChangeCount = maxMCSupport;
                    candidate.methodChangeSupportByMax = candidate.methodChangeCount / (double) maxMCSupport;
                }
                if(candidate.methodChangeSupportByMax < mcSupportLowerBound) {
                    candidate.methodChangeSupportByMax = mcSupportLowerBound;
                }
                if(candidate.libraryConcurrenceCount != 0) {
                    candidate.libraryConcurrenceSupport = candidate.ruleCount / (double) candidate.libraryConcurrenceCount;
                }
                candidate.confidence2 =
                        Math.pow(candidate.ruleSupportByMax, 1) *
                                Math.pow(candidate.libraryConcurrenceSupport, 0.5) *
                                Math.pow(candidate.commitDistance, 2);
                candidate.confidence = candidate.confidence2 *
//                        1;
                        Math.pow(candidate.methodChangeSupportByMax, 0.5);

            }
            candidateList.sort((a, b) -> {
                int r = Double.compare(b.confidence, a.confidence);
                if(r != 0) return r;
                return Double.compare(b.confidence2, a.confidence2);
            });
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

    public List<LibraryMigrationPattern> miningSingleDepSeq(List<Long> depSeq, Set<Long> fromIdLimit, List<String> commitList) {
        Map<Long, LibraryMigrationPattern> patternMap = new HashMap<>();
        Set<Long> removeIds = new HashSet<>();
        Map<Long, String> removeId2Commit = new HashMap<>();
        int i = depSeq.size() - 1;
        int commitIt = 0;
        if(commitList != null) commitIt = commitList.size() - 1;
        while(i > 0) {
            String currCommit0 = null;
            if(commitList != null) {
                currCommit0 = commitList.get(commitIt--);
            }
            String currCommit = currCommit0;
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
                    removeId2Commit.put(-currentId, currCommit);
                }
            }
            ++i;
            for (Long addId : addIds) {
                removeIds.remove(addId);
            }
            for (Long addId : addIds) {
                removeIds.forEach(removeId -> {
                    LibraryMigrationPattern pattern = patternMap.computeIfAbsent(
                            removeId, k -> new LibraryMigrationPattern(k, new LinkedList<>(), new LinkedList<>()));
                    pattern.toIdList.add(addId);
                    pattern.startEndCommitList.add(new String[]{currCommit, removeId2Commit.get(removeId)});
                });
            }
        }
        for (LibraryMigrationPattern pattern : patternMap.values()) {
            List<Long> toIdList = new ArrayList<>(pattern.toIdList);
            List<Long> tmp = new ArrayList<>(toIdList.size());
            List<String[]> startEndCommitList = new ArrayList<>(pattern.startEndCommitList);
            List<String[]> tmp2 = new ArrayList<>(startEndCommitList.size());
            Set<Long> idSet = new HashSet<>();
            int len = toIdList.size();
            for (int index = len - 1; index >= 0; index--) {
                long id = toIdList.get(index);
                if(!idSet.contains(id)) {
                    idSet.add(id);
                    tmp.add(id);
                    tmp2.add(startEndCommitList.get(index));
                }
            }
            len = tmp.size();
            pattern.toIdList.clear();
            pattern.startEndCommitList.clear();
            for (int index = len - 1; index >= 0; index--) {
                pattern.toIdList.add(tmp.get(index));
                pattern.startEndCommitList.add(tmp2.get(index));
            }
        }
        return new ArrayList<>(patternMap.values());
    }

    public List<Long> simplifyLibIdList(List<Long> libIds, List<String> commitList, List<String> commitResult) {
        List<Long> result = new ArrayList<>(libIds.size());
        Set<Long> currentLibs = new HashSet<>();
        List<Long> currentList = new LinkedList<>();
        Iterator<String> commitIt = null;
        if(commitList != null) {
            commitIt = commitList.iterator();
        }
        for (Long libId : libIds) {
            if(libId == 0) {
                String commitId = commitIt == null ? null : commitIt.next();
                if(currentList.isEmpty()) continue;
                result.addAll(currentList);
                result.add(0L);
                currentList.clear();
                if(commitResult != null) {
                    commitResult.add(commitId);
                }
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
