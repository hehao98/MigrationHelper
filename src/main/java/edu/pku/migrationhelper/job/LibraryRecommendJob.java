package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.LibraryGroupArtifact;
import edu.pku.migrationhelper.mapper.LibraryGroupArtifactMapper;
import edu.pku.migrationhelper.service.DependencyChangePatternAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.event.EventListener;
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
public class LibraryRecommendJob {

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private DependencyChangePatternAnalysisService dependencyChangePatternAnalysisService;

    private Logger LOG = LoggerFactory.getLogger(getClass());

    private Map<Long, LibraryGroupArtifact> groupArtifactCache = null;

    private String apiSupportFile;

    private String dependencySeqFile;

    private String queryFile;

    private String outputFile;

    @EventListener(ApplicationReadyEvent.class)
    public void miningLibrariesSave2File() throws Exception {
        buildGroupArtifactCache();
        Map<Long, Map<Long, Integer>> methodChangeSupportMap = buildMethodChangeSupportMap(apiSupportFile);
        List<List<Long>> rdsList = buildRepositoryDepSeq(dependencySeqFile);
        List<LibraryGroupArtifact> queryList = readLibraryFromQueryFile(queryFile);
        Set<Long> fromIdLimit = new HashSet<>();
        queryList.forEach(e -> fromIdLimit.add(e.getId()));
        Map<Long, List<DependencyChangePatternAnalysisService.LibraryMigrationCandidate>> result =
                dependencyChangePatternAnalysisService.miningLibraryMigrationCandidate(
                        rdsList, fromIdLimit, methodChangeSupportMap);
        FileWriter resultWriter = new FileWriter(outputFile);
        result.forEach((fromId, candidateList) -> {
            LibraryGroupArtifact fromLib = groupArtifactCache.get(fromId);
            candidateList = candidateList.stream()
                    .filter(candidate -> {
                        LibraryGroupArtifact toLib = groupArtifactCache.get(candidate.toId);
                        return !Objects.equals(toLib.getGroupId(), fromLib.getGroupId());
                    }).collect(Collectors.toList());
            if(candidateList.isEmpty()) return;
            candidateList = candidateList.stream()
                    .limit(20)
                    .collect(Collectors.toList());
            try {
                resultWriter.write(fromLib.getGroupId());
                resultWriter.write(":");
                resultWriter.write(fromLib.getArtifactId());
                for (DependencyChangePatternAnalysisService.LibraryMigrationCandidate candidate : candidateList) {
                    LibraryGroupArtifact toLib = groupArtifactCache.get(candidate.toId);
                    resultWriter.write(",");
                    resultWriter.write(toLib.getGroupId());
                    resultWriter.write(":");
                    resultWriter.write(toLib.getArtifactId());
                }
                resultWriter.write("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        resultWriter.close();
        LOG.info("Success");
    }

    public synchronized void buildGroupArtifactCache() {
        if(groupArtifactCache != null) return;
        List<LibraryGroupArtifact> list = libraryGroupArtifactMapper.findAll();
        Map<Long, LibraryGroupArtifact> map = new HashMap<>(list.size() * 2);
        for (LibraryGroupArtifact groupArtifact : list) {
            map.put(groupArtifact.getId(), groupArtifact);
        }
        groupArtifactCache = Collections.unmodifiableMap(map);
    }

    public List<LibraryGroupArtifact> readLibraryFromQueryFile(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
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

    public static Map<Long, Map<Long, Integer>> buildMethodChangeSupportMap(String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = reader.readLine();
        Map<Long, Map<Long, Integer>> result = new HashMap<>(100000);
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",");
            Long fromId = Long.parseLong(attrs[0]);
            Long toId = Long.parseLong(attrs[1]);
            Integer counter = Integer.parseInt(attrs[2]);
            result.computeIfAbsent(fromId, k -> new HashMap<>()).put(toId, counter);
        }
        reader.close();
        return result;
    }

    public static List<List<Long>> buildRepositoryDepSeq(String fileName) throws Exception {
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

    /* configuration getter and setter */

    public String getApiSupportFile() {
        return apiSupportFile;
    }

    public LibraryRecommendJob setApiSupportFile(String apiSupportFile) {
        this.apiSupportFile = apiSupportFile;
        return this;
    }

    public String getDependencySeqFile() {
        return dependencySeqFile;
    }

    public LibraryRecommendJob setDependencySeqFile(String dependencySeqFile) {
        this.dependencySeqFile = dependencySeqFile;
        return this;
    }

    public String getQueryFile() {
        return queryFile;
    }

    public LibraryRecommendJob setQueryFile(String queryFile) {
        this.queryFile = queryFile;
        return this;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public LibraryRecommendJob setOutputFile(String outputFile) {
        this.outputFile = outputFile;
        return this;
    }
}
