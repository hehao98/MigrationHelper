package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.service.WocRepositoryAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.ExecutorService;

/**
 * Created by xuyul on 2020/2/23.
 */
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "WocRepoAnalysisJob")
public class WocRepoAnalysisJob {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private WocRepositoryAnalysisService wocRepositoryAnalysisService;

    @Value("${migration-helper.woc-repo-analysis-job.repository-list-file}")
    private String repositoryListFile;

    @Autowired
    @Qualifier("ThreadPool")
    private ExecutorService executorService;

    @EventListener(ApplicationReadyEvent.class)
    public void run() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(repositoryListFile));
        int jobId = 1;
        String line;
        while((line = reader.readLine()) != null) {
            String[] attrs = line.split(",");
            if(attrs.length < 2) {
                continue;
            }
            if(!"Java".equals(attrs[1])) {
                continue;
            }
            String[] urlFields = attrs[0].split("/");
            if(urlFields.length < 2) {
                continue;
            }
            String repoName = urlFields[urlFields.length - 2] + "_" + urlFields[urlFields.length - 1];
            executorService.submit(new SingleRepoJob(jobId++, repoName));
        }
    }

    public class SingleRepoJob implements Runnable {

        private int jobId;

        private String repoName;

        public SingleRepoJob(int jobId, String repoName) {
            this.jobId = jobId;
            this.repoName = repoName;
        }

        @Override
        public void run() {
            try {
                LOG.info("jobId = {} start, repoName = {}", jobId, repoName);
                wocRepositoryAnalysisService.analyzeRepositoryLibrary(repoName);
                LOG.info("jobId = {} success, repoName = {}", jobId, repoName);
            } catch (Exception e) {
                LOG.error("jobId = " + jobId + " fail, repoName = " + repoName, e);
            }
        }
    }
}
