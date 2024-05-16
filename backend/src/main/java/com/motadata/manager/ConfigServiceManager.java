package com.motadata.manager;

import com.motadata.utils.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static com.motadata.Bootstrap.*;
import static com.motadata.utils.Constants.*;

public class ConfigServiceManager extends AbstractVerticle
{
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
    public void start(Promise<Void> startPromise) throws Exception
    {
        var eventBus = vertx.eventBus();

        // INSERTING DATA TO DB
        eventBus.localConsumer(INSERT_EVENT, msg -> {
            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals(DISCOVERY_PROFILE_TABLE))
            {
                try(var conn = Utils.getConnection(); var dpInsertStmt = conn.prepareStatement(DISCOVERY_PROFILE_INSERT_QUERY))
                {
                    dpInsertStmt.setString(1, jsonObj.getString(DISCOVERY_NAME));
                    dpInsertStmt.setString(2, jsonObj.getString(OBJECT_IP));
                    dpInsertStmt.setString(3, jsonObj.getString(PORT));

                    var rowsInserted = dpInsertStmt.executeUpdate();

                    if(rowsInserted > 0)
                    {
                        LOGGER.info(ROWS_INSERTED_CONTAINER, rowsInserted, jsonObj.getString(TABLE_NAME));

                        msg.reply(SUCCESS);
                    }


                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());

                    msg.fail(500, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(CREDENTIAL_PROFILE_TABLE))
            {
                try(var conn = Utils.getConnection(); var cpInsertStmt = conn.prepareStatement(CREDENTIAL_PROFILE_INSERT_QUERY))
                {
                    cpInsertStmt.setString(1, jsonObj.getString(CREDENTIAL_NAME));
                    cpInsertStmt.setString(2, jsonObj.getString(PROTOCOL));
                    cpInsertStmt.setString(3, jsonObj.getString(VERSION));
                    cpInsertStmt.setString(4, jsonObj.getString(SNMP_COMMUNITY));

                    var rowsInserted = cpInsertStmt.executeUpdate();

                    if(rowsInserted > 0)
                    {
                        LOGGER.info(ROWS_INSERTED_CONTAINER, rowsInserted, jsonObj.getString(TABLE_NAME));

                        msg.reply(SUCCESS);
                    }

                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());

                    msg.fail(500, e.getMessage());
                }

            }
            else
            {
                msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, INVALID_TABLE));
            }

        });

        // FETCHING ALL DATA FROM DB
        eventBus.<JsonObject>localConsumer(GET_ALL_EVENT, requestObj -> {
            var jsonObj = requestObj.body();

            if(jsonObj.getString(TABLE_NAME).equals(CREDENTIAL_PROFILE_TABLE))
            {
                try(var conn = Utils.getConnection(); var cpSelectAllStmt = conn.createStatement())
                {
                    var credentials = cpSelectAllStmt.executeQuery(CREDENTIAL_PROFILE_SELECT_ALL_QUERY);

                    var credentialProfiles = new JsonArray();

                    while(credentials.next())
                    {
                        credentialProfiles.add(new JsonObject().put(CREDENTIAL_NAME, credentials.getString(CREDENTIAL_NAME)).put(PROTOCOL, credentials.getString(PROTOCOL)).put(CREDENTIAL_PROFILE_ID, credentials.getInt(CREDENTIAL_PROFILE_ID)).put(VERSION, credentials.getString(VERSION)).put(SNMP_COMMUNITY, credentials.getString(SNMP_COMMUNITY)));
                    }

                    if(credentialProfiles.isEmpty())
                    {
                        throw new SQLException("No credential profile found!");
                    }

                    LOGGER.info("credential profiles fetched successfully");

                    requestObj.reply(credentialProfiles);

                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());

                    requestObj.fail(404, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(DISCOVERY_PROFILE_TABLE))
            {
                try(var conn = Utils.getConnection(); var dpSelectAllStmt = conn.createStatement())
                {
                    var discoveryProfilesRS = dpSelectAllStmt.executeQuery(DISCOVERY_PROFILE_SELECT_ALL_QUERY);

                    var discoveryProfiles = new JsonArray();

                    while(discoveryProfilesRS.next())
                    {
                        discoveryProfiles.add(new JsonObject().put(DISCOVERY_PROFILE_ID, discoveryProfilesRS.getInt(DISCOVERY_PROFILE_ID)).put(DISCOVERY_NAME, discoveryProfilesRS.getString(DISCOVERY_NAME)).put(OBJECT_IP, discoveryProfilesRS.getString(OBJECT_IP)).put(PORT, discoveryProfilesRS.getInt(PORT)).put(IS_PROVISIONED, discoveryProfilesRS.getInt(IS_PROVISIONED)).put(IS_DISCOVERED, discoveryProfilesRS.getInt(IS_DISCOVERED)));
                    }

                    if(discoveryProfiles.isEmpty())
                    {
                        throw new SQLException("No discovery profile found!");
                    }

                    LOGGER.info("discovery profiles fetched successfully");

                    requestObj.reply(discoveryProfiles);

                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());

                    requestObj.fail(404, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(GET_PROVISIONED_DEVICES_EVENT))
            {
                try(var conn = Utils.getConnection(); var provDevicesStmt = conn.createStatement())
                {
                    var provDeviceRS = provDevicesStmt.executeQuery(PROVISIONED_DEVICES_SELECT_ALL_QUERY);

                    var objects = new JsonArray();

                    while(provDeviceRS.next())
                    {
                        objects.add(provDeviceRS.getString(OBJECT_IP));
                    }

                    if(objects.isEmpty())
                    {
                        throw new SQLException("No provisioned devices found!");
                    }

                    LOGGER.info("Provisioned devices fetched successfully");

                    requestObj.reply(objects);

                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());

                    requestObj.fail(404, e.getMessage());
                }
            }
            else
            {
                requestObj.fail(500, INVALID_TABLE);
            }


        });

        // FETCHING DATA FROM DB
        eventBus.localConsumer(GET_EVENT, msg -> {

            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals(DISCOVERY_PROFILE_TABLE))
            {
                try
                {
                    var discoveryProfile = getDiscoveryProfile(Integer.parseInt(jsonObj.getString(DISCOVERY_PROFILE_ID)));

                    if(discoveryProfile.isEmpty())
                    {
                        throw new SQLException("No discovery profile found!");
                    }
                    else
                    {
                        LOGGER.info("discovery profile id = {} fetched successfully", jsonObj.getString(DISCOVERY_PROFILE_ID));

                        msg.reply(discoveryProfile);
                    }

                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());

                    msg.fail(500, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(CREDENTIAL_PROFILE_TABLE))
            {
                try
                {
                    var credentialProfile = getCredentialProfile(Integer.parseInt(jsonObj.getString(CREDENTIAL_PROFILE_ID)));

                    if(credentialProfile.isEmpty())
                    {
                        throw new SQLException("No credential profile found!");
                    }
                    else
                    {
                        LOGGER.info("credential profile id = {} fetched successfully", jsonObj.getString(CREDENTIAL_PROFILE_ID));

                        msg.reply(credentialProfile);
                    }

                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());

                    msg.fail(500, e.getMessage());
                }

            }
            else if(jsonObj.getString(TABLE_NAME).equals(NETWORK_INTERFACE_TABLE))
            {
                jsonObj.remove(TABLE_NAME);

                try(var conn = Utils.getConnection(); var getMetricsStmt = conn.prepareStatement(METRICS_SELECT_QUERY))
                {
                    getMetricsStmt.setString(1, jsonObj.getString(OBJECT_IP));
                    getMetricsStmt.setInt(2, jsonObj.getInteger(INTERFACE_INDEX));

                    var record = getMetricsStmt.executeQuery();

                    while(record.next())
                    {
                        jsonObj.put(RESULT, new JsonObject().put(INTERFACE_INDEX, record.getInt(INTERFACE_INDEX)).put(INTERFACE_NAME, record.getString(INTERFACE_NAME)).put(INTERFACE_DESCRIPTION, record.getString(INTERFACE_DESCRIPTION)).put(INTERFACE_ALIAS, record.getString(INTERFACE_ALIAS)).put(INTERFACE_PHYSICAL_ADDRESS, record.getString(INTERFACE_PHYSICAL_ADDRESS)).put(INTERFACE_OPERATIONAL_STATUS, record.getInt(INTERFACE_OPERATIONAL_STATUS)).put(INTERFACE_ADMIN_STATUS, record.getInt(INTERFACE_ADMIN_STATUS)).put(INTERFACE_SENT_ERROR_PACKET, record.getBigDecimal(INTERFACE_SENT_ERROR_PACKET)).put(INTERFACE_RECEIVED_ERROR_PACKET, record.getBigDecimal(INTERFACE_RECEIVED_ERROR_PACKET)).put(INTERFACE_SENT_OCTETS, record.getBigDecimal(INTERFACE_SENT_OCTETS)).put(INTERFACE_RECEIVED_OCTETS, record.getBigDecimal(INTERFACE_RECEIVED_OCTETS)).put(INTERFACE_SPEED, record.getInt(INTERFACE_SPEED)).put(POLL_TIME, record.getString(POLL_TIME)));
                    }


                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());

                    msg.fail(500, e.getMessage());
                }
                if(!jsonObj.isEmpty())
                {
                    msg.reply(jsonObj);
                }
                else
                {
                    msg.fail(500, "Metrics not found for " + jsonObj.getString(OBJECT_IP));
                }
            }
            else
            {
                msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, INVALID_TABLE));
            }
        });

        // UPDATING DATA IN DB
        eventBus.localConsumer(UPDATE_EVENT, msg -> {
            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals(DISCOVERY_PROFILE_TABLE))
            {
                // updating discovery profile will stop polling and device need to re-discover
                try(var conn = Utils.getConnection(); var dpUpdateStmt = conn.prepareStatement(DISCOVERY_PROFILE_UPDATE_QUERY))
                {
                    dpUpdateStmt.setString(1, jsonObj.getString(DISCOVERY_NAME));

                    dpUpdateStmt.setInt(2, jsonObj.getInteger(PORT));

                    dpUpdateStmt.setInt(3, jsonObj.getInteger(DISCOVERY_PROFILE_ID));

                    var rowsUpdated = dpUpdateStmt.executeUpdate();

                    if(rowsUpdated > 0)
                    {
                        LOGGER.info(ROWS_UPDATED_CONTAINER, rowsUpdated, CREDENTIAL_PROFILE_TABLE);

                        var discoveryProfile = getDiscoveryProfile(Integer.parseInt(jsonObj.getString(DISCOVERY_PROFILE_ID)));

                        msg.reply(discoveryProfile);
                    }
                    else
                    {
                        LOGGER.info(KEY_VAL_NOT_FOUND_CONTAINER, DISCOVERY_PROFILE_ID, jsonObj.getString(DISCOVERY_PROFILE_ID));

                        msg.fail(500, "discovery.profile.id = " + jsonObj.getString(DISCOVERY_PROFILE_ID) + " not found");
                    }

                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());

                    msg.fail(500, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(CREDENTIAL_PROFILE_TABLE))
            {
                // change in credential_profile will not affect already discovered devices/polling
                try(var conn = Utils.getConnection(); var updateStmt = conn.prepareStatement(CREDENTIAL_PROFILE_UPDATE_QUERY))
                {
                    updateStmt.setString(1, jsonObj.getString(VERSION));
                    updateStmt.setString(2, jsonObj.getString(SNMP_COMMUNITY));
                    updateStmt.setInt(3, jsonObj.getInteger(CREDENTIAL_PROFILE_ID));

                    var rowsUpdated = updateStmt.executeUpdate();

                    if(rowsUpdated > 0)
                    {
                        LOGGER.info(ROWS_UPDATED_CONTAINER, rowsUpdated, jsonObj.getString(TABLE_NAME));

                        var credentialProfile = getCredentialProfile(Integer.parseInt(jsonObj.getString(CREDENTIAL_PROFILE_ID)));

                        msg.reply(credentialProfile);
                    }
                    else
                    {
                        LOGGER.info(KEY_VAL_NOT_FOUND_CONTAINER, CREDENTIAL_PROFILE_ID, jsonObj.getString(CREDENTIAL_PROFILE_ID));

                        msg.fail(500, "credential.profile.id = " + jsonObj.getString(CREDENTIAL_PROFILE_ID) + " not found");
                    }

                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());

                    msg.fail(500, e.getMessage());
                }
            }
            else if(jsonObj.getString(EVENT_NAME).equals(PROVISION_DEVICE))
            {
                try(var conn = Utils.getConnection(); var provisionStatusStmt = conn.prepareStatement(PROVISION_STATUS_UPDATE_QUERY))
                {
                        provisionStatusStmt.setInt(1, 1);

                        provisionStatusStmt.setInt(2, jsonObj.getInteger(DISCOVERY_PROFILE_ID));

                        var rowsUpdated = provisionStatusStmt.executeUpdate();

                        if(rowsUpdated > 0)
                        {
                            LOGGER.info(ROWS_UPDATED_CONTAINER, rowsUpdated, jsonObj.getString(TABLE_NAME));

                            msg.reply(SUCCESS);
                        }
                        else
                        {
                            LOGGER.info(KEY_VAL_NOT_FOUND_CONTAINER, DISCOVERY_PROFILE_ID, jsonObj.getString(DISCOVERY_PROFILE_ID));

                            msg.fail(500, "discovery.profile.id = " + jsonObj.getString(DISCOVERY_PROFILE_ID) + " not found");
                        }

                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());
                    msg.fail(500, e.getMessage());
                }
            }
            else
            {
                msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, INVALID_TABLE));
            }
        });

        // DELETING DATA FROM DB
        eventBus.localConsumer(DELETE_EVENT, msg -> {

            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals(DISCOVERY_PROFILE_TABLE))
            {
                try(var conn = Utils.getConnection(); var stmt = conn.prepareStatement(DISCOVER_PROFILE_DELETE_QUERY))
                {
                    stmt.setInt(1, Integer.parseInt(jsonObj.getString(DISCOVERY_PROFILE_ID)));

                    var rowsDeleted = stmt.executeUpdate();

                    if(rowsDeleted > 0)
                    {
                        LOGGER.info(ROWS_DELETED_CONTAINER, rowsDeleted, jsonObj.getString(TABLE_NAME));

                        msg.reply(SUCCESS);
                    }
                    else
                    {
                        LOGGER.info(KEY_VAL_NOT_FOUND_CONTAINER, DISCOVERY_PROFILE_ID, jsonObj.getString(DISCOVERY_PROFILE_ID));

                        msg.reply(FAILED);
                    }

                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());
                    msg.fail(500, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(CREDENTIAL_PROFILE_TABLE))
            {
                // before deleting cred profile, check if it is being used or not
                try(var conn = Utils.getConnection(); var cpDeleteStmt = conn.prepareStatement(CREDENTIAL_PROFILE_DELETE_QUERY); var pmCountSelectStmt = conn.prepareStatement(PROFILE_MAPPING_COUNT_SELECT_QUERY))
                {
                    pmCountSelectStmt.setInt(1, Integer.parseInt(jsonObj.getString(CREDENTIAL_PROFILE_ID)));

                    var countRs = pmCountSelectStmt.executeQuery();

                    while(countRs.next())
                    {
                        if(countRs.getInt(1) == 0)
                        {
                            cpDeleteStmt.setInt(1, Integer.parseInt(jsonObj.getString(CREDENTIAL_PROFILE_ID)));

                            var rowsDeleted = cpDeleteStmt.executeUpdate();

                            if(rowsDeleted > 0)
                            {
                                LOGGER.info(ROWS_DELETED_CONTAINER, rowsDeleted, jsonObj.getString(TABLE_NAME));

                                msg.reply(SUCCESS);
                            }
                            else
                            {
                                LOGGER.info(KEY_VAL_NOT_FOUND_CONTAINER, CREDENTIAL_PROFILE_ID, jsonObj.getString(CREDENTIAL_PROFILE_ID));

                                msg.fail(500, String.format("credential.profile.id = %s not found", jsonObj.getString(CREDENTIAL_PROFILE_ID)));
                            }
                        }
                        else
                        {
                            msg.fail(500, String.format("credential.profile.id = %s is currently in use", jsonObj.getString(CREDENTIAL_PROFILE_ID)));
                        }
                    }


                } catch(SQLException e)
                {
                    LOGGER.info(ERROR_CONTAINER, e.getMessage());

                    msg.fail(500, e.getMessage());
                }
            }
            else
            {
                msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, INVALID_TABLE));
            }
        });

        // MAKING CONTEXT FOR DISCOVERY
        eventBus.localConsumer(MAKE_DISCOVERY_CONTEXT_EVENT, msg -> {
            // example: reqArray = [1,2,3,4]
            var reqArray = new JsonArray(msg.body().toString());

            var contexts = new JsonArray();

            try
            {
                for(var discProfileId : reqArray)
                {
                    var discoveryProfile = getDiscoveryProfile(Integer.parseInt(discProfileId.toString()));

                    if(Utils.pingCheck(discoveryProfile.getString(OBJECT_IP)))
                    {
                        discoveryProfile.put(REQUEST_TYPE, DISCOVERY).put(PLUGIN_NAME, NETWORK);

                        if(discoveryProfile.getInteger(IS_DISCOVERED) == TRUE || discoveryProfile.getInteger(IS_PROVISIONED) == TRUE)
                        {
                            msg.fail(500, new JsonObject().put(ERROR, "Error in running discovery").put(ERR_MESSAGE, "Device ID " + discProfileId + " is already discovered or provisioned").put(ERR_STATUS_CODE, 500).toString());

                            return;
                        }

                        // TODO: take credentials ids from req body and loop over it
//                        var credentialsArray = new JsonArray();
//
//                        for(var credentialId : )
//                        {
//                            var credentialProfile = getCredentialProfile(Integer.parseInt(credentialId.toString()));
//
//                            credentialsArray.add(credentialProfile);
//                        }
//
//                        discoveryProfile.put(CREDENTIALS, credentialsArray);

                        contexts.add(discoveryProfile);
                    }
                }

                if(!contexts.isEmpty())
                {
                    msg.reply(contexts);
                }
                else
                {
                    msg.fail(500, new JsonObject().put(ERROR, "Error in running discovery").put(ERR_MESSAGE, "No device is eligible for discovery!").put(ERR_STATUS_CODE, 500).toString());
                }
            } catch(SQLException | NullPointerException e)
            {
                msg.fail(500, new JsonObject().put(ERROR, "Error in running discovery").put(ERR_MESSAGE, e.getMessage()).put(ERR_STATUS_CODE, 500).toString());
            }

        });

        // POST DISCOVERY SUCCESS
        eventBus.localConsumer(POST_DISCOVERY_SUCCESS_EVENT, msg -> {
            var jsonObj = new JsonObject(msg.body().toString());

            try(var conn = Utils.getConnection(); var dpUpdateStmt = conn.prepareStatement(DISCOVERY_STATUS_UPDATE_QUERY); var uniqueCredProfStmt = conn.prepareStatement(PROFILE_MAPPING_UNIQUE_CREDENTIAL_PROFILE_IDS_QUERY))
            {
                dpUpdateStmt.setInt(1, jsonObj.getInteger(DISCOVERY_PROFILE_ID));

                if(dpUpdateStmt.executeUpdate() > 0)
                {
                    LOGGER.info("Discovery status changed to 1 for discovery.profile.id: {}", jsonObj.getInteger(DISCOVERY_PROFILE_ID));
                }
                else
                {
                    throw new SQLException("Error in updating disc profile");
                }

                uniqueCredProfStmt.setInt(1, jsonObj.getInteger(DISCOVERY_PROFILE_ID));

                var uniqueCredsRS = uniqueCredProfStmt.executeQuery();

                // if mapping = 0
                if(!uniqueCredsRS.isBeforeFirst())
                {
                    if(insertProfileMapping(jsonObj.getInteger(DISCOVERY_PROFILE_ID), jsonObj.getInteger(CREDENTIAL_PROFILE_ID)) > 0)
                    {
                        LOGGER.info("Mapping added for [discId:credId]: [{}:{}]", jsonObj.getInteger(DISCOVERY_PROFILE_ID), jsonObj.getInteger(CREDENTIAL_PROFILE_ID));
                    }
                }
                // if credential profiles are present
                else
                {
                    var credentialsArray = new JsonArray();

                    while(uniqueCredsRS.next())
                    {
                        credentialsArray.add(uniqueCredsRS.getInt(CREDENTIAL_PROFILE_ID));
                    }

                    // if mapping for credential profile & discovery profile does not exist
                    if(!credentialsArray.contains(jsonObj.getInteger(CREDENTIAL_PROFILE_ID)))
                    {
                        if(insertProfileMapping(jsonObj.getInteger(DISCOVERY_PROFILE_ID), jsonObj.getInteger(CREDENTIAL_PROFILE_ID)) > 0)
                        {
                            LOGGER.info("Mapping added for [discId:credId]: [{}:{}]", jsonObj.getInteger(DISCOVERY_PROFILE_ID), jsonObj.getInteger(CREDENTIAL_PROFILE_ID));
                        }
                    }
                    // if mapping for credential profile & discovery profile exists
                    else
                    {
                        LOGGER.info("Mapping already exists for [discId:credId]: [{}:{}]", jsonObj.getInteger(DISCOVERY_PROFILE_ID), jsonObj.getInteger(CREDENTIAL_PROFILE_ID));
                    }

                }
            } catch(SQLException e)
            {
                LOGGER.error(ERROR_CONTAINER, e.getMessage());
            }
        });

        // PROVISION_DEVICES
        eventBus.localConsumer(GET_PROVISIONED_DEVICES_EVENT, msg -> {

            var contexts = new JsonArray();

            try
            {
                var monitors = getObjectProfiles();

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
                msg.fail(500, new JsonObject().put(ERROR, "Error provisioning devices!").put(ERR_MESSAGE, e.getMessage()).put(ERR_STATUS_CODE, 500).toString());
            }

            if(!contexts.isEmpty())
            {
                msg.reply(contexts);
            }
            else
            {
                msg.fail(500, new JsonObject().put(ERROR, "Error provisioning devices!").put(ERR_MESSAGE, "No devices qualified for provisioning").put(ERR_STATUS_CODE, 500).toString());
            }

        });

        // STORE POLLED DATA
        eventBus.localConsumer(STORE_POLLED_DATA_EVENT, msg -> {
            var polledData = new JsonArray(msg.body().toString());

            for(var data : polledData)
            {
                var monitor = new JsonObject(data.toString());

                var interfaceData = monitor.getJsonObject(RESULT).getJsonArray(INTERFACE);

                var objectIp = monitor.getString(OBJECT_IP);

                var snmpCommunity = monitor.getString(SNMP_COMMUNITY);

                var snmpPort = monitor.getInteger(PORT);

                try(var conn = Utils.getConnection(); var metricsInsertStmt = conn.prepareStatement(METRICS_INSERT_QUERY))
                {
                    for(var singleInterface : interfaceData)
                    {
                        var interfaceObj = new JsonObject(singleInterface.toString());

                        metricsInsertStmt.setString(1, objectIp);
                        metricsInsertStmt.setString(2, snmpCommunity);
                        metricsInsertStmt.setInt(3, snmpPort);

                        metricsInsertStmt.setInt(4, interfaceObj.containsKey(INTERFACE_INDEX) ? interfaceObj.getInteger(INTERFACE_INDEX) : -999);
                        metricsInsertStmt.setString(5, interfaceObj.containsKey(INTERFACE_NAME) ? interfaceObj.getString(INTERFACE_NAME) : "");
                        metricsInsertStmt.setInt(6, interfaceObj.containsKey(INTERFACE_OPERATIONAL_STATUS) ? interfaceObj.getInteger(INTERFACE_OPERATIONAL_STATUS) : -999);
                        metricsInsertStmt.setInt(7, interfaceObj.containsKey(INTERFACE_ADMIN_STATUS) ? interfaceObj.getInteger(INTERFACE_ADMIN_STATUS) : -999);
                        metricsInsertStmt.setString(8, interfaceObj.containsKey(INTERFACE_DESCRIPTION) ? interfaceObj.getString(INTERFACE_DESCRIPTION) : "");
                        metricsInsertStmt.setBigDecimal(9, BigDecimal.valueOf(interfaceObj.containsKey(INTERFACE_SENT_ERROR_PACKET) ? interfaceObj.getInteger(INTERFACE_SENT_ERROR_PACKET) : -999));
                        metricsInsertStmt.setBigDecimal(10, BigDecimal.valueOf(interfaceObj.containsKey(INTERFACE_RECEIVED_ERROR_PACKET) ? interfaceObj.getInteger(INTERFACE_RECEIVED_ERROR_PACKET) : -999));
                        metricsInsertStmt.setBigDecimal(11, BigDecimal.valueOf(interfaceObj.containsKey(INTERFACE_SENT_OCTETS) ? interfaceObj.getInteger(INTERFACE_SENT_OCTETS) : -999));
                        metricsInsertStmt.setBigDecimal(12, BigDecimal.valueOf(interfaceObj.containsKey(INTERFACE_RECEIVED_OCTETS) ? interfaceObj.getInteger(INTERFACE_RECEIVED_OCTETS) : -999));
                        metricsInsertStmt.setInt(13, interfaceObj.containsKey(INTERFACE_SPEED) ? interfaceObj.getInteger(INTERFACE_SPEED) : -999);
                        metricsInsertStmt.setString(14, interfaceObj.containsKey(INTERFACE_ALIAS) ? interfaceObj.getString(INTERFACE_ALIAS) : "");
                        metricsInsertStmt.setString(15, interfaceObj.containsKey(INTERFACE_PHYSICAL_ADDRESS) ? interfaceObj.getString(INTERFACE_PHYSICAL_ADDRESS) : "");

                        metricsInsertStmt.addBatch();
                    }

                    var rowsInserted = metricsInsertStmt.executeBatch();

                    LOGGER.debug("{} rows data inserted for {}", rowsInserted.length, objectIp);
                } catch(SQLException e)
                {
                    LOGGER.error(ERROR_CONTAINER, e.getMessage());
                }
            }

        });

        // REMOVE PROVISIONED DEVICE
        eventBus.localConsumer(STOP_POLLING_EVENT, msg -> {

            var discProfileId = Integer.parseInt(msg.body().toString());

            try(var conn = Utils.getConnection(); var provStatusUpdataStmt = conn.prepareStatement(PROVISION_STATUS_UPDATE_QUERY))
            {
                provStatusUpdataStmt.setInt(1, 0);

                provStatusUpdataStmt.setInt(2, discProfileId);

                if(provStatusUpdataStmt.executeUpdate() > 0)
                {
                    LOGGER.info("Provision status changed to 0 for {}: {}", DISCOVERY_PROFILE_ID, discProfileId);
                }
                else
                {
                    throw new SQLException("Error in updating provision status of discovery profile: " + discProfileId);
                }

            } catch(SQLException e)
            {
                LOGGER.error(ERROR_CONTAINER, e.getMessage());

                msg.fail(500, e.getMessage());
            }

            msg.reply(SUCCESS);

        });

        startPromise.complete();
    }


    private JsonObject getDiscoveryProfile(int discoveryProfileId) throws SQLException
    {
        var discoveryProfile = new JsonObject();

        try(var conn = Utils.getConnection(); var dpSelectStmt = conn.prepareStatement(DISCOVERY_PROFILE_SELECT_QUERY))
        {
            dpSelectStmt.setInt(1, discoveryProfileId);

            var discProfileRS = dpSelectStmt.executeQuery();

            if(!discProfileRS.isBeforeFirst())
            {
                throw new SQLException("No discovery profile found!");
            }
            else
            {
                while(discProfileRS.next())
                {
                    discoveryProfile.put(DISCOVERY_PROFILE_ID, discProfileRS.getInt(DISCOVERY_PROFILE_ID)).put(DISCOVERY_NAME, discProfileRS.getString(DISCOVERY_NAME)).put(OBJECT_IP, discProfileRS.getString(OBJECT_IP)).put(PORT, discProfileRS.getInt(PORT)).put(IS_PROVISIONED, discProfileRS.getInt(IS_PROVISIONED)).put(IS_DISCOVERED, discProfileRS.getInt(IS_DISCOVERED));
                }
            }
        }
        return discoveryProfile;
    }

    private JsonObject getCredentialProfile(int credProfileId) throws SQLException
    {
        var credentialProfile = new JsonObject();

        try(var conn = Utils.getConnection(); var dpSelectStmt = conn.prepareStatement(CREDENTIAL_PROFILE_SELECT_QUERY))
        {
            dpSelectStmt.setInt(1, credProfileId);

            var credProfileRS = dpSelectStmt.executeQuery();

            if(!credProfileRS.isBeforeFirst())
            {
                throw new SQLException("No credential profile found!");
            }
            else
            {
                while(credProfileRS.next())
                {
                    credentialProfile.put(VERSION, credProfileRS.getString(VERSION)).put(CREDENTIAL_PROFILE_ID, credProfileRS.getInt(CREDENTIAL_PROFILE_ID)).put(CREDENTIAL_NAME, credProfileRS.getString(CREDENTIAL_NAME)).put(PROTOCOL, credProfileRS.getString(PROTOCOL)).put(VERSION, credProfileRS.getString(VERSION)).put(SNMP_COMMUNITY, credProfileRS.getString(SNMP_COMMUNITY));
                }
            }

        }

        return credentialProfile;
    }

    private int insertProfileMapping(int discProfileId, int credProfileId) throws SQLException
    {
        try(var conn = Utils.getConnection(); var pmStmt = conn.prepareStatement(PROFILE_MAPPING_INSERT_QUERY);)
        {
            pmStmt.setInt(1, discProfileId);

            pmStmt.setInt(2, credProfileId);

            return pmStmt.executeUpdate();
        }
    }

    private JsonArray getObjectProfiles() throws SQLException
    {
        var objectProfiles = new JsonArray();

        try(var conn = Utils.getConnection(); var dpSelectAllStmt = conn.createStatement())
        {
            var objectsRS = dpSelectAllStmt.executeQuery(PROFILE_MAPPING_SELECT_ALL_QUERY);

            if(!objectsRS.isBeforeFirst())
            {
                throw new SQLException("No profile mapping found!");
            }
            else
            {
                while(objectsRS.next())
                {
                    objectProfiles.add(new JsonObject().put(OBJECT_IP, objectsRS.getString(OBJECT_IP)).put(DISCOVERY_NAME, objectsRS.getString(DISCOVERY_NAME)).put(VERSION, objectsRS.getString(VERSION)).put(PORT, objectsRS.getInt(PORT)).put(SNMP_COMMUNITY, objectsRS.getString(SNMP_COMMUNITY)).put(IS_PROVISIONED, objectsRS.getInt(IS_PROVISIONED)).put(IS_DISCOVERED, objectsRS.getInt(IS_DISCOVERED)).put(DISCOVERY_PROFILE_ID, objectsRS.getInt(DISCOVERY_PROFILE_ID)).put(CREDENTIAL_PROFILE_ID, objectsRS.getInt(CREDENTIAL_PROFILE_ID)).put(REQUEST_TYPE, COLLECT).put(PLUGIN_NAME, NETWORK));
                }
            }
        }

        return objectProfiles;
    }

}
