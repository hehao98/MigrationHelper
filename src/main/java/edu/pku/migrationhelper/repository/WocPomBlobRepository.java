package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.woc.WocPomBlob;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WocPomBlobRepository extends MongoRepository<WocPomBlob, String> {
}
