package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.api.LibraryVersionToClass;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LibraryVersionToClassRepository extends MongoRepository<LibraryVersionToClass, Long> {
}
