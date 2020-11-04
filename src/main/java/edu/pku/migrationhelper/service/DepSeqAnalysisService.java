package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.LibraryMigrationCandidate;
import edu.pku.migrationhelper.data.woc.WocAPICount;
import edu.pku.migrationhelper.data.woc.WocCommit;
import edu.pku.migrationhelper.data.woc.WocDepSeq;
import edu.pku.migrationhelper.data.woc.WocDepSeqItem;
import edu.pku.migrationhelper.repository.WocAPICountRepository;
import edu.pku.migrationhelper.repository.WocCommitRepository;
import edu.pku.migrationhelper.repository.WocDepSeqRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepSeqAnalysisService {

    public static final int DefaultMinPatternSupport = 0;

    public static final double DefaultMinMCSupportPercent = 0.1;

    public static class LibraryMigrationPattern {
        public long fromId;
        public List<Long> toIdList; // order by timestamp desc
        public List<String[]> startEndCommitList;
        public List<Integer> commitDistanceList;

        public LibraryMigrationPattern(
                long fromId,
                List<Long> toIdList,
                List<String[]> startEndCommitList,
                List<Integer> commitDistanceList
        ) {
            this.fromId = fromId;
            this.toIdList = toIdList;
            this.startEndCommitList = startEndCommitList;
            this.commitDistanceList = commitDistanceList;
        }
    }

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Autowired
    private WocDepSeqRepository depSeqRepository;

    @Autowired
    private WocCommitRepository wocCommitRepository;

    @Autowired
    private WocAPICountRepository wocAPICountRepository;

    @Autowired
    private GroupArtifactService groupArtifactService;

    private Map<Long, Map<Long, Integer>> methodChangeSupportMap;

    private List<List<Long>> repositoryDepSeq;

    private List<List<String>> depSeqCommitList;

    private List<String> depSeqRepoList;

    private List<String> depSeqFileList;

    @PostConstruct
    public void initializeMethodChangeSupportMap() {
        if (activeProfile.equals("web")) {
            LOG.info("Skipping the initialization of this module to save memory usage...");
            return;
        }
        List<WocAPICount> wocAPICounts = wocAPICountRepository.findAll();
        methodChangeSupportMap = new HashMap<>(10000000);
        for (WocAPICount count : wocAPICounts) {
            Long fromId = groupArtifactService.getIdByName(count.getFromLib());
            Long toId = groupArtifactService.getIdByName(count.getToLib());
            methodChangeSupportMap.computeIfAbsent(fromId, k -> new HashMap<>()).put(toId, (int) count.getCount());
        }
    }

    @PostConstruct
    public void initializeRepositoryDepSeq() {
        if (activeProfile.equals("web")) {
            LOG.info("Skipping the initialization of this module to save memory usage...");
            return;
        }
        LOG.info("Initializing repository dependency sequence...");
        repositoryDepSeq = new LinkedList<>();
        depSeqCommitList = new LinkedList<>();
        depSeqRepoList = new LinkedList<>();
        depSeqFileList = new LinkedList<>();
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
            depSeqFileList.add(seq.getFileName());
        }
    }

    public Map<Long, List<LibraryMigrationCandidate>> miningLibraryMigrationCandidate(Set<Long> fromIdLimit) {
        return miningLibraryMigrationCandidate(fromIdLimit, DefaultMinPatternSupport, DefaultMinMCSupportPercent);
    }

    /**
     * The core algorithm for mining migration rules from dependency change sequences
     * @param fromIdLimit The queries of source libraries
     * @param minPatternSupport (Currently not used)
     * @param mcSupportLowerBound The minimum value for API Support, range [0,1] default 0.1
     * @return Ranked candidate migration rules indexed by library MongoDB ID
     */
    public Map<Long, List<LibraryMigrationCandidate>> miningLibraryMigrationCandidate(
            Set<Long> fromIdLimit,
            int minPatternSupport,
            double mcSupportLowerBound
    ) {
        Map<Long, Map<Long, LibraryMigrationCandidate>> candidateMap = new HashMap<>();
        Map<Long, Map<Long, Integer>> occurCounter = new HashMap<>();

        // Mine all dependency change sequences
        Iterator<String> repoNameIt = depSeqRepoList.iterator();
        Iterator<String> fileNameIt = depSeqFileList.iterator();
        Iterator<List<String>> commitListIt = depSeqCommitList.iterator();
        Set<List<Long>> analyzedDepSeqs = new HashSet<>();
        for (List<Long> depSeq : repositoryDepSeq) {
            String repoName = repoNameIt.next();
            String fileName = fileNameIt.next();
            List<String> commitList0 = commitListIt.next();
            List<String> commitList = new ArrayList<>(commitList0.size());

            depSeq = simplifyDepSeq(depSeq, commitList0, commitList);

            if (analyzedDepSeqs.contains(depSeq)) {
                continue;
            } else {
                analyzedDepSeqs.add(depSeq);
            }

            calcOccurCounter(depSeq, occurCounter);

            List<LibraryMigrationPattern> patternList = miningSingleDepSeq(depSeq, fromIdLimit, commitList);

            for (LibraryMigrationPattern pattern : patternList) {
                Map<Long, LibraryMigrationCandidate> toId2Candidate = candidateMap.computeIfAbsent(pattern.fromId, k -> new HashMap<>());
                int position = 1;
                Iterator<String[]> startEndCommitIt = pattern.startEndCommitList.iterator();
                Iterator<Integer> commitDistanceIt = pattern.commitDistanceList.iterator();
                for (Long toId : pattern.toIdList) {
                    String[] startEndCommit = startEndCommitIt.next();
                    Integer commitDistance = commitDistanceIt.next();
                    LibraryMigrationCandidate candidate = toId2Candidate.computeIfAbsent(
                            toId, k -> new LibraryMigrationCandidate(pattern.fromId, toId));
                    candidate.ruleCount++;
                    if (startEndCommit[0].equals(startEndCommit[1])) candidate.ruleCountSameCommit++;
                    candidate.repoCommitList.add(new String[]{repoName, startEndCommit[0], startEndCommit[1], fileName});
                    candidate.commitDistanceList.add(commitDistance);
                }
            }
        }

        LOG.info("{} raw dep seqs in which {} dep seqs are analyzed", repositoryDepSeq.size(), analyzedDepSeqs.size());

        Map<Long, List<LibraryMigrationCandidate>> result = new HashMap<>();

        // Filter out some rules by minPatternSupport
        candidateMap.forEach((fromId, toIdCandidateMap) -> {
            List<LibraryMigrationCandidate> candidateList = new ArrayList<>(toIdCandidateMap.values());
            candidateList = candidateList.stream()
                    .filter(candidate -> candidate.ruleCount >= minPatternSupport)
                    .collect(Collectors.toList());
            result.put(fromId, candidateList);
        });

        // Compute all the necessary metrics
        result.forEach((fromId, candidateList) -> {

            int totalPatternSupport = 0;
            int totalMCSupport = 0;
            int maxPatternSupport = 0;
            int maxPatternSupportSameCommit = 0;
            int maxMCSupport = 0;

            for (LibraryMigrationCandidate candidate : candidateList) {
                if(methodChangeSupportMap.containsKey(fromId)) {
                    candidate.methodChangeCount = methodChangeSupportMap.get(fromId)
                            .getOrDefault(candidate.toId, 0);
                }
                maxPatternSupport = Math.max(maxPatternSupport, candidate.ruleCount);
                maxPatternSupportSameCommit = Math.max(maxPatternSupport, candidate.ruleCountSameCommit);
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

            // Compute all the necessary metrics
            for (LibraryMigrationCandidate candidate : candidateList) {
                candidate.commitDistanceSupport = 0;
                for (Integer dis : candidate.commitDistanceList) {
                    candidate.commitDistanceSupport += Math.pow(1.0 / (double)(dis + 1), 2);
                }
                candidate.commitDistanceSupport = candidate.commitDistanceSupport / candidate.commitDistanceList.size();

                if(totalPatternSupport != 0) {
                    candidate.ruleSupportByTotal = candidate.ruleCount / (double) totalPatternSupport;
                }
                if(maxPatternSupport != 0) {
                    candidate.maxRuleCount = maxPatternSupport;
                    candidate.ruleSupportByMax = candidate.ruleCount / (double) maxPatternSupport;
                    candidate.ruleSupportByMaxSameCommit = candidate.ruleCountSameCommit / (double) maxPatternSupportSameCommit;
                }

                if(totalMCSupport != 0) {
                    candidate.methodChangeSupportByTotal = candidate.methodChangeCount / (double) totalMCSupport;
                }
                if(maxMCSupport != 0) {
                    candidate.maxMethodChangeCount = maxMCSupport;
                    candidate.methodChangeSupportByMax = candidate.methodChangeCount / (double) maxMCSupport;
                }

                if(candidate.libraryConcurrenceCount != 0) {
                    candidate.libraryConcurrenceSupport = candidate.ruleCount / (double) candidate.libraryConcurrenceCount;
                }

                candidate.confidence2 = Math.pow(candidate.ruleSupportByMax, 1)
                                * Math.pow(candidate.libraryConcurrenceSupport, 0.5)
                                * Math.pow(candidate.commitDistanceSupport, 2)
                                * Math.max(mcSupportLowerBound, candidate.methodChangeSupportByMax);
            }

            // Identify possible migrations in this step
            Set<String> commitSHAs = new HashSet<>();
            for (LibraryMigrationCandidate candidate : candidateList) {
                for (String[] repoCommit : candidate.repoCommitList) {
                    commitSHAs.add(repoCommit[1]);
                    commitSHAs.add(repoCommit[2]);
                }
            }
            Map<String, WocCommit> commits = new HashMap<>();
            for (WocCommit commit: wocCommitRepository.findAllById(commitSHAs)) {
                commits.put(commit.getId(), commit);
            }
            for (LibraryMigrationCandidate candidate : candidateList) {
                String fromLib = groupArtifactService.getGroupArtifactById(candidate.fromId).getGroupArtifactId();
                String toLib = groupArtifactService.getGroupArtifactById(candidate.toId).getGroupArtifactId();
                for (String[] repoCommit : candidate.repoCommitList) {
                    String startCommitMessage = commits.containsKey(repoCommit[1]) ? commits.get(repoCommit[1]).getMessage() : "";
                    String endCommitMessage = commits.containsKey(repoCommit[2]) ? commits.get(repoCommit[2]).getMessage() : "";

                    if (isPossibleMigration(fromLib, toLib, startCommitMessage, endCommitMessage)) {
                        candidate.possibleCommitList.add(repoCommit);
                    }
                }

                candidate.commitMessageSupport = Math.log1p(candidate.possibleCommitList.size()) / Math.log(2);

                candidate.confidence =
                        candidate.ruleSupportByMaxSameCommit
                                * Math.max(mcSupportLowerBound, candidate.methodChangeSupportByMax)
                                * candidate.commitDistanceSupport
                                * candidate.commitMessageSupport;
            }

            candidateList.sort((a, b) -> {
                int r = Double.compare(b.confidence, a.confidence);
                if (r != 0) return r;
                return Double.compare(b.confidence2, a.confidence2);
            });
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
        Map<String, Integer> commit2Id = new HashMap<>();
        for (int i = 0; i < commitList.size(); ++i) {
            commit2Id.put(commitList.get(i), i);
        }

        int i = depSeq.size() - 1;
        int commitIt = commitList.size() - 1;
        while (i > 0) {
            String currCommit = commitList.get(commitIt--);
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
                            removeId, k -> new LibraryMigrationPattern(k, new LinkedList<>(), new LinkedList<>(), new LinkedList<>()));
                    pattern.toIdList.add(addId);
                    pattern.startEndCommitList.add(new String[]{currCommit, removeId2Commit.get(removeId)});
                    pattern.commitDistanceList.add(commit2Id.get(removeId2Commit.get(removeId)) - commit2Id.get(currCommit));
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
     *   3. Remove (+lib) items that have been added before
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
                if (libId > 0 && !currentLibs.contains(libId)) {
                    currentList.add(libId);
                    currentLibs.add(libId);
                } else if (libId < 0 && currentLibs.contains(-libId)) {
                    currentLibs.remove(-libId);
                    currentList.add(libId);
                }
            }
        }
        return result;
    }

    /**
     * Try to heuristically split a groupId:artifactId into meaningful string segments
     * @param groupArtifact groupId:artifactId
     * @return potentially meaningful strings from groupArtifact
     */
    public Set<String> splitGroupArtifact(String groupArtifact) {
        Set<String> useless = new HashSet<>(Arrays.asList(
                "com", "org", "net", "apache", "core", "api", "all", "impl"
        ));
        return Arrays.stream(groupArtifact.toLowerCase().split("[:\\-.]"))
                .filter(str -> str.length() >= 2 && !useless.contains(str))
                .collect(Collectors.toSet());
    }

    public boolean containAnyPart(String commitMessage, Set<String> parts) {
        for (String part : parts)
            if (commitMessage.contains(part))
                return true;
        return false;
    }

    /**
     * Determine whether a commit pair is a possible migration by analyzing commit messages
     * @param fromGroupArtifact  removed groupId:artifactId
     * @param toGroupArtifact    added groupId:artifactId
     * @param startCommitMessage the commit message where toGroupArtifact is added
     * @param endCommitMessage   the commit message where fromGroupArtifact is removed
     * @return whether the commit pair is a possible migration
     */
    public boolean isPossibleMigration(
            String fromGroupArtifact,
            String toGroupArtifact,
            String startCommitMessage,
            String endCommitMessage
    ) {
        Set<String> fromLibParts = splitGroupArtifact(fromGroupArtifact);
        Set<String> toLibParts = splitGroupArtifact(toGroupArtifact);
        Set<String> addKeywords = new HashSet<>(Arrays.asList(
                "use", "adopt", "introduc", "upgrad", "updat", "采用", "升级"
        ));
        Set<String> removeKeywords = new HashSet<>(Arrays.asList(
                "remov", "delet", "abandon", "删除", "移除"
        ));
        Set<String> migrationKeywords = new HashSet<>(Arrays.asList(
                "migrat", "switch", "replac", "instead", "move", "swap",
                "unify", "convert", "chang", "迁移", "替换", "修改"
        ));
        // Set<String> cleanupKeywords = new HashSet<>(Arrays.asList(
        //         "pom", "clean", "remove"
        // ));

        startCommitMessage = startCommitMessage.toLowerCase();
        endCommitMessage = endCommitMessage.toLowerCase();
        if (startCommitMessage.equals(endCommitMessage)) {
            if (containAnyPart(startCommitMessage, toLibParts)) {
                if (containAnyPart(startCommitMessage, migrationKeywords)
                        || containAnyPart(startCommitMessage, fromLibParts)
                        || containAnyPart(startCommitMessage, addKeywords))
                    return true;
            }
            return containAnyPart(startCommitMessage, fromLibParts)
                    && containAnyPart(startCommitMessage, migrationKeywords);
        } else { // Different commit
            return containAnyPart(startCommitMessage, toLibParts)
                    && (containAnyPart(startCommitMessage, addKeywords))
                    && containAnyPart(endCommitMessage, fromLibParts)
                    && containAnyPart(endCommitMessage, removeKeywords);
        }
    }
}
