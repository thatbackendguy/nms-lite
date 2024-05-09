CREATE TABLE nmsDB.credential_profile
(
    `cred.profile.id` VARCHAR(255) NOT NULL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    protocol          VARCHAR(255) NOT NULL,
    version           VARCHAR(255) NOT NULL,
    `snmp.community`  VARCHAR(255) NOT NULL
);

CREATE TABLE nmsDB.discovery_profile
(
    name              VARCHAR(255) NOT NULL UNIQUE,
    `disc.profile.id` VARCHAR(255) NOT NULL PRIMARY KEY,
    `object.ip`       VARCHAR(15)  NOT NULL,
    `cred.profile.id` VARCHAR(255) NOT NULL,
    `snmp.port`       VARCHAR(5)   NOT NULL DEFAULT '161',
    FOREIGN KEY (`cred.profile.id`) REFERENCES credential_profile (`cred.profile.id`)
);