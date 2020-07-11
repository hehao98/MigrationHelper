package edu.pku.migrationhelper.job;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import edu.pku.migrationhelper.data.lib.LioProject;
import edu.pku.migrationhelper.mapper.LioProjectWithRepositoryMapper;
import edu.pku.migrationhelper.repository.LioProjectRepository;
import edu.pku.migrationhelper.service.EvaluationService;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by xuyul on 2020/2/16.
 */
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "LioJarParseJob")
public class LioJarParseJob implements CommandLineRunner {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    @Qualifier("ThreadPool")
    private ExecutorService executorService;

    @Autowired
    private LioProjectRepository lioProjectRepository;

    @Autowired
    private LibraryIdentityService libraryIdentityService;

    @Autowired
    private EvaluationService evaluationService;

    @Value("${migration-helper.lio-jar-parse.data-source}")
    @Parameter(names = "--data-source")
    private String dataSource;

    @Value("${migration-helper.lio-jar-parse.limit-count}")
    @Parameter(names = "--limit")
    private int limitCount;

    @Value("${migration-helper.lio-jar-parse.extract-version-only}")
    @Parameter(names = "--extract-version-only", description = "if true, extract versions, update if necessary")
    private boolean extractVersionOnly;

    @Value("${migration-helper.lio-jar-parse.extract-dependencies}")
    @Parameter(names = "--extract-dependencies", description = "if true, extract dependency for each version")
    private boolean extractDependencies;

    @Value("${migration-helper.lio-jar-parse.reverse-order}")
    @Parameter(names = "--reverse-order")
    private boolean reverseOrder;

    @Value("${migration-helper.lio-jar-parse.random-order}")
    @Parameter(names = "--random-order")
    private boolean randomOrder;

    @Override
    public void run(String ...args) throws Exception {
        JCommander.newBuilder().addObject(this).build().parse(args);

        long memoryInBytes = Runtime.getRuntime().maxMemory();
        LOG.info("Maximum amount of memory to use: {} MB", memoryInBytes / 1024 / 1024);

        List<Long> needParseIds = new ArrayList<>();
        if (dataSource.equals("all")) {
            needParseIds = evaluationService.getLioProjectIdsByCombinedPopularity(limitCount);
        } else if (dataSource.equals("ground-truth")) {
            needParseIds = evaluationService.getLioProjectIdsInGroundTruth();
        } else {
            LOG.error("Unknown data source parameter {}, supported: all, ground-truth", dataSource);
            System.exit(SpringApplication.exit(context, () -> -1));
        }

        if (reverseOrder) {
            LOG.info("Reverse parse order (from least popular to most popular)");
            Collections.reverse(needParseIds);
        }
        if (randomOrder) {
            LOG.info("Randomize parse order");
            Collections.shuffle(needParseIds);
        }

        int i = 0;
        int size = needParseIds.size();
        LOG.info("{} libraries to parse", size);
        for (Long id : needParseIds) {
            executorService.submit(new SingleProjectParse(id, size - i));
            ++i;
        }
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        LOG.info("Finished to parse all libraries");
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private class SingleProjectParse implements Runnable {

        private final long projectId;

        private final long jobId;

        public SingleProjectParse(long projectId, long jobId) {
            this.projectId = projectId;
            this.jobId = jobId;
        }

        @Override
        public void run() {
            Optional<LioProject> opt = lioProjectRepository.findById(projectId);
            if (!opt.isPresent()) {
               LOG.error("Project not found, id = {}", projectId);
               return;
            }
            LioProject p = opt.get();
            if(!"Maven".equals(p.getPlatform())) {
                LOG.error("Project not Maven, id = {}", projectId);
                return;
            }
            String name = p.getName();
            String[] nameSplits = name.split(":");
            if(nameSplits.length != 2) {
                LOG.error("nameSplits.length != 2, id = {}, name = {}", projectId, name);
                return;
            }
            LOG.info("Parse job start jobId = {}, projectId = {}, name = {}", jobId, projectId, name);
            try {
                if (extractVersionOnly) {
                    libraryIdentityService.extractVersions(nameSplits[0], nameSplits[1]);
                } else if (extractDependencies) {
                    libraryIdentityService.extractDependencies(nameSplits[0], nameSplits[1]);
                } else {
                    libraryIdentityService.parseGroupArtifact(nameSplits[0], nameSplits[1]);
                }
            } catch (Exception e) {
                LOG.error("Parse jar fail, id = {}, library = {}, {}", projectId, name, e);
            }
            LOG.info("Parse job finished jobId = {}, projectId = {}, library = {}", jobId, projectId, name);
        }
    }
}
