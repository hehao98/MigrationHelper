package edu.pku.migrationhelper.job;

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

    private String queryFile;

    private String outputFile;

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 2) {
            LOG.info("Usage: LibraryRecommendJob <Query File> <Output File>");
            System.exit(SpringApplication.exit(context, () -> -1));
        }

        this.queryFile = args[0];
        this.outputFile = args[1];
        LOG.info("Read libraries from {} and output results to {}", this.queryFile, this.outputFile);

        List<LibraryGroupArtifact> queryList = readLibraryFromQueryFile();
        Set<Long> fromIdLimit = new HashSet<>();
        queryList.forEach(e -> fromIdLimit.add(e.getId()));

        LOG.info("Generating recommendation result...");
        Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result =
                dependencyChangePatternAnalysisService.miningLibraryMigrationCandidate(fromIdLimit);

        LOG.info("Writing results to csv...");
        outputCsv(queryList, result);

        LOG.info("Doing evaluation...");
        EvaluationService.EvaluationResult evaluationResult = evaluationService.evaluate(result, 10);
        evaluationService.printEvaluationResult(evaluationResult, System.out);

        LOG.info("Success");
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private List<LibraryGroupArtifact> readLibraryFromQueryFile() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(queryFile));
        String line;
        List<LibraryGroupArtifact> result = new LinkedList<>();
        while((line = reader.readLine()) != null) {
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

    private void outputCsv(List<LibraryGroupArtifact> queryList, Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result) {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputFile), CSVFormat.DEFAULT)) {
            printer.printRecord("fromId", "toId", "fromGroupArtifact", "toGroupArtifact", "confidence",
                    "ruleFreq", "relativeRuleFreq", "concurrence", "concurrenceAdjustment", "commitDistance", "apiSupport");
            for (LibraryGroupArtifact fromLib : queryList) {
                Long fromId = fromLib.getId();
                List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>
                        candidateList = result.get(fromId);

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
                    printer.printRecord(fromId, candidate.toId, fromLib.getGroupArtifactId(),
                            toLib.getGroupArtifactId(), candidate.confidence, candidate.ruleCount,
                            candidate.ruleSupportByMax, candidate.libraryConcurrenceCount, candidate.libraryConcurrenceSupport,
                            candidate.commitDistance, candidate.methodChangeSupportByMax);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static List<List<Long>> buildRepositoryDepSeq(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = reader.readLine();
        List<List<Long>> result = new LinkedList<>();
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            if(attrs.length < 3) {
                System.out.println(line);
            }
            String libIdString = attrs[2]; // pomOnly
//            String libIdString = attrs[3]; // codeWithDup
//            String libIdString = attrs[4]; // codeWithoutDup
//            String libIdString = attrs[5]; // pomWithCodeDel
//            String libIdString = attrs[6]; // pomWithCodeAdd
            if ("".equals(libIdString)) continue;
            String[] libIds = libIdString.split(";");
            List<Long> libIdList = new ArrayList<>(libIds.length);
            for (String libId : libIds) {
                libIdList.add(Long.parseLong(libId));
            }
            result.add(libIdList);
        }
        reader.close();
        return result;
    }

    public static List<List<String>> buildDepSeqCommitList(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = reader.readLine();
        List<List<String>> result = new LinkedList<>();
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            if(attrs.length < 3) {
                System.out.println(line);
            }
            String libIdString = attrs[2]; // pomOnly
            if ("".equals(libIdString)) continue;
            String commitListString = attrs[7];
            int len = commitListString.length();
            int commitCount = len / 40;
            List<String> commitList = new ArrayList<>(commitCount);
            for (int i = 0; i < commitCount; i++) {
                commitList.add(commitListString.substring(i * 40, i * 40 + 40));
            }
            result.add(commitList);
        }
        reader.close();
        return result;
    }

    public static List<String> buildDepSeqRepoList(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = reader.readLine();
        List<String> result = new LinkedList<>();
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",", -1);
            if(attrs.length < 3) {
                System.out.println(line);
            }
            String libIdString = attrs[2]; // pomOnly
            if ("".equals(libIdString)) continue;
            result.add(attrs[1]);
        }
        reader.close();
        return result;
    }
}
