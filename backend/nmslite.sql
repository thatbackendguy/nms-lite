CREATE TABLE `credential_profile`
(
    `cred.profile.id` int          NOT NULL AUTO_INCREMENT,
    `cred.name`       varchar(255) NOT NULL,
    `protocol`        varchar(255) NOT NULL,
    `version`         varchar(255) NOT NULL,
    `snmp.community`  varchar(255) NOT NULL DEFAULT 'public',
    PRIMARY KEY (`cred.profile.id`),
    UNIQUE KEY `cred.name` (`cred.name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 36
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;



CREATE TABLE `discovery_profile`
(
    `disc.profile.id` int          NOT NULL AUTO_INCREMENT,
    `disc.name`       varchar(255) NOT NULL,
    `object.ip`       varchar(15)  NOT NULL,
    `snmp.port`       int          NOT NULL DEFAULT '161',
    `is.provisioned`  tinyint(1)   NOT NULL DEFAULT '0',
    `is.discovered`   tinyint(1)   NOT NULL DEFAULT '0',
    `credentials`     json         NOT NULL,
    PRIMARY KEY (`disc.profile.id`),
    UNIQUE KEY `disc.name` (`disc.name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 7
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;


CREATE TABLE `profile_mapping`
(
    `disc.profile.id` int NOT NULL,
    `cred.profile.id` int NOT NULL,
    KEY `disc.profile.id` (`disc.profile.id`),
    KEY `cred.profile.id` (`cred.profile.id`),
    CONSTRAINT `profile_mapping_ibfk_1` FOREIGN KEY (`disc.profile.id`) REFERENCES `discovery_profile` (`disc.profile.id`),
    CONSTRAINT `profile_mapping_ibfk_2` FOREIGN KEY (`cred.profile.id`) REFERENCES `credential_profile` (`cred.profile.id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `network_interface`
(
    `record.id`                       int          NOT NULL AUTO_INCREMENT,
    `object.ip`                       varchar(15)  NOT NULL,
    `snmp.community`                  varchar(255) NOT NULL,
    `snmp.port`                       int          NOT NULL,
    `interface.index`                 int               DEFAULT NULL,
    `interface.name`                  varchar(255)      DEFAULT NULL,
    `interface.operational.status`    int               DEFAULT NULL,
    `interface.admin.status`          int               DEFAULT NULL,
    `interface.description`           varchar(255)      DEFAULT NULL,
    `interface.sent.error.packet`     bigint            DEFAULT NULL,
    `interface.received.error.packet` bigint            DEFAULT NULL,
    `interface.sent.octets`           bigint            DEFAULT NULL,
    `interface.received.octets`       bigint            DEFAULT NULL,
    `interface.speed`                 bigint            DEFAULT NULL,
    `interface.alias`                 varchar(255)      DEFAULT NULL,
    `interface.physical.address`      varchar(255)      DEFAULT NULL,
    `created.at`                      timestamp    NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`record.id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
