package edu.pku.migrationhelper.repository;

import edu.pku.migrationhelper.data.LioProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LioProjectRepository extends MongoRepository<LioProject, Long> {
    Optional<LioProject> findByName(String name);
    Page<LioProject> findAll(Pageable pageable);
}
