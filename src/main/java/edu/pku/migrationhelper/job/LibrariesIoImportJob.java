package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.lio.LioProject;
import edu.pku.migrationhelper.data.lio.LioProjectDependency;
import edu.pku.migrationhelper.data.lio.LioRepository;
import edu.pku.migrationhelper.data.lio.LioRepositoryDependency;
import edu.pku.migrationhelper.repository.LioProjectDependencyRepository;
import edu.pku.migrationhelper.repository.LioProjectRepository;
import edu.pku.migrationhelper.repository.LioRepositoryDependencyRepository;
import edu.pku.migrationhelper.repository.LioRepositoryRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "LibrariesIoImportJob")
public class LibrariesIoImportJob implements CommandLineRunner {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    @Value("${migration-helper.libraries-io-import.project-with-repository-path}")
    private String projectWithRepositoryPath;

    @Value("${migration-helper.libraries-io-import.project-dependency-path}")
    private String projectDependencyPath;

    @Value("${migration-helper.libraries-io-import.repository-path}")
    private String repositoryPath;

    @Value("${migration-helper.libraries-io-import.repository-dependency-path}")
    private String repositoryDependencyPath;

    @Autowired
    private LioProjectRepository lioProjectRepository;

    @Autowired
    private LioProjectDependencyRepository lioProjectDependencyRepository;

    @Autowired
    private LioRepositoryRepository lioRepositoryRepository;

