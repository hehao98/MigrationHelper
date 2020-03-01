package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.mapper.*;
import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Created by xuyul on 2020/2/29.
 */
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "CreateTableJob")
public class CreateTableJob {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private BlobInfoMapper blobInfoMapper;

    @Autowired
    private CommitInfoMapper commitInfoMapper;

    @Autowired
    private LibraryGroupArtifactMapper libraryGroupArtifactMapper;

    @Autowired
    private LibrarySignatureToVersionMapper librarySignatureToVersionMapper;

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

    @Autowired
    private LibraryVersionToSignatureMapper libraryVersionToSignatureMapper;

    @Autowired
    private LioProjectWithRepositoryMapper lioProjectWithRepositoryMapper;

    @Autowired
    private MethodSignatureMapper methodSignatureMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void run() throws Exception {

        for (int i = 0; i < 128; i++) {
            blobInfoMapper.createTable(i);
        }

        for (int i = 0; i < 128; i++) {
            commitInfoMapper.createTable(i);
        }

        libraryGroupArtifactMapper.createTable();

        for (int i = 0; i < 128; i++) {
            librarySignatureToVersionMapper.createTable(i);
        }

        libraryVersionMapper.createTable();

        libraryVersionToSignatureMapper.createTable();

        lioProjectWithRepositoryMapper.createTable();

        for (int i = 0; i < 128; i++) {
            long ii = (long) i;
            long ai = ii << LibraryIdentityService.MAX_SIGNATURE_BIT;
            methodSignatureMapper.createTable(i);
            methodSignatureMapper.setAutoIncrement(i, ai);
        }

        LOG.info("Create Table Success");
    }

}
