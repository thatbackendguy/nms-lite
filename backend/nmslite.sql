CREATE TABLE `credential_profile`
(
    `cred.profile.id` int          NOT NULL AUTO_INCREMENT,
    `cred.name`       varchar(255) NOT NULL,
    `protocol`        varchar(255) NOT NULL,
    `version`         varchar(255) NOT NULL,
    `snmp.community`  varchar(255) NOT NULL DEFAULT 'public',
    PRIMARY KEY (`cred.profile.id`),
    UNIQUE KEY `cred.name` (`cred.name`)
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
