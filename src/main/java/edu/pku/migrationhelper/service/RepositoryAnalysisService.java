package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.*;
import edu.pku.migrationhelper.mapper.LibraryGroupArtifactMapper;
import edu.pku.migrationhelper.mapper.LibraryVersionMapper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created by xuyul on 2020/2/4.
 */
public abstract class RepositoryAnalysisService {

    protected Logger LOG = LoggerFactory.getLogger(getClass());

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

    public static abstract class AbstractRepository {
        public String repositoryName;
        public Map<String, BlobInfo> blobCache = new HashMap<>();
        public Map<String, CommitInfo> commitCache = new HashMap<>();
    }

    public static class BlobInCommit {
        public String blobId;
        public String fileName;
    }

    public abstract AbstractRepository openRepository(String repositoryName);

    public abstract void closeRepository(AbstractRepository repository);

    public abstract void forEachCommit(AbstractRepository repository, Consumer<String> commitIdConsumer);

    public abstract List<String> getCommitParents(AbstractRepository repository, String commitId);

    public abstract List<BlobInCommit> getBlobsInCommit(AbstractRepository repository, String commitId);

    public abstract String getBlobContent(AbstractRepository repository, String blobId);

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

    public int getRepositoryAnalyzeStatus(String repositoryName) {
        AbstractRepository repository = openRepository(repositoryName);
        if(repository == null) {
            return 0;
        }
        try {
            int limit = 100;
            MutableBoolean successExist = new MutableBoolean(false);
            MutableBoolean missingExist = new MutableBoolean(false);
            List<String> commitList = new ArrayList<>(limit);
            forEachCommit(repository, commitId -> {
                if (successExist.booleanValue() && missingExist.booleanValue()) {
                    return;
                }
                commitList.add(commitId);
                if (commitList.size() >= limit) {
                    long count = 0;//commitInfoMapper.countIdIn(commitList); // TODO
                    if (count < commitList.size()) {
                        missingExist.setTrue();
                    }
                    if (count > 0) {
                        successExist.setTrue();
                    }
                    commitList.clear();
                }
            });
            if (successExist.booleanValue() && missingExist.booleanValue()) {
                return 2;
            }
            if (commitList.size() > 0) {
                long count = 0;//commitInfoMapper.countIdIn(commitList); // TODO
                if (count > 0) {
                    successExist.setTrue();
                }
                if (count < commitList.size()) {
                    missingExist.setTrue();
                }
            }
            if (!successExist.booleanValue()) {
                return 0;
            }
            if (successExist.booleanValue() && !missingExist.booleanValue()) {
                return 1;
            }
            return 2;
        } finally {
            closeRepository(repository);
        }
    }

