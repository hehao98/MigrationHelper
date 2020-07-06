package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.service.MongoDbUtilService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "MongoDbInitializeJob")
public class MongoDbInitializeJob implements CommandLineRunner {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private MongoDbUtilService mongoDbUtilService;

    @Override
    public void run(String[] args) {
        mongoDbUtilService.initMongoDb();
        LOG.info("Success");
        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
