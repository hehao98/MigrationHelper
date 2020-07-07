package edu.pku.migrationhelper.service;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import edu.pku.migrationhelper.data.*;
import edu.pku.migrationhelper.data.api.MethodSignatureOld;
import edu.pku.migrationhelper.mapper.*;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created by xuyul on 2020/2/4.
 */
public abstract class RepositoryAnalysisService {

    protected Logger LOG = LoggerFactory.getLogger(getClass());

    @Value("${migration-helper.repository-analysis.pom-only}")
    private boolean pomOnly = false;

    @Autowired
    protected JavaCodeAnalysisService javaCodeAnalysisService;

    @Autowired
    protected PomAnalysisService pomAnalysisService;

    @Autowired
    protected LibraryIdentityService libraryIdentityService;

    @Autowired
    protected GitObjectStorageService gitObjectStorageService;

    @Autowired
    protected LibraryVersionMapper libraryVersionMapper;

    @Autowired
    protected LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    protected MethodChangeMapper methodChangeMapper;

    @Autowired
    protected RepositoryAnalyzeStatusMapper repositoryAnalyzeStatusMapper;

    @Autowired
    protected RepositoryDepSeqMapper repositoryDepSeqMapper;

    @Autowired
    protected LibraryOverlapMapper libraryOverlapMapper;

    public static abstract class AbstractRepository {
        public String repositoryName;
        public Map<String, BlobInfo> blobCache = new HashMap<>(200000);
        public Map<String, CommitInfo> commitCache = new HashMap<>(20000);
    }

    public static class BlobInCommit {
        public String blobId;
        public String fileName;
    }

    protected Map<Long, Set<Long>> libraryOverlapMap = null;

    public abstract AbstractRepository openRepository(String repositoryName);

    public abstract void closeRepository(AbstractRepository repository);

    public abstract void forEachCommit(AbstractRepository repository, Consumer<String> commitIdConsumer);

    public abstract void forEachCommit(AbstractRepository repository, Consumer<String> commitIdConsumer, int offset, int limit);

    public abstract List<String> getCommitParents(AbstractRepository repository, String commitId);

    public abstract List<BlobInCommit> getBlobsInCommit(AbstractRepository repository, String commitId);

    public abstract String getBlobContent(AbstractRepository repository, String blobId);

    public abstract Integer getCommitTime(AbstractRepository repository, String commitId);

    public BlobInfo getBlobInfo(AbstractRepository repository, String blobId) {
        BlobInfo blobInfo = repository.blobCache.get(blobId);
        if(blobInfo == null) {
            blobInfo = gitObjectStorageService.getBlobById(blobId);
            repository.blobCache.put(blobId, blobInfo);
        }
        return blobInfo;
    }

    public void saveBlobInfo(AbstractRepository repository, BlobInfo blobInfo) {
        gitObjectStorageService.saveBlob(blobInfo);
        repository.blobCache.put(blobInfo.getBlobIdString(), blobInfo);
    }

    public CommitInfo getCommitInfo(AbstractRepository repository, String commitId) {
        CommitInfo commitInfo = repository.commitCache.get(commitId);
        if(commitInfo == null) {
            commitInfo = gitObjectStorageService.getCommitById(commitId);
            repository.commitCache.put(commitId, commitInfo);
        }
        return commitInfo;
    }

    public void saveCommitInfo(AbstractRepository repository, CommitInfo commitInfo) {
        gitObjectStorageService.saveCommit(commitInfo);
        repository.commitCache.put(commitInfo.getCommitIdString(), commitInfo);
    }

    public RepositoryAnalyzeStatus.RepoType getRepoType() {
        if(this instanceof WocRepositoryAnalysisService) {
            return RepositoryAnalyzeStatus.RepoType.WoC;
        } else if(this instanceof GitRepositoryAnalysisService) {
            return RepositoryAnalyzeStatus.RepoType.Git;
        } else {
            throw new RuntimeException("Unknown RepoType");
        }
    }

    protected synchronized void buildLibraryOverlapMap() {
        if(libraryOverlapMap != null) return;
        Map<Long, Set<Long>> map = new HashMap<>(100000);
        List<LibraryOverlap> overlapList = libraryOverlapMapper.findAll();
        for (LibraryOverlap overlap : overlapList) {
            Long id1 = overlap.getGroupArtifactId1();
            Long id2 = overlap.getGroupArtifactId2();
            map.computeIfAbsent(id1, k -> new HashSet<>()).add(id2);
            map.computeIfAbsent(id2, k -> new HashSet<>()).add(id1);
        }
        libraryOverlapMap = Collections.unmodifiableMap(map);
    }

    protected Set<Long> makeLibrarySetUnique(Set<Long> pomGAIds, Set<Long> codeGaIds) {
        Set<Long> uniqueSet = new HashSet<>();
        Set<Long> extendSet = new HashSet<>();
        for (Long pomGAId : pomGAIds) {
            uniqueSet.add(pomGAId);
            extendSet.add(pomGAId);
            Set<Long> overlap = libraryOverlapMap.get(pomGAId);
            if(overlap != null) {
                extendSet.addAll(overlap);
            }
        }
        List<Long> originalIds = new ArrayList<>(codeGaIds);
        originalIds.sort(Long::compareTo);
        for (Long gaId : originalIds) {
            if(extendSet.contains(gaId)) continue;
            uniqueSet.add(gaId);
            extendSet.add(gaId);
            Set<Long> overlap = libraryOverlapMap.get(gaId);
            if(overlap != null) {
                extendSet.addAll(overlap);
            }
        }
        return uniqueSet;
    }

