package com.motadata.utils;

import org.slf4j.Logger;

public class Constants
{
    private Constants() {}

    public static final String HOST = "host";

    public static final String DATABASE_URI = "database.uri";

    public static final String POLLING_INTERVAL = "polling.interval";

    public static final String DATABASE_USERNAME = "database.username";

    public static final String DATABASE_PASSWORD = "database.password";

    public static final String CONTENT_TYPE = "Content-Type";

    public static final String APP_JSON = "application/json";

    public static final String OBJECT_IP = "object.ip";

    public static final String SNMP_COMMUNITY = "community";

    public static final String RESULT = "result";

    public static final String PORT = "port";

    public static final String REQUEST_TYPE = "request.type";

    public static final String REQ_CONTAINER = "{} {} {}";

    public static final String TABLE_NAME = "table.name";

    public static final String ERROR = "error";

    public static final String DISCOVERY_NAME = "discovery.name";

    public static final String CREDENTIAL_NAME = "credential.name";

    public static final String STATUS = "status";

    public static final String MESSAGE = "message";

    public static final String ERR_MESSAGE = "error.message";

    public static final String ERR_STATUS_CODE = "error.code";

    public static final String SUCCESS = "success";

    public static final String FAILED = "failed";

    public static final String PROTOCOL = "protocol";

    public static final String DISCOVERY_PROFILE_ID = "discovery.profile.id";

    public static final String CREDENTIAL_PROFILE_ID = "credential.profile.id";

    public static final String CREDENTIAL_PROFILE_IDS = "credential.profile.ids";

    public static final String VERSION = "version";

    public static final String INSERT_EVENT = "insert.data";

    public static final String GET_EVENT = "get.data";

    public static final String GET_ALL_EVENT = "get.all.data";

    public static final String UPDATE_EVENT = "update.data";

    public static final String DELETE_EVENT = "delete.data";

    public static final String IS_PROVISIONED = "is.provisioned";

    public static final String IS_DISCOVERED = "is.discovered";

    public static final String MAKE_DISCOVERY_CONTEXT_EVENT = "make.discovery.context";

    public static final String DISCOVERY = "Discovery";

    public static final String COLLECT = "Collect";

    public static final String PLUGIN_NAME = "plugin.name";

    public static final String NETWORK = "Network";

    public static final int TRUE = 1;

    public static final int FALSE = 0;

    public static final String RUN_DISCOVERY_EVENT = "discovery.run";

    public static final String POST_DISCOVERY_SUCCESS_EVENT = "post.discovery.success";

    public static final String DISCOVERY_PROFILE_TABLE = "discovery_profile";

    public static final String NETWORK_INTERFACE_TABLE = "network_interface";

    public static final String CREDENTIAL_PROFILE_TABLE = "credential_profile";

    public static final String GET_PROVISIONED_DEVICES_EVENT = "get.provisioned.devices";

    public static final String STORE_POLLED_DATA_EVENT = "store.polled.data";

    public static final String STOP_POLLING_EVENT = "polling.stop";

    public static final String INTERFACE_INDEX = "interface.index";

    public static final String INTERFACE_NAME = "interface.name";

    public static final String INTERFACE_OPERATIONAL_STATUS = "interface.operational.status";

    public static final String INTERFACE_ADMIN_STATUS = "interface.admin.status";

    public static final String INTERFACE_DESCRIPTION = "interface.description";

    public static final String INTERFACE_SENT_ERROR_PACKET = "interface.sent.error.packet";

    public static final String INTERFACE_RECEIVED_ERROR_PACKET = "interface.received.error.packet";

    public static final String INTERFACE_SENT_OCTETS = "interface.sent.octets";

    public static final String INTERFACE_RECEIVED_OCTETS = "interface.received.octets";

    public static final String INTERFACE_SPEED = "interface.speed";

    public static final String INTERFACE_ALIAS = "interface.alias";

    public static final String INTERFACE_PHYSICAL_ADDRESS = "interface.physical.address";

    public static final String POLL_TIME = "poll.time";

    public static final String INTERFACE = "interface";

    public static final String ROWS_INSERTED_CONTAINER = "{} rows inserted in {}";

    public static final String ROWS_UPDATED_CONTAINER = "{} rows updated in {}";

    public static final String ROWS_DELETED_CONTAINER = "{} rows deleted in {}";

    public static final String KEY_VAL_NOT_FOUND_CONTAINER = "{} = {} not found";

    public static final String ERROR_CONTAINER = "Error: {}";

    public static final String INVALID_TABLE = "Invalid table name!";

    public static final String REQUEST_BODY_ERROR = "Invalid request body!";

    public static final String EMPTY_STRING = "";

    public static final String PROVISION_DEVICE = "provision.device";

    public static final String CREDENTIALS = "credentials";

    public static final String INCORRECT_DATATYPE = "Invalid datatype, Bad request!";
}
