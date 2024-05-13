CREATE TABLE `nmsDB`.`credential_profile`
(
    `cred.profile.id` INT AUTO_INCREMENT PRIMARY KEY,
    `cred.name`       VARCHAR(255) NOT NULL UNIQUE,
    protocol          VARCHAR(255) NOT NULL,
    version           VARCHAR(255) NOT NULL,
    `snmp.community`  VARCHAR(255) NOT NULL
);

CREATE TABLE `nmsDB`.`discovery_profile`
(
    `disc.profile.id` INT AUTO_INCREMENT PRIMARY KEY,
    `disc.name`       VARCHAR(255) NOT NULL UNIQUE,
    `object.ip`       VARCHAR(15)  NOT NULL,
    `cred.profile.id` INT,
    `snmp.port`       VARCHAR(5)   NOT NULL DEFAULT '161',
    `is.provisioned`  TINYINT(1)   NOT NULL DEFAULT 0,
    `is.discovered`   TINYINT(1)   NOT NULL DEFAULT 0,
    FOREIGN KEY (`cred.profile.id`) REFERENCES nmsDB.credential_profile (`cred.profile.id`),
    UNIQUE KEY (`object.ip`, `cred.profile.id`)
);

CREATE TABLE `nmsDB`.`profile_mapping`
(
    `disc.profile.id` INT NOT NULL,
    `cred.profile.id` INT NOT NULL,
    FOREIGN KEY (`disc.profile.id`) REFERENCES nmsDB.discovery_profile (`disc.profile.id`),
    FOREIGN KEY (`cred.profile.id`) REFERENCES nmsDB.credential_profile (`cred.profile.id`)
);