    public RepositoryDepSeq getRepositoryDepSeq(String repositoryName) {
        RepositoryAnalyzeStatus.RepoType repoType = getRepoType();
        RepositoryAnalyzeStatus analyzeStatus = repositoryAnalyzeStatusMapper.findByRepoTypeAndRepoName(
                repoType, repositoryName);
        if(analyzeStatus == null) return null;
        if(analyzeStatus.getAnalyzeStatus() != RepositoryAnalyzeStatus.AnalyzeStatus.Success) return null;
        RepositoryDepSeq depSeq = repositoryDepSeqMapper.findById(analyzeStatus.getId());
        if(depSeq != null) return depSeq;
        depSeq = new RepositoryDepSeq()
                .setId(analyzeStatus.getId())
                .setRepoName(analyzeStatus.getRepoName());
        AbstractRepository repository = openRepository(repositoryName);
        if(repository == null) {
            return null;
        }
        try {
            if(!pomOnly) {
                buildLibraryOverlapMap();
            }
            List<CommitInfo> commitList = new LinkedList<>();
            forEachCommit(repository, commitId -> {
                CommitInfo commitInfo = gitObjectStorageService.getCommitById(commitId);
                if(commitInfo == null) return;
                boolean needCalc = false;
                if(commitInfo.getPomAddGroupArtifactIdList() != null && !commitInfo.getPomAddGroupArtifactIdList().isEmpty()) {
                    needCalc = true;
                }
                if(commitInfo.getPomDeleteGroupArtifactIdList() != null && !commitInfo.getPomDeleteGroupArtifactIdList().isEmpty()) {
                    needCalc = true;
                }
                if(commitInfo.getCodeAddGroupArtifactIdList() != null && !commitInfo.getCodeAddGroupArtifactIdList().isEmpty()) {
                    needCalc = true;
                }
                if(commitInfo.getCodeDeleteGroupArtifactIdList() != null && !commitInfo.getCodeDeleteGroupArtifactIdList().isEmpty()) {
                    needCalc = true;
                }
                if(!needCalc) {
                    return;
                }
                Integer commitTime = getCommitTime(repository, commitId);
                if(commitTime == null) return;
                commitInfo.setCommitTime(commitTime);
                commitList.add(commitInfo);
            });
            commitList.sort(Comparator.comparingInt(CommitInfo::getCommitTime));
            List<Long> pomOnlyList = new LinkedList<>();
            List<CommitInfo> pomOnlyCommits = new LinkedList<>();
            List<Long> codeWithDupList = new LinkedList<>();
            List<Long> codeWithoutDupList = new LinkedList<>();
            List<Long> pomWithCodeDelList = new LinkedList<>();
            List<Long> pomWithCodeAddList = new LinkedList<>();
            for (CommitInfo commitInfo : commitList) {
                Set<Long> commitSeq = new HashSet<>();

                // calc pomOnly
                List<Long> pomAddIdList = commitInfo.getPomAddGroupArtifactIdList();
                if(pomAddIdList == null) {
                    pomAddIdList = new ArrayList<>(0);
                }
                commitSeq.addAll(pomAddIdList);
                List<Long> pomDeleteIdList = commitInfo.getPomDeleteGroupArtifactIdList();
                if(pomDeleteIdList == null) {
                    pomDeleteIdList = new ArrayList<>(0);
                }
                for (Long gaId : pomDeleteIdList) {
                    commitSeq.add(-gaId);
                }
                if(!commitSeq.isEmpty()) {
                    List<Long> pomOnlySeq = new ArrayList<>(commitSeq);
                    pomOnlySeq.sort(Long::compare);
                    pomOnlyList.addAll(pomOnlySeq);
                    pomOnlyList.add(0L);
                    pomOnlyCommits.add(commitInfo);
                }

                if(pomOnly) {
                    continue;
                }

                // calc codeWithDup
                List<Long> codeAddIdList = commitInfo.getCodeAddGroupArtifactIdList();
                if(codeAddIdList == null) {
                    codeAddIdList = new ArrayList<>(0);
                }
                commitSeq.addAll(codeAddIdList);
                List<Long> codeDeleteIdList = commitInfo.getCodeDeleteGroupArtifactIdList();
                if(codeDeleteIdList == null) {
                    codeDeleteIdList = new ArrayList<>(0);
                }
                for (Long gaId : codeDeleteIdList) {
                    commitSeq.add(-gaId);
                }
                if(!commitSeq.isEmpty()) {
                    List<Long> codeWithDupSeq = new ArrayList<>(commitSeq);
                    codeWithDupSeq.sort(Long::compare);
                    codeWithDupList.addAll(codeWithDupSeq);
                    codeWithDupList.add(0L);
                }

                // calc codeWithoutDup
                List<Long> codeGAList = commitInfo.getCodeGroupArtifactIdList();
                if(codeGAList == null) {
                    codeGAList = new ArrayList<>(0);
                }
                Set<Long> codeCurrGASet = new HashSet<>(codeGAList);
                Set<Long> codePreGASet = new HashSet<>(codeGAList);
                codePreGASet.removeAll(codeAddIdList);
                codePreGASet.addAll(codeDeleteIdList);

                List<Long> pomGAList = commitInfo.getPomGroupArtifactIdList();
                if(pomGAList == null) {
                    pomGAList = new ArrayList<>(0);
                }
                Set<Long> pomCurrGASet = new HashSet<>(pomGAList);
                Set<Long> pomPreGASet = new HashSet<>(pomGAList);
                pomPreGASet.removeAll(pomAddIdList);
                pomPreGASet.addAll(pomDeleteIdList);

                codeCurrGASet = makeLibrarySetUnique(pomCurrGASet, codeCurrGASet);
                codePreGASet = makeLibrarySetUnique(pomPreGASet, codePreGASet);
                commitSeq.clear();
                for (Long gaId : codePreGASet) {
                    if(!codeCurrGASet.contains(gaId)) {
                        commitSeq.add(-gaId);
                    }
                }
                for (Long gaId : codeCurrGASet) {
                    if(!codePreGASet.contains(gaId)) {
                        commitSeq.add(gaId);
                    }
                }
                if(!commitSeq.isEmpty()) {
                    List<Long> codeWithoutDupSeq = new ArrayList<>(commitSeq);
                    codeWithoutDupSeq.sort(Long::compare);
                    codeWithoutDupList.addAll(codeWithoutDupSeq);
                    codeWithoutDupList.add(0L);
                }

                // calc pomWithCodeDel
                commitSeq.clear();
                commitSeq.addAll(pomAddIdList);
                Set<Long> deleteIds = new HashSet<>();
                for (Long codeGAId : codeDeleteIdList) {
                    if(pomCurrGASet.contains(codeGAId)) {
                        deleteIds.add(codeGAId);
                    }
                }
                deleteIds.addAll(pomDeleteIdList);
                for (Long delId : deleteIds) {
                    commitSeq.add(-delId);
                }
                if(!commitSeq.isEmpty()) {
                    List<Long> pomWithCodeDelSeq = new ArrayList<>(commitSeq);
                    pomWithCodeDelSeq.sort(Long::compare);
                    pomWithCodeDelList.addAll(pomWithCodeDelSeq);
                    pomWithCodeDelList.add(0L);
                }

                // calc pomWithCodeAdd
                for (Long addId : codeAddIdList) {
                    if(pomCurrGASet.contains(addId)) continue;
                    commitSeq.add(addId);
                }
                if(!commitSeq.isEmpty()) {
                    List<Long> pomWithCodeAddSeq = new ArrayList<>(commitSeq);
                    pomWithCodeAddSeq.sort(Long::compare);
                    pomWithCodeAddList.addAll(pomWithCodeAddSeq);
                    pomWithCodeAddList.add(0L);
                }
            }
            byte[] pomOnlyCommitsBytes = new byte[pomOnlyCommits.size()*20];
            int i = 0;
            for (CommitInfo pomOnlyCommit : pomOnlyCommits) {
                for (byte b : pomOnlyCommit.getCommitId()) {
                    pomOnlyCommitsBytes[i++] = b;
                }
            }
            depSeq.setPomOnlyList(pomOnlyList)
                    .setPomOnlyCommits(pomOnlyCommitsBytes)
                    .setCodeWithDupList(codeWithDupList)
                    .setCodeWithoutDupList(codeWithoutDupList)
                    .setPomWithCodeDelList(pomWithCodeDelList)
                    .setPomWithCodeAddList(pomWithCodeAddList);
            repositoryDepSeqMapper.insert(depSeq);
            return depSeq;
        } finally {
            closeRepository(repository);
        }
    }

