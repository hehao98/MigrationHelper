package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.web.AccessLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccessLogRepository  extends MongoRepository<AccessLog, String> {
}
