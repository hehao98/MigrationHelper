package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.ClassSignature;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ClassSignatureRepository extends MongoRepository<ClassSignature, String> {
    Optional<ClassSignature> findById(String id);
    List<ClassSignature> findByClassName(String className);
}