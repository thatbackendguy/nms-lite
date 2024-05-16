CREATE TABLE `credential_profile`
(
    `credential.profile.id` int          NOT NULL AUTO_INCREMENT,
    `credential.name`       varchar(255) NOT NULL,
    `protocol`              varchar(255) NOT NULL,
    `version`               varchar(255) NOT NULL,
    `community`             varchar(255) NOT NULL DEFAULT 'public',
    PRIMARY KEY (`credential.profile.id`),
    UNIQUE KEY `credential.name` (`credential.name`)
);


CREATE TABLE `discovery_profile`
(
    `discovery.profile.id` int          NOT NULL AUTO_INCREMENT,
    `discovery.name`       varchar(255) NOT NULL,
    `object.ip`            varchar(15)  NOT NULL,
    `port`                 int          NOT NULL DEFAULT '161',
    `is.provisioned`       tinyint(1)   NOT NULL DEFAULT '0',
    `is.discovered`        int          NOT NULL DEFAULT '0',
    PRIMARY KEY (`discovery.profile.id`),
    UNIQUE KEY `discovery.name` (`discovery.name`)
);


CREATE TABLE `profile_mapping`
(
    `discovery.profile.id`  int NOT NULL,
    `credential.profile.id` int NOT NULL,
    KEY `profile_mapping_ibfk_1` (`discovery.profile.id`),
    KEY `profile_mapping_ibfk_2` (`credential.profile.id`),
    CONSTRAINT `profile_mapping_ibfk_1` FOREIGN KEY (`discovery.profile.id`) REFERENCES `discovery_profile` (`discovery.profile.id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `profile_mapping_ibfk_2` FOREIGN KEY (`credential.profile.id`) REFERENCES `credential_profile` (`credential.profile.id`)
);

CREATE TABLE `network_interface`
(
    `object.ip`                       varchar(50) NOT NULL,
    `interface.index`                 int              DEFAULT NULL,
    `interface.name`                  varchar(255)     DEFAULT NULL,
    `interface.operational.status`    int              DEFAULT NULL,
    `interface.admin.status`          int              DEFAULT NULL,
    `interface.description`           varchar(255)     DEFAULT NULL,
    `interface.sent.error.packet`     bigint   DEFAULT NULL,
    `interface.received.error.packet` bigint   DEFAULT NULL,
    `interface.sent.octets`           bigint   DEFAULT NULL,
    `interface.received.octets`       bigint   DEFAULT NULL,
    `interface.speed`                 bigint           DEFAULT NULL,
    `interface.alias`                 varchar(255)     DEFAULT NULL,
    `interface.physical.address`      varchar(50)      DEFAULT NULL,
    `poll.time`                       timestamp   NULL DEFAULT NULL
);
