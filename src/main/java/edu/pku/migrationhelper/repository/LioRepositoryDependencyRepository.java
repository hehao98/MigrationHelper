package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.lio.LioRepositoryDependency;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LioRepositoryDependencyRepository extends MongoRepository<LioRepositoryDependency, Long> {
    Optional<LioRepositoryDependency> findByRepositoryNameWithOwner(String name);
}