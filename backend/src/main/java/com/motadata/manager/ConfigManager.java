package com.motadata.manager;

import com.motadata.utils.Jdbc;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static com.motadata.Bootstrap.*;
import static com.motadata.utils.Constants.*;

public class ConfigManager extends AbstractVerticle
{
    private final String cpInsertQ = "INSERT INTO `nmsDB`.`credential_profile` (`cred.name`, `protocol`, `version`, `snmp.community`) VALUES (?,?,?,?);";

    private final String cpSelectQ = "SELECT * FROM `credential_profile` where `cred.profile.id`=?;";

    private final String cpSelectAllQ = "SELECT * FROM `credential_profile`";

    private final String cpUpdateQ = "UPDATE `credential_profile` SET `version` = ?, `snmp.community` = ? WHERE `cred.profile.id` = ?;";

    private final String dpInsertQ = "INSERT INTO `nmsDB`.`discovery_profile` (`disc.name`,`object.ip`,`snmp.port`,`credentials`) VALUES (?,?,?,?);";

    private final String dpSelectQ = "SELECT * FROM `discovery_profile` WHERE `disc.profile.id` = ?";

    private final String dpSelectAllQ = "SELECT * FROM `discovery_profile`";

    private final String dpUpdateQ = "UPDATE `discovery_profile` SET `is.discovered` = 0,`is.provisioned` = 0,`disc.name` = ?, `snmp.port` = ? WHERE `disc.profile.id` = ?;";

    private final String dpUpdateIsDiscStatusQ = "UPDATE `discovery_profile` SET `is.discovered` = 1 WHERE `disc.profile.id` = ?;";

    private final String dpUpdateProvReqStatusQ = "UPDATE `discovery_profile` SET `provision.request` = 1 WHERE `disc.profile.id` = ?;";

    private final String dpUpdateIsProvStatusQ = "UPDATE `discovery_profile` SET `is.provisioned` = 1 WHERE `disc.profile.id` = ?;";

    private final String cpDeleteQ = "DELETE FROM `credential_profile` WHERE `cred.profile.id` = ?;";

    private final String dpDeleteQ = "DELETE FROM `discovery_profile` WHERE `disc.profile.id` = ?;";

    private final String pmInsertQ = "INSERT INTO `nmsDB`.`profile_mapping` (`disc.profile.id`,`cred.profile.id`) VALUES (?,?);";

    private final String uniqueCredProfileIdsQ = "SELECT distinct(`cred.profile.id`) FROM nmsDB.profile_mapping where `disc.profile.id`=?;";

    private final String pmSelectAllQ = "SELECT pm.`disc.profile.id`, pm.`cred.profile.id`, cp.`cred.name`, cp.`protocol`, cp.`version`, cp.`snmp.community`, dp.`disc.name`, dp.`object.ip`, dp.`snmp.port`, dp.`is.provisioned`, dp.`is.discovered`, dp.`provision.request`, cp.`version` FROM profile_mapping pm JOIN credential_profile cp ON pm.`cred.profile.id` = cp.`cred.profile.id` JOIN discovery_profile dp ON pm.`disc.profile.id` = dp.`disc.profile.id`;";

    private final String pmCountSelectQ = "SELECT COUNT(*) as count FROM profile_mapping WHERE `cred.profile.id` = ?;";

    private final String metricsInsertQ = "INSERT INTO network_interface (`object.ip`,`snmp.community`,`snmp.port`,`interface.index`,`interface.name`,`interface.operational.status`,`interface.admin.status`,`interface.description`,`interface.sent.error.packet`,`interface.received.error.packet`,`interface.sent.octets`,`interface.received.octets`,`interface.speed`,`interface.alias`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        var eventBus = vertx.eventBus();

