package edu.pku.migrationhelper.service;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by xuyul on 2020/2/4.
 */
@Service
public class GitRepositoryAnalysisService extends RepositoryAnalysisService {

    @Value("${migration-helper.git-repository-analysis.repository-path}")
    private String repositoryPath;

    public static class GitRepository implements AbstractRepository {
        public Repository repository;
    }

    @Override
    public AbstractRepository openRepository(String repositoryName) {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            GitRepository gitRepository = new GitRepository();
            File repositoryDir = new File(repositoryPath);
            repositoryDir = new File(repositoryDir, repositoryName);
            gitRepository.repository = builder
                    .setGitDir(repositoryDir)
                    .readEnvironment()
                    .findGitDir()
                    .build();
            return gitRepository;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void closeRepository(AbstractRepository repository) {
        GitRepository gitRepository = (GitRepository) repository;
        gitRepository.repository.close();
    }

    @Override
    public void forEachCommit(AbstractRepository repo, Consumer<String> commitIdConsumer) {
        GitRepository gitRepository = (GitRepository) repo;
        Repository repository = gitRepository.repository;
        Collection<Ref> allRefs = repository.getAllRefs().values();
        try (RevWalk revWalk = new RevWalk( repository )) {
            for( Ref ref : allRefs ) {
                revWalk.markStart( revWalk.parseCommit( ref.getObjectId() ));
            }
            for( RevCommit commit : revWalk ) {
                commitIdConsumer.accept(commit.name());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<BlobInCommit> getBlobsInCommit(AbstractRepository repo, String commitId) {
        try {
            GitRepository gitRepository = (GitRepository) repo;
            Repository repository = gitRepository.repository;
            List<BlobInCommit> result = new LinkedList<>();
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(ObjectId.fromString(commitId));
                RevTree tree = commit.getTree();
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setPostOrderTraversal(false);
                    while(treeWalk.next()) {
                        BlobInCommit bic = new BlobInCommit();
                        bic.fileName = treeWalk.getPathString();
                        bic.blobId = treeWalk.getNameString();
                        result.add(bic);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBlobContent(AbstractRepository repo, String blobId) {
        try {
            GitRepository gitRepository = (GitRepository) repo;
            Repository repository = gitRepository.repository;
            ObjectLoader loader = repository.open(ObjectId.fromString(blobId));
            return new String(loader.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
