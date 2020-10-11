package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.LibraryMigrationCandidate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LibraryMigrationCandidateRepository extends MongoRepository<LibraryMigrationCandidate, Long> {
    List<LibraryMigrationCandidate> findByFromId(long fromId);
}
