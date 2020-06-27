package edu.pku.migrationhelper.job;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;
import edu.pku.migrationhelper.data.LibraryGroupArtifact;
import edu.pku.migrationhelper.data.LibraryVersion;
import edu.pku.migrationhelper.mapper.LibraryGroupArtifactMapper;
import edu.pku.migrationhelper.mapper.LibraryVersionMapper;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import sun.misc.Version;

import javax.lang.model.type.ArrayType;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "LibraryUpgradeRecommendJob")
public class LibraryUpgradeRecommendJob implements CommandLineRunner {

    final private Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private LibraryIdentityService libraryIdentityService;

    private static class VersionCandidate {
        String groupId;
        String artifactId;
        String fromVersion;
        String toVersion;
        long addedAPIs = 0;
        long removedAPIs = 0;

        public VersionCandidate(String groupId, String artifactId, String fromVersion, String toVersion) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
        }
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 2) {
            LOG.info("Usage: ./run-xxx.sh LibraryUpgradeRecommendJob <library-input-csv> <output-folder>");
            System.exit(SpringApplication.exit(context, () -> -1));
        }

        String inputCSVPath = args[0];
        String outputFolder = args[1];
        LOG.info("Reading libraries from {} and saving results to {}", inputCSVPath, outputFolder);

        List<LibraryVersion> versions = readInputCSV(inputCSVPath);
        LOG.info("{}", versions);

        List<VersionCandidate> candidates = new ArrayList<>();
        for (LibraryVersion version : versions) {
            LibraryGroupArtifact lib = libraryGroupArtifactMapper.findById(version.getGroupArtifactId());

            LOG.info("Generating candidates for {}-{}", lib, version);
            Semver semver = new Semver(version.toString(), Semver.SemverType.LOOSE);
            List<LibraryVersion> toVersions = libraryVersionMapper.findByGroupArtifactId(lib.getId()).stream()
                    .filter(v -> {
                        try {
                            return new Semver(v.toString(), Semver.SemverType.LOOSE).isGreaterThan(semver);
                        } catch (SemverException e) {
                            LOG.warn("Illegal version string {}, {}", v, e);
                            LOG.warn("{} will be considered as a candidate", v);
                            return true;
                        }
                    })
                    .collect(Collectors.toList());

            LOG.info("Generating recommendation output for {}-{}", lib, version);
            for (LibraryVersion toVersion : toVersions) {
                LOG.info("Candidate version: {}-{}", lib, toVersion);
                VersionCandidate candidate = new VersionCandidate(
                        lib.getGroupId(), lib.getArtifactId(), version.toString(), toVersion.toString());
                candidates.add(candidate);
            }
        }

        outputSummaryCSV(outputFolder, candidates);

        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private List<LibraryVersion> readInputCSV(String path) throws IOException {
        List<LibraryVersion> result = new ArrayList<>();
        try (CSVParser parser = new CSVParser(new FileReader(path), CSVFormat.EXCEL)) {
            for (CSVRecord record : parser) {
                String name = record.get("Name");
                String groupId = name.split(":")[0];
                String artifactId = name.split(":")[1];
                String versionString = record.get("Version");

                LibraryGroupArtifact lib = libraryGroupArtifactMapper.findByGroupIdAndArtifactId(groupId, artifactId);
                if (lib == null || !lib.isParsed()) {
                    LOG.error("{}:{} does not exist or is not parsed in our library database!", groupId, artifactId);
                    LOG.info("trying to download and parse {}:{} before proceeding...", groupId, artifactId);
                    libraryIdentityService.parseGroupArtifact(groupId, artifactId, false);
                    lib = libraryGroupArtifactMapper.findByGroupIdAndArtifactId(groupId, artifactId);
                    assert lib != null;
                }

                LibraryVersion version = libraryVersionMapper.findByGroupArtifactIdAndVersion(lib.getId(), versionString);
                if (version == null) {
                    LOG.error("{}:{}-{} does not exist in our version database! it will be skipped during recommendation",
                            groupId, artifactId, versionString);
                    continue;
                }

                result.add(version);
            }
        } catch (IOException e) {
            LOG.error("Error while reading CSV file {}, {}", path, e);
            throw e;
        }
        return result;
    }

    private void outputSummaryCSV(String outputPath, List<VersionCandidate> candidates) throws IOException {
        Path path = Paths.get(outputPath, "summary.csv");
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(path.toString()), CSVFormat.EXCEL)) {
            printer.printRecord("groupId", "artifactId", "fromVersion", "toVersion", "addedAPIs", "removedAPIs");
            for (VersionCandidate c : candidates) {
                printer.printRecord(c.groupId, c.artifactId, c.fromVersion, c.toVersion, c.addedAPIs, c.removedAPIs);
            }
        } catch (IOException e) {
            LOG.error("Error while writing to CSV file {}, {}", path, e);
            throw e;
        }
    }
}