    public List<CommitInfo> getRepositoryDependencyChangeCommits(String repositoryName) {
        List<CommitInfo> result = new LinkedList<>();
        AbstractRepository repository = openRepository(repositoryName);
        if(repository == null) {
            return result;
        }
        try {
            forEachCommit(repository, commitId -> {
                CommitInfo commitInfo = gitObjectStorageService.getCommitById(commitId);
                if(commitInfo == null) return;
                boolean change = false;
                if(commitInfo.getCodeAddGroupArtifactIdList() != null && !commitInfo.getCodeAddGroupArtifactIdList().isEmpty()) {
                    change = true;
                } else if (commitInfo.getCodeDeleteGroupArtifactIdList() != null && !commitInfo.getCodeDeleteGroupArtifactIdList().isEmpty()) {
                    change = true;
                } else if (commitInfo.getPomAddGroupArtifactIdList() != null && !commitInfo.getPomAddGroupArtifactIdList().isEmpty()) {
                    change = true;
                } else if (commitInfo.getPomDeleteGroupArtifactIdList() != null && !commitInfo.getPomDeleteGroupArtifactIdList().isEmpty()) {
                    change = true;
                } else if (commitInfo.getMethodChangeIdList() != null && !commitInfo.getMethodChangeIdList().isEmpty()) {
                    change = true;
                }
                if(change) {
                    result.add(commitInfo);
                }
            });
            return result;
        } finally {
            closeRepository(repository);
        }
    }

    public int getRepositoryAnalyzeStatus(String repositoryName) {
        AbstractRepository repository = openRepository(repositoryName);
        if(repository == null) {
            return 0;
        }
        try {
            MutableBoolean successExist = new MutableBoolean(false);
            forEachCommit(repository, commitId -> {
                if (successExist.booleanValue()) {
                    return;
                }
                CommitInfo commitInfo = gitObjectStorageService.getCommitById(commitId);
                if(commitInfo != null) {
                    successExist.setTrue();
                }
            });
            if(successExist.booleanValue()) return 1;
            else return 0;
        } finally {
            closeRepository(repository);
        }
    }

    public void analyzeRepositoryLibrary(String repositoryName) {
        RepositoryAnalyzeStatus.RepoType repoType = getRepoType();
        RepositoryAnalyzeStatus analyzeStatus = repositoryAnalyzeStatusMapper.findByRepoTypeAndRepoName(repoType, repositoryName);
        if(analyzeStatus == null) {
            analyzeStatus = new RepositoryAnalyzeStatus()
                    .setRepoType(repoType)
                    .setRepoName(repositoryName)
                    .setAnalyzeStatus(RepositoryAnalyzeStatus.AnalyzeStatus.Analyzing)
                    .setStartTime(new Date())
                    .setEndTime(null);
            repositoryAnalyzeStatusMapper.insert(analyzeStatus);
        }
        if(analyzeStatus.getAnalyzeStatus() != RepositoryAnalyzeStatus.AnalyzeStatus.Analyzing) {
            return;
        }
        AbstractRepository repository = openRepository(repositoryName);
        if(repository == null) {
            analyzeStatus.setEndTime(new Date())
                    .setAnalyzeStatus(RepositoryAnalyzeStatus.AnalyzeStatus.Error);
            repositoryAnalyzeStatusMapper.update(analyzeStatus);
            throw new RuntimeException("open repository fail");
        }
        try {
            MutableBoolean hasCommit = new MutableBoolean(false);
            forEachCommit(repository, commitId -> {
                hasCommit.setTrue();
                CommitInfo thisCommit = analyzeCommitSelfInfo(repository, commitId);
                if((!commitNeedAnalyzeDependencyChange(thisCommit)) &&
                        (!commitNeedAnalyzeMethodChange(thisCommit))) {
                    return;
                }
                List<String> parentIds = getCommitParents(repository, commitId);
                if (parentIds.size() == 1) {
                    CommitInfo parentCommit = analyzeCommitSelfInfo(repository, parentIds.get(0));
                    thisCommit = analyzeCommitDiff(repository, thisCommit, parentCommit);
                } else if (parentIds.size() == 0) {
                    thisCommit = analyzeCommitDiff(repository, thisCommit, null);
                } else {
                    // ignore merge commit
                }
            });
            RepositoryAnalyzeStatus.AnalyzeStatus status = hasCommit.booleanValue() ?
                    RepositoryAnalyzeStatus.AnalyzeStatus.Success : RepositoryAnalyzeStatus.AnalyzeStatus.NoCommit;
            analyzeStatus.setEndTime(new Date())
                    .setAnalyzeStatus(status);
            repositoryAnalyzeStatusMapper.update(analyzeStatus);
        } catch (Exception e) {
            analyzeStatus.setEndTime(new Date())
                    .setAnalyzeStatus(RepositoryAnalyzeStatus.AnalyzeStatus.Error);
            repositoryAnalyzeStatusMapper.update(analyzeStatus);
            throw e;
        } finally {
            closeRepository(repository);
        }
    }

