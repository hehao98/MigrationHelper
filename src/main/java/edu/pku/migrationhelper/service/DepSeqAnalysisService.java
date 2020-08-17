package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.lib.LibraryGroupArtifact;
import edu.pku.migrationhelper.data.woc.WocDepSeq;
import edu.pku.migrationhelper.data.woc.WocDepSeqItem;
import edu.pku.migrationhelper.mapper.LibraryGroupArtifactMapper;
import edu.pku.migrationhelper.repository.LibraryGroupArtifactRepository;
import edu.pku.migrationhelper.repository.WocDepSeqRepository;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepSeqAnalysisService {

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

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private WocDepSeqRepository depSeqRepository;

    @Autowired
    private GroupArtifactService groupArtifactService;

    @Value("${migration-helper.dependency-change-pattern-analysis.method-change-support-file}")
    private String methodChangeSupportFile;

    @Value("${migration-helper.dependency-change-pattern-analysis.dependency-seq-file}")
    private String dependencySeqFile;

    private Map<Long, Map<Long, Integer>> methodChangeSupportMap;

    private List<List<Long>> repositoryDepSeq;

    private List<List<String>> depSeqCommitList;

    private List<String> depSeqRepoList;

    // TODO avoid this after the mysql library is deprecated, this is extremely error prone!!!!
    @Autowired
    private LibraryGroupArtifactRepository libraryGroupArtifactRepository;
    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;
    private final Map<Long, Long> idCache = new HashMap<>();
    private long libMysqlIdToMongoDbId(long mysqlId) {
        if (mysqlId == 0) return 0;
        if (idCache.containsKey(mysqlId)) return idCache.get(mysqlId);
        LibraryGroupArtifact lib = libraryGroupArtifactMapper.findById(mysqlId);
        long mongoId = libraryGroupArtifactRepository.findByGroupIdAndArtifactId(lib.getGroupId(), lib.getArtifactId()).get().getId();
        idCache.put(mysqlId, mongoId);
        return mongoId;
    }

    @PostConstruct
    public void initializeMethodChangeSupportMap() throws IOException {
        if (!new File(methodChangeSupportFile).isFile()) {
            LOG.error("Cannot load method change support file, this service will not work properly");
            return;
        }
        LOG.info("Initializing method change support map...");
        BufferedReader reader = new BufferedReader(new FileReader(methodChangeSupportFile));
        String line = reader.readLine();
        methodChangeSupportMap = new HashMap<>(100000);
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",");
            Long fromId = libMysqlIdToMongoDbId(Long.parseLong(attrs[0]));
            Long toId = libMysqlIdToMongoDbId(Long.parseLong(attrs[1]));
            Integer counter = Integer.parseInt(attrs[2]);
            methodChangeSupportMap.computeIfAbsent(fromId, k -> new HashMap<>()).put(toId, counter);
        }
        reader.close();
    }

    //@PostConstruct
    public void initializeRepositoryDepSeqOld() throws IOException {
        if (!new File(dependencySeqFile).isFile()) {
            LOG.error("Cannot load dependency sequence file, this service will not work properly");
            return;
        }
        LOG.info("Initializing repository dependency sequence...");
        BufferedReader reader = new BufferedReader(new FileReader(dependencySeqFile));
        String line = reader.readLine();
        repositoryDepSeq = new LinkedList<>();
        while ((line = reader.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            if(attrs.length < 3) {
                System.out.println(line);
            }
            String libIdString = attrs[2]; // pomOnly
            if ("".equals(libIdString)) continue;
            String[] libIds = libIdString.split(";");
            List<Long> libIdList = new ArrayList<>(libIds.length);
            for (String libId : libIds) {
                long id = Long.parseLong(libId);
                if (id < 0) libIdList.add(-libMysqlIdToMongoDbId(-id));
                else libIdList.add(libMysqlIdToMongoDbId(id));
            }
            repositoryDepSeq.add(libIdList);
        }
        reader.close();
    }

    //@PostConstruct
    public void initializeDepSeqCommitList() throws IOException {
        if (!new File(dependencySeqFile).isFile()) {
            LOG.error("Cannot load dependency sequence file, this service will not work properly");
            return;
        }
        LOG.info("Initializing repository dependency sequence commit list...");
        BufferedReader reader = new BufferedReader(new FileReader(dependencySeqFile));
        String line = reader.readLine();
        depSeqCommitList = new LinkedList<>();
        while ((line = reader.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            if (attrs.length < 3) {
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
            depSeqCommitList.add(commitList);
        }
        reader.close();
    }

    //@PostConstruct
    public void initializeDepSeqRepoList() throws IOException {
        if (!new File(dependencySeqFile).isFile()) {
            LOG.error("Cannot load dependency sequence file, this service will not work properly");
            return;
        }
        LOG.info("Initializing repository dependency sequence repository list...");
        BufferedReader reader = new BufferedReader(new FileReader(dependencySeqFile));
        String line = reader.readLine();
        depSeqRepoList = new LinkedList<>();
        while ((line = reader.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            if(attrs.length < 3) {
                System.out.println(line);
            }
            String libIdString = attrs[2]; // pomOnly
            if ("".equals(libIdString)) continue;
            depSeqRepoList.add(attrs[1]);
        }
        reader.close();
    }


    @PostConstruct
    public void initializeRepositoryDepSeq() {
        LOG.info("Initializing repository dependency sequence...");
        repositoryDepSeq = new LinkedList<>();
        depSeqCommitList = new LinkedList<>();
        depSeqRepoList = new LinkedList<>();
        for (WocDepSeq seq : depSeqRepository.findAll()) {
            List<Long> libIdList = new ArrayList<>();
            for (WocDepSeqItem item : seq.getSeq()) {
                for (String change : item.getChanges()) {
                    String libName = change.substring(1);
                    if (!groupArtifactService.exist(libName)) {
                        continue;
                    }
                    if (change.startsWith("+")) {
                        libIdList.add(groupArtifactService.getIdByName(libName));
                    } else {
                        libIdList.add(-groupArtifactService.getIdByName(libName));
                    }
                }
                libIdList.add(0L);
            }
            // LOG.info("{}", libIdList);
            repositoryDepSeq.add(libIdList);

            List<String> commitList = new ArrayList<>();
            for (WocDepSeqItem item : seq.getSeq()) {
                commitList.add(item.getCommit());
            }
            depSeqCommitList.add(commitList);
            depSeqRepoList.add(seq.getRepoName());
        }
    }

    public Map<Long, List<LibraryMigrationCandidate>> miningLibraryMigrationCandidate(Set<Long> fromIdLimit, boolean outputRepoCommit) {
        return miningLibraryMigrationCandidate(fromIdLimit, DefaultMinPatternSupport, DefaultMinMCSupportPercent,true, true);
    }

    /**
     * 依赖变更序列挖掘算法，从依赖变更序列中，挖掘库迁移规则
     * @param fromIdLimit 挖掘出来的库迁移规则的原库限定范围，null表示不限定
     * @param minPatternSupport 最小支持度值
     * @param mcSupportLowerBound APISupport指标值中的最小值，范围[0,1]
     * @param returnRepoName if true, fill in positionList and repoCommitList in returned list
     * @param returnCommits if true, fill in positionList and repoCommitList in returned list
     * @return 挖掘出来的库迁移规则，Key是原库Id，Value是该原库拥有的所有库迁移规则列表，以推荐顺序排序
     */
    public Map<Long, List<LibraryMigrationCandidate>> miningLibraryMigrationCandidate(
            Set<Long> fromIdLimit,
            int minPatternSupport,
            double mcSupportLowerBound,
            boolean returnRepoName, // List<String> repoNameCollection,
            boolean returnCommits // List<List<String>> depSeqCommitsCollection
    ) {
        Map<Long, Map<Long, Integer>> occurCounter = new HashMap<>();
        Map<Long, Map<Long, LibraryMigrationCandidate>> candidateMap = new HashMap<>();
        Iterator<String> repoNameIt = returnRepoName ? depSeqRepoList.iterator() : null;
        Iterator<List<String>> commitListIt = returnCommits ? depSeqCommitList.iterator() : null;
        for (List<Long> depSeq : repositoryDepSeq) {
            String repoName = repoNameIt == null ? null : repoNameIt.next();
//            System.out.println(repoName);
            List<String> commitList0 = commitListIt == null ? null : commitListIt.next();
            List<String> commitList = commitList0 == null ? null : new ArrayList<>(commitList0.size());
            depSeq = simplifyDepSeq(depSeq, commitList0, commitList);
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

    /**
     * Update occurCounter based on one depSeq
     * @param depSeq dependency change sequence
     * @param occurCounter number of times (lib1, lib2) co-occurs
     */
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

    public List<List<Long>> getRepositoryDepSeq() { return repositoryDepSeq; }

    /**
     * Mine single depSeq to extract a set of patterns to be analyzed in subsequent steps
     * @param depSeq the dependency sequence to mine
     * @param fromIdLimit the set of libraries to consider only
     * @param commitList the list of commit along with depSeq
     * @return the set of patterns
     */
    public List<LibraryMigrationPattern> miningSingleDepSeq(List<Long> depSeq, Set<Long> fromIdLimit, List<String> commitList) {
        Map<Long, LibraryMigrationPattern> patternMap = new HashMap<>();
        Set<Long> removeIds = new HashSet<>();
        Map<Long, String> removeId2Commit = new HashMap<>();
        int i = depSeq.size() - 1;
        int commitIt = 0;
        if (commitList != null) commitIt = commitList.size() - 1;
        while (i > 0) {
            String currCommit0 = null;
            if(commitList != null) {
                currCommit0 = commitList.get(commitIt--);
            }
            String currCommit = currCommit0;
            long currentId = depSeq.get(i--);
            List<Long> addIds = new LinkedList<>();
            while (i >= 0 && (currentId = depSeq.get(i--)) != 0L) {
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

    /**
     * Simplify a depSeq by the following
     *   1. Remove commits with no depSeq changes
     *   2. Remove (-lib) items that have not been added before
     * @param depSeq dependency sequence to simplify
     * @param commitList the list of commit related to the deqSeq should also be simplified, if not null
     * @param commitResult the simplified commit list will be put into here
     * @return the simplified dependency sequence
     */
    public List<Long> simplifyDepSeq(List<Long> depSeq, List<String> commitList, List<String> commitResult) {
        List<Long> result = new ArrayList<>(depSeq.size());
        Set<Long> currentLibs = new HashSet<>();
        List<Long> currentList = new LinkedList<>();
        Iterator<String> commitIt = null;
        if (commitList != null) {
            commitIt = commitList.iterator();
        }
        for (Long libId : depSeq) {
            if (libId == 0) {
                String commitId = commitIt == null ? null : commitIt.next();
                if (currentList.isEmpty()) continue;
                result.addAll(currentList);
                result.add(0L);
                currentList.clear();
                if (commitResult != null) {
                    commitResult.add(commitId);
                }
            } else {
                if (libId > 0) {
                    if(currentLibs.contains(libId)) continue;
                    currentLibs.add(libId);
                } else {
                    if(!currentLibs.contains(-libId)) continue;
                    currentLibs.remove(-libId);
                }
                currentList.add(libId);
            }
        }
        return result;
    }
}
