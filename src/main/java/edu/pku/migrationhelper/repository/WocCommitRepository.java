package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.woc.WocCommit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WocCommitRepository extends MongoRepository<WocCommit, String> {
    List<WocCommit> findAllById(Iterable<String> ids);
}
