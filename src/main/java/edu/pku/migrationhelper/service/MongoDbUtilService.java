package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.ClassSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;

@Service
public class MongoDbUtilService {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private MongoTemplate mongoTemplate;

    public void initMongoDB() {
        setIndexForClassSignatureCollection();
    }

    public void setIndexForClassSignatureCollection() {
        mongoTemplate.indexOps(ClassSignature.class).ensureIndex(new Index().on("className", Sort.Direction.ASC));
    }
}
