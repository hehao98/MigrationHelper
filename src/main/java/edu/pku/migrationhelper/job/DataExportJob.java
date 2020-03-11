package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.BlobInfo;
import edu.pku.migrationhelper.data.CommitInfo;
import edu.pku.migrationhelper.data.LioProjectWithRepository;
import edu.pku.migrationhelper.data.MethodChange;
import edu.pku.migrationhelper.mapper.*;
import edu.pku.migrationhelper.service.GitRepositoryAnalysisService;
import edu.pku.migrationhelper.service.WocRepositoryAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

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
    private MethodChangeMapper methodChangeMapper;

    @Autowired
    private LioProjectWithRepositoryMapper lioProjectWithRepositoryMapper;

    @Autowired
    private WocRepositoryAnalysisService wocRepositoryAnalysisService;

    @Autowired
    private GitRepositoryAnalysisService gitRepositoryAnalysisService;

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
            case "LibraryVersion": {
                exportLibraryVersion(writer);
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

    private String concatList(List<Long> list) {
        StringBuilder sb = new StringBuilder();
        for (Long aLong : list) {
            sb.append(aLong);
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
            String[] attrs = line.split(",");
            if(attrs.length < 2) {
                continue;
            }
            if(!"Java".equals(attrs[1])) {
                continue;
            }
            String[] urlFields = attrs[0].split("/");
            if(urlFields.length < 2) {
                continue;
            }
            String repoName = urlFields[urlFields.length - 2] + "_" + urlFields[urlFields.length - 1];
            int status = wocEnabled ?
                    wocRepositoryAnalysisService.getRepositoryAnalyzeStatus(repoName) :
                    gitRepositoryAnalysisService.getRepositoryAnalyzeStatus(repoName);
            outputLine(writer, repoName, status);
        }
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
