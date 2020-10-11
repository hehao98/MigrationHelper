package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.woc.WocAPICount;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WocAPICountRepository extends MongoRepository<WocAPICount, String> {
}
