package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.woc.WocRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WocRepositoryRepository extends MongoRepository<WocRepository, Long> {
}
