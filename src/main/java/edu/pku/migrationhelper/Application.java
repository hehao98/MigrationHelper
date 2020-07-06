package edu.pku.migrationhelper;

import edu.pku.migrationhelper.config.MongoDbConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Created by xuyul on 2020/1/2.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        TransactionAutoConfiguration.class,
})
public class Application {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(Application.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }
}
