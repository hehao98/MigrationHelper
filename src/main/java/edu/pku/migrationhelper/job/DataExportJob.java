package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.*;
import edu.pku.migrationhelper.mapper.*;
import edu.pku.migrationhelper.service.GitObjectStorageService;
import edu.pku.migrationhelper.service.GitRepositoryAnalysisService;
import edu.pku.migrationhelper.service.RepositoryAnalysisService;
import edu.pku.migrationhelper.service.WocRepositoryAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by xuyul on 2020/2/24.
 */
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "DataExportJob")
public class DataExportJob implements CommandLineRunner {

    @Value("${migration-helper.woc.enabled}")
    private boolean wocEnabled = false;

    @Value("${migration-helper.woc-repo-analysis-job.repository-list-file}")
    private String repositoryListFile;

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private BlobInfoMapper blobInfoMapper;

    @Autowired
    private CommitInfoMapper commitInfoMapper;

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private MethodChangeMapper methodChangeMapper;

    @Autowired
    private LioProjectWithRepositoryMapper lioProjectWithRepositoryMapper;

    @Autowired
    private LibraryOverlapMapper libraryOverlapMapper;

    @Autowired
    private RepositoryAnalyzeStatusMapper repositoryAnalyzeStatusMapper;

    @Autowired
    private WocRepositoryAnalysisService wocRepositoryAnalysisService;

    @Autowired
    private GitRepositoryAnalysisService gitRepositoryAnalysisService;

    @Autowired
    private GitObjectStorageService gitObjectStorageService;

