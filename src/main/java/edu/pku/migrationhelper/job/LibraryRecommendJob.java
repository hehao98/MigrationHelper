package edu.pku.migrationhelper.job;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import edu.pku.migrationhelper.data.LibraryMigrationCandidate;
import edu.pku.migrationhelper.data.lib.LibraryGroupArtifact;
import edu.pku.migrationhelper.repository.LibraryMigrationCandidateRepository;
import edu.pku.migrationhelper.service.DepSeqAnalysisService;
import edu.pku.migrationhelper.service.EvaluationService;
import edu.pku.migrationhelper.service.GroupArtifactService;
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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "LibraryRecommendJob")
@ConfigurationProperties(prefix = "migration-helper.library-recommend-job")
public class LibraryRecommendJob implements CommandLineRunner {

    final private Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private GroupArtifactService groupArtifactService;

    @Autowired
    private DepSeqAnalysisService depSeqAnalysisService;

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private LibraryMigrationCandidateRepository libraryMigrationCandidateRepository;

    @Parameter(names = {"-q", "--query-file"}, required = true)
    public String queryFile = null;

    @Parameter(names = {"-o", "--output-file",})
    public String outputFile = null;

    @Parameter(names = {"--save-to-mongodb"})
    public boolean saveToMongoDB = false;

    @Parameter(names = {"-e", "--evaluate"})
    public boolean evaluate = false;

    @Parameter(names = {"-r", "--output-repo-commit"})
    public String outputRepoCommit = null;

    @Override
    public void run(String... args) throws Exception {
        JCommander parser = JCommander.newBuilder().addObject(this).build();
        try {
            parser.parse(args);
        } catch (ParameterException e) {
            parser.usage();
            System.exit(SpringApplication.exit(context, () -> -1));
        }

        LOG.info("Read libraries from {} and output results to {}, evaluate = {}, outputRepoCommit = {}, saveToMongoDB = {}",
                queryFile, outputFile, evaluate, outputRepoCommit, saveToMongoDB);

        List<LibraryGroupArtifact> queryList = readLibraryFromQueryFile();
        Set<Long> fromIdLimit = new HashSet<>();
        queryList.forEach(e -> fromIdLimit.add(e.getId()));

        LOG.info("Generating recommendation result...");
        Map<Long, List<LibraryMigrationCandidate>> result =
                depSeqAnalysisService.miningLibraryMigrationCandidate(fromIdLimit);

        EvaluationService.EvaluationResult evaluationResult = null;
        if (evaluate) {
            LOG.info("Doing evaluation...");
            evaluationResult = evaluationService.evaluate(result, 20);
            evaluationService.printEvaluationResult(evaluationResult, System.out);
        }

        if (outputFile != null) {
            LOG.info("Writing recommendation results to {}", outputFile);
            outputCsv(queryList, result, evaluationResult);
        }
        if (outputRepoCommit != null) {
            LOG.info("Writing recommendation commits to {}", outputRepoCommit);
            outputRelatedRepoAndCommit(result);
        }
        if (saveToMongoDB) {
            int savedEntries = 0;
            int totalEntries = 0;
            LOG.info("Saving recommendation results to MongoDB...");
            for (long fromId : result.keySet()) {
                totalEntries += result.get(fromId).size();
                if (!libraryMigrationCandidateRepository.findByFromId(fromId).isEmpty())
                    continue;
                libraryMigrationCandidateRepository.saveAll(result.get(fromId));
                savedEntries += result.get(fromId).size();
            }
            LOG.info("{} migration candidates and {} are saved (others already exists)", totalEntries, savedEntries);
        }

        LOG.info("Success");
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private List<LibraryGroupArtifact> readLibraryFromQueryFile() throws Exception {
        List<LibraryGroupArtifact> result = new LinkedList<>();
        Set<String> libs = new HashSet<>();

        if (queryFile.endsWith(".csv")) {
            try (CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new FileReader(queryFile))) {
                for (CSVRecord record : parser) {
                    String groupId;
                    String artifactId;
                    if (parser.getHeaderNames().contains("name")) {
                        String name = record.get("name");
                        groupId = name.split(":")[0];
                        artifactId = name.split(":")[1];
                    } else {
                        groupId = record.get("groupId");
                        artifactId = record.get("artifactId");
                    }
                    if (libs.contains(groupId + ":" + artifactId)) {
                        LOG.info("Duplicate input {}:{} encountered, skipping", groupId, artifactId);
                        continue;
                    }
                    LibraryGroupArtifact lib = groupArtifactService.getGroupArtifactByName(groupId + ":" + artifactId);
                    if (lib == null) {
                        LOG.warn("groupArtifact not found: {}:{}", groupId, artifactId);
                        continue;
                    }
                    result.add(lib);
                    libs.add(groupId + ":" + artifactId);
                }
            }
        } else if (queryFile.endsWith(".txt")) {
            BufferedReader reader = new BufferedReader(new FileReader(queryFile));
            String line;
            while ((line = reader.readLine()) != null) {
                if (libs.contains(line)) {
                    LOG.info("Duplicate input {} encountered, skipping", line);
                    continue;
                }
                LibraryGroupArtifact lib = groupArtifactService.getGroupArtifactByName(line);
                if (lib == null) {
                    LOG.warn("groupArtifact not found: {}", line);
                    continue;
                }
                result.add(lib);
                libs.add(line);
            }
            reader.close();
        }
        return result;
    }

