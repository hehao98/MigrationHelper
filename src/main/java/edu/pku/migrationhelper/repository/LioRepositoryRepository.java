package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.LioRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LioRepositoryRepository extends MongoRepository<LioRepository, Long> {
    Optional<LioRepository> findByName(String name);
}
