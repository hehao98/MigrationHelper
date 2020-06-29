package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.mapper.LioProjectWithRepositoryMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that provide evaluation facilities such as
 *   1. loading and querying ground truth
 *   2. select libraries by different criteria
 */
@Service
public class EvaluationService {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private static class GroundTruth {
        String fromLib;
        String toLib;
        List<String> fromGroupArtifacts;
        List<String> toGroupArtifacts;
    }

    @Value("${migration-helper.evaluation.ground-truth-file}")
    private String groundTruthFile;

    @Autowired
    private LioProjectWithRepositoryMapper lioProjectWithRepositoryMapper;

    private List<GroundTruth> groundTruths;

    @PostConstruct
    public void initializeGroundTruth() throws IOException {
        groundTruths = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new FileReader(groundTruthFile))) {
            for (CSVRecord record : parser) {
                GroundTruth gt = new GroundTruth();
                gt.fromLib = record.get("fromLibrary");
                gt.toLib = record.get("toLibrary");
                if (!record.get("fromGroupArtifacts").equals(""))
                    gt.fromGroupArtifacts = Arrays.asList(record.get("fromGroupArtifacts").split(";"));
                else
                    gt.fromGroupArtifacts = new ArrayList<>();
                if (!record.get("toGroupArtifacts").equals(""))
                    gt.toGroupArtifacts = Arrays.asList(record.get("toGroupArtifacts").split(";"));
                else
                    gt.fromGroupArtifacts = new ArrayList<>();
                groundTruths.add(gt);
            }
        }
    }

    public List<Long> getLioProjectIdsInGroundTruth() {
        Set<Long> result = new HashSet<>();
        for (GroundTruth gt : groundTruths) {
            result.addAll(gt.fromGroupArtifacts.stream()
                    .map(s -> lioProjectWithRepositoryMapper.findByName(s).getId())
                    .collect(Collectors.toList()));
            result.addAll(gt.toGroupArtifacts.stream()
                    .map(s -> lioProjectWithRepositoryMapper.findByName(s).getId())
                    .collect(Collectors.toList()));
        }
        return new ArrayList<>(result);
    }

    public List<Long> getLioProjectIdsByCombinedPopularity(int limitCount) {
        LOG.info("Get libraries by combining results from different popularity measure, limit = {}", limitCount);

        Set<Long> idSet = new HashSet<>();
        List<Long> needParseIds = new LinkedList<>();
        Iterator<Long>[] idsArray = new Iterator[7];
        idsArray[0] = lioProjectWithRepositoryMapper.selectIdOrderByDependentProjectsCountLimit(limitCount).iterator();
        idsArray[1] = lioProjectWithRepositoryMapper.selectIdOrderByDependentRepositoriesCountLimit(limitCount).iterator();
        idsArray[2] = lioProjectWithRepositoryMapper.selectIdOrderByRepositoryForkCountLimit(limitCount).iterator();
        idsArray[3] = lioProjectWithRepositoryMapper.selectIdOrderByRepositoryStarCountLimit(limitCount).iterator();
        idsArray[4] = lioProjectWithRepositoryMapper.selectIdOrderByRepositoryWatchersCountLimit(limitCount).iterator();
        idsArray[5] = lioProjectWithRepositoryMapper.selectIdOrderBySourceRankLimit(limitCount).iterator();
        idsArray[6] = lioProjectWithRepositoryMapper.selectIdOrderByRepositorySourceRankLimit(limitCount).iterator();
        while (true) {
            boolean remain = false;
            for (Iterator<Long> longIterator : idsArray) {
                if (longIterator.hasNext()) {
                    remain = true;
                    long id = longIterator.next();
                    if (!idSet.contains(id)) {
                        needParseIds.add(id);
                        idSet.add(id);
                    }
                }
            }
            if(!remain) break;
        }
        return needParseIds;
    }
}
