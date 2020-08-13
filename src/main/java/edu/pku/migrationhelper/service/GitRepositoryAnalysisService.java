package edu.pku.migrationhelper.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

@Deprecated
@Service
public class GitRepositoryAnalysisService extends RepositoryAnalysisService {

    @Value("${migration-helper.git-repository-analysis.repository-path}")
    private String repositoryPath;

    public static class GitRepository extends AbstractRepository {
        public Repository repository;

    }

    @Override
    public AbstractRepository openRepository(String repositoryName) {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            GitRepository gitRepository = new GitRepository();
            gitRepository.repositoryName = repositoryName;
            File repositoryDir = new File(repositoryPath);
            repositoryDir = new File(repositoryDir, repositoryName);
            repositoryDir = new File(repositoryDir, ".git");
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
        try(Git git = new Git(repository)) {
            Iterable<RevCommit> commits = git.log().all().call();
            for( RevCommit commit : commits ) {
                commitIdConsumer.accept(commit.name());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void forEachCommit(AbstractRepository repo, Consumer<String> commitIdConsumer, int offset, int limit) {
        GitRepository gitRepository = (GitRepository) repo;
        Repository repository = gitRepository.repository;
        try(Git git = new Git(repository)) {
            Iterable<RevCommit> commits = git.log().all().call();
            int i = 0;
            for( RevCommit commit : commits ) {
                if(i++ < offset) continue;
                if(i > offset + limit) break;
                commitIdConsumer.accept(commit.name());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getCommitParents(AbstractRepository repo, String commitId) {
        try {
            GitRepository gitRepository = (GitRepository) repo;
            Repository repository = gitRepository.repository;
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(ObjectId.fromString(commitId));
                RevCommit[] parents = commit.getParents();
                List<String> result = new ArrayList<>(parents.length);
                for (RevCommit parent : parents) {
                    result.add(parent.name());
                }
                return result;
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
                        bic.blobId = treeWalk.getObjectId(0).getName();
                        result.add(bic);
                    }
                }
            }
            if(LOG.isDebugEnabled()) {
                LOG.debug("getBlobsInCommit result commitId = {}", commitId);
                for (BlobInCommit blobInCommit : result) {
                    LOG.debug("blobId = {}, fileName = {}", blobInCommit.blobId, blobInCommit.fileName);
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

    @Override
    public Integer getCommitTime(AbstractRepository repo, String commitId) {
        try {
            GitRepository gitRepository = (GitRepository) repo;
            Repository repository = gitRepository.repository;
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(ObjectId.fromString(commitId));
                if(commit == null) return null;
                return commit.getCommitTime();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
