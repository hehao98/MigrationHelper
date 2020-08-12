package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.woc.WocDepSeq;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WocDepSeqRepository extends MongoRepository<WocDepSeq, String> {
}
