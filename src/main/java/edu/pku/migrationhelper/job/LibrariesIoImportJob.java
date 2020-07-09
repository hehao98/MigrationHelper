package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.lib.LioProject;
import edu.pku.migrationhelper.repository.LioProjectRepository;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuyul on 2020/2/16.
 */
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "LibrariesIoImportJob")
public class LibrariesIoImportJob implements CommandLineRunner {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    @Value("${migration-helper.libraries-io-import.project-with-repository-path}")
    private String projectWithRepositoryPath;

    @Autowired
    private LioProjectRepository lioProjectRepository;

    @Override
    public void run(String... args) throws Exception {
        LOG.info("start import libraries.io project with repository");
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
        if(results.size() > 0) {
            lioProjectRepository.saveAll(results);
        }
        LOG.info("import success");
        System.exit(SpringApplication.exit(context));
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
}