    private void outputCsv(
            List<LibraryGroupArtifact> queryList,
            Map<Long, List<LibraryMigrationCandidate>> recommendationResult,
            EvaluationService.EvaluationResult evaluationResult
    ) {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputFile), CSVFormat.DEFAULT)) {
            printer.printRecord("fromLib", "toLib", "confidence", "confidence2",
                    "ruleCountSameCommit", "ruleCount", "ruleFreqSameCommit", "ruleFreq",
                    "concurrence", "concurrenceAdjustment", "commitDistance", "methodChangeCount",
                    "apiSupport", "possibleCommitCount", "commitMessageSupport");
            for (LibraryGroupArtifact fromLib : queryList) {
                Long fromId = fromLib.getId();
                List<LibraryMigrationCandidate>
                        candidateList = recommendationResult.get(fromId);
                if (candidateList == null) continue;

                candidateList = candidateList.stream()
                        .filter(candidate -> {
                            LibraryGroupArtifact toLib = groupArtifactService.getGroupArtifactById(candidate.toId);
                            return !Objects.equals(toLib.getGroupId(), fromLib.getGroupId());
                        }).collect(Collectors.toList());

                if(candidateList.isEmpty()) continue;

                for (LibraryMigrationCandidate candidate : candidateList) {
                    LibraryGroupArtifact toLib = groupArtifactService.getGroupArtifactById(candidate.toId);
                    // String isCorrect = evaluationResult == null ?
                             // "unknown" : evaluationResult.correctnessMap.get(fromId).get(candidate.toId).toString();
                    printer.printRecord(
                            fromLib.getGroupArtifactId(), toLib.getGroupArtifactId(),
                            candidate.confidence, candidate.confidence2,
                            candidate.ruleCountSameCommit, candidate.ruleCount,
                            candidate.ruleSupportByMaxSameCommit, candidate.ruleSupportByMax,
                            candidate.libraryConcurrenceCount,
                            candidate.libraryConcurrenceSupport,
                            candidate.commitDistanceSupport,
                            candidate.methodChangeCount,
                            candidate.methodChangeSupportByMax,
                            candidate.possibleCommitList.size(),
                            candidate.commitMessageSupport);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void outputRelatedRepoAndCommit(
            Map<Long, List<LibraryMigrationCandidate>> result
    ) throws IOException {
        FileWriter writer = new FileWriter(outputRepoCommit);
        writer.write("fromLib,toLib,repoCommits,possibleCommits\n");
        result.forEach((fromId, candidateList) -> {
            LibraryGroupArtifact fromLib = groupArtifactService.getGroupArtifactById(fromId);
            candidateList = candidateList.stream()
                    .filter(candidate -> {
                        LibraryGroupArtifact toLib = groupArtifactService.getGroupArtifactById(candidate.toId);
                        return !Objects.equals(toLib.getGroupId(), fromLib.getGroupId());
                    }).collect(Collectors.toList());
            if (candidateList.isEmpty()) return;
            try {
                for (LibraryMigrationCandidate candidate : candidateList) {
                    writer.write(fromLib.getGroupArtifactId());
                    LibraryGroupArtifact toLib = groupArtifactService.getGroupArtifactById(candidate.toId);
                    writer.write(",");
                    writer.write(toLib.getGroupArtifactId());
                    writer.write(",");
                    for (String[] repoCommit : candidate.repoCommitList) {
                        for (int i = 0; i <= 3; ++i) {
                            writer.write(repoCommit[i]);
                            if (i != 3) writer.write(";");
                            else writer.write(" ");
                        }
                    }
                    writer.write(",");
                    for (String[] repoCommit : candidate.possibleCommitList) {
                        for (int i = 0; i <= 3; ++i) {
                            writer.write(repoCommit[i]);
                            if (i != 3) writer.write(";");
                            else writer.write(" ");
                        }
                    }
                    writer.write("\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        writer.close();
    }
}
