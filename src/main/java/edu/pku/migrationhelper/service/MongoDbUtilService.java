package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.CustomSequences;
import edu.pku.migrationhelper.data.api.ClassSignature;
import edu.pku.migrationhelper.data.lib.*;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;


@Service
public class MongoDbUtilService {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private MongoOperations mongo;

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
        mongoTemplate.indexOps(LibraryVersionToDependency.class)
                .ensureIndex(new CompoundIndexDefinition(compoundIndex).unique());

        compoundIndex = new Document();
        compoundIndex.put("groupId", 1);
        compoundIndex.put("artifactId", 1);
        mongoTemplate.indexOps(LibraryGroupArtifact.class)
                .ensureIndex(new CompoundIndexDefinition(compoundIndex).unique());

        compoundIndex = new Document();
        compoundIndex.put("groupArtifactId", 1);
        compoundIndex.put("version", 1);
        mongoTemplate.indexOps(LibraryVersion.class)
                .ensureIndex(new CompoundIndexDefinition(compoundIndex).unique());

        mongoTemplate.indexOps(LioProject.class).ensureIndex(new Index().on("name", Sort.Direction.ASC));
    }

    public String getDbName() {
        return mongoTemplate.getDb().getName();
    }

    public long getNextIdForCollection(String collectionName)
    {
        CustomSequences counter = mongo.findAndModify(
                query(where("_id").is(collectionName)),
                new Update().inc("seq",1),
                options().returnNew(true).upsert(true),
                CustomSequences.class);
        assert counter != null;
        return counter.getSeq();
    }
}