    public boolean commitNeedAnalyzeDependencyChange(CommitInfo commitInfo) {
        if(pomOnly) {
            return commitInfo.getPomAddGroupArtifactIdList() == null ||
                    commitInfo.getPomDeleteGroupArtifactIdList() == null;
        } else {
            return commitInfo.getCodeAddGroupArtifactIdList() == null ||
                    commitInfo.getCodeDeleteGroupArtifactIdList() == null ||
                    commitInfo.getPomAddGroupArtifactIdList() == null ||
                    commitInfo.getPomDeleteGroupArtifactIdList() == null;
        }
    }

    public boolean commitNeedAnalyzeMethodChange(CommitInfo commitInfo) {
        return commitInfo.getMethodChangeIds() == null && !pomOnly;
    }

    public CommitInfo analyzeCommitDiff(AbstractRepository repository, CommitInfo thisCommit, CommitInfo parentCommit) {
        if(commitNeedAnalyzeDependencyChange(thisCommit)) {
            List<Long> parentCodeIds;
            List<Long> parentPomIds;
            if(parentCommit == null) {
                parentCodeIds = new ArrayList<>(0);
                parentPomIds = new ArrayList<>(0);
            } else {
                parentCodeIds = parentCommit.getCodeGroupArtifactIdList();
                parentPomIds = parentCommit.getPomGroupArtifactIdList();
            }
            if(!pomOnly) {
                calcIdsDiff(thisCommit.getCodeGroupArtifactIdList(), parentCodeIds,
                        thisCommit::setCodeAddGroupArtifactIdList, thisCommit::setCodeDeleteGroupArtifactIdList);
            }
            calcIdsDiff(thisCommit.getPomGroupArtifactIdList(), parentPomIds,
                    thisCommit::setPomAddGroupArtifactIdList, thisCommit::setPomDeleteGroupArtifactIdList);
        }

        if(commitNeedAnalyzeMethodChange(thisCommit)) {
            List<BlobInCommit[]> diffBlobs = getCommitBlobDiff(repository, thisCommit, parentCommit);
            Map<List<Long>, MethodChange> methodChangeMap = new HashMap<>();
            for (BlobInCommit[] diffBlob : diffBlobs) {
                List<Set<Long>[]> deleteAddSigIds = analyzeBlobDiff(repository, diffBlob[0], diffBlob[1]);
                for (Set<Long>[] deleteAddSigId : deleteAddSigIds) {
                    List<Long> deleteIds = new ArrayList<>(deleteAddSigId[0]);
                    List<Long> addIds = new ArrayList<>(deleteAddSigId[1]);
                    deleteIds.sort(Long::compareTo);
                    addIds.sort(Long::compareTo);
                    List<Long> key = new ArrayList<>(deleteIds.size() + 1 + addIds.size());
                    key.addAll(deleteIds);
                    key.add(-1L);
                    key.addAll(addIds);
                    if(methodChangeMap.containsKey(key)) {
                        MethodChange methodChange = methodChangeMap.get(key);
                        methodChange.setCounter(methodChange.getCounter() + 1);
                    } else {
                        MethodChange methodChange = new MethodChange();
                        methodChange.setDeleteSignatureIdList(deleteIds);
                        methodChange.setAddSignatureIdList(addIds);
                        methodChange.setCounter(1);
                        methodChangeMap.put(key, methodChange);
                    }
                }
            }
            Set<Long> allSignatureIds = new HashSet<>();
            for (MethodChange mc : methodChangeMap.values()) {
                allSignatureIds.addAll(mc.getDeleteSignatureIdList());
                allSignatureIds.addAll(mc.getAddSignatureIdList());
            }
            Map<Long, List<Long>> s2ga = libraryIdentityService.getSignatureToGroupArtifact(allSignatureIds);
            for (MethodChange mc : methodChangeMap.values()) {
                Set<Long> deleteGA = new HashSet<>();
                Set<Long> addGA = new HashSet<>();
                for (Long signatureId : mc.getDeleteSignatureIdList()) {
                    deleteGA.addAll(s2ga.get(signatureId));
                }
                for (Long signatureId : mc.getAddSignatureIdList()) {
                    addGA.addAll(s2ga.get(signatureId));
                }
                List<Long> deleteIds = new ArrayList<>(deleteGA);
                List<Long> addIds = new ArrayList<>(addGA);
                deleteIds.sort(Long::compareTo);
                addIds.sort(Long::compareTo);
                mc.setDeleteGroupArtifactIdList(deleteIds);
                mc.setAddGroupArtifactIdList(addIds);
            }
            List<MethodChange> methodChanges = saveMethodChange(methodChangeMap.values());
            List<Long> methodChangeIds = new ArrayList<>(methodChanges.size() * 2);
            for (MethodChange methodChange : methodChanges) {
                methodChangeIds.add(methodChange.getId());
                methodChangeIds.add(methodChange.getCounter());
            }
            thisCommit.setMethodChangeIdList(methodChangeIds);
        }

        saveCommitInfo(repository, thisCommit);
        return thisCommit;
    }

    public static int getMethodChangeSliceKey(long methodChangeId) {
        return (int)(methodChangeId >> MethodChangeMapper.MAX_ID_BIT) & (MethodChangeMapper.MAX_TABLE_COUNT - 1);
    }

