package edu.pku.migrationhelper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.pku.migrationhelper.data.*;
import edu.pku.migrationhelper.mapper.*;
import edu.pku.migrationhelper.util.JsonUtils;
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
    protected BlobInfoMapper blobInfoMapper;

    @Autowired
    protected CommitInfoMapper commitInfoMapper;

    @Autowired
    protected MethodSignatureMapper methodSignatureMapper;

    @Autowired
    protected LibrarySignatureMapMapper librarySignatureMapMapper;

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
            blobInfo = blobInfoMapper.findByBlobId(blobId);
            repository.blobCache.put(blobId, blobInfo);
        }
        return blobInfo;
    }

    public void saveBlobInfo(AbstractRepository repository, BlobInfo blobInfo) {
        blobInfoMapper.insert(Collections.singletonList(blobInfo));
        repository.blobCache.put(blobInfo.getBlobId(), blobInfo);
    }

    public CommitInfo getCommitInfo(AbstractRepository repository, String commitId) {
        CommitInfo commitInfo = repository.commitCache.get(commitId);
        if(commitInfo == null) {
            commitInfo = commitInfoMapper.findByCommitId(commitId);
            repository.commitCache.put(commitId, commitInfo);
        }
        return commitInfo;
    }

    public void saveCommitInfo(AbstractRepository repository, CommitInfo commitInfo) {
        commitInfoMapper.insert(Collections.singletonList(commitInfo));
        repository.commitCache.put(commitInfo.getCommitId(), commitInfo);
    }

    public void analyzeRepositoryLibrary(String repositoryName) {
        AbstractRepository repository = openRepository(repositoryName);
        if(repository == null) {
            throw new RuntimeException("open repository fail");
        }
        forEachCommit(repository, commitId -> {
            CommitInfo thisCommit = analyzeCommitSelfInfo(repository, commitId);
            List<String> parentIds = getCommitParents(repository, commitId);
            if(parentIds.size() == 1) {
                CommitInfo parentCommit = analyzeCommitSelfInfo(repository, parentIds.get(0));
                thisCommit = analyzeCommitDiff(repository, thisCommit, parentCommit);
            } else if (parentIds.size() == 0) {
                thisCommit = analyzeCommitDiff(repository, thisCommit, null);
            } else {
                // ignore merge commit
            }
        });
        closeRepository(repository);
    }

    private CommitInfo analyzeCommitDiff(AbstractRepository repository, CommitInfo thisCommit, CommitInfo parentCommit) {
        if(StringUtils.isEmpty(thisCommit.getCodeAddGroupArtifactIds()) ||
                StringUtils.isEmpty(thisCommit.getCodeDeleteGroupArtifactIds()) ||
                StringUtils.isEmpty(thisCommit.getPomAddGroupArtifactIds()) ||
                StringUtils.isEmpty(thisCommit.getPomDeleteGroupArtifactIds())) {
            String parentCodeIds;
            String parentPomIds;
            if(parentCommit == null) {
                parentCodeIds = "[]";
                parentPomIds = "[]";
            } else {
                parentCodeIds = parentCommit.getCodeGroupArtifactIds();
                parentPomIds = parentCommit.getPomGroupArtifactIds();
            }
            calcIdsDiff(thisCommit.getCodeGroupArtifactIds(), parentCodeIds,
                    thisCommit::setCodeAddGroupArtifactIds, thisCommit::setCodeDeleteGroupArtifactIds);
            calcIdsDiff(thisCommit.getPomGroupArtifactIds(), parentPomIds,
                    thisCommit::setPomAddGroupArtifactIds, thisCommit::setPomDeleteGroupArtifactIds);
        }

        if(StringUtils.isEmpty(thisCommit.getMethodChangeIds())) {
            // TODO method change analyze
        }

        saveCommitInfo(repository, thisCommit);
        return thisCommit;
    }

    private void calcIdsDiff(String thisIdsJson, String parentIdsJson, Consumer<String> addIdsJson, Consumer<String> deleteIdsJson) {
        Set<Long> thisIds = new HashSet<>(JsonUtils.readStringAsObject(thisIdsJson, new TypeReference<List<Long>>() {}));
        Set<Long> parentIds = new HashSet<>(JsonUtils.readStringAsObject(parentIdsJson, new TypeReference<List<Long>>() {}));
        Set<Long> addIds = new HashSet<>(thisIds);
        addIds.removeAll(parentIds);
        Set<Long> deleteIds = new HashSet<>(parentIds);
        deleteIds.removeAll(thisIds);
        addIdsJson.accept(JsonUtils.writeObjectAsString(new ArrayList<>(addIds)));
        deleteIdsJson.accept(JsonUtils.writeObjectAsString(new ArrayList<>(deleteIds)));
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
            commitInfo.setCommitId(commitId);
        }
        List<BlobInCommit> blobs = getBlobsInCommit(repository, commitId);
        Set<Long> codeVersionIds = new HashSet<>();
        Set<Long> codeGaIds = new HashSet<>();
        Set<Long> pomVersionIds = new HashSet<>();
        Set<Long> pomGaIds = new HashSet<>();
        for (BlobInCommit blob : blobs) {
            BlobInfo blobInfo = analyzeBlobInfo(repository, blob);
            if(blobInfo.getBlobType() == BlobInfo.BlobType.Java) {
                codeVersionIds.addAll(JsonUtils.readStringAsObject(blobInfo.getLibraryVersionIds(), new TypeReference<List<Long>>() {}));
                codeGaIds.addAll(JsonUtils.readStringAsObject(blobInfo.getLibraryGroupArtifactIds(), new TypeReference<List<Long>>() {}));
            } else if (blobInfo.getBlobType() == BlobInfo.BlobType.POM) {
                pomVersionIds.addAll(JsonUtils.readStringAsObject(blobInfo.getLibraryVersionIds(), new TypeReference<List<Long>>() {}));
                pomGaIds.addAll(JsonUtils.readStringAsObject(blobInfo.getLibraryGroupArtifactIds(), new TypeReference<List<Long>>() {}));
            }
        }
        commitInfo.setCodeLibraryVersionIds(JsonUtils.writeObjectAsString(new ArrayList<>(codeVersionIds)));
        commitInfo.setCodeGroupArtifactIds(JsonUtils.writeObjectAsString(new ArrayList<>(codeGaIds)));
        commitInfo.setPomLibraryVersionIds(JsonUtils.writeObjectAsString(new ArrayList<>(pomVersionIds)));
        commitInfo.setPomGroupArtifactIds(JsonUtils.writeObjectAsString(new ArrayList<>(pomGaIds)));
        saveCommitInfo(repository, commitInfo);
        return commitInfo;
    }

    private BlobInfo analyzeBlobInfo(AbstractRepository repository, BlobInCommit blob) {
        BlobInfo blobInfo = getBlobInfo(repository, blob.blobId);
        if(blobInfo == null) {
            LOG.debug("new blob: {}", blob.blobId);
            blobInfo = new BlobInfo();
            blobInfo.setBlobId(blob.blobId);
        } else {
            LOG.debug("exist blob: {}, blobType = {}", blobInfo.getBlobId(), blobInfo.getBlobType());
            return blobInfo;
        }
        Set<Long> signatureIds = new HashSet<>();
        Set<Long> versionIds = new HashSet<>();
        Set<Long> groupArtifactIds = new HashSet<>();
        if(isBlobJavaCode(blob)) {
            blobInfo.setBlobType(BlobInfo.BlobType.Java);
            try {
                LOG.debug("begin analyzeJavaContent blobId = {}", blob.blobId);
                String content = getBlobContent(repository, blob.blobId);
                analyzeJavaContent(content, signatureIds, versionIds, groupArtifactIds);
                LOG.debug("end analyzeJavaContent blobId = {}", blob.blobId);
            } catch (Exception e) {
                LOG.error("analyzeJavaContent fail", e);
                // not java content
                LOG.warn("blob is not java content, set to other, blobId = {}", blobInfo.getBlobId());
                blobInfo.setBlobType(BlobInfo.BlobType.Other);
                signatureIds.clear();
                versionIds.clear();
                groupArtifactIds.clear();
            }
        } else if (isBlobPom(blob)) {
            blobInfo.setBlobType(BlobInfo.BlobType.POM);
            try {
                LOG.debug("begin analyzePomContent blobId = {}", blob.blobId);
                String content = getBlobContent(repository, blob.blobId);
                analyzePomContent(content, signatureIds, versionIds, groupArtifactIds);
                LOG.debug("end analyzePomContent blobId = {}", blob.blobId);
            } catch (Exception e) {
                LOG.error("analyzePomContent fail", e);
                // not pom content
                LOG.warn("blob is not pom content, set to other, blobId = {}", blobInfo.getBlobId());
                blobInfo.setBlobType(BlobInfo.BlobType.Other);
                signatureIds.clear();
                versionIds.clear();
                groupArtifactIds.clear();
            }
        } else {
            blobInfo.setBlobType(BlobInfo.BlobType.Other);
        }
        blobInfo.setLibrarySignatureIds(JsonUtils.writeObjectAsString(new ArrayList<>(signatureIds)));
        blobInfo.setLibraryVersionIds(JsonUtils.writeObjectAsString(new ArrayList<>(versionIds)));
        blobInfo.setLibraryGroupArtifactIds(JsonUtils.writeObjectAsString(new ArrayList<>(groupArtifactIds)));
        saveBlobInfo(repository, blobInfo);
        return blobInfo;
    }

    public void analyzeJavaContent(String content, Set<Long> signatureIds, Set<Long> versionIds, Set<Long> groupArtifactIds) {
        List<MethodSignature> signatureList = javaCodeAnalysisService.analyzeJavaCode(content);
        Set<Long> sids = new HashSet<>();
        for (MethodSignature methodSignature : signatureList) {
            Long id = methodSignatureMapper.findId(
                    methodSignature.getPackageName(), methodSignature.getClassName(),
                    methodSignature.getMethodName(), methodSignature.getParamList());
            if(id != null) {
                sids.add(id);
            } else {
                List<MethodSignature> candidates = methodSignatureMapper.findList(
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
        List<Long> vids = librarySignatureMapMapper.findVersionIds(sids);
        versionIds.addAll(vids);
        groupArtifactIds.addAll(libraryVersionMapper.findGroupArtifactIds(vids));
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
