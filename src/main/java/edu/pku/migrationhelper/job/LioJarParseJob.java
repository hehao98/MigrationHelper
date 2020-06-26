package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.LioProjectWithRepository;
import edu.pku.migrationhelper.mapper.LioProjectWithRepositoryMapper;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
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

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    @Qualifier("ThreadPool")
    private ExecutorService executorService;

    @Value("${migration-helper.lio-jar-parse.limit-count}")
    private int limitCount;

    @Value("${migration-helper.lio-jar-parse.extract-version-only}")
    private boolean extractVersionOnly = false;

    @Autowired
    private LioProjectWithRepositoryMapper lioProjectWithRepositoryMapper;

    @Autowired
    private LibraryIdentityService libraryIdentityService;

    @Override
    public void run(String ...args) throws Exception {
        long memoryInBytes = Runtime.getRuntime().maxMemory();
        LOG.info("Maximum amount of memory to use: {} MB", memoryInBytes / 1024 / 1024);

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
            for(int i = 0; i < idsArray.length; ++i) {
                if(idsArray[i].hasNext()) {
                    remain = true;
                    long id = idsArray[i].next();
                    if(!idSet.contains(id)) {
                        needParseIds.add(id);
                        idSet.add(id);
                    }
                }
            }
            if(!remain) break;
        }

        // Collections.reverse(needParseIds);

        int i = 0;
        int size = needParseIds.size();
        LOG.info("{} libraries to parse", size);
        for (Long id : needParseIds) {
            executorService.submit(new SingleProjectParse(id, size - i));
            ++i;
        }
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        LOG.info("Finished to parse all libraries");
        System.exit(SpringApplication.exit(context));
    }

    private class SingleProjectParse implements Runnable {

        private long projectId;

        private long jobId;

        public SingleProjectParse(long projectId, long jobId) {
            this.projectId = projectId;
            this.jobId = jobId;
        }

        @Override
        public void run() {
            LioProjectWithRepository p = lioProjectWithRepositoryMapper.findById(projectId);
            if (p == null) {
               LOG.error("project not found, id = {}", projectId);
               return;
            }
            if(!"Maven".equals(p.getPlatform())) {
                LOG.error("project not Maven, id = {}", projectId);
                return;
            }
            String name = p.getName();
            String[] nameSplits = name.split(":");
            if(nameSplits.length != 2) {
                LOG.error("nameSplits.length != 2, id = {}, name = {}", projectId, name);
                return;
            }
            LOG.info("parse job start jobId = {}, projectId = {}, name = {}", jobId, projectId, name);
            try {
                libraryIdentityService.parseGroupArtifact(nameSplits[0], nameSplits[1], extractVersionOnly);
            } catch (Exception e) {
                LOG.error("parse jar fail, id = " + projectId, e);
            }
            LOG.info("parse job finished jobId = {}, projectId = {}, library = {}", jobId, projectId, name);
        }
    }
}
