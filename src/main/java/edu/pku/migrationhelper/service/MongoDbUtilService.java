package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.api.ClassSignature;
import edu.pku.migrationhelper.data.api.LibraryVersionToClass;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;

@Service
public class MongoDbUtilService {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private MongoTemplate mongoTemplate;

    public void initMongoDb() {
        mongoTemplate.indexOps(ClassSignature.class).ensureIndex(new Index().on("className", Sort.Direction.ASC));
        Document compoundIndex = new Document();
        compoundIndex.put("groupId", 1);
        compoundIndex.put("artifactId", 1);
        compoundIndex.put("version", 1);
        mongoTemplate.indexOps(LibraryVersionToClass.class)
                .ensureIndex(new CompoundIndexDefinition(compoundIndex).unique());
    }

    public String getDbName() {
        return mongoTemplate.getDb().getName();
    }
}
