package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.mapper.*;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Deprecated
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "CreateTableJob")
public class CreateTableJob {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private BlobInfoMapper blobInfoMapper;

    @Autowired
    private CommitInfoMapper commitInfoMapper;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private LibraryOverlapMapper libraryOverlapMapper;

    @Autowired
    private LibrarySignatureToVersionMapper librarySignatureToVersionMapper;

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private LibraryVersionToSignatureMapper libraryVersionToSignatureMapper;

    @Autowired
    private LioProjectWithRepositoryMapper lioProjectWithRepositoryMapper;

    @Autowired
    private MethodChangeMapper methodChangeMapper;

    @Autowired
    private MethodSignatureMapper methodSignatureMapper;

    @Autowired
    private RepositoryAnalyzeStatusMapper repositoryAnalyzeStatusMapper;

    @Autowired
    private RepositoryDepSeqMapper repositoryDepSeqMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void run() throws Exception {
        for (int i = 0; i < BlobInfoMapper.MAX_TABLE_COUNT; i++) {
            blobInfoMapper.createTable(i);
        }

        for (int i = 0; i < CommitInfoMapper.MAX_TABLE_COUNT; i++) {
            commitInfoMapper.createTable(i);
        }

        libraryGroupArtifactMapper.createTable();

        libraryOverlapMapper.createTable();

        for (int i = 0; i < MethodSignatureMapper.MAX_TABLE_COUNT; i++) {
            librarySignatureToVersionMapper.createTable(i);
        }

        libraryVersionMapper.createTable();

        libraryVersionToSignatureMapper.createTable();

        lioProjectWithRepositoryMapper.createTable();

        for (int i = 0; i < MethodChangeMapper.MAX_TABLE_COUNT; i++) {
            long ii = (long) i;
            long ai = ii << MethodChangeMapper.MAX_ID_BIT;
            methodChangeMapper.createTable(i);
            methodChangeMapper.setAutoIncrement(i, ai);
        }

        for (int i = 0; i < MethodSignatureMapper.MAX_TABLE_COUNT; i++) {
            long ii = (long) i;
            long ai = ii << MethodSignatureMapper.MAX_ID_BIT;
            methodSignatureMapper.createTable(i);
            methodSignatureMapper.setAutoIncrement(i, ai);
        }

        repositoryAnalyzeStatusMapper.createTable();

        repositoryDepSeqMapper.createTable();

        LOG.info("Create Table Success");
        System.exit(SpringApplication.exit(context));
    }

}