    @Autowired
    private LioRepositoryDependencyRepository lioRepositoryDependencyRepository;

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 1) {
            printUsageAndExit();
        }
        switch (args[0]) {
            case "projectWithRepository":
                importProjectWithRepository();
                break;
            case "projectDependency":
                importProjectDependency();
                break;
            case "repository":
                importRepository();
                break;
            case "repositoryDependency":
                importRepositoryDependency();
                break;
            default:
                printUsageAndExit();
        }
        LOG.info("Import success");
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private void printUsageAndExit() {
        LOG.error("Usage: LibrariesIoImportJob <collectionName>");
        LOG.error("Supported collection names: projectWithRepository, projectDependency, repository, repositoryDependency");
        System.exit(SpringApplication.exit(context, () -> -1));
    }

    private void importProjectWithRepository() throws IOException {
        LOG.info("Start import libraries.io project with repository (i.e. Java Maven libraries)");
        FileReader fileReader = new FileReader(projectWithRepositoryPath);
        CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(fileReader);
        int insertLimit = 100;
        List<LioProject> results = new ArrayList<>(insertLimit);
        for (CSVRecord record : parser) {
            if ("Maven".equals(record.get("Platform"))) {
                LioProject p = new LioProject();
                p.setId(getRecordLong(record, "ID"));
                p.setPlatform(record.get("Platform"));
                p.setLanguage(record.get("Language"));
                p.setName(record.get("Name"));
                p.setRepositoryUrl(record.get("Repository URL"));
                p.setRepositoryId(getRecordLong(record, "Repository ID"));
                p.setSourceRank(getRecordInt(record, "SourceRank"));
                p.setRepositoryStarCount(getRecordInt(record, "Repository Stars Count"));
                p.setRepositoryForkCount(getRecordInt(record, "Repository Forks Count"));
                p.setRepositoryWatchersCount(getRecordInt(record, "Repository Watchers Count"));
                p.setRepositorySourceRank(getRecordInt(record, "Repository SourceRank"));
                p.setDependentProjectsCount(getRecordInt(record, "Dependent Projects Count"));
                p.setDependentRepositoriesCount(getRecordInt(record, "Dependent Repositories Count"));
                results.add(p);
            }
            if (results.size() >= insertLimit) {
                lioProjectRepository.saveAll(results);
                results.clear();
            }
        }
        if (results.size() > 0) {
            lioProjectRepository.saveAll(results);
        }
        fileReader.close();
    }

    private void importProjectDependency() throws IOException {
        LOG.info("Start import project dependency (i.e. dependency of Java Maven libraries)");
        FileReader fileReader = new FileReader(projectDependencyPath);
        CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(fileReader);
        int insertLimit = 100000;
        int total = 0;
        List<LioProjectDependency> results = new ArrayList<>(insertLimit);
        for (CSVRecord record : parser) {
            if (record.get("Platform").toLowerCase().equals("maven")) {
                LioProjectDependency p = new LioProjectDependency()
                        .setId(getRecordLong(record, "ID"))
                        .setPlatform(record.get("Platform"))
                        .setProjectName(record.get("Project Name"))
                        .setProjectId(getRecordLong(record, "Project ID"))
                        .setVersionNumber(record.get("Version Number"))
                        .setVersionId(getRecordLong(record, "Version ID"))
                        .setDependencyName(record.get("Dependency Name"))
                        .setDependencyPlatform(record.get("Dependency Platform"))
                        .setDependencyKind(record.get("Dependency Kind"))
                        .setOptionalDependency(getRecordBoolean(record, "Optional Dependency"))
                        .setDependencyRequirements(record.get("Dependency Requirements"))
                        .setDependencyProjectId(getRecordLong(record, "Dependency Project ID"));
                results.add(p);
            }
            if (results.size() >= insertLimit) {
                lioProjectDependencyRepository.saveAll(results);
                results.clear();
                total += insertLimit;
                LOG.info("{} entries added", total);
            }
        }
        if (results.size() > 0) {
            lioProjectDependencyRepository.saveAll(results);
        }
        fileReader.close();
    }

    private void importRepository() throws IOException {
        LOG.info("Start import libraries.io Java repository");
        FileReader fileReader = new FileReader(repositoryPath);
        CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(fileReader);
        int insertLimit = 10000;
        List<LioRepository> results = new ArrayList<>(insertLimit);
        for (CSVRecord record : parser) {
            if (!record.get("Language").equals("Java")) {
                continue;
            }
            LioRepository p = new LioRepository()
                    .setId(getRecordLong(record, "ID"))
                    .setHostType(record.get("Host Type"))
                    .setNameWithOwner(record.get("Name with Owner"))
                    .setDescription(record.get("Description"))
                    .setFork(getRecordBoolean(record, "Fork"))
                    .setForkSourceNameWithOwner(record.get("Fork Source Name with Owner"))
                    .setCreatedTimestamp(record.get("Created Timestamp"))
                    .setUpdatedTimestamp(record.get("Updated Timestamp"))
                    .setLastPushedTimestamp(record.get("Last pushed Timestamp"))
                    .setHomepageURL(record.get("Homepage URL"))
                    .setMirrorURL(record.get("Mirror URL"))
                    .setSize(getRecordLong(record, "Size"))
                    .setLanguage(record.get("Language"))
                    .setStarsCount(getRecordLong(record, "Stars Count"))
                    .setForksCount(getRecordLong(record, "Forks Count"))
                    .setOpenIssuesCount(getRecordLong(record, "Open Issues Count"))
                    .setWatchersCount(getRecordLong(record, "Watchers Count"))
                    .setContributorsCount(getRecordLong(record, "Contributors Count"))
                    .setReadmeFilename(record.get("Readme filename"))
                    .setChangeLogFilename(record.get("Changelog filename"))
                    .setContributingGuidelinesFilename(record.get("Contributing guidelines filename"))
                    .setLicenseFilename(record.get("License filename"))
                    .setCodeOfConductFilename(record.get("Code of Conduct filename"));
            results.add(p);
            if (results.size() >= insertLimit) {
                lioRepositoryRepository.saveAll(results);
                results.clear();
            }
        }
        if (results.size() > 0) {
            lioRepositoryRepository.saveAll(results);
        }
        fileReader.close();
    }

    private void importRepositoryDependency() throws IOException {
        LOG.info("Start import libraries.io Java repository with dependencies");
        FileReader fileReader = new FileReader(repositoryDependencyPath);
        CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(fileReader);
        int insertLimit = 100000;
        int total = 0;
        List<LioRepositoryDependency> results = new ArrayList<>(insertLimit);
        for (CSVRecord record : parser) {
            if (!record.get("Manifest Platform").toLowerCase().equals("maven")) {
                continue;
            }
            LioRepositoryDependency p = new LioRepositoryDependency()
                    .setId(getRecordLong(record, "ID"))
                    .setHostType(record.get("Host Type"))
                    .setRepositoryNameWithOwner(record.get("Repository Name with Owner"))
                    .setRepositoryId(record.get("Repository ID"))
                    .setManifestPlatform(record.get("Manifest Platform"))
                    .setManifestFilePath(record.get("Manifest Filepath"))
                    .setGitBranch(record.get("Git branch"))
                    .setManifestKind(record.get("Manifest kind"))
                    .setOptional(getRecordBoolean(record, "Optional"))
                    .setDependencyProjectName(record.get("Dependency Project Name"))
                    .setDependencyRequirements(record.get("Dependency Requirements"))
                    .setDependencyKind(record.get("Dependency Kind"))
                    .setDependencyProjectId(getRecordLong(record, "Dependency Project ID"));
            results.add(p);
            if (results.size() >= insertLimit) {
                lioRepositoryDependencyRepository.saveAll(results);
                results.clear();
                total += insertLimit;
                LOG.info("{} entries added", total);
            }
        }
        if (results.size() > 0) {
            lioRepositoryDependencyRepository.saveAll(results);
        }
        fileReader.close();
    }

    private long getRecordLong(CSVRecord record, String key) {
        try {
            return Long.parseLong(record.get(key));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int getRecordInt(CSVRecord record, String key) {
        try {
            return Integer.parseInt(record.get(key));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean getRecordBoolean(CSVRecord record, String key) {
        String str = record.get(key).toLowerCase();
        return str.equals("true");
    }
}
