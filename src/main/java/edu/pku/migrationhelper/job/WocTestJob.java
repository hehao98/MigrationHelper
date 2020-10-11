package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.service.GitRepositoryAnalysisService;
import edu.pku.migrationhelper.service.RepositoryAnalysisService;
import edu.pku.migrationhelper.service.WocRepositoryAnalysisService;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Deprecated
// @Component
// @ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "WocTestJob")
public class WocTestJob {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private GitRepositoryAnalysisService gitRepositoryAnalysisService;

    @Autowired
    private WocRepositoryAnalysisService wocRepositoryAnalysisService;

    @EventListener(ApplicationReadyEvent.class)
    public void run() throws Exception {
        RepositoryAnalysisService.AbstractRepository git = gitRepositoryAnalysisService.openRepository("jgit-cookbook");
        RepositoryAnalysisService.AbstractRepository woc = wocRepositoryAnalysisService.openRepository("centic9_jgit-cookbook");
        testForEachCommit(git, woc);
        testGetCommitParents(git, woc);
        testGetBlobsInCommit(git, woc);
        testGetBlobContent(git, woc);
        gitRepositoryAnalysisService.closeRepository(git);
        wocRepositoryAnalysisService.closeRepository(woc);
    }

    public void testForEachCommit(RepositoryAnalysisService.AbstractRepository git, RepositoryAnalysisService.AbstractRepository woc){
        Set<String> gitCommits = new HashSet<>();
        Set<String> wocCommits = new HashSet<>();
        gitRepositoryAnalysisService.forEachCommit(git, gitCommits::add);
        wocRepositoryAnalysisService.forEachCommit(woc, wocCommits::add);
        Set<String> gitDiffs = new HashSet<>(gitCommits);
        gitDiffs.removeAll(wocCommits);
        Set<String> wocDiffs = new HashSet<>(wocCommits);
        wocDiffs.removeAll(gitCommits);
        LOG.info("[testForEachCommit] git commit size = {}, woc commit size = {}, git diff size = {}, woc diff size = {}",
                gitCommits.size(), wocCommits.size(), gitDiffs.size(), wocDiffs.size());
        if(gitDiffs.size() > 0) {
            LOG.info("git diff:");
            gitDiffs.forEach(LOG::info);
        }
        if(wocDiffs.size() > 0) {
            LOG.info("woc diff:");
            wocDiffs.forEach(LOG::info);
        }
    }

    public void testGetCommitParents(RepositoryAnalysisService.AbstractRepository git, RepositoryAnalysisService.AbstractRepository woc){
        int fail = 0;
        fail += testGetCommitParents0(git, woc, "256d3387cbb318f91e50a33f83d3b96102947370") ? 0 : 1; // init commit
        fail += testGetCommitParents0(git, woc, "f148a699e8330b95263e560a56353bf81bc57670") ? 0 : 1; // merge commit
        fail += testGetCommitParents0(git, woc, "05d18a76875716fbdbd2c200091b40caa06c713d") ? 0 : 1;
        fail += testGetCommitParents0(git, woc, "766869389980f912ec17d27884fc79325260eb97") ? 0 : 1; // merge commit
        fail += testGetCommitParents0(git, woc, "360ee9b676611cb24d41692d2ae8461abcd7ae19") ? 0 : 1;
        LOG.info("[testGetCommitParents] total = {}, fail = {}", 5, fail);
    }

    public boolean testGetCommitParents0(RepositoryAnalysisService.AbstractRepository git, RepositoryAnalysisService.AbstractRepository woc, String commitId) {
        Set<String> gitCommits = new HashSet<>(gitRepositoryAnalysisService.getCommitParents(git, commitId));
        Set<String> wocCommits = new HashSet<>(wocRepositoryAnalysisService.getCommitParents(woc, commitId));
        Set<String> diff = new HashSet<>(gitCommits);
        diff.removeAll(wocCommits);
        if(diff.size() != 0 || gitCommits.size() != wocCommits.size()) {
            LOG.info("testGetCommitParents0 fail, commitId = {}", commitId);
            LOG.info("git:");
            gitCommits.forEach(LOG::info);
            LOG.info("woc:");
            wocCommits.forEach(LOG::info);
            return false;
        }
        return true;
    }

