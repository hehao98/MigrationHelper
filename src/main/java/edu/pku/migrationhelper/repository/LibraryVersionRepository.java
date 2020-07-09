package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.lib.LibraryVersion;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface LibraryVersionRepository extends MongoRepository<LibraryVersion, Long> {
    List<LibraryVersion> findByGroupArtifactId(long groupArtifactId);
}
