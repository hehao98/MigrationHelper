package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.util.MathUtils;
import edu.pku.migrationhelper.woc.WocHdbDriver;
import edu.pku.migrationhelper.woc.WocObjectDriver;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by xuyul on 2020/2/4.
 */
@Service
@ConfigurationProperties(prefix = "migration-helper.woc-repository-analysis")
public class WocRepositoryAnalysisService extends RepositoryAnalysisService {

    @Value("${migration-helper.woc.enabled}")
    private boolean wocEnabled = false;

    @Value("${migration-helper.woc.object-enabled}")
    private boolean wocObjectEnabled = false;

    public static class WocRepository extends AbstractRepository {
        public WocObjectDriver blobDriver;
        public Map<String, List<BlobInCommit>> treeCache = new HashMap<>(100000);
    }

    private WocHdbDriver p2c;

    private WocHdbDriver c2pc;

//    private WocHdbDriver c2b;

//    private WocHdbDriver b2f;

    private WocHdbDriver c2ta;

    private WocHdbDriver blobIndex;

    private WocHdbDriver commitIndex;

    private WocHdbDriver treeIndex;

    @PostConstruct
    public void postConstruct() {
        if(!wocEnabled) return;
        p2c = new WocHdbDriver(p2cBase, 32, WocHdbDriver.ContentType.Text, WocHdbDriver.ContentType.SHA1List);
        c2pc = new WocHdbDriver(c2pcBase, 32, WocHdbDriver.ContentType.SHA1, WocHdbDriver.ContentType.SHA1List);
//        c2b = new WocHdbDriver(c2bBase, 32, WocHdbDriver.ContentType.SHA1, WocHdbDriver.ContentType.SHA1List);
//        b2f = new WocHdbDriver(b2fBase, 32, WocHdbDriver.ContentType.SHA1, WocHdbDriver.ContentType.LZFText);
        c2ta = new WocHdbDriver(c2taBase, 32, WocHdbDriver.ContentType.SHA1, WocHdbDriver.ContentType.Text);
        p2c.openDatabaseFile(true);
        c2pc.openDatabaseFile();
//        c2b.openDatabaseFile();
//        b2f.openDatabaseFile();
        c2ta.openDatabaseFile();
        if(!wocObjectEnabled) return;
        blobIndex = new WocHdbDriver(blobIndexBase, 128, WocHdbDriver.ContentType.SHA1, WocHdbDriver.ContentType.BerNumberList);
        commitIndex = new WocHdbDriver(commitIndexBase, 128, WocHdbDriver.ContentType.SHA1, WocHdbDriver.ContentType.LZFText);
        treeIndex = new WocHdbDriver(treeIndexBase, 128, WocHdbDriver.ContentType.SHA1, WocHdbDriver.ContentType.LZFText);
        blobIndex.openDatabaseFile();
        commitIndex.openDatabaseFile();
        treeIndex.openDatabaseFile();
    }

    public void closeAllWocDatabase() {
        if(!wocEnabled) return;
        p2c.closeDatabaseFile();
        c2pc.closeDatabaseFile();
//        c2b.closeDatabaseFile();
//        b2f.closeDatabaseFile();
        c2ta.closeDatabaseFile();
        if(!wocObjectEnabled) return;
        blobIndex.closeDatabaseFile();
        commitIndex.closeDatabaseFile();
        treeIndex.closeDatabaseFile();
    }

    @Override
    public AbstractRepository openRepository(String repositoryName) {
        WocRepository repository = new WocRepository();
        repository.repositoryName = repositoryName;
        if(wocObjectEnabled) {
            repository.blobDriver = new WocObjectDriver(blobContentBase, 128);
            repository.blobDriver.openDatabaseFile();
        } else {
            repository.blobDriver = null;
        }
        return repository;
    }

    @Override
    public void closeRepository(AbstractRepository repo) {
        WocRepository repository = (WocRepository) repo;
        repository.treeCache = null;
        repository.blobCache = null;
        repository.commitCache = null;
        if(repository.blobDriver != null) {
            repository.blobDriver.closeDatabaseFile();
            repository.blobDriver = null;
        }
    }

    @Override
    public void forEachCommit(AbstractRepository repo, Consumer<String> commitIdConsumer) {
        WocRepository repository = (WocRepository) repo;
        List<String> commitIds = p2c.getSHA1ListValue(repository.repositoryName);
        if(commitIds == null) {
            LOG.warn("no commit found, repositoryName = {}", repository.repositoryName);
            return;
        }
        commitIds.forEach(commitIdConsumer);
    }

    @Override
    public void forEachCommit(AbstractRepository repo, Consumer<String> commitIdConsumer, int offset, int limit) {
        WocRepository repository = (WocRepository) repo;
        List<String> commitIds = p2c.getSHA1ListValue(repository.repositoryName);
        if(commitIds == null) {
            LOG.warn("no commit found, repositoryName = {}", repository.repositoryName);
            return;
        }
        commitIds.stream()
                .skip(offset)
                .limit(limit)
                .forEach(commitIdConsumer);
    }

