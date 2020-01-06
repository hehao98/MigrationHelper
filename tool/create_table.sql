CREATE TABLE `library_group_artifact` (
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                        `group_id` varchar(63) DEFAULT NULL,
                                        `artifact_id` varchar(63) DEFAULT NULL,
                                        `version_extracted` bit(1) NOT NULL,
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `unique` (`group_id`,`artifact_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `library_version` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                 `group_artifact_id` bigint(20) NOT NULL,
                                 `version` varchar(63) DEFAULT NULL,
                                 `downloaded` bit(1) NOT NULL,
                                 `parsed` bit(1) NOT NULL,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `unique` (`group_artifact_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