    public static int getMethodChangeSliceKey(MethodChange methodChange) {
        byte[] deleteIds = methodChange.getDeleteSignatureIds();
        if(deleteIds != null && deleteIds.length > 0) {
            return deleteIds[0] & (MethodChangeMapper.MAX_TABLE_COUNT - 1);
        }
        byte[] addIds = methodChange.getAddSignatureIds();
        if(addIds != null && addIds.length > 0) {
            return addIds[0] & (MethodChangeMapper.MAX_TABLE_COUNT - 1);
        }
        return 0;
    }

    private List<MethodChange> saveMethodChange(Collection<MethodChange> methodChanges) {
        for (MethodChange methodChange : methodChanges) {
            int sliceKey = getMethodChangeSliceKey(methodChange);
            methodChangeMapper.insertOne(sliceKey, methodChange);
            Long id = methodChangeMapper.findId(sliceKey, methodChange.getDeleteSignatureIds(), methodChange.getAddSignatureIds());
            if(id == null) {
                id = methodChangeMapper.findId(sliceKey, methodChange.getDeleteSignatureIds(), methodChange.getAddSignatureIds());
            }
            methodChange.setId(id);
        }
        return new ArrayList<>(methodChanges);
    }

    private List<String> splitContent2Lines(String content) {
        List<String> result = new LinkedList<>();
        char[] ca = content.toCharArray();
        int start = 0;
        for (int i = 0; i < ca.length; i++) {
            if(ca[i] == '\n') {
                result.add(new String(ca, start, i - start));
                start = i + 1;
            }
        }
        if(start != ca.length) {
            result.add(new String(ca, start, ca.length - start));
        }
        return new ArrayList<>(result);
    }

    // return List<[deleteSignatureIds, addSignatureIds]>
    public List<Set<Long>[]> analyzeBlobDiff(AbstractRepository repository, BlobInCommit parentBlob, BlobInCommit thisBlob) {
        List<Set<Long>[]> result = new LinkedList<>();
        if(parentBlob == null || thisBlob == null) {
            return result;
        }
        BlobInfo thisBlobInfo = analyzeBlobInfo(repository, thisBlob);
        if(thisBlobInfo.getBlobTypeEnum() != BlobInfo.BlobType.Java) {
            return result;
        }
        BlobInfo parentBlobInfo = analyzeBlobInfo(repository, parentBlob);
        if(parentBlobInfo.getBlobTypeEnum() != BlobInfo.BlobType.Java) {
            return result;
        }
        try {
            String thisContent = getBlobContent(repository, thisBlob.blobId);
            String parentContent = getBlobContent(repository, parentBlob.blobId);
            if (thisContent == null || parentContent == null) {
                return result;
            }
            List<String> thisLines = splitContent2Lines(thisContent);
            List<String> parentLines = splitContent2Lines(parentContent);
            Patch<String> patch = DiffUtils.diff(parentLines, thisLines);

            Map<Long, Set<Long>> thisLine2Sig = buildLine2SigMap(thisBlobInfo);
            Map<Long, Set<Long>> parentLine2Sig = buildLine2SigMap(parentBlobInfo);
            List<MyDiffDelta> deltaList = mergeDiffDelta(patch.getDeltas());

            if(LOG.isDebugEnabled()) {
                int line = 1;
                for (String lineContent : parentLines) {
                    System.out.println(line + ": " + lineContent);
                    ++line;
                }
                line = 1;
                for (String lineContent : thisLines) {
                    System.out.println(line + ": " + lineContent);
                    ++line;
                }

                System.out.println("thisLine2Sig: ");
                thisLine2Sig.forEach((lineNumber, sids) -> System.out.println(lineNumber + ": " + sids));
                System.out.println("parentLine2Sig: ");
                parentLine2Sig.forEach((lineNumber, sids) -> System.out.println(lineNumber + ": " + sids));

                for (MyDiffDelta delta : deltaList) {
                    System.out.println(delta.deltaType);
                    for (long i = delta.delStart; i < delta.delEnd; i++) {
                        line = (int) (i + 1);
                        System.out.println("--- " + line + ": " + parentLines.get(line - 1));
                    }
                    for (long i = delta.addStart; i < delta.addEnd; i++) {
                        line = (int) (i + 1);
                        System.out.println("+++ " + line + ": " + thisLines.get(line - 1));
                    }
                }
            }

            for (MyDiffDelta delta : deltaList) {
                if(delta.deltaType != DeltaType.CHANGE) continue;
                Set<Long> deleteIds = calcChunkSignatureIds(parentLine2Sig, delta.delStart + 1, delta.delEnd + 1);
                Set<Long> addIds = calcChunkSignatureIds(thisLine2Sig, delta.addStart + 1, delta.addEnd + 1);
                Set<Long> intersect = new HashSet<>(deleteIds);
                intersect.retainAll(addIds);
                deleteIds.removeAll(intersect);
                addIds.removeAll(intersect);
                if(deleteIds.isEmpty() || addIds.isEmpty()) {
                    continue;
                } else {
                    result.add(new Set[]{deleteIds, addIds});
                }
            }
            return result;
        } catch (Exception e) {
            LOG.error("analyzeBlobDiff fail", e);
            LOG.error("analyzeBlobDiff fail, parentBlob = {}, thisBlob = {}", parentBlob.blobId, thisBlob.blobId);
            return new ArrayList<>(0);
        }
    }

    private static Set<Long> calcChunkSignatureIds(Map<Long, Set<Long>> line2Sig, long start, long end) {
        Set<Long> result = new HashSet<>();
        for (long i = start; i < end; i++) {
            if(line2Sig.containsKey(i)) {
                result.addAll(line2Sig.get(i));
            }
        }
        return result;
    }

    public static class MyDiffDelta {
        DeltaType deltaType;
        long delStart = 0; // include, 0-base
        long delEnd = 0; // exclude
        long addStart = 0; // include, 0-base
        long addEnd = 0; // exclude
    }

