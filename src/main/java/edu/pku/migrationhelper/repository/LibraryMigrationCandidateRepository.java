package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.LibraryMigrationCandidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LibraryMigrationCandidateRepository extends MongoRepository<LibraryMigrationCandidate, Long> {
    List<LibraryMigrationCandidate> findByFromId(long fromId);
    Optional<LibraryMigrationCandidate> findByFromIdAndToId(long fromId, long toId);
    List<LibraryMigrationCandidate> findByFromIdOrderByConfidenceDesc(long fromId);
    Page<LibraryMigrationCandidate> findByFromIdOrderByConfidenceDesc(long fromId, Pageable pageable);
}
