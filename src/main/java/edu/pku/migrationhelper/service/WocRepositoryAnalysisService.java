package edu.pku.migrationhelper.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by xuyul on 2020/2/4.
 */
@Service
public class WocRepositoryAnalysisService extends RepositoryAnalysisService {
    @Override
    public AbstractRepository openRepository(String repositoryName) {
        return null;
    }

    @Override
    public void closeRepository(AbstractRepository repository) {

    }

    @Override
    public void forEachCommit(AbstractRepository repository, Consumer<String> commitIdConsumer) {

    }

    @Override
    public List<String> getCommitParents(AbstractRepository repository, String commitId) {
        return null;
    }

    @Override
    public List<BlobInCommit> getBlobsInCommit(AbstractRepository repository, String commitId) {
        return null;
    }

    @Override
    public String getBlobContent(AbstractRepository repository, String blobId) {
        return null;
    }
}
