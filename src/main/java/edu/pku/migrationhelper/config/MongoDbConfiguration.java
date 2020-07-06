package edu.pku.migrationhelper.config;

import com.mongodb.client.MongoClient;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
// @EnableAutoConfiguration // create MongoTemplate and MongoOperations
@EnableMongoRepositories(basePackages = "edu.pku.migrationhelper.repository")
public class MongoDbConfiguration {
}
