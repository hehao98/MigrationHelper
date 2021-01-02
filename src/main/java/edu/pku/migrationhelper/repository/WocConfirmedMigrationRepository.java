package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.woc.WocConfirmedMigration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WocConfirmedMigrationRepository extends MongoRepository<WocConfirmedMigration, String> {
    Page<WocConfirmedMigration> findAll(Pageable pageable);
}