    public static <T> List<MyDiffDelta> mergeDiffDelta(Collection<AbstractDelta<T>> deltaList) {
        TreeMap<Long, MyDiffDelta> delMap = new TreeMap<>();
        TreeMap<Long, MyDiffDelta> addMap = new TreeMap<>();
        TreeMap<Long, MyDiffDelta> chgMap = new TreeMap<>();
        for (AbstractDelta<?> delta : deltaList) {
            MyDiffDelta myDelta = new MyDiffDelta();
            myDelta.deltaType = delta.getType();
            myDelta.delStart = delta.getSource().getPosition();
            myDelta.delEnd = myDelta.delStart + delta.getSource().getLines().size();
            myDelta.addStart = delta.getTarget().getPosition();
            myDelta.addEnd = myDelta.addStart + delta.getTarget().getLines().size();
            switch (myDelta.deltaType) {
                case CHANGE: {
                    chgMap.put(myDelta.delStart, myDelta);
                    break;
                }
                case INSERT: {
                    addMap.put(myDelta.addStart, myDelta);
                    break;
                }
                case DELETE: {
                    delMap.put(myDelta.delStart, myDelta);
                    break;
                }
            }
        }
        List<MyDiffDelta> result = new ArrayList<>(deltaList.size());
        while(!chgMap.isEmpty()) {
            Iterator<MyDiffDelta> it = chgMap.values().iterator();
            MyDiffDelta delta = it.next();
            long originalKey = delta.delStart;
            Map.Entry<Long, MyDiffDelta> entry;
            MyDiffDelta delDelta;
            MyDiffDelta addDelta;
            MyDiffDelta chgDelta;
            // del: [a, b), chg: [b, c) -> [x, y) => chg: [a, c) -> [x, y)
            entry = delMap.floorEntry(delta.delStart);
            if(entry != null) {
                delDelta = entry.getValue();
                if(delDelta.delEnd == delta.delStart) {
                    delMap.remove(entry.getKey());
                    delta.delStart = delDelta.delStart;
                    modifyChangeMap(chgMap, originalKey, delta);
                    continue;
                }
            }
            // del: [c, d), chg: [b, c) -> [x, y) => chg: [b, d) -> [x, y)
            delDelta = delMap.get(delta.delEnd);
            if(delDelta != null) {
                delMap.remove(delta.delEnd);
                delta.delEnd = delDelta.delEnd;
                continue;
            }
            // add: [z, x), chg: [b, c) -> [x, y) => chg: [b, c) -> [z, y)
            entry = addMap.floorEntry(delta.addStart);
            if(entry != null) {
                addDelta = entry.getValue();
                if(addDelta.addEnd == delta.addStart) {
                    addMap.remove(entry.getKey());
                    delta.addStart = addDelta.addStart;
                    continue;
                }
            }
            // add: [y, z), chg: [b, c) -> [x, y) => chg: [b, c) -> [x, z)
            addDelta = addMap.get(delta.addEnd);
            if(addDelta != null) {
                addMap.remove(delta.addEnd);
                delta.addEnd = addDelta.addEnd;
                continue;
            }
            // chg: [a, b) -> [z, x), chg: [b, c) -> [x, y) => chg: [a, c) -> [z, y)
            entry = chgMap.floorEntry(delta.delStart);
            if(entry != null) {
                chgDelta = entry.getValue();
                if(chgDelta.delEnd == delta.delStart && chgDelta.addEnd == delta.addStart) {
                    chgMap.remove(entry.getKey());
                    delta.delStart = chgDelta.delStart;
                    delta.addStart = chgDelta.addStart;
                    modifyChangeMap(chgMap, originalKey, delta);
                    continue;
                }
            }
            // chg: [c, d) -> [y, z), chg: [b, c) -> [x, y) => chg: [b, d) -> [x, z)
            chgDelta = chgMap.get(delta.delEnd);
            if(chgDelta != null && chgDelta.addStart == delta.addEnd) {
                chgMap.remove(delta.delEnd);
                delta.delEnd = chgDelta.delEnd;
                delta.addEnd = chgDelta.addEnd;
                continue;
            }
            // can not merge
            chgMap.remove(originalKey);
            result.add(delta);
        }
        result.addAll(delMap.values());
        result.addAll(addMap.values());
        return result;
    }

    private static void modifyChangeMap(TreeMap<Long, MyDiffDelta> chgMap, long originalKey, MyDiffDelta newValue) {
        chgMap.remove(originalKey);
        chgMap.put(newValue.delStart, newValue);
    }

    private Map<Long, Set<Long>> buildLine2SigMap(BlobInfo blobInfo) {
        Map<Long, Set<Long>> result = new HashMap<>();
        if(blobInfo.getLibrarySignatureIdList() == null) return result;
        Iterator<Long> it = blobInfo.getLibrarySignatureIdList().iterator();
        while(it.hasNext()) {
            long signatureId = it.next();
            if(!it.hasNext()) break;
            long startLine = it.next();
            if(!it.hasNext()) break;
            long endLine = it.next();
            if(endLine - startLine > 100) {
                LOG.warn("endLine - startLine > 100, blobId = {}", blobInfo.getBlobIdString());
                endLine = startLine + 100;
            }
            for (long i = startLine; i <= endLine; i++) {
                result.computeIfAbsent(i, k -> new HashSet<>()).add(signatureId);
            }
        }
        if(LOG.isDebugEnabled()) {
            LOG.debug("blobId line2SigMap: {}", blobInfo.getBlobIdString());
            result.forEach((line, sids) -> {
                LOG.debug("{}: {}", line, sids);
            });
        }
        return result;
    }

