package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.woc.WocConfirmedMigration;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WocConfirmedMigrationRepository extends MongoRepository<WocConfirmedMigration, String> {
}
