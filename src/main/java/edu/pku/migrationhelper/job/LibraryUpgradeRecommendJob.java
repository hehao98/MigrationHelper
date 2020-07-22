package edu.pku.migrationhelper.job;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;
import edu.pku.migrationhelper.data.api.APIChange;
import edu.pku.migrationhelper.data.lib.LibraryGroupArtifact;
import edu.pku.migrationhelper.data.lib.LibraryVersion;
import edu.pku.migrationhelper.repository.LibraryGroupArtifactRepository;
import edu.pku.migrationhelper.repository.LibraryVersionRepository;
import edu.pku.migrationhelper.service.APIDiffService;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "LibraryUpgradeRecommendJob")
public class LibraryUpgradeRecommendJob implements CommandLineRunner {

    final private Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private LibraryGroupArtifactRepository libraryGroupArtifactRepository;

    @Autowired
    private LibraryVersionRepository libraryVersionRepository;

    @Autowired
    private LibraryIdentityService libraryIdentityService;

    @Autowired
    private APIDiffService apiDiffService;

    private static class VersionCandidate {
        public String groupId;
        public String artifactId;
        public String fromVersion;
        public String toVersion;
        public APIChange apiChange;

        public VersionCandidate(String groupId, String artifactId, String fromVersion, String toVersion) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
        }
    }

    private static class Arguments {
        @Parameter(names = {"-l", "--library", "--group-artifact-id"}, description = "Maven library, groupId:artifactId")
        public String library;
        @Parameter(names = {"-g", "--group-id"}, description = "Maven group ID of the library")
        public String groupId;
        @Parameter(names = {"-a", "--artifact-id"}, description = "Maven artifact ID of the library")
        public String artifactId;
        @Parameter(names = {"-v", "--version"}, description = "The version to be updated")
        public String version;
        @Parameter(names = {"-o", "--output-folder"}, description = "The output folder, default=test_output/")
        public String outputFolder = "test_output/";
        @Parameter(names = {"-i", "--input-csv"}, description = "Input CSV with a Name row and a Version row")
        public String inputCSVPath;
    }

    @Override
    public void run(String... args) throws Exception {
        Arguments params = new Arguments();
        JCommander parser = JCommander.newBuilder().addObject(params).build();
        try {
            parser.parse(args);
        } catch (ParameterException e) {
            parser.usage();
            System.exit(SpringApplication.exit(context, () -> -1));
        }
        if (params.inputCSVPath == null && params.version == null
                && (params.library == null || (params.groupId == null && params.artifactId == null))) {
            parser.usage();
            System.exit(SpringApplication.exit(context, () -> -1));
        }
        if (params.library != null) {
            params.groupId = params.library.split(":")[0];
            params.artifactId = params.library.split(":")[1];
        }
        File outputPath = new File(params.outputFolder);
        if (!outputPath.exists() && !outputPath.mkdirs()) {
            LOG.error("output directory {} does not exist and cannot be created", params.outputFolder);
            System.exit(SpringApplication.exit(context, () -> -1));
        } else if (outputPath.exists() && !outputPath.isDirectory()) {
            LOG.error("output path {} is not a directory", params.outputFolder);
            System.exit(SpringApplication.exit(context, () -> -1));
        }

        List<VersionCandidate> allCandidates = new ArrayList<>();
        if (params.inputCSVPath != null) {
            LOG.info("Reading libraries from {}", params.inputCSVPath);
            List<LibraryVersion> versions = readInputCSV(params.inputCSVPath);
            for (LibraryVersion version : versions) {
                List<VersionCandidate> candidates = generateVersionCandidates(version);
                allCandidates.addAll(candidates);
            }
        } else {
            Optional<LibraryVersion> verOpt = getLibraryVersion(params.groupId, params.artifactId, params.version);
            if (!verOpt.isPresent()) {
                LOG.error("Error getting version for {}:{}-{}", params.groupId, params.artifactId, params.version);
                System.exit(SpringApplication.exit(context, () -> -1));
            }
            allCandidates.addAll(generateVersionCandidates(verOpt.get()));
        }

        LOG.info("Saving results in {}", params.outputFolder);
        for (VersionCandidate candidate : allCandidates) {
            outputChangedAPIs(params.outputFolder, candidate);
        }
        outputSummaryCSV(params.outputFolder, allCandidates);
        LOG.info("Success");
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private Optional<LibraryVersion> getLibraryVersion(String groupId, String artifactId, String versionString) {
        Optional<LibraryGroupArtifact> libOpt = libraryGroupArtifactRepository.findByGroupIdAndArtifactId(groupId, artifactId);
        if (!libOpt.isPresent() || !libOpt.get().isVersionExtracted() || !libOpt.get().isParsed() || libOpt.get().isParseError()) {
            LOG.error("{}:{} does not exist or is not parsed in our library database!", groupId, artifactId);
            LOG.info("Trying to download and parse {}:{} before proceeding...", groupId, artifactId);
            libraryIdentityService.parseGroupArtifact(groupId, artifactId);
            libOpt = libraryGroupArtifactRepository.findByGroupIdAndArtifactId(groupId, artifactId);
            if (!libOpt.isPresent()) return Optional.empty();
        }
        LibraryGroupArtifact lib = libOpt.get();
        List<LibraryVersion> versions = libraryVersionRepository.findByGroupArtifactId(lib.getId());
        Optional<LibraryVersion> lvOpt = versions.stream().filter(x -> x.getVersion().equals(versionString)).findFirst();
        if (!lvOpt.isPresent() || !lvOpt.get().isParsed()) {
            LOG.error("{}:{}-{} does not exist or contain parse error in our version database! " +
                            "it will be skipped during recommendation",
                    groupId, artifactId, versionString);
        }
        return lvOpt;
    }

    private List<LibraryVersion> readInputCSV(String path) throws IOException {
        List<LibraryVersion> result = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(path))) {
            for (CSVRecord record : parser) {
                String groupId;
                String artifactId;
                String versionString;
                if (parser.getHeaderNames().contains("name")) {
                    String name = record.get("name");
                    groupId = name.split(":")[0];
                    artifactId = name.split(":")[1];
                } else {
                    groupId = record.get("groupId");
                    artifactId = record.get("artifactId");
                }
                versionString = record.get("version");
                Optional<LibraryVersion> lvOpt = getLibraryVersion(groupId, artifactId, versionString);
                lvOpt.ifPresent(result::add);
            }
        }
        return result;
    }

    private List<VersionCandidate> generateVersionCandidates(LibraryVersion version) {
        Optional<LibraryGroupArtifact> opt = libraryGroupArtifactRepository.findById(version.getGroupArtifactId());
        if (!opt.isPresent()) {
            throw new IllegalArgumentException(String.format("id-%d does not exist in group artifact database", version.getGroupArtifactId()));
        }

        LibraryGroupArtifact lib = opt.get();
        List<VersionCandidate> candidates = new ArrayList<>();
        LOG.info("Generating candidates for {}-{}", lib, version);
        Semver semver = new Semver(version.toString(), Semver.SemverType.LOOSE);
        List<LibraryVersion> toVersions = libraryVersionRepository.findByGroupArtifactId(lib.getId()).stream()
                .filter(v -> {
                    try {
                        return new Semver(v.toString(), Semver.SemverType.LOOSE).isGreaterThan(semver);
                    } catch (SemverException e) {
                        LOG.warn("Illegal version string {}, {}", v, e);
                        LOG.warn("{} will be considered as a candidate", v);
                        return true;
                    }
                })
                .filter(v -> {
                    if (v.isParsed()) return true;
                    else {
                        LOG.info("{}-{} is skipped because it is not parsed", lib, v);
                        return false;
                    }
                })
                .collect(Collectors.toList());

        LOG.info("Generating recommendation output for {}-{}", lib, version);
        for (LibraryVersion toVersion : toVersions) {
            LOG.info("Candidate version: {}-{}", lib, toVersion);
            VersionCandidate candidate = new VersionCandidate(
                    lib.getGroupId(), lib.getArtifactId(), version.getVersion(), toVersion.getVersion());
            candidate.apiChange = apiDiffService.diff(
                    lib.getGroupId(), lib.getArtifactId(), version.getVersion(), toVersion.getVersion());
            candidates.add(candidate);
        }
        return candidates;
    }

    private void outputSummaryCSV(String outputPath, List<VersionCandidate> candidates) throws IOException {
        Path path = Paths.get(outputPath, "summary.csv");
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(path.toString()), CSVFormat.DEFAULT)) {
            printer.printRecord("groupId", "artifactId", "fromVersion", "toVersion",
                    "changedClasses", "breakingChanges", "addedFields", "removedFields", "changedFields",
                    "addedMethods", "removedMethods", "changedMethods");
            for (VersionCandidate c : candidates) {
                printer.printRecord(c.groupId, c.artifactId, c.fromVersion, c.toVersion,
                        c.apiChange.getChangedClasses().size(),
                        c.apiChange.getBreakingChangeCount(),
                        c.apiChange.getAddedFieldCount(),
                        c.apiChange.getRemovedFieldCount(),
                        c.apiChange.getChangedFieldCount(),
                        c.apiChange.getAddedMethodCount(),
                        c.apiChange.getRemovedMethodCount(),
                        c.apiChange.getChangedMethodCount());
            }
        }
    }

    private void outputChangedAPIs(String outputPath, VersionCandidate candidate) throws IOException {
        Path path = Paths.get(outputPath, candidate.groupId + "#" + candidate.artifactId);
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }
        //Path jsonPath = Paths.get(path.toString(), String.format("changed-api-%s-%s.json",
                //candidate.fromVersion, candidate.toVersion));
        Path txtPath = Paths.get(path.toString(), String.format("changed-api-%s-%s.txt",
                candidate.fromVersion, candidate.toVersion));
        //Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //try (PrintWriter out = new PrintWriter(jsonPath.toFile())) {
        //    out.println(gson.toJson(candidate.apiChange));
        //}
        try (PrintStream out = new PrintStream(txtPath.toFile())) {
            candidate.apiChange.printAPIChange(out);
        }
    }
}
