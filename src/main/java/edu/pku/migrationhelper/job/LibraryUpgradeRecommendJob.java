package edu.pku.migrationhelper.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "LibraryUpgradeRecommendJob")
public class LibraryUpgradeRecommendJob implements CommandLineRunner {

    @Autowired
    private ConfigurableApplicationContext context;

    final private Logger LOG = LoggerFactory.getLogger(getClass());

    @Override
    public void run(String... args) {
        if (args.length < 2) {
            LOG.info("Usage: ./run-xxx.sh LibraryUpgradeRecommendJob <library-input-csv> <output-folder>");
            System.exit(SpringApplication.exit(context, () -> -1));
        }

        String inputCsvPath = args[0];
        String outputFolder = args[1];
        LOG.info("Reading libraries from {} and saving results to {}", inputCsvPath, outputFolder);



        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
