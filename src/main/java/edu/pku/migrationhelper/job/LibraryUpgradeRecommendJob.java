package edu.pku.migrationhelper.job;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;
import edu.pku.migrationhelper.data.lib.LibraryGroupArtifact;
import edu.pku.migrationhelper.data.lib.LibraryVersion;
import edu.pku.migrationhelper.data.api.MethodSignatureOld;
import edu.pku.migrationhelper.mapper.LibraryGroupArtifactMapper;
import edu.pku.migrationhelper.mapper.LibraryVersionMapper;
import edu.pku.migrationhelper.mapper.LibraryVersionToSignatureMapper;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import edu.pku.migrationhelper.service.MapperUtilService;
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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
    private MapperUtilService mapperUtilService;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private LibraryVersionToSignatureMapper libraryVersionToSignatureMapper;

    @Autowired
    private LibraryIdentityService libraryIdentityService;

    private static class VersionCandidate {
        String groupId;
        String artifactId;
        String fromVersion;
        String toVersion;
        long addedAPICount = 0;
        long removedAPICount = 0;

        public VersionCandidate(String groupId, String artifactId, String fromVersion, String toVersion) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
        }
    }

    private static class APIChanges {
        List<MethodSignatureOld> addedSignatures;
        List<MethodSignatureOld> removedSignatures;
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
                    .filter(v -> {
                        if (v.isDownloaded() && v.isParsed()) return true;
                        else {
                            LOG.info("{}-{} is skipped because it is not downloaded or contain parse error", lib, v);
                            return false;
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

            LOG.info("Writing APIs of {}-{}", lib, version);
            outputAllAPIs(outputFolder, version);
            for (LibraryVersion v : toVersions) {
                LOG.info("Writing APIs of {}-{}", lib, v);
                outputAllAPIs(outputFolder, v);
            }
        }

        for (VersionCandidate candidate : candidates) {
            APIChanges apiChanges = getAPIChanges(candidate);
            candidate.addedAPICount = apiChanges.addedSignatures.size();
            candidate.removedAPICount = apiChanges.removedSignatures.size();
            outputChangedAPIs(outputFolder, candidate, apiChanges);
        }
        outputSummaryCSV(outputFolder, candidates);

        LOG.info("Success");
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private APIChanges getAPIChanges(VersionCandidate candidate) {
        APIChanges result = new APIChanges();

        LibraryGroupArtifact lib = libraryGroupArtifactMapper.findByGroupIdAndArtifactId(
                candidate.groupId, candidate.artifactId);
        LibraryVersion fromVersion = libraryVersionMapper.findByGroupArtifactIdAndVersion(lib.getId(), candidate.fromVersion);
        LibraryVersion toVersion = libraryVersionMapper.findByGroupArtifactIdAndVersion(lib.getId(), candidate.toVersion);

        Set<Long> fromVersionSignatures = new HashSet<>(
                libraryVersionToSignatureMapper.findById(fromVersion.getId()).getSignatureIdList());
        Set<Long> toVersionSignatures = new HashSet<>(
                libraryVersionToSignatureMapper.findById(toVersion.getId()).getSignatureIdList());

        List<Long> addedSignatureIds = toVersionSignatures.stream()
                .filter(l -> !fromVersionSignatures.contains(l))
                .collect(Collectors.toList());
        List<MethodSignatureOld> addedSignatures = mapperUtilService.getMethodSignaturesByIds(addedSignatureIds)
                .stream().sorted(Comparator.comparing(MethodSignatureOld::toString)).collect(Collectors.toList());
        List<Long> removedSignatureIds =  fromVersionSignatures.stream()
                .filter(l -> !toVersionSignatures.contains(l)).collect(Collectors.toList());
        List<MethodSignatureOld> removedSignatures = mapperUtilService.getMethodSignaturesByIds(removedSignatureIds)
                .stream().sorted(Comparator.comparing(MethodSignatureOld::toString)).collect(Collectors.toList());
        LOG.info("{} from {} to {}, {} added APIs and {} removed APIs", lib, fromVersion, toVersion,
                addedSignatures.size(), removedSignatures.size());

        result.addedSignatures = addedSignatures;
        result.removedSignatures = removedSignatures;
        return result;
    }

    private List<LibraryVersion> readInputCSV(String path) throws IOException {
        List<LibraryVersion> result = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(path))) {
            for (CSVRecord record : parser) {
                String name = record.get("Name");
                String groupId = name.split(":")[0];
                String artifactId = name.split(":")[1];
                String versionString = record.get("Version");

                LibraryGroupArtifact lib = libraryGroupArtifactMapper.findByGroupIdAndArtifactId(groupId, artifactId);
                if (lib == null || !lib.isVersionExtracted() || !lib.isParsed() || lib.isParseError()) {
                    LOG.error("{}:{} does not exist or is not parsed in our library database!", groupId, artifactId);
                    LOG.info("Trying to download and parse {}:{} before proceeding...", groupId, artifactId);
                    libraryIdentityService.parseGroupArtifact(groupId, artifactId, false);
                    lib = libraryGroupArtifactMapper.findByGroupIdAndArtifactId(groupId, artifactId);
                    assert lib != null;
                }

                LibraryVersion version = libraryVersionMapper.findByGroupArtifactIdAndVersion(lib.getId(), versionString);
                if (version == null || version.isParseError() || !version.isDownloaded() || !version.isParsed()) {
                    LOG.error("{}:{}-{} does not exist or contain parse error in our version database! " +
                                    "it will be skipped during recommendation",
                            groupId, artifactId, versionString);
                    continue;
                }

                result.add(version);
            }
        }
        return result;
    }

    private void outputSummaryCSV(String outputPath, List<VersionCandidate> candidates) throws IOException {
        Path path = Paths.get(outputPath, "summary.csv");
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(path.toString()), CSVFormat.DEFAULT)) {
            printer.printRecord("groupId", "artifactId", "fromVersion", "toVersion", "addedAPIs", "removedAPIs");
            for (VersionCandidate c : candidates) {
                printer.printRecord(c.groupId, c.artifactId, c.fromVersion, c.toVersion, c.addedAPICount, c.removedAPICount);
            }
        }
    }

    private void outputAllAPIs(String outputPath, LibraryVersion version) throws IOException {
        LibraryGroupArtifact lib = libraryGroupArtifactMapper.findById(version.getGroupArtifactId());

        Path path = Paths.get(outputPath, String.format("%s-%s", lib.getGroupId(), lib.getArtifactId()));
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }
        path = Paths.get(path.toString(), String.format("api-%s.csv", version));

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(path.toString()), CSVFormat.DEFAULT)) {
            printer.printRecord("signatureId", "packageName", "className", "methodName", "paramList");
            List<Long> signatureIds = libraryVersionToSignatureMapper.findById(version.getId()).getSignatureIdList();
            LOG.info("{}-{} has {} different APIs", lib, version, signatureIds.size());

            List<MethodSignatureOld> signatures = mapperUtilService.getMethodSignaturesByIds(signatureIds);
            signatures.sort(Comparator.comparing(MethodSignatureOld::toString));
            for (MethodSignatureOld ms : signatures) {
                printer.printRecord(ms.getId(), ms.getPackageName(),
                        ms.getClassName(), ms.getMethodName(), ms.getParamList());
            }
        }
    }

    private void outputChangedAPIs(String outputPath, VersionCandidate candidate, APIChanges apiChanges) throws IOException {
        LibraryGroupArtifact lib = libraryGroupArtifactMapper.findByGroupIdAndArtifactId(
                candidate.groupId, candidate.artifactId);

        Path path = Paths.get(outputPath, String.format("%s-%s", lib.getGroupId(), lib.getArtifactId()));
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }
        path = Paths.get(path.toString(), String.format("changed-api-%s-%s.csv",
                candidate.fromVersion, candidate.toVersion));

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(path.toString()), CSVFormat.DEFAULT)) {
            printer.printRecord("signatureId", "changeType", "packageName",
                    "className", "methodName", "paramList");
            for (MethodSignatureOld ms: apiChanges.addedSignatures) {
                printer.printRecord(ms.getId(), "add", ms.getPackageName(),
                        ms.getClassName(), ms.getMethodName(), ms.getParamList());
            }
            for (MethodSignatureOld ms: apiChanges.removedSignatures) {
                printer.printRecord(ms.getId(), "remove", ms.getPackageName(),
                        ms.getClassName(), ms.getMethodName(), ms.getParamList());
            }
        }
    }
}