    @Override
    public List<String> getCommitParents(AbstractRepository repo, String commitId) {
        List<String> result = c2pc.getSHA1ListValue(commitId);
        if(result == null) return Collections.emptyList();
        return result;
    }

    @Override
    public List<BlobInCommit> getBlobsInCommit(AbstractRepository repo, String commitId) {
        String commitContent = commitIndex.getValue(commitId);
        if(commitContent == null) return null; // TODO
        String[] attrLine = commitContent.split("\n");
        String treeId = null;
        for (String line : attrLine) {
            if(line.startsWith("tree")) {
                treeId = line.substring(5, 45);
                break;
            }
        }
        if (treeId == null) return null; // TODO
        return getBlobsInTree((WocRepository) repo, treeId, "");
    }

    public List<BlobInCommit> getBlobsInTree(WocRepository repository, String treeId, String parentPath) {
        if(repository.treeCache.containsKey(treeId)) {
            return new ArrayList<>(repository.treeCache.get(treeId));
        }
        List<BlobInCommit> result = new LinkedList<>();
        byte[] treeContent = treeIndex.getValueBytes(treeId);
        if(treeContent == null) return null; // TODO
        int p = 0, len = treeContent.length;
        while(p < len) {
            int start = p;
            while(p < len && treeContent[p++] != 0x20);
            String mode = new String(treeContent, start, p - start - 1);
            boolean isTree = "40000".equals(mode);
            start = p;
            while(p < len && treeContent[p++] != 0x00);
            String fileName = new String(treeContent, start, p - start - 1);
            String objectId = MathUtils.toHexString(treeContent, p, 20);
            p += 20;
            if(isTree) {
                List<BlobInCommit> subTreeResult = getBlobsInTree(repository, objectId, parentPath + fileName + "/");
                if(subTreeResult == null) return null; // TODO
                result.addAll(subTreeResult);
            } else {
                BlobInCommit bic = new BlobInCommit();
                bic.fileName = parentPath + fileName;
                bic.blobId = objectId;
                result.add(bic);
            }
        }
        repository.treeCache.put(treeId, new ArrayList<>(result));
        return result;
    }

    @Override
    public String getBlobContent(AbstractRepository repo, String blobId) {
        WocRepository repository = (WocRepository) repo;
        List<Long> offsetLength = blobIndex.getBerNumberListValue(blobId);
        if(offsetLength == null) return null; // TODO
        if(offsetLength.size() != 2) {
            throw new RuntimeException("offsetLength.size() != 2");
        }
        try {
            return repository.blobDriver.getLZFString(blobId, offsetLength.get(0), offsetLength.get(1).intValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Integer getCommitTime(AbstractRepository repository, String commitId) {
        String ta = c2ta.getValue(commitId);
        if(ta == null) return null;
        String[] attrs = ta.split(";");
        if(attrs.length < 2) return null;
        try {
            return Integer.parseInt(attrs[0]);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * configuration fields
     */

    private String p2cBase;

    private String c2pcBase;

    private String c2bBase;

    private String b2fBase;

    private String c2taBase;

    private String blobIndexBase;

    private String commitIndexBase;

    private String treeIndexBase;

    private String blobContentBase;

    public String getP2cBase() {
        return p2cBase;
    }

    public WocRepositoryAnalysisService setP2cBase(String p2cBase) {
        this.p2cBase = p2cBase;
        return this;
    }

    public String getC2pcBase() {
        return c2pcBase;
    }

    public WocRepositoryAnalysisService setC2pcBase(String c2pcBase) {
        this.c2pcBase = c2pcBase;
        return this;
    }

    public String getC2bBase() {
        return c2bBase;
    }

    public WocRepositoryAnalysisService setC2bBase(String c2bBase) {
        this.c2bBase = c2bBase;
        return this;
    }

    public String getB2fBase() {
        return b2fBase;
    }

    public WocRepositoryAnalysisService setB2fBase(String b2fBase) {
        this.b2fBase = b2fBase;
        return this;
    }

    public String getC2taBase() {
        return c2taBase;
    }

    public WocRepositoryAnalysisService setC2taBase(String c2taBase) {
        this.c2taBase = c2taBase;
        return this;
    }

    public String getBlobIndexBase() {
        return blobIndexBase;
    }

    public WocRepositoryAnalysisService setBlobIndexBase(String blobIndexBase) {
        this.blobIndexBase = blobIndexBase;
        return this;
    }

    public String getCommitIndexBase() {
        return commitIndexBase;
    }

    public WocRepositoryAnalysisService setCommitIndexBase(String commitIndexBase) {
        this.commitIndexBase = commitIndexBase;
        return this;
    }

    public String getTreeIndexBase() {
        return treeIndexBase;
    }

    public WocRepositoryAnalysisService setTreeIndexBase(String treeIndexBase) {
        this.treeIndexBase = treeIndexBase;
        return this;
    }

    public String getBlobContentBase() {
        return blobContentBase;
    }

    public WocRepositoryAnalysisService setBlobContentBase(String blobContentBase) {
        this.blobContentBase = blobContentBase;
        return this;
    }
}
