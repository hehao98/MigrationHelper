package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.LioProjectWithRepository;
import edu.pku.migrationhelper.mapper.LioProjectWithRepositoryMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Value("${migration-helper.libraries-io-import.project-with-repository-path}")
    private String projectWithRepositoryPath;

    @Autowired
    private LioProjectWithRepositoryMapper lioProjectWithRepositoryMapper;

    @Override
    public void run(String... args) throws Exception {
        FileReader fileReader = new FileReader(projectWithRepositoryPath);
        CSVParser parser = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(fileReader);
        int insertLimit = 100;
        List<LioProjectWithRepository> results = new ArrayList<>(insertLimit);
        for (CSVRecord record : parser) {
            if ("Maven".equals(record.get("Platform")) && "Java".equals(record.get("Language"))) {
                LioProjectWithRepository p = new LioProjectWithRepository();
                p.setId(Long.parseLong(record.get("ID")));
                p.setPlatform(record.get("Platform"));
                p.setLanguage(record.get("Language"));
                p.setName(record.get("Name"));
                p.setRepositoryUrl(record.get("Repository URL"));
                p.setRepositoryId(Long.parseLong(record.get("Repository ID")));
                p.setSourceRank(Integer.parseInt(record.get("SourceRank")));
                p.setRepositoryStarCount(Integer.parseInt(record.get("Repository Stars Count")));
                p.setRepositoryForkCount(Integer.parseInt(record.get("Repository Forks Count")));
                p.setRepositoryWatchersCount(Integer.parseInt(record.get("Repository Watchers Count")));
                p.setRepositorySourceRank(Integer.parseInt(record.get("Repository SourceRank")));
                p.setDependentProjectsCount(Integer.parseInt(record.get("Dependent Projects Count")));
                p.setDependentRepositoriesCount(Integer.parseInt(record.get("Dependent Repositories Count")));
                results.add(p);
            }
            if (results.size() >= insertLimit) {
                lioProjectWithRepositoryMapper.insert(results);
                results.clear();
            }
        }
        if(results.size() > 0) {
            lioProjectWithRepositoryMapper.insert(results);
        }
    }
}
