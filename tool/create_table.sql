CREATE TABLE IF NOT EXISTS blob_info (
                           blob_id varchar(40) NOT NULL,
                           blob_type varchar(15) NOT NULL,
                           library_signature_ids text NOT NULL,
                           library_version_ids text NOT NULL,
                           PRIMARY KEY (blob_id)
);

CREATE TABLE IF NOT EXISTS commit_info (
                             commit_id varchar(40) NOT NULL,
                             code_library_version_ids text NOT NULL,
                             pom_library_version_ids text NOT NULL,
                             PRIMARY KEY (commit_id)
);

CREATE TABLE IF NOT EXISTS library_group_artifact (
                                        id integer PRIMARY KEY AUTOINCREMENT,
                                        group_id varchar(63) DEFAULT NULL,
                                        artifact_id varchar(63) DEFAULT NULL,
                                        version_extracted tinyint NOT NULL,
                                        UNIQUE (group_id,artifact_id)
);

CREATE TABLE IF NOT EXISTS library_signature_map (
                                       library_version_id integer NOT NULL,
                                       method_signature_id integer NOT NULL,
                                       PRIMARY KEY (library_version_id,method_signature_id)
);

CREATE INDEX IF NOT EXISTS library_signature_map_index ON library_signature_map
  (method_signature_id,library_version_id);

CREATE TABLE IF NOT EXISTS library_version (
                                 id integer PRIMARY KEY AUTOINCREMENT,
                                 group_artifact_id integer NOT NULL,
                                 version varchar(63) DEFAULT NULL,
                                 downloaded tinyint NOT NULL,
                                 parsed tinyint NOT NULL,
                                 UNIQUE (group_artifact_id,version)
);

CREATE TABLE IF NOT EXISTS method_signature (
                                  id integer PRIMARY KEY AUTOINCREMENT,
                                  package_name varchar(127) NOT NULL,
                                  class_name varchar(127) NOT NULL,
                                  method_name varchar(127) NOT NULL,
                                  param_list varchar(639) NOT NULL,
                                  UNIQUE (package_name,class_name,method_name,param_list)
);
