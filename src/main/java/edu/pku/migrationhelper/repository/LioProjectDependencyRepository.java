package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.lio.LioProjectDependency;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LioProjectDependencyRepository extends MongoRepository<LioProjectDependency, Long> {
}