        // INSERTING DATA TO DB
        eventBus.localConsumer(INSERT_EVENT, msg -> {
            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals(DISC_PROFILE_TABLE))
            {
                var credentialsDB = new JsonArray();

                try(var conn = Jdbc.getConnection(); var cpSelectAllStmt = conn.createStatement())
                {
                    var credentials = cpSelectAllStmt.executeQuery(cpSelectAllQ);

                    while(credentials.next())
                    {
                        credentialsDB.add(credentials.getInt(CRED_PROF_ID));
                    }

                    LOGGER.debug(credentialsDB.toString());

                } catch(SQLException e)
                {
                    LOGGER.error(e.getMessage());
                }

                // checking if the credential profile is present for binding or not
                var userCredentials = new JsonArray(jsonObj.getString(CREDENTIALS));

                var credentialsReqSet = new HashSet<>(userCredentials.stream().toList());

                var credentialsDBJsonSet = new HashSet<>(credentialsDB.stream().toList());

                if(!credentialsDBJsonSet.containsAll(credentialsReqSet))
                {
                    LOGGER.error("One or more credential IDs are not available in the database.");

                    msg.fail(500, "One or more credential IDs are not available in the database.");

                    return;
                }

                try(var conn = Jdbc.getConnection(); var dpInsertStmt = conn.prepareStatement(dpInsertQ))
                {
                    dpInsertStmt.setString(1, jsonObj.getString(DISC_NAME));
                    dpInsertStmt.setString(2, jsonObj.getString(OBJECT_IP));
                    dpInsertStmt.setString(3, jsonObj.getString(SNMP_PORT));
                    dpInsertStmt.setString(4, jsonObj.getJsonArray(CREDENTIALS).toString());

                    var rowsInserted = dpInsertStmt.executeUpdate();

                    if(rowsInserted > 0)
                    {
                        LOGGER.info("{} rows inserted in {}", rowsInserted, jsonObj.getString(TABLE_NAME));

                        msg.reply(SUCCESS);
                    }


                } catch(SQLException e)
                {
                    LOGGER.info("Error: {}", e.getMessage());

                    msg.fail(500, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(CRED_PROFILE_TABLE))
            {
                try(var conn = Jdbc.getConnection(); var cpInsertStmt = conn.prepareStatement(cpInsertQ))
                {
                    cpInsertStmt.setString(1, jsonObj.getString(CRED_NAME));
                    cpInsertStmt.setString(2, jsonObj.getString(PROTOCOL));
                    cpInsertStmt.setString(3, jsonObj.getString(VERSION));
                    cpInsertStmt.setString(4, jsonObj.getString(SNMP_COMMUNITY));

                    var rowsInserted = cpInsertStmt.executeUpdate();

                    if(rowsInserted > 0)
                    {
                        LOGGER.info("{} rows inserted in {}", rowsInserted, jsonObj.getString(TABLE_NAME));

                        msg.reply(SUCCESS);
                    }

                } catch(SQLException e)
                {
                    LOGGER.info("Error: {}", e.getMessage());

                    msg.fail(500, e.getMessage());
                }

            }
            else
            {
                msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, "Invalid table name!"));
            }

        });

        // FETCHING ALL DATA FROM DB
        eventBus.localConsumer(GET_ALL_EVENT, msg -> {
            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals(CRED_PROFILE_TABLE))
            {
                try(var conn = Jdbc.getConnection(); var cpSelectAllStmt = conn.createStatement())
                {
                    var credentials = cpSelectAllStmt.executeQuery(cpSelectAllQ);

                    var credentialProfiles = new JsonArray();

                    while(credentials.next())
                    {
                        credentialProfiles.add(new JsonObject().put(CRED_NAME, credentials.getString(CRED_NAME)).put(PROTOCOL, credentials.getString(PROTOCOL)).put(CRED_PROF_ID, credentials.getInt(CRED_PROF_ID)).put(VERSION, credentials.getString(VERSION)).put(SNMP_COMMUNITY, credentials.getString(SNMP_COMMUNITY)));
                    }

                    if(credentialProfiles.isEmpty())
                    {
                        throw new SQLException("No credential profile found!");
                    }

                    LOGGER.info("credential profiles fetched successfully");

                    msg.reply(credentialProfiles);

                } catch(SQLException e)
                {
                    LOGGER.info("Error: {}", e.getMessage());

                    msg.fail(404, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(DISC_PROFILE_TABLE))
            {
                try(var conn = Jdbc.getConnection(); var dpSelectAllStmt = conn.createStatement())
                {
                    var discoveryProfilesRS = dpSelectAllStmt.executeQuery(dpSelectAllQ);

                    var discoveryProfiles = new JsonArray();

                    while(discoveryProfilesRS.next())
                    {
                        discoveryProfiles.add(new JsonObject().put(DISC_PROF_ID, discoveryProfilesRS.getInt(DISC_PROF_ID)).put(DISC_NAME, discoveryProfilesRS.getString(DISC_NAME)).put(OBJECT_IP, discoveryProfilesRS.getString(OBJECT_IP)).put(SNMP_PORT, discoveryProfilesRS.getInt(SNMP_PORT)).put(IS_PROVISIONED, discoveryProfilesRS.getInt(IS_PROVISIONED)).put(IS_DISCOVERED, discoveryProfilesRS.getInt(IS_DISCOVERED)).put(CREDENTIALS, discoveryProfilesRS.getString(CREDENTIALS)));
                    }

                    if(discoveryProfiles.isEmpty())
                    {
                        throw new SQLException("No discovery profile found!");
                    }

                    LOGGER.info("discovery profiles fetched successfully");

                    msg.reply(discoveryProfiles);

                } catch(SQLException e)
                {
                    LOGGER.info("Error: {}", e.getMessage());

                    msg.fail(404, e.getMessage());
                }
            }


        });

        // FETCHING DATA FROM DB
        eventBus.localConsumer(GET_EVENT, msg -> {

            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals(DISC_PROFILE_TABLE))
            {
                try
                {
                    var discoveryProfile = getDiscoveryProfile(Integer.parseInt(jsonObj.getString(DISC_PROF_ID)));

                    if(discoveryProfile.isEmpty())
                    {
                        throw new SQLException("No discovery profile found!");
                    }
                    else
                    {
                        LOGGER.info("discovery profile id = {} fetched successfully", jsonObj.getString(DISC_PROF_ID));

                        msg.reply(discoveryProfile);
                    }

                } catch(SQLException e)
                {
                    LOGGER.info("Error: {}", e.getMessage());

                    msg.fail(500, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(CRED_PROFILE_TABLE))
            {
                try
                {
                    var credentialProfile = getCredentialProfile(Integer.parseInt(jsonObj.getString(CRED_PROF_ID)));

                    if(credentialProfile.isEmpty())
                    {
                        throw new SQLException("No credential profile found!");
                    }
                    else
                    {
                        LOGGER.info("credential profile id = {} fetched successfully", jsonObj.getString(CRED_PROF_ID));

                        msg.reply(credentialProfile);
                    }

                } catch(SQLException e)
                {
                    LOGGER.info("Error: {}", e.getMessage());

                    msg.fail(500, e.getMessage());
                }

            }
            else
            {
                msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, "Invalid table name!"));
            }
        });

        // UPDATING DATA IN DB
        eventBus.localConsumer(UPDATE_EVENT, msg -> {
            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals(DISC_PROFILE_TABLE))
            {
                try(var conn = Jdbc.getConnection(); var dpUpdateStmt = conn.prepareStatement(dpUpdateQ))
                {
                    dpUpdateStmt.setString(1, jsonObj.getString(DISC_NAME));

                    dpUpdateStmt.setInt(2, jsonObj.getInteger(SNMP_PORT));

                    dpUpdateStmt.setInt(3, jsonObj.getInteger(DISC_PROF_ID));

                    var rowsUpdated = dpUpdateStmt.executeUpdate();

                    if(rowsUpdated > 0)
                    {
                        LOGGER.info("{} rows updated in {}", rowsUpdated, CRED_PROFILE_TABLE);

                        var discoveryProfile = getDiscoveryProfile(Integer.parseInt(jsonObj.getString(DISC_PROF_ID)));

                        msg.reply(discoveryProfile);
                    }
                    else
                    {
                        LOGGER.info("{} = {} not found",DISC_PROF_ID, jsonObj.getString(DISC_PROF_ID));

                        msg.fail(500, "disc.profile.id = " + jsonObj.getString(DISC_PROF_ID) + " not found");
                    }

                } catch(SQLException e)
                {
                    LOGGER.info("Error: {}", e.getMessage());

                    msg.fail(500, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(CRED_PROFILE_TABLE))
            {
                try(var conn = Jdbc.getConnection(); var updateStmt = conn.prepareStatement(cpUpdateQ))
                {
                    updateStmt.setString(1, jsonObj.getString(VERSION));
                    updateStmt.setString(2, jsonObj.getString(SNMP_COMMUNITY));
                    updateStmt.setInt(3, jsonObj.getInteger(CRED_PROF_ID));

                    var rowsUpdated = updateStmt.executeUpdate();

                    if(rowsUpdated > 0)
                    {
                        LOGGER.info("{} rows updated in credential_profile", rowsUpdated);

                        var credentialProfile = getCredentialProfile(Integer.parseInt(jsonObj.getString(CRED_PROF_ID)));

                        msg.reply(credentialProfile);
                    }
                    else
                    {
                        LOGGER.info("cred.profile.id = {} not found", jsonObj.getString(CRED_PROF_ID));

                        msg.fail(500, "cred.profile.id = " + jsonObj.getString(CRED_PROF_ID) + " not found");
                    }

                } catch(SQLException e)
                {
                    LOGGER.info("Error: {}", e.getMessage());
                    msg.fail(500, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(PROFILE_MAPPING_TABLE))
            {
                try(var conn = Jdbc.getConnection(); var updateStmt = conn.prepareStatement(dpUpdateProvReqStatusQ))
                {
                    var discoveryProfile = getDiscoveryProfile(jsonObj.getInteger(DISC_PROF_ID));

                    // update only if: is.provisioned = 0
                    if(discoveryProfile.getInteger(IS_PROVISIONED)==FALSE)
                    {
                        updateStmt.setInt(1, jsonObj.getInteger(DISC_PROF_ID));

                        var rowsUpdated = updateStmt.executeUpdate();

                        if(rowsUpdated > 0)
                        {
                            LOGGER.info("{} rows updated in {}", rowsUpdated, jsonObj.getString(TABLE_NAME));

                            msg.reply(SUCCESS);
                        }
                        else
                        {
                            LOGGER.info("{} = {} not found", DISC_PROF_ID, jsonObj.getString(DISC_PROF_ID));

                            msg.fail(500, "disc.profile.id = " + jsonObj.getString(DISC_PROF_ID) + " not found");
                        }
                    }
                    else
                    {
                        msg.fail(500, "disc.profile.id = " + jsonObj.getString(DISC_PROF_ID) + " is already provisioned");
                    }


                } catch(SQLException e)
                {
                    LOGGER.info("Error: {}", e.getMessage());
                    msg.fail(500, e.getMessage());
                }
            }
            else
            {
                msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, "Invalid table name!"));
            }
        });

        // DELETING DATA FROM DB
        eventBus.localConsumer(DELETE_EVENT, msg -> {


            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals(DISC_PROFILE_TABLE))
            {
                try(var conn = Jdbc.getConnection(); var stmt = conn.prepareStatement(dpDeleteQ))
                {
                    stmt.setInt(1, Integer.parseInt(jsonObj.getString(DISC_PROF_ID)));

                    var rowsDeleted = stmt.executeUpdate();

                    if(rowsDeleted > 0)
                    {
                        LOGGER.info("{} rows deleted from discovery_profile", rowsDeleted);

                        msg.reply(SUCCESS);
                    }
                    else
                    {
                        LOGGER.info("disc.profile.id = {} not found", jsonObj.getString(DISC_PROF_ID));

                        msg.reply(FAILED);
                    }

                } catch(SQLException e)
                {
                    LOGGER.info("Error: {}", e.getMessage());
                    msg.fail(500, e.getMessage());
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals(CRED_PROFILE_TABLE))
            {
                try(var conn = Jdbc.getConnection(); var cpDeleteStmt = conn.prepareStatement(cpDeleteQ); var pmCountSelectStmt = conn.prepareStatement(pmCountSelectQ))
                {
                    pmCountSelectStmt.setInt(1, Integer.parseInt(jsonObj.getString(CRED_PROF_ID)));

                    var countRs = pmCountSelectStmt.executeQuery();

                    while(countRs.next())
                    {
                        if(countRs.getInt(1) == 0)
                        {
                            cpDeleteStmt.setInt(1, Integer.parseInt(jsonObj.getString(CRED_PROF_ID)));

                            var rowsDeleted = cpDeleteStmt.executeUpdate();

                            if(rowsDeleted > 0)
                            {
                                LOGGER.info("{} rows deleted from credential_profile", rowsDeleted);

                                msg.reply(SUCCESS);
                            }
                            else
                            {
                                LOGGER.info("cred.profile.id = {} not found", jsonObj.getString(CRED_PROF_ID));

                                msg.fail(500, String.format("cred.profile.id = %s not found", jsonObj.getString(CRED_PROF_ID)));
                            }
                        }
                        else
                        {
                            msg.fail(500, String.format("cred.profile.id = %s is currently in use", jsonObj.getString(CRED_PROF_ID)));
                        }
                    }


                } catch(SQLException e)
                {
                    LOGGER.info("Error: {}", e.getMessage());

                    msg.fail(500, e.getMessage());
                }
            }
            else
            {
                msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, "Invalid table name!"));
            }
        });

        // MAKING DISCOVERY CONTEXT
        eventBus.localConsumer(MAKE_DISCOVERY_CONTEXT, msg -> {
            var reqArray = new JsonArray(msg.body().toString());

            var contexts = new JsonArray();

            try
            {
                for(var discProfileId : reqArray)
                {
                    var discoveryProfile = getDiscoveryProfile(Integer.parseInt(discProfileId.toString()));

                    if(pingCheck(discoveryProfile.getString(OBJECT_IP)))
                    {
                        discoveryProfile.put(REQUEST_TYPE, DISCOVERY).put(PLUGIN_NAME, NETWORK);

                        var credentialStr = discoveryProfile.getString(CREDENTIALS);

                        if(discoveryProfile.getInteger(IS_DISCOVERED) == TRUE || discoveryProfile.getInteger(IS_PROVISIONED) == TRUE)
                        {
                            msg.fail(500, new JsonObject().put(ERROR, "Error in running discovery").put(ERR_MESSAGE, "Device ID " + discProfileId + " is already discovered or provisioned").put(ERR_STATUS_CODE, 500).toString());

                            return;
                        }

                        var credentialsArray = new JsonArray();

                        for(var credentialId : new JsonArray(credentialStr))
                        {
                            var credentialProfile = getCredentialProfile(Integer.parseInt(credentialId.toString()));

                            credentialsArray.add(credentialProfile);
                        }

                        discoveryProfile.put(CREDENTIALS, credentialsArray);

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
        eventBus.localConsumer(POST_DISC_SUCCESS, msg -> {
            var jsonObj = new JsonObject(msg.body().toString());

            try(var conn = Jdbc.getConnection(); var dpUpdateStmt = conn.prepareStatement(dpUpdateIsDiscStatusQ); var uniqueCredProfStmt = conn.prepareStatement(uniqueCredProfileIdsQ))
            {
                dpUpdateStmt.setInt(1, jsonObj.getInteger(DISC_PROF_ID));

                if(dpUpdateStmt.executeUpdate() > 0)
                {
                    LOGGER.info("Discovery status changed to 1 for disc.profile.id: {}", jsonObj.getInteger(DISC_PROF_ID));
                }
                else
                {
                    throw new SQLException("Error in updating disc profile");
                }

                uniqueCredProfStmt.setInt(1, jsonObj.getInteger(DISC_PROF_ID));

                var uniqueCredsRS = uniqueCredProfStmt.executeQuery();

                // if mapping = 0
                if(!uniqueCredsRS.isBeforeFirst())
                {
                    if(insertProfileMapping(jsonObj.getInteger(DISC_PROF_ID), jsonObj.getInteger(CRED_PROF_ID)) > 0)
                    {
                        LOGGER.info("Mapping added for [discId:credId]: [{}:{}]", jsonObj.getInteger(DISC_PROF_ID), jsonObj.getInteger(CRED_PROF_ID));
                    }
                }
                // if credential profiles are present
                else
                {
                    var credentialsArray = new JsonArray();

                    while(uniqueCredsRS.next())
                    {
                        credentialsArray.add(uniqueCredsRS.getInt(CRED_PROF_ID));
                    }

                    // if mapping for credential profile & discovery profile does not exist
                    if(!credentialsArray.contains(jsonObj.getInteger(CRED_PROF_ID)))
                    {
                        if(insertProfileMapping(jsonObj.getInteger(DISC_PROF_ID), jsonObj.getInteger(CRED_PROF_ID)) > 0)
                        {
                            LOGGER.info("Mapping added for [discId:credId]: [{}:{}]", jsonObj.getInteger(DISC_PROF_ID), jsonObj.getInteger(CRED_PROF_ID));
                        }
                    }
                    // if mapping for credential profile & discovery profile exists
                    else
                    {
                        LOGGER.info("Mapping already exists for [discId:credId]: [{}:{}]", jsonObj.getInteger(DISC_PROF_ID), jsonObj.getInteger(CRED_PROF_ID));
                    }

                }
            } catch(SQLException e)
            {
                LOGGER.error("Error: {}", e.getMessage());
            }
        });

        // PROVISION_DEVICES
        eventBus.localConsumer(PROVISION_DEVICES, msg -> {

            var contexts = new JsonArray();

            try
            {
                var monitors = getObjectProfiles();

                for(var monitor : monitors)
                {
                    var monitorObj = new JsonObject(monitor.toString());

                    if(monitorObj.getInteger(IS_PROVISIONED) == FALSE && monitorObj.getInteger(PROVISION_REQUEST) == TRUE && monitorObj.getInteger(IS_DISCOVERED) == TRUE && pingCheck(monitorObj.getString(OBJECT_IP)))
                    {
                        monitorObj.remove(IS_DISCOVERED);

                        monitorObj.remove(IS_PROVISIONED);

                        try(var conn = Jdbc.getConnection(); var dpUpdateStmt = conn.prepareStatement(dpUpdateIsProvStatusQ))
                        {
                            dpUpdateStmt.setInt(1, monitorObj.getInteger(DISC_PROF_ID));

                            if(dpUpdateStmt.executeUpdate() > 0)
                            {
                                LOGGER.info("Provision status changed to 1 for disc.profile.id: {}", monitorObj.getInteger(DISC_PROF_ID));
                            }
                            else
                            {
                                throw new SQLException("Error in updating disc profile");
                            }
                        } catch(SQLException e)
                        {
                            LOGGER.error("Error: {}", e.getMessage());
                        }

                        contexts.add(monitor);
                    }
                }

                if(!contexts.isEmpty())
                {
                    msg.reply(contexts);
                }
                else
                {
                    msg.fail(500, new JsonObject().put(ERROR, "Error provisioning devices!").put(ERR_MESSAGE, "No devices qualified for provisioning").put(ERR_STATUS_CODE, 500).toString());
                }

            } catch(SQLException e)
            {
                msg.fail(500, new JsonObject().put(ERROR, "Error provisioning devices!").put(ERR_MESSAGE, e.getMessage()).put(ERR_STATUS_CODE, 500).toString());
            }
        });

        // POLL DATA STORE
        eventBus.localConsumer(POLL_DATA_STORE, msg -> {
            var polledData = new JsonObject(msg.body().toString());

            var interfaceData = polledData.getJsonObject(RESULT).getJsonArray(INTERFACE);

            var objectIp = polledData.getString(OBJECT_IP);

            var snmpCommunity = polledData.getString(SNMP_COMMUNITY);

            var snmpPort = polledData.getInteger(SNMP_PORT);

            try(var conn = Jdbc.getConnection(); var metricsInsertStmt = conn.prepareStatement(metricsInsertQ))
            {
                for(var singleInterface: interfaceData)
                {
                    var interfaceObj = new JsonObject(singleInterface.toString());

                    metricsInsertStmt.setString(1, objectIp);
                    metricsInsertStmt.setString(2, snmpCommunity);
                    metricsInsertStmt.setInt(3, snmpPort);

                    metricsInsertStmt.setInt(4, interfaceObj.getInteger(INTERFACE_INDEX));
                    metricsInsertStmt.setString(5, interfaceObj.getString(INTERFACE_NAME));
                    metricsInsertStmt.setInt(6, interfaceObj.getInteger(INTERFACE_OPERATIONAL_STATUS));
                    metricsInsertStmt.setInt(7, interfaceObj.getInteger(INTERFACE_ADMIN_STATUS));
                    metricsInsertStmt.setString(8,interfaceObj.getString(INTERFACE_DESCRIPTION));
                    metricsInsertStmt.setInt(9, interfaceObj.getInteger(INTERFACE_SENT_ERROR_PACKET));
                    metricsInsertStmt.setInt(10, interfaceObj.getInteger(INTERFACE_RECEIVED_ERROR_PACKET));
                    metricsInsertStmt.setInt(11, interfaceObj.getInteger(INTERFACE_SENT_OCTETS));
                    metricsInsertStmt.setInt(12, interfaceObj.getInteger(INTERFACE_RECEIVED_OCTETS));
                    metricsInsertStmt.setInt(13, interfaceObj.getInteger(INTERFACE_SPEED));
                    metricsInsertStmt.setString(14, interfaceObj.getString(INTERFACE_ALIAS));

                    metricsInsertStmt.addBatch();
                }

                var rowsInserted = metricsInsertStmt.executeBatch();

                LOGGER.debug("{} rows inserted.",rowsInserted.length);
            }
            catch(SQLException e)
            {
                LOGGER.error("Error: {}", e.getMessage());
            }

        });

        startPromise.complete();
    }


    private JsonObject getDiscoveryProfile(int discoveryProfileId) throws SQLException
    {
        var discoveryProfile = new JsonObject();

        try(var conn = Jdbc.getConnection(); var dpSelectStmt = conn.prepareStatement(dpSelectQ))
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
                    discoveryProfile.put(DISC_PROF_ID, discProfileRS.getInt(DISC_PROF_ID)).put(DISC_NAME, discProfileRS.getString(DISC_NAME)).put(OBJECT_IP, discProfileRS.getString(OBJECT_IP)).put(SNMP_PORT, discProfileRS.getInt(SNMP_PORT)).put(IS_PROVISIONED, discProfileRS.getInt(IS_PROVISIONED)).put(IS_DISCOVERED, discProfileRS.getInt(IS_DISCOVERED)).put(CREDENTIALS, discProfileRS.getString(CREDENTIALS)).put(PROVISION_REQUEST, discProfileRS.getInt(PROVISION_REQUEST));
                }
            }
        }
        return discoveryProfile;
    }

    private JsonObject getCredentialProfile(int credProfileId) throws SQLException
    {
        var credentialProfile = new JsonObject();

        try(var conn = Jdbc.getConnection(); var dpSelectStmt = conn.prepareStatement(cpSelectQ))
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
                    credentialProfile.put(VERSION,credProfileRS.getString(VERSION)).put(CRED_PROF_ID, credProfileRS.getInt(CRED_PROF_ID)).put(CRED_NAME, credProfileRS.getString(CRED_NAME)).put(PROTOCOL, credProfileRS.getString(PROTOCOL)).put(VERSION, credProfileRS.getString(VERSION)).put(SNMP_COMMUNITY, credProfileRS.getString(SNMP_COMMUNITY));
                }
            }

        }

        return credentialProfile;
    }

    private int insertProfileMapping(int discProfileId, int credProfileId) throws SQLException
    {
        try(var conn = Jdbc.getConnection(); var pmStmt = conn.prepareStatement(pmInsertQ);)
        {
            pmStmt.setInt(1, discProfileId);

            pmStmt.setInt(2, credProfileId);

            return pmStmt.executeUpdate();
        }
    }

    public boolean pingCheck(String objectIp)
    {
        try
        {
            var processBuilder = new ProcessBuilder("fping", objectIp, "-c3", "-q");

            processBuilder.redirectErrorStream(true);

            var process = processBuilder.start();

            boolean isCompleted = process.waitFor(5, TimeUnit.SECONDS); // Wait for 5 seconds

            if(!isCompleted)
            {
                process.destroyForcibly();
            }
            else
            {
                var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;

                while((line = reader.readLine()) != null)
                {
                    if(line.contains("/0%"))
                    {
                        LOGGER.debug("{} device is UP!", objectIp);

                        return true;
                    }
                }
            }
        } catch(Exception exception)
        {
            LOGGER.error("Exception: {}", exception.getMessage());
        }

        LOGGER.debug("{} device is DOWN!", objectIp);

        return false;
    }

    private JsonArray getObjectProfiles() throws SQLException
    {
        var objectProfiles = new JsonArray();

        try(var conn = Jdbc.getConnection(); var dpSelectAllStmt = conn.createStatement())
        {
            var objectsRS = dpSelectAllStmt.executeQuery(pmSelectAllQ);

            if(!objectsRS.isBeforeFirst())
            {
                throw new SQLException("No profile mapping found!");
            }
            else
            {
                while(objectsRS.next())
                {
                    objectProfiles.add(new JsonObject().put(PROVISION_REQUEST, objectsRS.getInt(PROVISION_REQUEST)).put(OBJECT_IP, objectsRS.getString(OBJECT_IP)).put(DISC_NAME, objectsRS.getString(DISC_NAME)).put(VERSION, objectsRS.getString(VERSION)).put(SNMP_PORT, objectsRS.getInt(SNMP_PORT)).put(SNMP_COMMUNITY, objectsRS.getString(SNMP_COMMUNITY)).put(IS_PROVISIONED, objectsRS.getInt(IS_PROVISIONED)).put(IS_DISCOVERED, objectsRS.getInt(IS_DISCOVERED)).put(DISC_PROF_ID, objectsRS.getInt(DISC_PROF_ID)).put(CRED_PROF_ID, objectsRS.getInt(CRED_PROF_ID)).put(REQUEST_TYPE, COLLECT).put(PLUGIN_NAME, NETWORK));
                }
            }
        }

        return objectProfiles;
    }

}
