CREATE TABLE `library_group_artifact` (
                                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                          `group_id` varchar(63) DEFAULT NULL,
                                          `artifact_id` varchar(63) DEFAULT NULL,
                                          `version_extracted` bit(1) NOT NULL,
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `unique` (`group_id`,`artifact_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `library_signature_map` (
                                         `library_version_id` bigint(20) NOT NULL,
                                         `method_signature_id` bigint(20) NOT NULL,
                                         PRIMARY KEY (`library_version_id`,`method_signature_id`),
                                         KEY `index2` (`method_signature_id`,`library_version_id`)
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

CREATE TABLE `method_signature` (
                                    `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                    `package_name` varchar(127) NOT NULL,
                                    `class_name` varchar(127) NOT NULL,
                                    `method_name` varchar(127) NOT NULL,
                                    `param_list` varchar(639) NOT NULL,
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `unique` (`package_name`,`class_name`,`method_name`,`param_list`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



