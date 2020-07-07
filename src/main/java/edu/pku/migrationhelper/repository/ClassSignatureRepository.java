package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.api.ClassSignature;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * For other methods please refer to its parent class
 */
public interface ClassSignatureRepository extends MongoRepository<ClassSignature, String> {

    Optional<ClassSignature> findById(String id);

    List<ClassSignature> findByClassName(String className);

    /**
     * This is handy for querying classes by package name,
     *   as long as className has a sorted index
     */
    List<ClassSignature> findByClassNameStartingWith(String prefix);
}