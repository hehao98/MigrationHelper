CREATE TABLE `blob_info` (
                           `blob_id` binary(20) NOT NULL,
                           `blob_type` int NOT NULL,
                           `library_signature_ids` mediumblob,
                           `library_version_ids` mediumblob,
                           `library_group_artifact_ids` mediumblob,
                           PRIMARY KEY (`blob_id`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

CREATE TABLE `commit_info` (
                             `commit_id` binary(20) NOT NULL,
                             `code_library_version_ids` mediumblob,
                             `code_group_artifact_ids` mediumblob,
                             `code_delete_group_artifact_ids` mediumblob,
                             `code_add_group_artifact_ids` mediumblob,
                             `pom_library_version_ids` mediumblob,
                             `pom_group_artifact_ids` mediumblob,
                             `pom_delete_group_artifact_ids` mediumblob,
                             `pom_add_group_artifact_ids` mediumblob,
                             `method_change_ids` mediumblob,
                             PRIMARY KEY (`commit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;


CREATE TABLE `library_group_artifact` (
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                        `group_id` varchar(63) DEFAULT NULL,
                                        `artifact_id` varchar(63) DEFAULT NULL,
                                        `version_extracted` bit(1) NOT NULL,
                                        `parsed` bit(1) NOT NULL,
                                        `parse_error` bit(1) NOT NULL,
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `unique` (`group_id`,`artifact_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `library_signature_to_version` (
                                                   `signature_id` bigint(20) NOT NULL,
                                                   `version_ids` mediumblob,
                                                   `group_artifact_ids` mediumblob,
                                                   PRIMARY KEY (`signature_id`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

CREATE TABLE `library_version` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                 `group_artifact_id` bigint(20) NOT NULL,
                                 `version` varchar(63) DEFAULT NULL,
                                 `downloaded` bit(1) NOT NULL,
                                 `parsed` bit(1) NOT NULL,
                                 `parse_error` bit(1) NOT NULL,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `unique` (`group_artifact_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE `library_version_to_signature` (
                                                   `version_id` bigint(20) NOT NULL,
                                                   `signature_ids` mediumblob,
                                                   PRIMARY KEY (`version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

CREATE TABLE `lio_project_with_repository` (
                                             `id` bigint(20) NOT NULL,
                                             `platform` varchar(255) DEFAULT NULL,
                                             `language` varchar(255) DEFAULT NULL,
                                             `name` varchar(255) DEFAULT NULL,
                                             `repository_url` varchar(255) DEFAULT NULL,
                                             `repository_id` bigint(20) DEFAULT NULL,
                                             `source_rank` int(11) DEFAULT NULL,
                                             `repository_star_count` int(11) DEFAULT NULL,
                                             `repository_fork_count` int(11) DEFAULT NULL,
                                             `repository_watchers_count` int(11) DEFAULT NULL,
                                             `repository_source_rank` int(11) DEFAULT NULL,
                                             `dependent_projects_count` int(11) DEFAULT NULL,
                                             `dependent_repositories_count` int(11) DEFAULT NULL,
                                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `method_change` (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT,
                               `delete_signature_ids` varbinary(1023) NOT NULL,
                               `add_signature_ids` varbinary(1023) NOT NULL,
                               `delete_group_artifact_ids` varbinary(1023) NOT NULL,
                               `add_group_artifact_ids` varbinary(1023) NOT NULL,
                               `counter` bigint(20) NOT NULL,
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `unique_index` (`delete_signature_ids`,`add_signature_ids`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

CREATE TABLE `method_signature` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                  `package_name` varchar(255) NOT NULL,
                                  `class_name` varchar(255) NOT NULL,
                                  `method_name` varchar(255) NOT NULL,
                                  `param_list` varchar(2047) NOT NULL,
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `unique` (`package_name`,`class_name`,`method_name`,`param_list`)
) ENGINE=InnoDB DEFAULT CHARSET=ascii;
