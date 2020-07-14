package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.lib.LibraryGroupArtifact;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LibraryGroupArtifactRepository extends MongoRepository<LibraryGroupArtifact, Long> {
    Optional<LibraryGroupArtifact> findByGroupIdAndArtifactId(String groupId, String artifactId);
}
