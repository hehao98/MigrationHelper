package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.lib.LibraryVersionToClass;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LibraryVersionToClassRepository extends MongoRepository<LibraryVersionToClass, Long> {

    Optional<LibraryVersionToClass> findByGroupIdAndArtifactIdAndVersion(String groupId, String artifactId, String version);

    List<LibraryVersionToClass> findByGroupIdAndArtifactId(String groupId, String artifactId);
}
