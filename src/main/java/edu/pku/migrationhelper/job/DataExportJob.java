package edu.pku.migrationhelper.job;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.pku.migrationhelper.data.BlobInfo;
import edu.pku.migrationhelper.data.CommitInfo;
import edu.pku.migrationhelper.data.LioProjectWithRepository;
import edu.pku.migrationhelper.mapper.BlobInfoMapper;
import edu.pku.migrationhelper.mapper.CommitInfoMapper;
import edu.pku.migrationhelper.mapper.LibraryVersionMapper;
import edu.pku.migrationhelper.mapper.LioProjectWithRepositoryMapper;
import edu.pku.migrationhelper.service.GitRepositoryAnalysisService;
import edu.pku.migrationhelper.service.WocRepositoryAnalysisService;
import edu.pku.migrationhelper.util.JsonUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        int limit = 1000;
        boolean end = false;
        while(!end) {
            LOG.info("start export blobId = {}", blobId + 1);
            List<BlobInfo> blobList = blobInfoMapper.findList(tableNum, blobId, limit);
            end = blobList.size() < limit;
            if(end && tableNum < 127) {
                end = false;
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
                "codeDelGa", "pomDelGa", "codeDelGaDiff", "pomDelGaDiff");
        int tableNum = 0;
        long commitId = 0;
        int limit = 1000;
        boolean end = false;
        while(!end) {
            LOG.info("start export commitId = {}", commitId + 1);
            List<CommitInfo> commitList = commitInfoMapper.findList(tableNum, commitId, limit);
            end = commitList.size() < limit;
            if(end && tableNum < 127) {
                end = false;
                tableNum++;
            }
            for (CommitInfo commitInfo : commitList) {
                Set<Long> codeGa = new HashSet<>(commitInfo.getCodeGroupArtifactIdList());
                Set<Long> pomGa = new HashSet<>(commitInfo.getPomGroupArtifactIdList());
                Set<Long> codeAddGa = new HashSet<>(commitInfo.getCodeAddGroupArtifactIdList());
                Set<Long> pomAddGa = new HashSet<>(commitInfo.getPomAddGroupArtifactIdList());
                Set<Long> codeDelGa = new HashSet<>(commitInfo.getCodeDeleteGroupArtifactIdList());
                Set<Long> pomDelGa = new HashSet<>(commitInfo.getPomDeleteGroupArtifactIdList());
                outputLine(writer, ++commitId,
                        codeGa.size(), pomGa.size(), calcDiff(codeGa, pomGa), calcDiff(pomGa, codeGa),
                        codeAddGa.size(), pomAddGa.size(), calcDiff(codeAddGa, pomAddGa), calcDiff(pomAddGa, codeAddGa),
                        codeDelGa.size(), pomDelGa.size(), calcDiff(codeDelGa, pomDelGa), calcDiff(pomDelGa, codeDelGa));
            }
        }
    }

    public void exportLibraryVersion(FileWriter writer) throws Exception {
        outputLine(writer, "groupArtifactId", "count");
        List<LibraryVersionMapper.CountData> counts = libraryVersionMapper.countByGroupArtifact();
        for (LibraryVersionMapper.CountData count : counts) {
            outputLine(writer, count.groupArtifactId, count.count);
        }
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

    public int countJsonArray(String jsonArray) {
        if(jsonArray == null || "".equals(jsonArray) || "[]".equals(jsonArray)) return 0;
        char[] ca = jsonArray.toCharArray();
        int count = 0;
        for (char c : ca) {
            if(c == ',') count++;
        }
        return count + 1;
    }

    public Set<Long> readJsonArrayAsSet(String jsonArray) {
        if(jsonArray == null || "".equals(jsonArray)) return new HashSet<>();
        return new HashSet<>(JsonUtils.readStringAsObject(jsonArray, new TypeReference<List<Long>>() {}));
    }

    public int calcDiff(Set<Long> s1, Set<Long> s2) {
        Set<Long> s = new HashSet<>(s1);
        s.removeAll(s2);
        return s.size();
    }
}