    @Autowired
    @Qualifier("ThreadPool")
    private ExecutorService executorService;

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 2) {
            LOG.info("Usage: <DataType> <OutputFile>");
            return;
        }
        String dataType = args[0];
        String outputFile = args[1];
        FileWriter writer = new FileWriter(outputFile);
        switch (dataType) {
            case "BlobInfo": {
                exportBlobInfo(writer);
                break;
            }
            case "CommitInfo": {
                exportCommitInfo(writer);
                break;
            }
            case "MethodChangeCommit": {
                exportMethodChangeCommit(writer, args);
                break;
            }
            case "LibraryVersion": {
                exportLibraryVersion(writer);
                break;
            }
            case "LibraryGroupArtifact": {
                exportLibraryGroupArtifact(writer);
                break;
            }
            case "MethodChange": {
                exportMethodChange(writer);
                break;
            }
            case "MethodChangeDetail": {
                exportMethodChangeDetail(writer);
                break;
            }
            case "LioProject": {
                exportLioProject(writer);
                break;
            }
            case "LioProjectParseStatus": {
                exportLioProjectParseStatus(writer);
                break;
            }
            case "RepositoryDepSeq": {
                exportRepositoryDepSeq(writer, args);
                break;
            }
            case "CommitLibraryOverlap": {
                exportCommitLibraryOverlap(writer, args);
                break;
            }
            case "LibraryOverlap": {
                exportLibraryOverlap(writer, args);
                break;
            }
        }
        writer.close();
        LOG.info("Export success");
    }

    public void exportBlobInfo(FileWriter writer) throws Exception {
        outputLine(writer, "blobId", "blobType", "signatureCount", "groupArtifactCount");
        int tableNum = 0;
        long blobId = 0;
        long offset = 0;
        int limit = 1000;
        boolean end = false;
        while(!end) {
            LOG.info("start export table = {}, offset = {}", tableNum, offset);
            List<BlobInfo> blobList = blobInfoMapper.findList(tableNum, offset, limit);
            offset += blobList.size();
            end = blobList.size() < limit;
            if(end && tableNum < 127) {
                end = false;
                offset = 0;
                tableNum++;
            }
            for (BlobInfo blobInfo : blobList) {
                outputLine(writer, ++blobId, blobInfo.getBlobType(),
                        blobInfo.getLibrarySignatureIdList().size(),
                        blobInfo.getLibraryGroupArtifactIdList().size());
            }
        }
    }

    public void exportCommitInfo(FileWriter writer) throws Exception {
        outputLine(writer, "commitId",
                "codeGa", "pomGa", "codeGaDiff", "pomGaDiff",
                "codeAddGa", "pomAddGa", "codeAddGaDiff", "pomAddGaDiff",
                "codeDelGa", "pomDelGa", "codeDelGaDiff", "pomDelGaDiff",
                "methodChangeCount");
        long commitId = 0;
        int tableNum = 0;
        long offset = 0;
        int limit = 1000;
        boolean end = false;
        while(!end) {
            LOG.info("start export table = {}, offset = {}", tableNum, offset);
            List<CommitInfo> commitList = commitInfoMapper.findList(tableNum, offset, limit);
            offset += commitList.size();
            end = commitList.size() < limit;
            if(end && tableNum < 127) {
                end = false;
                offset = 0;
                tableNum++;
            }
            for (CommitInfo commitInfo : commitList) {
                Set<Long> codeGa = buildSetFromList(commitInfo.getCodeGroupArtifactIdList());
                Set<Long> pomGa = buildSetFromList(commitInfo.getPomGroupArtifactIdList());
                Set<Long> codeAddGa = buildSetFromList(commitInfo.getCodeAddGroupArtifactIdList());
                Set<Long> pomAddGa = buildSetFromList(commitInfo.getPomAddGroupArtifactIdList());
                Set<Long> codeDelGa = buildSetFromList(commitInfo.getCodeDeleteGroupArtifactIdList());
                Set<Long> pomDelGa = buildSetFromList(commitInfo.getPomDeleteGroupArtifactIdList());
                outputLine(writer, ++commitId,
                        codeGa.size(), pomGa.size(), calcDiff(codeGa, pomGa), calcDiff(pomGa, codeGa),
                        codeAddGa.size(), pomAddGa.size(), calcDiff(codeAddGa, pomAddGa), calcDiff(pomAddGa, codeAddGa),
                        codeDelGa.size(), pomDelGa.size(), calcDiff(codeDelGa, pomDelGa), calcDiff(pomDelGa, codeDelGa),
                        getMethodChangeCount(commitInfo));
            }
        }
    }

    private long getMethodChangeCount(CommitInfo commitInfo) {
        List<Long> methodChangeIds = commitInfo.getMethodChangeIdList();
        if(methodChangeIds == null) return 0;
        Iterator<Long> it = methodChangeIds.iterator();
        long result = 0;
        while(it.hasNext()) {
            it.next(); // id
            if(it.hasNext()) {
                result += it.next(); // count
            }
        }
        return result;
    }

    private <T> Set<T> buildSetFromList(List<T> list) {
        if(list == null) return new HashSet<>();
        return new HashSet<>(list);
    }

    public void exportLibraryVersion(FileWriter writer) throws Exception {
        outputLine(writer, "groupArtifactId", "count");
        List<LibraryVersionMapper.CountData> counts = libraryVersionMapper.countByGroupArtifact();
        for (LibraryVersionMapper.CountData count : counts) {
            outputLine(writer, count.groupArtifactId, count.count);
        }
    }

    public void exportLibraryGroupArtifact(FileWriter writer) throws Exception {
        outputLine(writer, "id", "groupId", "artifactId");
        List<LibraryGroupArtifact> gaList = libraryGroupArtifactMapper.findAll();
        for (LibraryGroupArtifact ga : gaList) {
            outputLine(writer, ga.getId(), ga.getGroupId(), ga.getArtifactId());
        }
    }

    public void exportMethodChange(FileWriter writer) throws Exception {
        outputLine(writer, "methodChangeId", "delSigCount", "addSigCount", "delGACount", "addGACount", "counter");
        int tableNum = 0;
        long offset = 0;
        int limit = 10000;
        boolean end = false;
        while(!end) {
            LOG.info("start export table = {}, offset = {}", tableNum, offset);
            List<MethodChange> methodChangeList = methodChangeMapper.findList(tableNum, offset, limit);
            offset += methodChangeList.size();
            end = methodChangeList.size() < limit;
            if(end && tableNum < 127) {
                end = false;
                offset = 0;
                tableNum++;
            }
            for (MethodChange methodChange : methodChangeList) {
                outputLine(writer, methodChange.getId(),
                        methodChange.getDeleteSignatureIdList().size(),
                        methodChange.getAddSignatureIdList().size(),
                        methodChange.getDeleteGroupArtifactIdList().size(),
                        methodChange.getAddGroupArtifactIdList().size(),
                        methodChange.getCounter());
            }
        }
    }

    public void exportMethodChangeDetail(FileWriter writer) throws Exception {
        outputLine(writer, "methodChangeId", "delSig", "addSig", "delGA", "addGA", "counter");
        int tableNum = 0;
        long offset = 0;
        int limit = 10000;
        boolean end = false;
        while(!end) {
            LOG.info("start export table = {}, offset = {}", tableNum, offset);
            List<MethodChange> methodChangeList = methodChangeMapper.findList(tableNum, offset, limit);
            offset += methodChangeList.size();
            end = methodChangeList.size() < limit;
            if(end && tableNum < 127) {
                end = false;
                offset = 0;
                tableNum++;
            }
            for (MethodChange methodChange : methodChangeList) {
                outputLine(writer, methodChange.getId(),
                        concatList(methodChange.getDeleteSignatureIdList()),
                        concatList(methodChange.getAddSignatureIdList()),
                        concatList(methodChange.getDeleteGroupArtifactIdList()),
                        concatList(methodChange.getAddGroupArtifactIdList()),
                        methodChange.getCounter());
            }
        }
    }

    private <T> String concatList(List<T> list) {
        if(list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (T value : list) {
            sb.append(value);
            sb.append(";");
        }
        if(sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public void exportLioProject(FileWriter writer) throws Exception {
        outputLine(writer, "id", "sourceRank", "repositoryStarCount",
                "repositoryForkCount", "repositoryWatchersCount", "repositorySourceRank",
                "dependentProjectsCount", "dependentRepositoriesCount");
        long id = 0;
        int limit = 1000;
        boolean end = false;
        while(!end) {
            LOG.info("start export id = {}", id + 1);
            List<LioProjectWithRepository> projectList = lioProjectWithRepositoryMapper.findList(id, limit);
            end = projectList.size() < limit;
            for (LioProjectWithRepository project : projectList) {
                ++id;
                outputLine(writer, project.getId(), project.getSourceRank(), project.getRepositoryStarCount(),
                        project.getRepositoryForkCount(), project.getRepositoryWatchersCount(), project.getRepositorySourceRank(),
                        project.getDependentProjectsCount(), project.getDependentRepositoriesCount());
            }
        }
    }

    public void exportLioProjectParseStatus(FileWriter writer) throws Exception {
        outputLine(writer, "repositoryName", "status");
        BufferedReader reader = new BufferedReader(new FileReader(repositoryListFile));
        String line;
        while((line = reader.readLine()) != null) {
            String repoName = WocRepoAnalysisJob.getRepoNameFromUrl(line);
            if(repoName == null) continue;
            int status = wocEnabled ?
                    wocRepositoryAnalysisService.getRepositoryAnalyzeStatus(repoName) :
                    gitRepositoryAnalysisService.getRepositoryAnalyzeStatus(repoName);
            outputLine(writer, repoName, status);
        }
    }

    public void exportMethodChangeCommit(FileWriter writer, String... args) throws Exception {
        int projectLimit = 110;
        if(args.length >= 3) {
            projectLimit = Integer.parseInt(args[2]);
        }
        outputLine(writer, "repositoryName", "commitSha1",
                "codeDepAdd", "codeDepDel", "pomDepAdd", "pomDepDel", "APIChangeBlocks");
        BufferedReader reader = new BufferedReader(new FileReader(repositoryListFile));
        String line;
        while((line = reader.readLine()) != null) {
            String repoName = WocRepoAnalysisJob.getRepoNameFromUrl(line);
            if(repoName == null) continue;
            if(projectLimit-- <= 0) break;
            LOG.info("dump project: {}, {}", repoName, projectLimit);
            List<CommitInfo> result = wocEnabled ?
                    wocRepositoryAnalysisService.getRepositoryDependencyChangeCommits(repoName) :
                    gitRepositoryAnalysisService.getRepositoryDependencyChangeCommits(repoName);
            for (CommitInfo commitInfo : result) {
                outputLine(writer, line, commitInfo.getCommitIdString(),
                        getListSizeSafe(commitInfo.getCodeAddGroupArtifactIdList()),
                        getListSizeSafe(commitInfo.getCodeDeleteGroupArtifactIdList()),
                        getListSizeSafe(commitInfo.getPomAddGroupArtifactIdList()),
                        getListSizeSafe(commitInfo.getPomDeleteGroupArtifactIdList()),
                        getListSizeSafe(commitInfo.getMethodChangeIdList()) / 2);
            }
        }
    }

    public void exportRepositoryDepSeq(FileWriter writer, String... args) throws Exception {
        int projectLimit = 1000;
        if(args.length >= 3) {
            projectLimit = Integer.parseInt(args[2]);
        }
        outputLine(writer, "id", "repositoryName", "pomOnly", "codeWithDup", "codeWithoutDup");
        BufferedReader reader = new BufferedReader(new FileReader(repositoryListFile));
        String line;
        Future[] futures = new Future[projectLimit];
        String[] repoNames = new String[projectLimit];
        while((line = reader.readLine()) != null) {
            int index = futures.length - projectLimit;
            String repoName = WocRepoAnalysisJob.getRepoNameFromUrl(line);
            if(repoName == null) continue;
            if(projectLimit-- <= 0) break;
            repoNames[index] = repoName;
            futures[index] = executorService.submit(() -> {
                LOG.info("dump project: {}, {}", repoName, index);
                RepositoryDepSeq result = wocEnabled ?
                        wocRepositoryAnalysisService.getRepositoryDepSeq(repoName) :
                        gitRepositoryAnalysisService.getRepositoryDepSeq(repoName);
                LOG.info("dump project success: {}, {}", repoName, index);
            });

        }
        for (int i = 0; i < futures.length; i++) {
            Future future = futures[i];
            String repoName = repoNames[i];
            if(future == null) continue;
            LOG.info("waiting project: {}, {}", repoName, i);
            try {
                future.get();
            } catch (Exception e) {
                LOG.error("waiting project error: " + repoName + " " + i, e);
                continue;
            }
            LOG.info("output project: {}, {}", repoName, i);
            RepositoryDepSeq result = wocEnabled ?
                    wocRepositoryAnalysisService.getRepositoryDepSeq(repoName) :
                    gitRepositoryAnalysisService.getRepositoryDepSeq(repoName);
            if(result == null) continue;

            outputLine(writer, result.getId(), repoName,
                    concatList(result.getPomOnlyList()),
                    concatList(result.getCodeWithDupList()),
                    concatList(result.getCodeWithoutDupList()));
        }
    }

    public void exportCommitLibraryOverlap(FileWriter writer, String... args) throws Exception {
        int projectOffset = 0;
        int projectLimit = 5000;
        int commitOffset = 0;
        int commitLimit = 10;
        if(args.length >= 3) {
            projectOffset = Integer.parseInt(args[2]);
        }
        if(args.length >= 4) {
            projectLimit = Integer.parseInt(args[3]);
        }
        if(args.length >= 5) {
            commitOffset = Integer.parseInt(args[4]);
        }
        if(args.length >= 6) {
            commitLimit = Integer.parseInt(args[5]);
        }
        LOG.info("build overlap map start");
        Map<Long, Set<Long>> overlapMap = new HashMap<>(100000);
        List<LibraryOverlap> overlapList = libraryOverlapMapper.findAll();
        for (LibraryOverlap overlap : overlapList) {
            Long id1 = overlap.getGroupArtifactId1();
            Long id2 = overlap.getGroupArtifactId2();
            overlapMap.computeIfAbsent(id1, k -> new HashSet<>()).add(id2);
            overlapMap.computeIfAbsent(id2, k -> new HashSet<>()).add(id1);
        }
        LOG.info("build overlap map success");
        List<RepositoryAnalyzeStatus> repoList = repositoryAnalyzeStatusMapper.findListByStatus(
                RepositoryAnalyzeStatus.AnalyzeStatus.Success, projectOffset, projectLimit);
        RepositoryAnalysisService service = wocEnabled ?
                wocRepositoryAnalysisService :
                gitRepositoryAnalysisService;
        outputLine(writer, "repoName", "commitId", "pomSize", "codeSize",
                "codeExistInPom", "duplicateCodeExistInPom", "codeNotExistInPom", "codeNotExistInPomUnique");
        for (RepositoryAnalyzeStatus repo : repoList) {
            String repoName = repo.getRepoName();
            LOG.info("analyze repo = {}", repoName);
            RepositoryAnalysisService.AbstractRepository repository = service.openRepository(repoName);
            if(repository == null) continue;
            try {
                service.forEachCommit(repository, commitId -> {
                    CommitInfo commitInfo = gitObjectStorageService.getCommitById(commitId);
                    if(commitInfo == null) return;
                    List<Long> codeGAIds = commitInfo.getCodeGroupArtifactIdList();
                    List<Long> pomGAIds = commitInfo.getPomGroupArtifactIdList();
                    if(codeGAIds == null || pomGAIds == null) return;
                    Set<Long> pomGAIdSet = new HashSet<>(pomGAIds);

                    Set<Long> codeIdExistInPom = new HashSet<>(codeGAIds);
                    codeIdExistInPom.retainAll(pomGAIdSet);

                    Set<Long> codeIdExistInPomExtend = new HashSet<>();
                    for (Long gaId : codeIdExistInPom) {
                        codeIdExistInPomExtend.add(gaId);
                        Set<Long> extendGaIds = overlapMap.get(gaId);
                        if(extendGaIds != null) {
                            codeIdExistInPomExtend.addAll(extendGaIds);
                        }
                    }

                    Set<Long> duplicateCodeIdExistInPom = new HashSet<>(codeGAIds);
                    duplicateCodeIdExistInPom.removeAll(codeIdExistInPom);
                    duplicateCodeIdExistInPom.retainAll(codeIdExistInPomExtend);

                    Set<Long> codeIdNotExistInPom = new HashSet<>(codeGAIds);
                    codeIdNotExistInPom.removeAll(codeIdExistInPom);
                    codeIdNotExistInPom.removeAll(duplicateCodeIdExistInPom);

                    Set<Long> codeIdNotExistInPomExtend = new HashSet<>();
                    Set<Long> codeIdNotExistInPomUnique = new HashSet<>();
                    for (Long gaId : codeIdNotExistInPom) {
                        if(codeIdNotExistInPomExtend.contains(gaId)) continue;
                        codeIdNotExistInPomUnique.add(gaId);
                        codeIdNotExistInPomExtend.add(gaId);
                        Set<Long> extendGaIds = overlapMap.get(gaId);
                        if(extendGaIds != null) {
                            codeIdNotExistInPomExtend.addAll(extendGaIds);
                        }
                    }

                    try {
                        outputLine(writer, repoName, commitId, pomGAIds.size(), codeGAIds.size(),
                                codeIdExistInPom.size(), duplicateCodeIdExistInPom.size(),
                                codeIdNotExistInPom.size(), codeIdNotExistInPomUnique.size());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, commitOffset, commitLimit);
            } finally {
                service.closeRepository(repository);
            }
        }
    }

    public void exportLibraryOverlap(FileWriter writer, String... args) throws Exception {
        outputLine(writer, "groupArtifactId1", "groupArtifactId2", "signatureCount");
        List<LibraryOverlap> overlapList = libraryOverlapMapper.findAll();
        for (LibraryOverlap overlap : overlapList) {
            outputLine(writer, overlap.getGroupArtifactId1(), overlap.getGroupArtifactId2(), overlap.getSignatureCount());
        }
    }

    public int getListSizeSafe(List<?> list) {
        if(list == null) return 0;
        return list.size();
    }

    public void outputLine(FileWriter writer, Object... fields) throws Exception {
        int i = 1;
        for (Object field : fields) {
            writer.write(field == null ? "null" : field.toString());
            if(fields.length == i++) {
                writer.write("\n");
            } else {
                writer.write(",");
            }
        }
    }

    public int calcDiff(Set<Long> s1, Set<Long> s2) {
        Set<Long> s = new HashSet<>(s1);
        s.removeAll(s2);
        return s.size();
    }
}
