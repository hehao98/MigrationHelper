package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.woc.WocHdbDriver;
import edu.pku.migrationhelper.woc.WocObjectDriver;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by xuyul on 2020/2/4.
 */
@Service
@ConfigurationProperties(prefix = "migration-helper.woc-repository-analysis")
public class WocRepositoryAnalysisService extends RepositoryAnalysisService {

    public static class WocRepository extends AbstractRepository {
        public WocObjectDriver blobDriver;
    }

    private WocHdbDriver p2c;

    private WocHdbDriver c2pc;

    private WocHdbDriver c2b;

    private WocHdbDriver b2f;

    private WocHdbDriver blobIndex;

    @PostConstruct
    public void postConstruct() {
        p2c = new WocHdbDriver(p2cBase, 32, WocHdbDriver.ContentType.Text, WocHdbDriver.ContentType.SHA1List);
        c2pc = new WocHdbDriver(c2pcBase, 32, WocHdbDriver.ContentType.SHA1, WocHdbDriver.ContentType.SHA1List);
        c2b = new WocHdbDriver(c2bBase, 32, WocHdbDriver.ContentType.SHA1, WocHdbDriver.ContentType.SHA1List);
        b2f = new WocHdbDriver(b2fBase, 32, WocHdbDriver.ContentType.SHA1, WocHdbDriver.ContentType.LZFText);
        blobIndex = new WocHdbDriver(blobIndexBase, 128, WocHdbDriver.ContentType.SHA1, WocHdbDriver.ContentType.BerNumberList);
        p2c.openDatabaseFile();
        c2pc.openDatabaseFile();
        c2b.openDatabaseFile();
        b2f.openDatabaseFile();
        blobIndex.openDatabaseFile();
    }

    @Override
    public AbstractRepository openRepository(String repositoryName) {
        WocRepository repository = new WocRepository();
        repository.repositoryName = repositoryName;
        repository.blobDriver = new WocObjectDriver(blobContentBase, 128);
        repository.blobDriver.openDatabaseFile();
        return repository;
    }

    @Override
    public void closeRepository(AbstractRepository repo) {
        WocRepository repository = (WocRepository) repo;
        repository.blobDriver.closeDatabaseFile();
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
    public List<String> getCommitParents(AbstractRepository repo, String commitId) {
        List<String> result = c2pc.getSHA1ListValue(commitId);
        if(result == null) return Collections.emptyList();
        return result;
    }

    @Override
    public List<BlobInCommit> getBlobsInCommit(AbstractRepository repo, String commitId) {
        List<String> blobIds = c2b.getSHA1ListValue(commitId);
        if(blobIds == null) return Collections.emptyList();
        List<BlobInCommit> result = new ArrayList<>(blobIds.size());
        for (String blobId : blobIds) {
            BlobInCommit bic = new BlobInCommit();
            bic.blobId = blobId;
            String fileNames = b2f.getValue(blobId);
            if(fileNames == null) {
                bic.fileName = "";
            } else {
                String[] fileNameList = fileNames.split(";");
                if(fileNameList.length > 0) {
                    bic.fileName = fileNameList[0];
                } else {
                    bic.fileName = fileNames;
                }
            }
            result.add(bic);
        }
        return result;
    }

    @Override
    public String getBlobContent(AbstractRepository repo, String blobId) {
        WocRepository repository = (WocRepository) repo;
        List<Long> offsetLength = blobIndex.getBerNumberListValue(blobId);
        if(offsetLength.size() != 2) {
            throw new RuntimeException("offsetLength.size() != 2");
        }
        try {
            return repository.blobDriver.getLZFString(blobId, offsetLength.get(0), offsetLength.get(1).intValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * configuration fields
     */

    private String p2cBase;

    private String c2pcBase;

    private String c2bBase;

    private String b2fBase;

    private String blobIndexBase;

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

    public String getBlobIndexBase() {
        return blobIndexBase;
    }

    public WocRepositoryAnalysisService setBlobIndexBase(String blobIndexBase) {
        this.blobIndexBase = blobIndexBase;
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
