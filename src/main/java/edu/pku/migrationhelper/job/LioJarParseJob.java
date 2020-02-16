package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.data.LioProjectWithRepository;
import edu.pku.migrationhelper.mapper.LioProjectWithRepositoryMapper;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Created by xuyul on 2020/2/16.
 */
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "LioJarParseJob")
public class LioJarParseJob implements CommandLineRunner {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ExecutorService executorService;

    @Value("${migration-helper.lio-jar-parse.limit-count}")
    private int limitCount;

    @Autowired
    private LioProjectWithRepositoryMapper lioProjectWithRepositoryMapper;

    @Autowired
    private LibraryIdentityService libraryIdentityService;

    @Override
    public void run(String... args) throws Exception {
        Set<Long> needParseIds = new HashSet<>();
        needParseIds.addAll(lioProjectWithRepositoryMapper.selectIdOrderByDependentProjectsCountLimit(limitCount));
        needParseIds.addAll(lioProjectWithRepositoryMapper.selectIdOrderByDependentRepositoriesCountLimit(limitCount));
        needParseIds.addAll(lioProjectWithRepositoryMapper.selectIdOrderByRepositoryForkCountLimit(limitCount));
        needParseIds.addAll(lioProjectWithRepositoryMapper.selectIdOrderByRepositoryStarCountLimit(limitCount));
        needParseIds.addAll(lioProjectWithRepositoryMapper.selectIdOrderByRepositoryWatchersCountLimit(limitCount));
        needParseIds.addAll(lioProjectWithRepositoryMapper.selectIdOrderBySourceRankLimit(limitCount));
        needParseIds.addAll(lioProjectWithRepositoryMapper.selectIdOrderByRepositorySourceRankLimit(limitCount));
        for (Long id : needParseIds) {
            executorService.submit(new SingleProjectParse(id));
        }
    }

    private class SingleProjectParse implements Runnable {

        private long projectId;

        public SingleProjectParse(long projectId) {
            this.projectId = projectId;
        }

        @Override
        public void run() {
            LioProjectWithRepository p = lioProjectWithRepositoryMapper.findById(projectId);
            if (p == null) {
               LOG.error("project not found, id = {}", projectId);
               return;
            }
            if(!"Maven".equals(p.getPlatform()) || !"Java".equals(p.getLanguage())) {
                LOG.error("project not Maven or not Java, id = {}", projectId);
                return;
            }
            String name = p.getName();
            String[] nameSplits = name.split(":");
            if(nameSplits.length != 2) {
                LOG.error("nameSplits.length != 2, id = {}, name = {}", projectId, name);
                return;
            }
            try {
                libraryIdentityService.parseGroupArtifact(nameSplits[0], nameSplits[1]);
            } catch (Exception e) {
                LOG.error("parse jar fail, id = " + projectId, e);
            }
        }
    }
}
