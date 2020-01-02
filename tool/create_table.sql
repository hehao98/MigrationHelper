CREATE TABLE `library_version` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                 `group_id` varchar(63) DEFAULT NULL,
                                 `artifact_id` varchar(63) DEFAULT NULL,
                                 `version` varchar(63) DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `query1` (`group_id`,`artifact_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