    // return List<[origin, new]>
    public List<BlobInCommit[]> getCommitBlobDiff(AbstractRepository repository, CommitInfo thisCommit, CommitInfo parentCommit) {
        List<BlobInCommit> thisBlobs = thisCommit.getBlobInCommit();
        if(thisBlobs == null) {
            thisBlobs = getBlobsInCommit(repository, thisCommit.getCommitIdString());
            thisCommit.setBlobInCommit(thisBlobs);
        }
        if(thisBlobs == null) {
            return new ArrayList<>(0);
        }
        if(parentCommit == null) {
            List<BlobInCommit[]> result = new ArrayList<>(thisBlobs.size());
            for (BlobInCommit thisBlob : thisBlobs) {
                result.add(new BlobInCommit[]{null, thisBlob});
            }
            return result;
        }
        List<BlobInCommit> parentBlobs = parentCommit.getBlobInCommit();;
        if(parentBlobs == null) {
            parentBlobs = getBlobsInCommit(repository, parentCommit.getCommitIdString());
            parentCommit.setBlobInCommit(parentBlobs);
        }
        if(parentBlobs == null) {
            return new ArrayList<>(0);
        }
        if(parentBlobs.isEmpty()) {
            List<BlobInCommit[]> result = new ArrayList<>(thisBlobs.size());
            for (BlobInCommit thisBlob : thisBlobs) {
                result.add(new BlobInCommit[]{null, thisBlob});
            }
            return result;
        }

        List<BlobInCommit[]> result = new LinkedList<>();
        Map<String, BlobInCommit> parentNameMap = new HashMap<>();
        Map<String, BlobInCommit> parentIdMap = new HashMap<>();
        for (BlobInCommit parentBlob : parentBlobs) {
            parentNameMap.put(parentBlob.fileName, parentBlob);
            parentIdMap.put(parentBlob.blobId, parentBlob);
        }
        for (BlobInCommit thisBlob : thisBlobs) {
            if(parentNameMap.containsKey(thisBlob.fileName)) { // equal or modify
                BlobInCommit parentBlob = parentNameMap.remove(thisBlob.fileName);
                parentIdMap.remove(parentBlob.blobId);
                if(!Objects.equals(parentBlob.blobId, thisBlob.blobId)) { // modify
                    result.add(new BlobInCommit[]{parentBlob, thisBlob});
                }
            } else if(parentIdMap.containsKey(thisBlob.blobId)) { // rename
                BlobInCommit parentBlob = parentIdMap.remove(thisBlob.blobId);
                parentNameMap.remove(parentBlob.fileName);
            } else { // new
                result.add(new BlobInCommit[]{null, thisBlob});
            }
        }
        for (BlobInCommit parentBlob : parentNameMap.values()) { // delete
            result.add(new BlobInCommit[]{parentBlob, null});
        }
        return result;
    }

    private void calcIdsDiff(List<Long> thisIdsJson, List<Long> parentIdsJson, Consumer<List<Long>> addIdsJson, Consumer<List<Long>> deleteIdsJson) {
        if(thisIdsJson == null || parentIdsJson == null) {
            return;
        }
        Set<Long> thisIds = new HashSet<>(thisIdsJson);
        Set<Long> parentIds = new HashSet<>(parentIdsJson);
        Set<Long> addIds = new HashSet<>(thisIds);
        addIds.removeAll(parentIds);
        Set<Long> deleteIds = new HashSet<>(parentIds);
        deleteIds.removeAll(thisIds);
        addIdsJson.accept(new ArrayList<>(addIds));
        deleteIdsJson.accept(new ArrayList<>(deleteIds));
    }

    private CommitInfo analyzeCommitSelfInfo(AbstractRepository repository, String commitId) {
        CommitInfo commitInfo = getCommitInfo(repository, commitId);
        if(pomOnly) {
            if(commitInfo != null &&
                    commitInfo.getPomLibraryVersionIds() != null  &&
                    commitInfo.getPomGroupArtifactIds() != null ) {
                return commitInfo;
            }
        } else {
            if(commitInfo != null &&
                    commitInfo.getCodeLibraryVersionIds() != null &&
                    commitInfo.getCodeGroupArtifactIds() != null  &&
                    commitInfo.getPomLibraryVersionIds() != null  &&
                    commitInfo.getPomGroupArtifactIds() != null ) {
                return commitInfo;
            }
        }
        if(commitInfo == null) {
            commitInfo = new CommitInfo();
            commitInfo.setCommitIdString(commitId);
        }
        List<BlobInCommit> blobs = getBlobsInCommit(repository, commitId);
        if(blobs == null) {
            saveCommitInfo(repository, commitInfo);
            return commitInfo;
        }
        Set<Long> codeVersionIds = new HashSet<>();
        Set<Long> codeGaIds = new HashSet<>();
        Set<Long> pomVersionIds = new HashSet<>();
        Set<Long> pomGaIds = new HashSet<>();
        for (BlobInCommit blob : blobs) {
            if(pomOnly && !isBlobPom(blob)) {
                continue;
            }
            BlobInfo blobInfo = analyzeBlobInfo(repository, blob);
            if(blobInfo.getBlobTypeEnum() == BlobInfo.BlobType.Java) {
                codeVersionIds.addAll(blobInfo.getLibraryVersionIdList());
                codeGaIds.addAll(blobInfo.getLibraryGroupArtifactIdList());
            } else if (blobInfo.getBlobTypeEnum() == BlobInfo.BlobType.POM) {
                pomVersionIds.addAll(blobInfo.getLibraryVersionIdList());
                pomGaIds.addAll(blobInfo.getLibraryGroupArtifactIdList());
            }
        }
        if(pomOnly) {
            commitInfo.setPomLibraryVersionIdList(new ArrayList<>(pomVersionIds));
            commitInfo.setPomGroupArtifactIdList(new ArrayList<>(pomGaIds));
        } else {
            commitInfo.setCodeLibraryVersionIdList(new ArrayList<>(codeVersionIds));
            commitInfo.setCodeGroupArtifactIdList(new ArrayList<>(codeGaIds));
            commitInfo.setPomLibraryVersionIdList(new ArrayList<>(pomVersionIds));
            commitInfo.setPomGroupArtifactIdList(new ArrayList<>(pomGaIds));
        }
        commitInfo.setBlobInCommit(blobs);
        saveCommitInfo(repository, commitInfo);
        return commitInfo;
    }