    public void analyzeRepositoryLibrary(String repositoryName) {
        AbstractRepository repository = openRepository(repositoryName);
        if(repository == null) {
            throw new RuntimeException("open repository fail");
        }
        try {
            forEachCommit(repository, commitId -> {
                CommitInfo thisCommit = analyzeCommitSelfInfo(repository, commitId);
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
        } finally {
            closeRepository(repository);
        }
    }

    private CommitInfo analyzeCommitDiff(AbstractRepository repository, CommitInfo thisCommit, CommitInfo parentCommit) {
        if(thisCommit.getCodeAddGroupArtifactIdList() == null ||
                thisCommit.getCodeDeleteGroupArtifactIdList() == null ||
                thisCommit.getPomAddGroupArtifactIdList() == null ||
                thisCommit.getPomDeleteGroupArtifactIdList() == null) {
            List<Long> parentCodeIds;
            List<Long> parentPomIds;
            if(parentCommit == null) {
                parentCodeIds = new ArrayList<>(0);
                parentPomIds = new ArrayList<>(0);
            } else {
                parentCodeIds = parentCommit.getCodeGroupArtifactIdList();
                parentPomIds = parentCommit.getPomGroupArtifactIdList();
            }
            calcIdsDiff(thisCommit.getCodeGroupArtifactIdList(), parentCodeIds,
                    thisCommit::setCodeAddGroupArtifactIdList, thisCommit::setCodeDeleteGroupArtifactIdList);
            calcIdsDiff(thisCommit.getPomGroupArtifactIdList(), parentPomIds,
                    thisCommit::setPomAddGroupArtifactIdList, thisCommit::setPomDeleteGroupArtifactIdList);
        }

        if(StringUtils.isEmpty(thisCommit.getMethodChangeIds())) {
            // TODO method change analyze
        }

        saveCommitInfo(repository, thisCommit);
        return thisCommit;
    }

    private void calcIdsDiff(List<Long> thisIdsJson, List<Long> parentIdsJson, Consumer<List<Long>> addIdsJson, Consumer<List<Long>> deleteIdsJson) {
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
        if(commitInfo != null &&
                !StringUtils.isEmpty(commitInfo.getCodeLibraryVersionIds()) &&
                !StringUtils.isEmpty(commitInfo.getCodeGroupArtifactIds()) &&
                !StringUtils.isEmpty(commitInfo.getPomLibraryVersionIds()) &&
                !StringUtils.isEmpty(commitInfo.getPomGroupArtifactIds())) {
            return commitInfo;
        }
        if(commitInfo == null) {
            commitInfo = new CommitInfo();
            commitInfo.setCommitIdString(commitId);
        }
        List<BlobInCommit> blobs = getBlobsInCommit(repository, commitId);
        Set<Long> codeVersionIds = new HashSet<>();
        Set<Long> codeGaIds = new HashSet<>();
        Set<Long> pomVersionIds = new HashSet<>();
        Set<Long> pomGaIds = new HashSet<>();
        for (BlobInCommit blob : blobs) {
            BlobInfo blobInfo = analyzeBlobInfo(repository, blob);
            if(blobInfo.getBlobTypeEnum() == BlobInfo.BlobType.Java) {
                codeVersionIds.addAll(blobInfo.getLibraryVersionIdList());
                codeGaIds.addAll(blobInfo.getLibraryGroupArtifactIdList());
            } else if (blobInfo.getBlobTypeEnum() == BlobInfo.BlobType.POM) {
                pomVersionIds.addAll(blobInfo.getLibraryVersionIdList());
                pomGaIds.addAll(blobInfo.getLibraryGroupArtifactIdList());
            }
        }
        commitInfo.setCodeLibraryVersionIdList(new ArrayList<>(codeVersionIds));
        commitInfo.setCodeGroupArtifactIdList(new ArrayList<>(codeGaIds));
        commitInfo.setPomLibraryVersionIdList(new ArrayList<>(pomVersionIds));
        commitInfo.setPomGroupArtifactIdList(new ArrayList<>(pomGaIds));
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
            LOG.debug("exist blob: {}, blobType = {}", blobInfo.getBlobId(), blobInfo.getBlobType());
            return blobInfo;
        }
        Set<Long> signatureIds = new HashSet<>();
        Set<Long> versionIds = new HashSet<>();
        Set<Long> groupArtifactIds = new HashSet<>();
        if(isBlobJavaCode(blob)) {
            blobInfo.setBlobTypeEnum(BlobInfo.BlobType.Java);
            try {
                LOG.debug("begin analyzeJavaContent blobId = {}", blob.blobId);
                String content = getBlobContent(repository, blob.blobId);
                analyzeJavaContent(content, signatureIds, versionIds, groupArtifactIds);
                LOG.debug("end analyzeJavaContent blobId = {}", blob.blobId);
            } catch (Exception e) {
                LOG.error("analyzeJavaContent fail", e);
                // not java content
                LOG.warn("blob is not java content, set to other, blobId = {}", blobInfo.getBlobId());
                blobInfo.setBlobTypeEnum(BlobInfo.BlobType.ErrorJava);
                signatureIds.clear();
                versionIds.clear();
                groupArtifactIds.clear();
            }
        } else if (isBlobPom(blob)) {
            blobInfo.setBlobTypeEnum(BlobInfo.BlobType.POM);
            try {
                LOG.debug("begin analyzePomContent blobId = {}", blob.blobId);
                String content = getBlobContent(repository, blob.blobId);
                analyzePomContent(content, signatureIds, versionIds, groupArtifactIds);
                LOG.debug("end analyzePomContent blobId = {}", blob.blobId);
            } catch (Exception e) {
                LOG.error("analyzePomContent fail", e);
                // not pom content
                LOG.warn("blob is not pom content, set to other, blobId = {}", blobInfo.getBlobId());
                blobInfo.setBlobTypeEnum(BlobInfo.BlobType.ErrorPOM);
                signatureIds.clear();
                versionIds.clear();
                groupArtifactIds.clear();
            }
        } else {
            blobInfo.setBlobTypeEnum(BlobInfo.BlobType.Other);
        }
        blobInfo.setLibrarySignatureIdList(new ArrayList<>(signatureIds));
        blobInfo.setLibraryVersionIdList(new ArrayList<>(versionIds));
        blobInfo.setLibraryGroupArtifactIdList(new ArrayList<>(groupArtifactIds));
        saveBlobInfo(repository, blobInfo);
        return blobInfo;
    }

    public void analyzeJavaContent(String content, Set<Long> signatureIds, Set<Long> versionIds, Set<Long> groupArtifactIds) {
        List<MethodSignature> signatureList = javaCodeAnalysisService.analyzeJavaCode(content);
        Set<Long> sids = new HashSet<>();
        for (MethodSignature methodSignature : signatureList) {
            MethodSignature ms = libraryIdentityService.getMethodSignature(methodSignature, null);
            if(ms != null) {
                sids.add(ms.getId());
            } else {
                List<MethodSignature> candidates = libraryIdentityService.getMethodSignatureList(
                        methodSignature.getPackageName(), methodSignature.getClassName(),
                        methodSignature.getMethodName());
                List<MethodSignature> matched = new LinkedList<>();
                String paramList = methodSignature.getParamList();
                String[] params = paramList == null ? new String[0] : paramList.split(",");
                for (MethodSignature candidate : candidates) {
                    String candidateParamList = candidate.getParamList();
                    String[] candidateParams = candidateParamList == null ? new String[0] : candidateParamList.split(",");
                    if(params.length != candidateParams.length) continue;
                    matched.add(candidate);
                }
                if(matched.size() > 0) {
                    matched.forEach(s -> sids.add(s.getId()));
                } else {
                    candidates.forEach(s -> sids.add(s.getId()));
                }
            }
        }
        signatureIds.addAll(sids);
        List<Long>[] vIdsAndGaIds = libraryIdentityService.getVersionIdsAndGroupArtifactIdsBySignatureIds(sids);
        versionIds.addAll(vIdsAndGaIds[0]);
        groupArtifactIds.addAll(vIdsAndGaIds[1]);
    }

    public void analyzePomContent(String content, Set<Long> signatureIds, Set<Long> versionIds, Set<Long> groupArtifactIds) throws Exception {
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
