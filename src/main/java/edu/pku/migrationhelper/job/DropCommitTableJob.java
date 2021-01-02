package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.mapper.BlobInfoMapper;
import edu.pku.migrationhelper.mapper.CommitInfoMapper;
import edu.pku.migrationhelper.mapper.MethodChangeMapper;
import edu.pku.migrationhelper.mapper.RepositoryAnalyzeStatusMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.Scanner;

@Deprecated
// @Component
// @ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "DropCommitTableJob")
public class DropCommitTableJob {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private BlobInfoMapper blobInfoMapper;

    @Autowired
    private CommitInfoMapper commitInfoMapper;

    @Autowired
    private MethodChangeMapper methodChangeMapper;

    @Autowired
    private RepositoryAnalyzeStatusMapper repositoryAnalyzeStatusMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void run() throws Exception {

        Scanner sc = new Scanner(System.in);

        System.out.println("Confirm to drop tables: ");

        String confirm = sc.next();

        if(!"Confirm".equals(confirm)) {
            System.out.println("Not Confirmed, exit");
            return;
        }

        for (int i = 0; i < BlobInfoMapper.MAX_TABLE_COUNT; i++) {
            blobInfoMapper.dropTable(i);
        }

        for (int i = 0; i < CommitInfoMapper.MAX_TABLE_COUNT; i++) {
            commitInfoMapper.dropTable(i);
        }

        for (int i = 0; i < MethodChangeMapper.MAX_TABLE_COUNT; i++) {
            methodChangeMapper.dropTable(i);
        }

        repositoryAnalyzeStatusMapper.dropTable();

        LOG.info("Drop Table Success");

        for (int i = 0; i < BlobInfoMapper.MAX_TABLE_COUNT; i++) {
            blobInfoMapper.createTable(i);
        }

        for (int i = 0; i < CommitInfoMapper.MAX_TABLE_COUNT; i++) {
            commitInfoMapper.createTable(i);
        }

        for (int i = 0; i < MethodChangeMapper.MAX_TABLE_COUNT; i++) {
            long ii = (long) i;
            long ai = ii << MethodChangeMapper.MAX_ID_BIT;
            methodChangeMapper.createTable(i);
            methodChangeMapper.setAutoIncrement(i, ai);
        }

        repositoryAnalyzeStatusMapper.createTable();

        LOG.info("Create Table Success");
    }

}