    private BlobInfo analyzeBlobInfo(AbstractRepository repository, BlobInCommit blob) {
        BlobInfo blobInfo = getBlobInfo(repository, blob.blobId);
        if(blobInfo == null) {
            LOG.debug("new blob: {}", blob.blobId);
            blobInfo = new BlobInfo();
            blobInfo.setBlobIdString(blob.blobId);
        } else {
            LOG.debug("exist blob: {}, blobType = {}", blobInfo.getBlobIdString(), blobInfo.getBlobTypeEnum());
            return blobInfo;
        }
        List<Long[]> signatureIdLines = new LinkedList<>(); // List<[signatureId, startLine, endLine]>
        Set<Long> versionIds = new HashSet<>();
        Set<Long> groupArtifactIds = new HashSet<>();
        if(isBlobJavaCode(blob)) {
            blobInfo.setBlobTypeEnum(BlobInfo.BlobType.Java);
            try {
                LOG.debug("begin analyzeJavaContent blobId = {}", blob.blobId);
                String content = getBlobContent(repository, blob.blobId);
                if(content == null) throw new RuntimeException("can not access blob: " + blob.blobId);
                analyzeJavaContent(content, signatureIdLines, versionIds, groupArtifactIds);
                LOG.debug("end analyzeJavaContent blobId = {}", blob.blobId);
            } catch (Exception e) {
                LOG.error("analyzeJavaContent fail", e);
                // not java content
                LOG.warn("blob is not java content, set to other, blobId = {}", blobInfo.getBlobIdString());
                blobInfo.setBlobTypeEnum(BlobInfo.BlobType.ErrorJava);
                signatureIdLines.clear();
                versionIds.clear();
                groupArtifactIds.clear();
            }
        } else if (isBlobPom(blob)) {
            blobInfo.setBlobTypeEnum(BlobInfo.BlobType.POM);
            try {
                LOG.debug("begin analyzePomContent blobId = {}", blob.blobId);
                String content = getBlobContent(repository, blob.blobId);
                if(content == null) throw new RuntimeException("can not access blob: " + blob.blobId);
                analyzePomContent(content, versionIds, groupArtifactIds);
                LOG.debug("end analyzePomContent blobId = {}", blob.blobId);
            } catch (Exception e) {
                LOG.error("analyzePomContent fail", e);
                // not pom content
                LOG.warn("blob is not pom content, set to other, blobId = {}", blobInfo.getBlobIdString());
                blobInfo.setBlobTypeEnum(BlobInfo.BlobType.ErrorPOM);
                versionIds.clear();
                groupArtifactIds.clear();
            }
        } else {
            blobInfo.setBlobTypeEnum(BlobInfo.BlobType.Other);
        }
        List<Long> signatureIds = new ArrayList<>(signatureIdLines.size() * 3);
        for (Long[] signatureIdLine : signatureIdLines) {
            signatureIds.add(signatureIdLine[0]);
            signatureIds.add(signatureIdLine[1]);
            signatureIds.add(signatureIdLine[2]);
        }
        blobInfo.setLibrarySignatureIdList(signatureIds);
        blobInfo.setLibraryVersionIdList(new ArrayList<>(versionIds));
        blobInfo.setLibraryGroupArtifactIdList(new ArrayList<>(groupArtifactIds));
        saveBlobInfo(repository, blobInfo);
        return blobInfo;
    }

    public void analyzeJavaContent(String content, List<Long[]> signatureIdLines, Set<Long> versionIds, Set<Long> groupArtifactIds) {
        List<MethodSignatureOld> signatureList = javaCodeAnalysisService.analyzeJavaCode(content);
        Set<Long> sids = new HashSet<>();
        for (MethodSignatureOld methodSignatureOld : signatureList) {
            MethodSignatureOld ms = libraryIdentityService.getMethodSignature(methodSignatureOld, null);
            long startLine = methodSignatureOld.getStartLine();
            long endLine = methodSignatureOld.getEndLine();
            if(ms != null) {
                sids.add(ms.getId());
                signatureIdLines.add(new Long[]{ms.getId(), startLine, endLine});
            } else {
                List<MethodSignatureOld> candidates = libraryIdentityService.getMethodSignatureList(
                        methodSignatureOld.getPackageName(), methodSignatureOld.getClassName(),
                        methodSignatureOld.getMethodName());
                List<MethodSignatureOld> matched = new LinkedList<>();
                String paramList = methodSignatureOld.getParamList();
                String[] params = paramList == null ? new String[0] : paramList.split(",");
                for (MethodSignatureOld candidate : candidates) {
                    String candidateParamList = candidate.getParamList();
                    String[] candidateParams = candidateParamList == null ? new String[0] : candidateParamList.split(",");
                    if(params.length != candidateParams.length) continue;
                    matched.add(candidate);
                }
                if(matched.size() == 0) {
                    matched = candidates;
                }
                matched.stream().sorted(Comparator.comparingLong(MethodSignatureOld::getId)).limit(1).forEach(s -> {
                    sids.add(s.getId());
                    signatureIdLines.add(new Long[]{s.getId(), startLine, endLine});
                });
            }
        }
        List<Long>[] vIdsAndGaIds = libraryIdentityService.getVersionIdsAndGroupArtifactIdsBySignatureIds(sids);
        versionIds.addAll(vIdsAndGaIds[0]);
        groupArtifactIds.addAll(vIdsAndGaIds[1]);
    }

    public void analyzePomContent(String content, Set<Long> versionIds, Set<Long> groupArtifactIds) throws Exception {
        List<PomAnalysisService.LibraryInfo> libraryInfoList = pomAnalysisService.analyzePom(content);
        for (PomAnalysisService.LibraryInfo libraryInfo : libraryInfoList) {
            LibraryVersion libraryVersion = libraryVersionMapper.findByGroupIdAndArtifactIdAndVersion(
                    libraryInfo.groupId, libraryInfo.artifactId, libraryInfo.version);
            if(libraryVersion != null) {
                versionIds.add(libraryVersion.getId());
                groupArtifactIds.add(libraryVersion.getGroupArtifactId());
            } else {
                LibraryGroupArtifact groupArtifact = libraryGroupArtifactMapper.findByGroupIdAndArtifactId(libraryInfo.groupId, libraryInfo.artifactId);
                if(groupArtifact != null) {
                    groupArtifactIds.add(groupArtifact.getId());
                }
            }
        }
    }

    public static boolean isBlobJavaCode(BlobInCommit blob) {
        return blob.fileName.toLowerCase().endsWith(".java");
    }

    public static boolean isBlobPom(BlobInCommit blob) {
        return blob.fileName.toLowerCase().endsWith("pom.xml");
    }
}
