package edu.pku.migrationhelper.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.WriteResultChecking;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "edu.pku.migrationhelper.repository")
public class MongoDbConfiguration extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.host}")
    private String host;

    @Value("${spring.data.mongodb.port}")
    private int port;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public @Bean MongoClient mongoClient() {
        return MongoClients.create(String.format("mongodb://%s:%d", host, port));
    }

    @Override
    public @Bean MongoTemplate mongoTemplate() throws Exception {
        MongoTemplate template = new MongoTemplate(mongoDbFactory(), mappingMongoConverter());
        template.setWriteResultChecking(WriteResultChecking.EXCEPTION);
        return template;
    }

    @Override
    public @Bean MappingMongoConverter mappingMongoConverter() throws Exception {
        MappingMongoConverter converter = super.mappingMongoConverter();
        //converter.setMapKeyDotReplacement("/");
        return converter;
    }
}