    public void testGetBlobsInCommit(RepositoryAnalysisService.AbstractRepository git, RepositoryAnalysisService.AbstractRepository woc){
        int fail = 0;
        fail += testGetBlobsInCommit0(git, woc, "256d3387cbb318f91e50a33f83d3b96102947370") ? 0 : 1;
        fail += testGetBlobsInCommit0(git, woc, "f148a699e8330b95263e560a56353bf81bc57670") ? 0 : 1;
        fail += testGetBlobsInCommit0(git, woc, "05d18a76875716fbdbd2c200091b40caa06c713d") ? 0 : 1;
        fail += testGetBlobsInCommit0(git, woc, "766869389980f912ec17d27884fc79325260eb97") ? 0 : 1;
        fail += testGetBlobsInCommit0(git, woc, "360ee9b676611cb24d41692d2ae8461abcd7ae19") ? 0 : 1;
        LOG.info("[testGetBlobsInCommit] total = {}, fail = {}", 5, fail);
    }

    public boolean testGetBlobsInCommit0(RepositoryAnalysisService.AbstractRepository git, RepositoryAnalysisService.AbstractRepository woc, String commitId) {
        List<RepositoryAnalysisService.BlobInCommit> gitBlobs = gitRepositoryAnalysisService.getBlobsInCommit(git, commitId);
        List<RepositoryAnalysisService.BlobInCommit> wocBlobs = wocRepositoryAnalysisService.getBlobsInCommit(woc, commitId);
        MutableBoolean success = new MutableBoolean(true);
        Map<String, String> gitBlobMap = new HashMap<>();
        gitBlobs.forEach(blob -> gitBlobMap.put(blob.blobId, blob.fileName));
        wocBlobs.forEach(blob -> {
            if(gitBlobMap.containsKey(blob.blobId)) {
                String fileName = gitBlobMap.remove(blob.blobId);
                if(!Objects.equals(fileName, blob.fileName)) {
                    success.setFalse();
                    LOG.info("testGetBlobsInCommit0 fail, commitId = {}, fileName not equal blobId = {}, gitName = {}, wocName = {}",
                            commitId, blob.blobId, fileName, blob.fileName);
                }
            } else {
                success.setFalse();
                LOG.info("testGetBlobsInCommit0 fail, commitId = {}, missing git blob = {}", commitId, blob.blobId);
            }
        });
        gitBlobMap.forEach((blobId, fileName) -> {
            success.setFalse();
            LOG.info("testGetBlobsInCommit0 fail, commitId = {}, missing woc blob = {}", commitId, blobId);
        });
        return success.booleanValue();
    }

    public void testGetBlobContent(RepositoryAnalysisService.AbstractRepository git, RepositoryAnalysisService.AbstractRepository woc){
        int fail = 0;
        fail += testGetBlobContent0(git, woc, "bf8a1537aa59312f603649694368e9592b3a83be") ? 0 : 1;
        fail += testGetBlobContent0(git, woc, "bf8a1537aa59312f603649694368e9592b3a83be") ? 0 : 1;
        fail += testGetBlobContent0(git, woc, "42ca2bdff8647a62c3a1bda548c4cc4f5ab53035") ? 0 : 1;
        fail += testGetBlobContent0(git, woc, "5f6331f26a0ff55d89ae3a30788f9b72633a6221") ? 0 : 1;
        fail += testGetBlobContent0(git, woc, "9153bcfd7aaf12b6fcbe3e1246cdac0e04a0cfa0") ? 0 : 1;
        LOG.info("[testGetBlobContent] total = {}, fail = {}", 5, fail);
    }

    public boolean testGetBlobContent0(RepositoryAnalysisService.AbstractRepository git, RepositoryAnalysisService.AbstractRepository woc, String blobId) {
        String gitContent = gitRepositoryAnalysisService.getBlobContent(git, blobId);
        String wocContent = wocRepositoryAnalysisService.getBlobContent(woc, blobId);
        if(!Objects.equals(gitContent, wocContent)) {
            LOG.info("testGetBlobContent0 fail, blobId = {}", blobId);
            return false;
        }
        return true;
    }
}
