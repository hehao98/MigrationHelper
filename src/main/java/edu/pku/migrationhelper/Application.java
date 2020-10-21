package edu.pku.migrationhelper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        TransactionAutoConfiguration.class,
})
public class Application {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--web")) {
            System.out.println("Running as Web server...");
            SpringApplication.run(Application.class);
        } else {
            System.out.println("Running as command line tool...");
            SpringApplication springApplication = new SpringApplication(Application.class);
            springApplication.setWebApplicationType(WebApplicationType.NONE);
            springApplication.run(args);
        }
    }
}
