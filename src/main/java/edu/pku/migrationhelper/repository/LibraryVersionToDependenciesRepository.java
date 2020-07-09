package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.lib.LibraryVersionToDependency;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LibraryVersionToDependenciesRepository extends MongoRepository<LibraryVersionToDependency, Long> {

    Optional<LibraryVersionToDependency> findByGroupIdAndArtifactIdAndVersion(String groupId, String artifactId, String version);

    List<LibraryVersionToDependency> findByGroupIdAndArtifactId(String groupId, String artifactId);
}
