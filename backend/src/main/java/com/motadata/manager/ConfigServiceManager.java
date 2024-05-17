package com.motadata.manager;


import com.motadata.utils.Utils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static com.motadata.utils.Constants.*;

public class ConfigServiceManager extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigServiceManager.class);

    private final String CREDENTIAL_PROFILE_INSERT_QUERY = "INSERT INTO `nmsDB`.`credential_profile` (`credential.name`, `protocol`, `version`, `community`) VALUES (?,?,?,?);";

    private final String CREDENTIAL_PROFILE_SELECT_QUERY = "SELECT * FROM `credential_profile` where `credential.profile.id`=?;";

    private final String CREDENTIAL_PROFILE_SELECT_ALL_QUERY = "SELECT * FROM `credential_profile`";

    private final String CREDENTIAL_PROFILE_UPDATE_QUERY = "UPDATE `credential_profile` SET `version` = ?, `community` = ? WHERE `credential.profile.id` = ?;";

    private final String DISCOVERY_PROFILE_INSERT_QUERY = "INSERT INTO `nmsDB`.`discovery_profile` (`discovery.name`,`object.ip`,`port`) VALUES (?,?,?);";

    private final String DISCOVERY_PROFILE_SELECT_QUERY = "SELECT * FROM `discovery_profile` WHERE `discovery.profile.id` = ?";

    private final String DISCOVERY_PROFILE_SELECT_ALL_QUERY = "SELECT * FROM `discovery_profile`";

    private final String DISCOVERY_PROFILE_UPDATE_QUERY = "UPDATE `discovery_profile` SET `is.discovered` = 0,`is.provisioned` = 0,`discovery.name` = ?, `port` = ? WHERE `discovery.profile.id` = ?;";

    private final String DISCOVERY_STATUS_UPDATE_QUERY = "UPDATE `discovery_profile` SET `is.discovered` = 1 WHERE `discovery.profile.id` = ?;";

    private final String PROVISION_STATUS_UPDATE_QUERY = "UPDATE `discovery_profile` SET `is.provisioned` = ? WHERE `discovery.profile.id` = ?;";

    private final String CREDENTIAL_PROFILE_DELETE_QUERY = "DELETE FROM `credential_profile` WHERE `credential.profile.id` = ?;";

    private final String DISCOVER_PROFILE_DELETE_QUERY = "DELETE FROM `discovery_profile` WHERE `discovery.profile.id` = ?;";

    private final String PROFILE_MAPPING_INSERT_QUERY = "INSERT INTO `nmsDB`.`profile_mapping` (`discovery.profile.id`,`credential.profile.id`) VALUES (?,?);";

    private final String PROFILE_MAPPING_UNIQUE_CREDENTIAL_PROFILE_IDS_QUERY = "SELECT distinct(`credential.profile.id`) FROM nmsDB.profile_mapping where `discovery.profile.id`=?;";

    private final String PROFILE_MAPPING_SELECT_ALL_QUERY = "SELECT pm.`discovery.profile.id`, pm.`credential.profile.id`, cp.`credential.name`, cp.`protocol`, cp.`version`, cp.`community`, dp.`discovery.name`, dp.`object.ip`, dp.`port`, dp.`is.provisioned`, dp.`is.discovered`, cp.`version` FROM profile_mapping pm JOIN credential_profile cp ON pm.`credential.profile.id` = cp.`credential.profile.id` JOIN discovery_profile dp ON pm.`discovery.profile.id` = dp.`discovery.profile.id`;";

    private final String PROFILE_MAPPING_COUNT_SELECT_QUERY = "SELECT COUNT(*) as count FROM profile_mapping WHERE `credential.profile.id` = ?;";

    private final String METRICS_INSERT_QUERY = "INSERT INTO network_interface (`object.ip`,`interface.index`,`interface.name`,`interface.operational.status`,`interface.admin.status`,`interface.description`,`interface.sent.error.packet`,`interface.received.error.packet`,`interface.sent.octets`,`interface.received.octets`,`interface.speed`,`interface.alias`,`interface.physical.address`,`poll.time`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

    private final String PROVISIONED_DEVICES_SELECT_ALL_QUERY = "SELECT `object.ip` FROM nmsDB.discovery_profile where `is.provisioned`=1;";

    private final String METRICS_SELECT_QUERY = "SELECT * FROM nmsDB.network_interface where `object.ip`=? and `interface.index`=? order by `poll.time` desc limit 1;";

    @Override
    public void start(Promise<Void> startPromise)
    {
        var eventBus = vertx.eventBus();

        // INSERTING DATA TO DB
        eventBus.<JsonObject>localConsumer(INSERT_EVENT, jsonObjectMessage -> {
            try
            {
                if(jsonObjectMessage.body().getString(TABLE_NAME).equals(DISCOVERY_PROFILE_TABLE))
                {
                    try(var conn = Utils.getConnection(); var insertDiscoveryProfile = conn.prepareStatement(DISCOVERY_PROFILE_INSERT_QUERY))
                    {
                        insertDiscoveryProfile.setString(1, jsonObjectMessage.body().getString(DISCOVERY_NAME));
                        insertDiscoveryProfile.setString(2, jsonObjectMessage.body().getString(OBJECT_IP));
                        insertDiscoveryProfile.setString(3, jsonObjectMessage.body().getString(PORT));

                        var rowsInserted = insertDiscoveryProfile.executeUpdate();

                        if(rowsInserted > 0)
                        {
                            LOGGER.trace(ROWS_INSERTED_CONTAINER, rowsInserted, jsonObjectMessage.body().getString(TABLE_NAME));

                            jsonObjectMessage.reply(SUCCESS);
                        }


                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), sqlException.getMessage());
                    }
                }
                else if(jsonObjectMessage.body().getString(TABLE_NAME).equals(CREDENTIAL_PROFILE_TABLE))
                {
                    try(var conn = Utils.getConnection(); var insertCredentialProfile = conn.prepareStatement(CREDENTIAL_PROFILE_INSERT_QUERY))
                    {
                        insertCredentialProfile.setString(1, jsonObjectMessage.body().getString(CREDENTIAL_NAME));
                        insertCredentialProfile.setString(2, jsonObjectMessage.body().getString(PROTOCOL));
                        insertCredentialProfile.setString(3, jsonObjectMessage.body().getString(VERSION));
                        insertCredentialProfile.setString(4, jsonObjectMessage.body().getString(SNMP_COMMUNITY));

                        var rowsInserted = insertCredentialProfile.executeUpdate();

                        if(rowsInserted > 0)
                        {
                            LOGGER.trace(ROWS_INSERTED_CONTAINER, rowsInserted, jsonObjectMessage.body().getString(TABLE_NAME));

                            jsonObjectMessage.reply(SUCCESS);
                        }

                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), sqlException.getMessage());
                    }
                }
                else
                {
                    jsonObjectMessage.reply(jsonObjectMessage.body().put(STATUS, FAILED).put(ERROR, INVALID_TABLE));
                }
            } catch(NumberFormatException | ClassCastException formatException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), INCORRECT_DATATYPE);
            } catch(NullPointerException nullPointerException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), REQUEST_BODY_ERROR);
            } catch(Exception exception)
            {
                jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), exception.getMessage());
            }
        });

        // FETCHING ALL DATA FROM DB
        eventBus.<JsonObject>localConsumer(GET_ALL_EVENT, jsonObjectMessage -> {
            try
            {

                if(jsonObjectMessage.body().getString(TABLE_NAME).equals(CREDENTIAL_PROFILE_TABLE))
                {
                    try(var conn = Utils.getConnection(); var selectAllCredentialProfiles = conn.createStatement())
                    {
                        var credentials = selectAllCredentialProfiles.executeQuery(CREDENTIAL_PROFILE_SELECT_ALL_QUERY);

                        var credentialProfiles = new JsonArray();

                        while(credentials.next())
                        {
                            credentialProfiles.add(new JsonObject().put(CREDENTIAL_NAME, credentials.getString(CREDENTIAL_NAME)).put(PROTOCOL, credentials.getString(PROTOCOL)).put(CREDENTIAL_PROFILE_ID, credentials.getInt(CREDENTIAL_PROFILE_ID)).put(VERSION, credentials.getString(VERSION)).put(SNMP_COMMUNITY, credentials.getString(SNMP_COMMUNITY)));
                        }

                        if(credentialProfiles.isEmpty())
                        {
                            jsonObjectMessage.fail(HttpResponseStatus.NOT_FOUND.code(), "No credential profile found!");
                        }

                        LOGGER.trace("All credential profiles fetched successfully");

                        jsonObjectMessage.reply(credentialProfiles);

                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.NOT_FOUND.code(), sqlException.getMessage());
                    }
                }
                else if(jsonObjectMessage.body().getString(TABLE_NAME).equals(DISCOVERY_PROFILE_TABLE))
                {
                    try(var conn = Utils.getConnection(); var selectAllDiscoveryProfiles = conn.createStatement())
                    {
                        var discoveryProfilesRS = selectAllDiscoveryProfiles.executeQuery(DISCOVERY_PROFILE_SELECT_ALL_QUERY);

                        var discoveryProfiles = new JsonArray();

                        while(discoveryProfilesRS.next())
                        {
                            discoveryProfiles.add(new JsonObject().put(DISCOVERY_PROFILE_ID, discoveryProfilesRS.getInt(DISCOVERY_PROFILE_ID)).put(DISCOVERY_NAME, discoveryProfilesRS.getString(DISCOVERY_NAME)).put(OBJECT_IP, discoveryProfilesRS.getString(OBJECT_IP)).put(PORT, discoveryProfilesRS.getInt(PORT)).put(IS_PROVISIONED, discoveryProfilesRS.getInt(IS_PROVISIONED)).put(IS_DISCOVERED, discoveryProfilesRS.getInt(IS_DISCOVERED)));
                        }

                        if(discoveryProfiles.isEmpty())
                        {
                            jsonObjectMessage.fail(HttpResponseStatus.NOT_FOUND.code(), "No discovery profile found!");
                        }

                        LOGGER.trace("discovery profiles fetched successfully");

                        jsonObjectMessage.reply(discoveryProfiles);

                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.NOT_FOUND.code(), sqlException.getMessage());
                    }
                }
                else if(jsonObjectMessage.body().getString(TABLE_NAME).equals(GET_PROVISIONED_DEVICES_EVENT))
                {
                    try(var conn = Utils.getConnection(); var selectProvisionedDevices = conn.createStatement())
                    {
                        var provisionedDevices = selectProvisionedDevices.executeQuery(PROVISIONED_DEVICES_SELECT_ALL_QUERY);

                        var objects = new JsonArray();

                        while(provisionedDevices.next())
                        {
                            objects.add(provisionedDevices.getString(OBJECT_IP));
                        }

                        if(objects.isEmpty())
                        {
                            jsonObjectMessage.fail(HttpResponseStatus.NOT_FOUND.code(), "No provisioned devices found!");
                        }

                        LOGGER.trace("Provisioned devices fetched successfully");

                        jsonObjectMessage.reply(objects);

                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.NOT_FOUND.code(), sqlException.getMessage());
                    }
                }
                else
                {
                    jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), INVALID_TABLE);
                }
            } catch(NumberFormatException | ClassCastException formatException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), INCORRECT_DATATYPE);
            } catch(NullPointerException nullPointerException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), REQUEST_BODY_ERROR);
            } catch(Exception exception)
            {
                jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), exception.getMessage());
            }

        });

        // FETCHING DATA FROM DB
        eventBus.<JsonObject>localConsumer(GET_EVENT, jsonObjectMessage -> {
            try
            {
                if(jsonObjectMessage.body().getString(TABLE_NAME).equals(DISCOVERY_PROFILE_TABLE))
                {
                    try
                    {
                        var discoveryProfile = getDiscoveryProfile(Integer.parseInt(jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID)));

                        if(discoveryProfile.isEmpty())
                        {
                            jsonObjectMessage.fail(HttpResponseStatus.NOT_FOUND.code(), "No discovery profile found!");
                        }
                        else
                        {
                            LOGGER.trace("discovery profile id = {} fetched successfully", jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID));

                            jsonObjectMessage.reply(discoveryProfile);
                        }

                    } catch(SQLException e)
                    {
                        LOGGER.error(ERROR_CONTAINER, e.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), e.getMessage());
                    }
                }
                else if(jsonObjectMessage.body().getString(TABLE_NAME).equals(CREDENTIAL_PROFILE_TABLE))
                {
                    try
                    {
                        var credentialProfile = getCredentialProfile(Integer.parseInt(jsonObjectMessage.body().getString(CREDENTIAL_PROFILE_ID)));

                        if(credentialProfile.isEmpty())
                        {
                            jsonObjectMessage.fail(HttpResponseStatus.NOT_FOUND.code(), "No credential profile found!");
                        }
                        else
                        {
                            LOGGER.trace("credential profile id = {} fetched successfully", jsonObjectMessage.body().getString(CREDENTIAL_PROFILE_ID));

                            jsonObjectMessage.reply(credentialProfile);
                        }

                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), sqlException.getMessage());
                    }

                }
                else if(jsonObjectMessage.body().getString(TABLE_NAME).equals(NETWORK_INTERFACE_TABLE))
                {
                    jsonObjectMessage.body().remove(TABLE_NAME);

                    try(var conn = Utils.getConnection(); var getMetrics = conn.prepareStatement(METRICS_SELECT_QUERY))
                    {
                        getMetrics.setString(1, jsonObjectMessage.body().getString(OBJECT_IP));

                        getMetrics.setInt(2, jsonObjectMessage.body().getInteger(INTERFACE_INDEX));

                        var record = getMetrics.executeQuery();

                        if(!record.isBeforeFirst())
                        {
                            jsonObjectMessage.fail(HttpResponseStatus.NOT_FOUND.code(), "No records found for object.ip: " + jsonObjectMessage.body().getString(OBJECT_IP) + " and interface.index: " + jsonObjectMessage.body().getInteger(INTERFACE_INDEX));
                        }

                        while(record.next())
                        {
                            jsonObjectMessage.body().put(RESULT, new JsonObject().put(INTERFACE_INDEX, record.getInt(INTERFACE_INDEX)).put(INTERFACE_NAME, record.getString(INTERFACE_NAME)).put(INTERFACE_DESCRIPTION, record.getString(INTERFACE_DESCRIPTION)).put(INTERFACE_ALIAS, record.getString(INTERFACE_ALIAS)).put(INTERFACE_PHYSICAL_ADDRESS, record.getString(INTERFACE_PHYSICAL_ADDRESS)).put(INTERFACE_OPERATIONAL_STATUS, record.getInt(INTERFACE_OPERATIONAL_STATUS)).put(INTERFACE_ADMIN_STATUS, record.getInt(INTERFACE_ADMIN_STATUS)).put(INTERFACE_SENT_ERROR_PACKET, record.getInt(INTERFACE_SENT_ERROR_PACKET)).put(INTERFACE_RECEIVED_ERROR_PACKET, record.getInt(INTERFACE_RECEIVED_ERROR_PACKET)).put(INTERFACE_SENT_OCTETS, record.getInt(INTERFACE_SENT_OCTETS)).put(INTERFACE_RECEIVED_OCTETS, record.getInt(INTERFACE_RECEIVED_OCTETS)).put(INTERFACE_SPEED, record.getInt(INTERFACE_SPEED)).put(POLL_TIME, record.getString(POLL_TIME)));
                        }

                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), sqlException.getMessage());
                    }
                    if(!jsonObjectMessage.body().isEmpty())
                    {
                        jsonObjectMessage.reply(jsonObjectMessage.body());
                    }
                    else
                    {
                        jsonObjectMessage.fail(HttpResponseStatus.NOT_FOUND.code(), "Metrics not found for " + jsonObjectMessage.body().getString(OBJECT_IP));
                    }
                }
                else
                {
                    jsonObjectMessage.reply(jsonObjectMessage.body().put(STATUS, FAILED).put(ERROR, INVALID_TABLE));
                }
            } catch(NumberFormatException | ClassCastException formatException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), INCORRECT_DATATYPE);
            } catch(NullPointerException nullPointerException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), REQUEST_BODY_ERROR);
            } catch(Exception exception)
            {
                jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), exception.getMessage());
            }
        });

        // UPDATING DATA IN DB
        eventBus.<JsonObject>localConsumer(UPDATE_EVENT, jsonObjectMessage -> {
            try
            {

                if(jsonObjectMessage.body().getString(TABLE_NAME).equals(DISCOVERY_PROFILE_TABLE))
                {
                    // updating discovery profile will stop polling and device need to re-discover
                    try(var conn = Utils.getConnection(); var updateDiscoveryProfile = conn.prepareStatement(DISCOVERY_PROFILE_UPDATE_QUERY))
                    {
                        updateDiscoveryProfile.setString(1, jsonObjectMessage.body().getString(DISCOVERY_NAME));

                        updateDiscoveryProfile.setInt(2, jsonObjectMessage.body().getInteger(PORT));

                        updateDiscoveryProfile.setInt(3, Integer.parseInt(jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID)));

                        var rowsUpdated = updateDiscoveryProfile.executeUpdate();

                        if(rowsUpdated > 0)
                        {
                            LOGGER.trace(ROWS_UPDATED_CONTAINER, rowsUpdated, CREDENTIAL_PROFILE_TABLE);

                            var discoveryProfile = getDiscoveryProfile(Integer.parseInt(jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID)));

                            jsonObjectMessage.reply(discoveryProfile);
                        }
                        else
                        {
                            LOGGER.trace(KEY_VAL_NOT_FOUND_CONTAINER, DISCOVERY_PROFILE_ID, jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID));

                            jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "discovery.profile.id = " + jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID) + " not found");
                        }

                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), sqlException.getMessage());
                    }
                }
                else if(jsonObjectMessage.body().getString(TABLE_NAME).equals(CREDENTIAL_PROFILE_TABLE))
                {
                    // change in credential_profile will not affect already discovered devices/polling
                    try(var conn = Utils.getConnection(); var updateCredentialProfile = conn.prepareStatement(CREDENTIAL_PROFILE_UPDATE_QUERY))
                    {
                        updateCredentialProfile.setString(1, jsonObjectMessage.body().getString(VERSION));
                        updateCredentialProfile.setString(2, jsonObjectMessage.body().getString(SNMP_COMMUNITY));
                        updateCredentialProfile.setInt(3, Integer.parseInt(jsonObjectMessage.body().getString(CREDENTIAL_PROFILE_ID)));

                        var rowsUpdated = updateCredentialProfile.executeUpdate();

                        if(rowsUpdated > 0)
                        {
                            LOGGER.trace(ROWS_UPDATED_CONTAINER, rowsUpdated, jsonObjectMessage.body().getString(TABLE_NAME));

                            var credentialProfile = getCredentialProfile(Integer.parseInt(jsonObjectMessage.body().getString(CREDENTIAL_PROFILE_ID)));

                            jsonObjectMessage.reply(credentialProfile);
                        }
                        else
                        {
                            LOGGER.trace(KEY_VAL_NOT_FOUND_CONTAINER, CREDENTIAL_PROFILE_ID, jsonObjectMessage.body().getString(CREDENTIAL_PROFILE_ID));

                            jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "credential.profile.id = " + jsonObjectMessage.body().getString(CREDENTIAL_PROFILE_ID) + " not found");
                        }

                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), sqlException.getMessage());
                    }
                }
                else if(jsonObjectMessage.body().getString(TABLE_NAME).equals(PROVISION_DEVICE))
                {
                    try(var conn = Utils.getConnection(); var updateDiscoveryProfile = conn.prepareStatement(PROVISION_STATUS_UPDATE_QUERY))
                    {
                        var discoveryProfile = getDiscoveryProfile(jsonObjectMessage.body().getInteger(DISCOVERY_PROFILE_ID));

                        if(discoveryProfile.getInteger(IS_PROVISIONED) == FALSE)
                        {
                            updateDiscoveryProfile.setInt(1, 1);

                            updateDiscoveryProfile.setInt(2, jsonObjectMessage.body().getInteger(DISCOVERY_PROFILE_ID));

                            var rowsUpdated = updateDiscoveryProfile.executeUpdate();

                            if(rowsUpdated > 0)
                            {
                                LOGGER.trace(ROWS_UPDATED_CONTAINER, rowsUpdated, jsonObjectMessage.body().getString(TABLE_NAME));

                                jsonObjectMessage.reply(SUCCESS);
                            }
                            else
                            {
                                LOGGER.trace(KEY_VAL_NOT_FOUND_CONTAINER, DISCOVERY_PROFILE_ID, jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID));

                                jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "discovery.profile.id = " + jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID) + " not found");
                            }
                        }
                        else
                        {
                            LOGGER.trace("Device: {} already provisioned", jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID));

                            jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "Device = " + jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID) + " already provisioned!");
                        }


                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), sqlException.getMessage());
                    }
                }
                else
                {
                    jsonObjectMessage.reply(jsonObjectMessage.body().put(STATUS, FAILED).put(ERROR, INVALID_TABLE));
                }
            } catch(NumberFormatException | ClassCastException formatException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), INCORRECT_DATATYPE);
            } catch(NullPointerException nullPointerException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), REQUEST_BODY_ERROR);
            } catch(Exception exception)
            {
                jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), exception.getMessage());
            }
        });

        // DELETING DATA FROM DB
        eventBus.<JsonObject>localConsumer(DELETE_EVENT, jsonObjectMessage -> {
            try
            {

                if(jsonObjectMessage.body().getString(TABLE_NAME).equals(DISCOVERY_PROFILE_TABLE))
                {
                    try(var conn = Utils.getConnection(); var deleteDiscoveryProfile = conn.prepareStatement(DISCOVER_PROFILE_DELETE_QUERY))
                    {
                        deleteDiscoveryProfile.setInt(1, Integer.parseInt(jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID)));

                        var rowsDeleted = deleteDiscoveryProfile.executeUpdate();

                        if(rowsDeleted > 0)
                        {
                            LOGGER.trace(ROWS_DELETED_CONTAINER, rowsDeleted, jsonObjectMessage.body().getString(TABLE_NAME));

                            jsonObjectMessage.reply(SUCCESS);
                        }
                        else
                        {
                            LOGGER.trace(KEY_VAL_NOT_FOUND_CONTAINER, DISCOVERY_PROFILE_ID, jsonObjectMessage.body().getString(DISCOVERY_PROFILE_ID));

                            jsonObjectMessage.reply(FAILED);
                        }

                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), sqlException.getMessage());
                    }
                }
                else if(jsonObjectMessage.body().getString(TABLE_NAME).equals(CREDENTIAL_PROFILE_TABLE))
                {
                    // before deleting cred profile, check if it is being used or not
                    try(var conn = Utils.getConnection(); var deleteCredentialProfile = conn.prepareStatement(CREDENTIAL_PROFILE_DELETE_QUERY); var selectMappedCredentialProfiles = conn.prepareStatement(PROFILE_MAPPING_COUNT_SELECT_QUERY))
                    {
                        selectMappedCredentialProfiles.setInt(1, Integer.parseInt(jsonObjectMessage.body().getString(CREDENTIAL_PROFILE_ID)));

                        var mappedCredentialProfiles = selectMappedCredentialProfiles.executeQuery();

                        while(mappedCredentialProfiles.next())
                        {
                            if(mappedCredentialProfiles.getInt(1) == 0)
                            {
                                deleteCredentialProfile.setInt(1, Integer.parseInt(jsonObjectMessage.body().getString(CREDENTIAL_PROFILE_ID)));

                                var rowsDeleted = deleteCredentialProfile.executeUpdate();

                                if(rowsDeleted > 0)
                                {
                                    LOGGER.trace(ROWS_DELETED_CONTAINER, rowsDeleted, jsonObjectMessage.body().getString(TABLE_NAME));

                                    jsonObjectMessage.reply(SUCCESS);
                                }
                                else
                                {
                                    LOGGER.trace(KEY_VAL_NOT_FOUND_CONTAINER, CREDENTIAL_PROFILE_ID, jsonObjectMessage.body().getString(CREDENTIAL_PROFILE_ID));

                                    jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), String.format("credential.profile.id = %s not found", jsonObjectMessage.body().getString(CREDENTIAL_PROFILE_ID)));
                                }
                            }
                            else
                            {
                                jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), String.format("credential.profile.id = %s is currently in use", jsonObjectMessage.body().getString(CREDENTIAL_PROFILE_ID)));
                            }
                        }


                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                        jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), sqlException.getMessage());
                    }
                }
                else
                {
                    jsonObjectMessage.reply(jsonObjectMessage.body().put(STATUS, FAILED).put(ERROR, INVALID_TABLE));
                }
            } catch(NumberFormatException | ClassCastException formatException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), INCORRECT_DATATYPE);
            } catch(NullPointerException nullPointerException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), REQUEST_BODY_ERROR);
            } catch(Exception exception)
            {
                jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), exception.getMessage());
            }
        });

        // MAKING CONTEXT FOR DISCOVERY
        eventBus.<JsonArray>localConsumer(MAKE_DISCOVERY_CONTEXT_EVENT, jsonArrayMessage -> {
            try
            {
                var contexts = new JsonArray();

                try
                {
                    for(var object : jsonArrayMessage.body())
                    {
                        var monitor = new JsonObject(object.toString());

                        var discoveryProfile = getDiscoveryProfile(monitor.getInteger(DISCOVERY_PROFILE_ID));

                        if(Utils.pingCheck(discoveryProfile.getString(OBJECT_IP)))
                        {
                            discoveryProfile.put(REQUEST_TYPE, DISCOVERY).put(PLUGIN_NAME, NETWORK);

                            if(discoveryProfile.getInteger(IS_DISCOVERED) == TRUE || discoveryProfile.getInteger(IS_PROVISIONED) == TRUE)
                            {
                                jsonArrayMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), new JsonObject().put(ERROR, "Error in running discovery").put(ERR_MESSAGE, "Device ID: " + monitor.getInteger(DISCOVERY_PROFILE_ID) + " is already discovered or provisioned").put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).toString());

                                return;
                            }

                            var credentialProfileIds = monitor.getJsonArray(CREDENTIAL_PROFILE_IDS);

                            var credentialProfiles = new JsonArray();

                            for(var credentialId : credentialProfileIds)
                            {
                                var credentialProfile = getCredentialProfile(Integer.parseInt(credentialId.toString()));

                                credentialProfiles.add(credentialProfile);
                            }

                            discoveryProfile.put(CREDENTIALS, credentialProfiles);

                            discoveryProfile.remove(IS_PROVISIONED);

                            discoveryProfile.remove(IS_DISCOVERED);

                            contexts.add(discoveryProfile);
                        }
                    }

                    if(!contexts.isEmpty())
                    {
                        jsonArrayMessage.reply(contexts);
                    }
                    else
                    {
                        jsonArrayMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), new JsonObject().put(ERROR, "Error in running discovery").put(ERR_MESSAGE, "No device is eligible for discovery!").put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code()).toString());
                    }
                } catch(SQLException | NullPointerException exception)
                {
                    jsonArrayMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), new JsonObject().put(ERROR, "Error in running discovery").put(ERR_MESSAGE, exception.getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).toString());
                }
            } catch(NumberFormatException | ClassCastException formatException)
            {
                jsonArrayMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), INCORRECT_DATATYPE);
            } catch(NullPointerException nullPointerException)
            {
                jsonArrayMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), REQUEST_BODY_ERROR);
            } catch(Exception exception)
            {
                jsonArrayMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), exception.getMessage());
            }
        });

        // POST DISCOVERY SUCCESS
        eventBus.<JsonObject>localConsumer(POST_DISCOVERY_SUCCESS_EVENT, jsonObjectMessage -> {


            try(var conn = Utils.getConnection(); var updateDiscoveryProfile = conn.prepareStatement(DISCOVERY_STATUS_UPDATE_QUERY); var selectUniqueCredentialProfiles = conn.prepareStatement(PROFILE_MAPPING_UNIQUE_CREDENTIAL_PROFILE_IDS_QUERY))
            {
                updateDiscoveryProfile.setInt(1, jsonObjectMessage.body().getInteger(DISCOVERY_PROFILE_ID));

                if(updateDiscoveryProfile.executeUpdate() > 0)
                {
                    LOGGER.trace("Discovery status changed to 1 for discovery.profile.id: {}", jsonObjectMessage.body().getInteger(DISCOVERY_PROFILE_ID));
                }
                else
                {
                    LOGGER.error("Error in updating discovery profile");
                }

                selectUniqueCredentialProfiles.setInt(1, jsonObjectMessage.body().getInteger(DISCOVERY_PROFILE_ID));

                var uniqueCredentialProfiles = selectUniqueCredentialProfiles.executeQuery();

                // if mapping = 0
                if(!uniqueCredentialProfiles.isBeforeFirst())
                {
                    if(insertProfileMapping(jsonObjectMessage.body().getInteger(DISCOVERY_PROFILE_ID), jsonObjectMessage.body().getInteger(CREDENTIAL_PROFILE_ID)) > 0)
                    {
                        LOGGER.trace("Mapping added for [discId:credId]: [{}:{}]", jsonObjectMessage.body().getInteger(DISCOVERY_PROFILE_ID), jsonObjectMessage.body().getInteger(CREDENTIAL_PROFILE_ID));
                    }
                }
                // if credential profiles are present
                else
                {
                    var credentialsArray = new JsonArray();

                    while(uniqueCredentialProfiles.next())
                    {
                        credentialsArray.add(uniqueCredentialProfiles.getInt(CREDENTIAL_PROFILE_ID));
                    }

                    // if mapping for credential profile & discovery profile does not exist
                    if(!credentialsArray.contains(jsonObjectMessage.body().getInteger(CREDENTIAL_PROFILE_ID)))
                    {
                        if(insertProfileMapping(jsonObjectMessage.body().getInteger(DISCOVERY_PROFILE_ID), jsonObjectMessage.body().getInteger(CREDENTIAL_PROFILE_ID)) > 0)
                        {
                            LOGGER.trace("Mapping added for [discId:credId]: [{}:{}]", jsonObjectMessage.body().getInteger(DISCOVERY_PROFILE_ID), jsonObjectMessage.body().getInteger(CREDENTIAL_PROFILE_ID));
                        }
                    }
                    // if mapping for credential profile & discovery profile exists
                    else
                    {
                        LOGGER.warn("Mapping already exists for [discId:credId]: [{}:{}]", jsonObjectMessage.body().getInteger(DISCOVERY_PROFILE_ID), jsonObjectMessage.body().getInteger(CREDENTIAL_PROFILE_ID));
                    }

                }
            } catch(SQLException sqlException)
            {
                LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());
            } catch(NumberFormatException | ClassCastException formatException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), INCORRECT_DATATYPE);
            } catch(NullPointerException nullPointerException)
            {
                jsonObjectMessage.fail(HttpResponseStatus.BAD_REQUEST.code(), REQUEST_BODY_ERROR);
            } catch(Exception exception)
            {
                jsonObjectMessage.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), exception.getMessage());
            }
        });

        // PROVISION_DEVICES
        eventBus.localConsumer(GET_PROVISIONED_DEVICES_EVENT, msg -> {
            try
            {
                var contexts = new JsonArray();

                try
                {
                    var monitors = getMappedProfiles();

                    for(var monitor : monitors)
                    {
                        var monitorObj = new JsonObject(monitor.toString());

                        if(monitorObj.getInteger(IS_PROVISIONED) == TRUE && monitorObj.getInteger(IS_DISCOVERED) == TRUE && Utils.pingCheck(monitorObj.getString(OBJECT_IP)))
                        {
                            monitorObj.remove(IS_DISCOVERED);

                            monitorObj.remove(IS_PROVISIONED);

                            contexts.add(monitor);
                        }
                    }

                } catch(SQLException e)
                {
                    msg.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), new JsonObject().put(ERROR, "Error provisioning devices!").put(ERR_MESSAGE, e.getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).toString());
                }

                if(!contexts.isEmpty())
                {
                    msg.reply(contexts);
                }
                else
                {
                    msg.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), new JsonObject().put(ERROR, "Error provisioning devices!").put(ERR_MESSAGE, "No devices qualified for provisioning").put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).toString());
                }
            } catch(NumberFormatException | ClassCastException formatException)
            {
                msg.fail(HttpResponseStatus.BAD_REQUEST.code(), INCORRECT_DATATYPE);
            } catch(NullPointerException nullPointerException)
            {
                msg.fail(HttpResponseStatus.BAD_REQUEST.code(), REQUEST_BODY_ERROR);
            } catch(Exception exception)
            {
                msg.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), exception.getMessage());
            }
        });

        // STORE POLLED DATA
        eventBus.<JsonArray>localConsumer(STORE_POLLED_DATA_EVENT, polledData -> {
            try
            {

                for(var data : polledData.body())
                {
                    var monitor = new JsonObject(data.toString());

                    var interfaceData = monitor.getJsonObject(RESULT).getJsonArray(INTERFACE);

                    var objectIp = monitor.getString(OBJECT_IP);

                    var pollTime = monitor.getString(POLL_TIME);

                    try(var conn = Utils.getConnection(); var insertPolledMetrics = conn.prepareStatement(METRICS_INSERT_QUERY))
                    {
                        for(var singleInterface : interfaceData)
                        {
                            var interfaceObj = new JsonObject(singleInterface.toString());

                            insertPolledMetrics.setString(1, objectIp);

                            insertPolledMetrics.setInt(2, interfaceObj.containsKey(INTERFACE_INDEX) ? interfaceObj.getInteger(INTERFACE_INDEX) : -999);
                            insertPolledMetrics.setString(3, interfaceObj.containsKey(INTERFACE_NAME) ? interfaceObj.getString(INTERFACE_NAME) : "");
                            insertPolledMetrics.setInt(4, interfaceObj.containsKey(INTERFACE_OPERATIONAL_STATUS) ? interfaceObj.getInteger(INTERFACE_OPERATIONAL_STATUS) : -999);
                            insertPolledMetrics.setInt(5, interfaceObj.containsKey(INTERFACE_ADMIN_STATUS) ? interfaceObj.getInteger(INTERFACE_ADMIN_STATUS) : -999);
                            insertPolledMetrics.setString(6, interfaceObj.containsKey(INTERFACE_DESCRIPTION) ? interfaceObj.getString(INTERFACE_DESCRIPTION) : "");
                            insertPolledMetrics.setInt(7, interfaceObj.containsKey(INTERFACE_SENT_ERROR_PACKET) ? interfaceObj.getInteger(INTERFACE_SENT_ERROR_PACKET) : -999);
                            insertPolledMetrics.setInt(8, interfaceObj.containsKey(INTERFACE_RECEIVED_ERROR_PACKET) ? interfaceObj.getInteger(INTERFACE_RECEIVED_ERROR_PACKET) : -999);
                            insertPolledMetrics.setInt(9, interfaceObj.containsKey(INTERFACE_SENT_OCTETS) ? interfaceObj.getInteger(INTERFACE_SENT_OCTETS) : -999);
                            insertPolledMetrics.setInt(10, interfaceObj.containsKey(INTERFACE_RECEIVED_OCTETS) ? interfaceObj.getInteger(INTERFACE_RECEIVED_OCTETS) : -999);
                            insertPolledMetrics.setInt(11, interfaceObj.containsKey(INTERFACE_SPEED) ? interfaceObj.getInteger(INTERFACE_SPEED) : -999);
                            insertPolledMetrics.setString(12, interfaceObj.containsKey(INTERFACE_ALIAS) ? interfaceObj.getString(INTERFACE_ALIAS) : "");
                            insertPolledMetrics.setString(13, interfaceObj.containsKey(INTERFACE_PHYSICAL_ADDRESS) ? interfaceObj.getString(INTERFACE_PHYSICAL_ADDRESS) : "");
                            insertPolledMetrics.setString(14, pollTime);

                            insertPolledMetrics.addBatch();
                        }

                        var rowsInserted = insertPolledMetrics.executeBatch();

                        LOGGER.trace("{} rows data inserted for {}", rowsInserted.length, objectIp);

                    } catch(SQLException sqlException)
                    {
                        LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());
                    }
                }
            } catch(NumberFormatException | ClassCastException formatException)
            {
                polledData.fail(HttpResponseStatus.BAD_REQUEST.code(), INCORRECT_DATATYPE);
            } catch(NullPointerException nullPointerException)
            {
                polledData.fail(HttpResponseStatus.BAD_REQUEST.code(), REQUEST_BODY_ERROR);
            } catch(Exception exception)
            {
                polledData.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), exception.getMessage());
            }
        });

        // REMOVE PROVISIONED DEVICE
        eventBus.localConsumer(STOP_POLLING_EVENT, msg -> {
            try
            {
                var discProfileId = Integer.parseInt(msg.body().toString());

                try(var conn = Utils.getConnection(); var updateDiscoveryProfile = conn.prepareStatement(PROVISION_STATUS_UPDATE_QUERY))
                {
                    var discoveryProfile = getDiscoveryProfile(discProfileId);

                    if(discoveryProfile.getInteger(IS_PROVISIONED) == TRUE)
                    {
                        updateDiscoveryProfile.setInt(1, 0);

                        updateDiscoveryProfile.setInt(2, discProfileId);

                        if(updateDiscoveryProfile.executeUpdate() > 0)
                        {
                            LOGGER.trace("Provision status changed to 0 for {}: {}", DISCOVERY_PROFILE_ID, discProfileId);
                        }
                        else
                        {
                            msg.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "Error in updating provision status of discovery profile: " + discProfileId);
                        }
                    }
                    else
                    {
                        msg.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "Device is not provisioned");
                    }


                } catch(SQLException sqlException)
                {
                    LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());

                    msg.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), sqlException.getMessage());
                }

                msg.reply(SUCCESS);
            } catch(NumberFormatException | ClassCastException formatException)
            {
                msg.fail(HttpResponseStatus.BAD_REQUEST.code(), INCORRECT_DATATYPE);

            } catch(NullPointerException nullPointerException)
            {
                msg.fail(HttpResponseStatus.BAD_REQUEST.code(), REQUEST_BODY_ERROR);

            } catch(Exception exception)
            {
                msg.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), exception.getMessage());
            }
        });

        startPromise.complete();
    }

    private JsonObject getDiscoveryProfile(int discoveryProfileId) throws SQLException
    {
        var discoveryProfile = new JsonObject();

        try(var conn = Utils.getConnection(); var selectDiscoveryProfile = conn.prepareStatement(DISCOVERY_PROFILE_SELECT_QUERY))
        {
            selectDiscoveryProfile.setInt(1, discoveryProfileId);

            var discoveryProfiles = selectDiscoveryProfile.executeQuery();

            if(!discoveryProfiles.isBeforeFirst())
            {
                throw new SQLException("No discovery profile found! ID: " + discoveryProfileId);
            }
            else
            {
                while(discoveryProfiles.next())
                {
                    discoveryProfile.put(DISCOVERY_PROFILE_ID, discoveryProfiles.getInt(DISCOVERY_PROFILE_ID)).put(DISCOVERY_NAME, discoveryProfiles.getString(DISCOVERY_NAME)).put(OBJECT_IP, discoveryProfiles.getString(OBJECT_IP)).put(PORT, discoveryProfiles.getInt(PORT)).put(IS_PROVISIONED, discoveryProfiles.getInt(IS_PROVISIONED)).put(IS_DISCOVERED, discoveryProfiles.getInt(IS_DISCOVERED));
                }
            }
        }
        return discoveryProfile;
    }

    private JsonObject getCredentialProfile(int credProfileId) throws SQLException
    {
        var credentialProfile = new JsonObject();

        try(var conn = Utils.getConnection(); var selectCredentialProfile = conn.prepareStatement(CREDENTIAL_PROFILE_SELECT_QUERY))
        {
            selectCredentialProfile.setInt(1, credProfileId);

            var credentialProfiles = selectCredentialProfile.executeQuery();

            if(!credentialProfiles.isBeforeFirst())
            {
                throw new SQLException("No credential profile found! ID: " + credProfileId);
            }
            else
            {
                while(credentialProfiles.next())
                {
                    credentialProfile.put(VERSION, credentialProfiles.getString(VERSION)).put(CREDENTIAL_PROFILE_ID, credentialProfiles.getInt(CREDENTIAL_PROFILE_ID)).put(CREDENTIAL_NAME, credentialProfiles.getString(CREDENTIAL_NAME)).put(PROTOCOL, credentialProfiles.getString(PROTOCOL)).put(VERSION, credentialProfiles.getString(VERSION)).put(SNMP_COMMUNITY, credentialProfiles.getString(SNMP_COMMUNITY));
                }
            }

        }

        return credentialProfile;
    }

    private int insertProfileMapping(int discProfileId, int credProfileId) throws SQLException
    {
        try(var conn = Utils.getConnection(); var insertMappedCredentials = conn.prepareStatement(PROFILE_MAPPING_INSERT_QUERY))
        {
            insertMappedCredentials.setInt(1, discProfileId);

            insertMappedCredentials.setInt(2, credProfileId);

            return insertMappedCredentials.executeUpdate();
        }
    }

    private JsonArray getMappedProfiles() throws SQLException
    {
        var mappedProfiles = new JsonArray();

        try(var conn = Utils.getConnection(); var selectMappedDiscoveryProfiles = conn.createStatement())
        {
            var MappedDiscoveryProfiles = selectMappedDiscoveryProfiles.executeQuery(PROFILE_MAPPING_SELECT_ALL_QUERY);

            if(!MappedDiscoveryProfiles.isBeforeFirst())
            {
                throw new SQLException("No profile mapping found!");
            }
            else
            {
                while(MappedDiscoveryProfiles.next())
                {
                    mappedProfiles.add(new JsonObject().put(OBJECT_IP, MappedDiscoveryProfiles.getString(OBJECT_IP)).put(DISCOVERY_NAME, MappedDiscoveryProfiles.getString(DISCOVERY_NAME)).put(VERSION, MappedDiscoveryProfiles.getString(VERSION)).put(PORT, MappedDiscoveryProfiles.getInt(PORT)).put(SNMP_COMMUNITY, MappedDiscoveryProfiles.getString(SNMP_COMMUNITY)).put(IS_PROVISIONED, MappedDiscoveryProfiles.getInt(IS_PROVISIONED)).put(IS_DISCOVERED, MappedDiscoveryProfiles.getInt(IS_DISCOVERED)).put(DISCOVERY_PROFILE_ID, MappedDiscoveryProfiles.getInt(DISCOVERY_PROFILE_ID)).put(CREDENTIAL_PROFILE_ID, MappedDiscoveryProfiles.getInt(CREDENTIAL_PROFILE_ID)).put(REQUEST_TYPE, COLLECT).put(PLUGIN_NAME, NETWORK));
                }
            }
        }

        return mappedProfiles;
    }

}
