package edu.pku.migrationhelper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.pku.migrationhelper.data.BlobInfo;
import edu.pku.migrationhelper.data.CommitInfo;
import edu.pku.migrationhelper.data.LibraryVersion;
import edu.pku.migrationhelper.data.MethodSignature;
import edu.pku.migrationhelper.mapper.*;
import edu.pku.migrationhelper.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

    public static abstract class AbstractRepository {
        public Map<String, BlobInfo> blobCache = new HashMap<>();
    }

    public static class BlobInCommit {
        public String blobId;
        public String fileName;
    }

    public abstract AbstractRepository openRepository(String repositoryName);

    public abstract void closeRepository(AbstractRepository repository);

    public abstract void forEachCommit(AbstractRepository repository, Consumer<String> commitIdConsumer);

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

    public void analyzeRepositoryLibrary(String repositoryName) {
        AbstractRepository repository = openRepository(repositoryName);
        if(repository == null) {
            throw new RuntimeException("open repository fail");
        }
        forEachCommit(repository, commitId -> {
            if(commitInfoMapper.findByCommitId(commitId) != null) {
                return;
            }
            CommitInfo commitInfo = new CommitInfo();
            commitInfo.setCommitId(commitId);
            List<BlobInCommit> blobs = getBlobsInCommit(repository, commitId);
            Set<Long> codeIds = new HashSet<>();
            Set<Long> pomIds = new HashSet<>();
            for (BlobInCommit blob : blobs) {
                BlobInfo blobInfo = getBlobInfo(repository, blob.blobId);
                if(blobInfo == null) {
                    LOG.debug("new blob: {}", blob.blobId);
                    blobInfo = new BlobInfo();
                    blobInfo.setBlobId(blob.blobId);
                } else {
                    LOG.debug("exist blob: {}, blobType = {}", blobInfo.getBlobId(), blobInfo.getBlobType());
                    if(blobInfo.getBlobType() == BlobInfo.BlobType.Java) {
                        codeIds.addAll(JsonUtils.readStringAsObject(blobInfo.getLibraryVersionIds(), new TypeReference<List<Long>>() {}));
                    } else if (blobInfo.getBlobType() == BlobInfo.BlobType.POM) {
                        pomIds.addAll(JsonUtils.readStringAsObject(blobInfo.getLibraryVersionIds(), new TypeReference<List<Long>>() {}));
                    }
                    continue;
                }
                List<Long> signatureIds = new LinkedList<>();
                List<Long> versionIds = new LinkedList<>();
                if(isBlobJavaCode(blob)) {
                    blobInfo.setBlobType(BlobInfo.BlobType.Java);
                    try {
                        LOG.debug("begin analyzeJavaContent blobId = {}", blob.blobId);
                        String content = getBlobContent(repository, blob.blobId);
                        analyzeJavaContent(content, signatureIds, versionIds);
                        LOG.debug("end analyzeJavaContent blobId = {}", blob.blobId);
                    } catch (Exception e) {
                        LOG.error("analyzeJavaContent fail", e);
                        // not java content
                        LOG.warn("blob is not java content, set to other, blobId = {}", blobInfo.getBlobId());
                        blobInfo.setBlobType(BlobInfo.BlobType.Other);
                        signatureIds.clear();
                        versionIds.clear();
                    }
                    codeIds.addAll(versionIds);
                } else if (isBlobPom(blob)) {
                    blobInfo.setBlobType(BlobInfo.BlobType.POM);
                    try {
                        LOG.debug("begin analyzePomContent blobId = {}", blob.blobId);
                        String content = getBlobContent(repository, blob.blobId);
                        analyzePomContent(content, signatureIds, versionIds);
                        LOG.debug("end analyzePomContent blobId = {}", blob.blobId);
                    } catch (Exception e) {
                        LOG.error("analyzePomContent fail", e);
                        // not pom content
                        LOG.warn("blob is not pom content, set to other, blobId = {}", blobInfo.getBlobId());
                        blobInfo.setBlobType(BlobInfo.BlobType.Other);
                        signatureIds.clear();
                        versionIds.clear();
                    }
                    pomIds.addAll(versionIds);
                } else {
                    blobInfo.setBlobType(BlobInfo.BlobType.Other);
                }
                blobInfo.setLibrarySignatureIds(JsonUtils.writeObjectAsString(signatureIds));
                blobInfo.setLibraryVersionIds(JsonUtils.writeObjectAsString(versionIds));
                saveBlobInfo(repository, blobInfo);
            }
            commitInfo.setCodeLibraryVersionIds(JsonUtils.writeObjectAsString(new ArrayList<>(codeIds)));
            commitInfo.setPomLibraryVersionIds(JsonUtils.writeObjectAsString(new ArrayList<>(pomIds)));
            commitInfoMapper.insert(Collections.singletonList(commitInfo));
        });
        closeRepository(repository);
    }

    public void analyzeJavaContent(String content, List<Long> signatureIds, List<Long> versionIds) {
        List<MethodSignature> signatureList = javaCodeAnalysisService.analyzeJavaCode(content);
        for (MethodSignature methodSignature : signatureList) {
            Long id = methodSignatureMapper.findId(
                    methodSignature.getPackageName(), methodSignature.getClassName(),
                    methodSignature.getMethodName(), methodSignature.getParamList());
            if(id != null) {
                signatureIds.add(id);
            } else {
                List<Long> ids = methodSignatureMapper.findIds(
                        methodSignature.getPackageName(), methodSignature.getClassName(),
                        methodSignature.getMethodName());
                signatureIds.addAll(ids);
            }
        }
        versionIds.addAll(librarySignatureMapMapper.findVersionIds(signatureIds));
    }

    public void analyzePomContent(String content, List<Long> signatureIds, List<Long> versionIds) throws Exception {
        List<PomAnalysisService.LibraryInfo> libraryInfoList = pomAnalysisService.analyzePom(content);
        for (PomAnalysisService.LibraryInfo libraryInfo : libraryInfoList) {
            LibraryVersion libraryVersion = libraryVersionMapper.findByGroupIdAndArtifactIdAndVersion(
                    libraryInfo.groupId, libraryInfo.artifactId, libraryInfo.version);
            if(libraryVersion != null) {
                versionIds.add(libraryVersion.getId());
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
