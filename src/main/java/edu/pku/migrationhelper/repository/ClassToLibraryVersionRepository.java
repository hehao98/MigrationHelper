package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.api.ClassToLibraryVersion;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClassToLibraryVersionRepository extends MongoRepository<ClassToLibraryVersion, String>  {}
