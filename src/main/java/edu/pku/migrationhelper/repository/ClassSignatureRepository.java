package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.ClassSignature;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClassSignatureRepository extends MongoRepository<ClassSignature, Long> {
    ClassSignature findById(long id);
    ClassSignature findByClassName(String className);
}