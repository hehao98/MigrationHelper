package edu.pku.migrationhelper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins(
                        "http://localhost:8080",
                        "http://migration-helper.net",
                        "http://migration-helper.org",
                        "http://migration-helper.com"
                );
            }
        };
    }
}
