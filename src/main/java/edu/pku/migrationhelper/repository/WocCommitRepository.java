package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.woc.WocCommit;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WocCommitRepository extends MongoRepository<WocCommit, String> {
}
