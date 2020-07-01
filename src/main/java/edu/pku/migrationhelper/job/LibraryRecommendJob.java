package edu.pku.migrationhelper.job;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import edu.pku.migrationhelper.data.LibraryGroupArtifact;
import edu.pku.migrationhelper.mapper.LibraryGroupArtifactMapper;
import edu.pku.migrationhelper.service.DependencyChangePatternAnalysisService;
import edu.pku.migrationhelper.service.EvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;

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
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private DependencyChangePatternAnalysisService dependencyChangePatternAnalysisService;

    @Autowired
    private EvaluationService evaluationService;

    @Parameter(names = {"-q", "--query-file"}, required = true)
    public String queryFile;

    @Parameter(names = {"-o", "--output-file",}, required = true)
    public String outputFile;

    @Parameter(names = {"-e", "--evaluate"})
    public boolean evaluate = false;

    @Parameter(names = {"-r", "--output-repo-commit"})
    public boolean outputRepoCommit = false;

    @Override
    public void run(String... args) throws Exception {
        JCommander parser = JCommander.newBuilder().addObject(this).build();
        try {
            parser.parse(args);
        } catch (ParameterException e) {
            parser.usage();
            System.exit(SpringApplication.exit(context, () -> -1));
        }
        LOG.info("Read libraries from {} and output results to {}, evaluate = {}, outputRepoCommit = {}",
                queryFile, outputFile, evaluate, outputRepoCommit);

        List<LibraryGroupArtifact> queryList = readLibraryFromQueryFile();
        Set<Long> fromIdLimit = new HashSet<>();
        queryList.forEach(e -> fromIdLimit.add(e.getId()));

        LOG.info("Generating recommendation result...");
        Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result =
                dependencyChangePatternAnalysisService.miningLibraryMigrationCandidate(fromIdLimit, outputRepoCommit);

        EvaluationService.EvaluationResult evaluationResult = null;
        if (evaluate) {
            LOG.info("Doing evaluation...");
            evaluationResult = evaluationService.evaluate(result, 10);
            evaluationService.printEvaluationResult(evaluationResult, System.out);
            LOG.info("Running RQ1...");
            evaluationService.runRQ1(result);
            LOG.info("Running RQ2...");
            evaluationService.runRQ2();
            LOG.info("Running RQ3...");
            evaluationService.runRQ3(result);
        }

        LOG.info("Writing results to csv...");
        outputCsv(queryList, result, evaluationResult);
        if (outputRepoCommit) outputRelatedRepoAndCommit(result);

        LOG.info("Success");
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private List<LibraryGroupArtifact> readLibraryFromQueryFile() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(queryFile));
        String line;
        List<LibraryGroupArtifact> result = new LinkedList<>();
        while ((line = reader.readLine()) != null) {
            String[] ga = line.split(":");
            LibraryGroupArtifact groupArtifact = libraryGroupArtifactMapper.findByGroupIdAndArtifactId(ga[0], ga[1]);
            if(groupArtifact == null) {
                LOG.warn("groupArtifact not found: {}", line);
                continue;
            }
            result.add(groupArtifact);
        }
        reader.close();
        return result;
    }

    private void outputCsv(
            List<LibraryGroupArtifact> queryList,
            Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> recommendationResult,
            EvaluationService.EvaluationResult evaluationResult
    ) {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputFile), CSVFormat.DEFAULT)) {
            printer.printRecord("fromId", "toId", "fromGroupArtifact", "toGroupArtifact", "isCorrect", "confidence",
                    "ruleFreq", "relativeRuleFreq", "concurrence", "concurrenceAdjustment", "commitDistance", "apiSupport");
            for (LibraryGroupArtifact fromLib : queryList) {
                Long fromId = fromLib.getId();
                List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>
                        candidateList = recommendationResult.get(fromId);

                candidateList = candidateList.stream()
                        .filter(candidate -> {
                            LibraryGroupArtifact toLib = libraryGroupArtifactMapper.findById(candidate.toId);
                            return !Objects.equals(toLib.getGroupId(), fromLib.getGroupId());
                        }).collect(Collectors.toList());

                if(candidateList.isEmpty()) continue;
                candidateList = candidateList.stream()
                        .limit(20)
                        .collect(Collectors.toList());

                for (DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate : candidateList) {
                    LibraryGroupArtifact toLib = libraryGroupArtifactMapper.findById(candidate.toId);
                    String isCorrect = evaluationResult == null ?
                            "unknown" : evaluationResult.correctnessMap.get(fromId).get(candidate.toId).toString();
                    printer.printRecord(fromId, candidate.toId,
                            fromLib.getGroupArtifactId(), toLib.getGroupArtifactId(),
                            isCorrect,
                            candidate.confidence, candidate.ruleCount,
                            candidate.ruleSupportByMax, candidate.libraryConcurrenceCount,
                            candidate.libraryConcurrenceSupport,
                            candidate.commitDistance, candidate.methodChangeSupportByMax);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void outputRelatedRepoAndCommit(
            Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result
    ) throws IOException {
        FileWriter correct = new FileWriter("export/CorrectLibraryMigration.csv");
        FileWriter unknown = new FileWriter("export/UnknownLibraryMigration.csv");
        result.forEach((fromId, candidateList) -> {
            LibraryGroupArtifact fromLib = libraryGroupArtifactMapper.findById(fromId);
            candidateList = candidateList.stream()
                    .filter(candidate -> {
                        LibraryGroupArtifact toLib = libraryGroupArtifactMapper.findById(candidate.toId);
                        return !Objects.equals(toLib.getGroupId(), fromLib.getGroupId());
                    }).collect(Collectors.toList());
            if (candidateList.isEmpty()) return;
            boolean isTruth;
            if (evaluationService.getGroundTruthMap().containsKey(fromId)) {
                isTruth = true;
                Set<Long> thisTruth = evaluationService.getGroundTruthMap().get(fromId);
                candidateList = candidateList.stream()
                        .filter(candidate -> thisTruth.contains(candidate.toId))
                        .limit(20)
                        .collect(Collectors.toList());
            } else {
                isTruth = false;
                candidateList = candidateList.stream()
                        .limit(20)
                        .collect(Collectors.toList());
            }
            if (candidateList.isEmpty()) return;
            FileWriter writer = isTruth ? correct : unknown;
            try {
                for (DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate : candidateList) {
                    writer.write(fromLib.getGroupId());
                    writer.write(":");
                    writer.write(fromLib.getArtifactId());
                    LibraryGroupArtifact toLib = libraryGroupArtifactMapper.findById(candidate.toId);
                    writer.write(",");
                    writer.write(toLib.getGroupId());
                    writer.write(":");
                    writer.write(toLib.getArtifactId());
                    for (String[] repoCommit : candidate.repoCommitList) {
                        writer.write(",");
                        writer.write(repoCommit[0]);
                        writer.write(";");
                        writer.write(repoCommit[1]);
                        writer.write(";");
                        writer.write(repoCommit[2]);
                    }
                    writer.write("\n");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        correct.close();
        unknown.close();
    }
}
