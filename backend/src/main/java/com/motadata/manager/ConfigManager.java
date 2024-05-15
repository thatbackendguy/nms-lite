package com.motadata.manager;

import com.motadata.utils.Jdbc;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

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

    private final String cpDeleteQ = "DELETE FROM `credential_profile` WHERE `cred.profile.id` = ?;";

    private final String dpDeleteQ = "DELETE FROM `discovery_profile` WHERE `disc.profile.id` = ?;";

    private final String pmInsertQ = "INSERT INTO `nmsDB`.`profile_mapping` (`disc.profile.id`,`cred.profile.id`) VALUES (?,?);";

    private final String uniqueCredProfileIdsQ = "SELECT distinct(`cred.profile.id`) FROM nmsDB.profile_mapping where `disc.profile.id`=?;";

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

                    dpUpdateStmt.setString(2, jsonObj.getString(SNMP_PORT));

                    dpUpdateStmt.setInt(3, Integer.parseInt(jsonObj.getString(DISC_PROF_ID)));

                    var rowsUpdated = dpUpdateStmt.executeUpdate();

                    if(rowsUpdated > 0)
                    {
                        LOGGER.info("{} rows updated in credential_profile", rowsUpdated);

                        var discoveryProfile = getDiscoveryProfile(Integer.parseInt(jsonObj.getString(DISC_PROF_ID)));

                        msg.reply(discoveryProfile);
                    }
                    else
                    {
                        LOGGER.info("disc.profile.id = {} not found", jsonObj.getString(DISC_PROF_ID));

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
                    updateStmt.setInt(3, Integer.parseInt(jsonObj.getString(CRED_PROF_ID)));

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
                // TODO: add validation before deleting credential profile to check if it is bind with any device or not

                try(var conn = Jdbc.getConnection(); var stmt = conn.prepareStatement(cpDeleteQ))
                {
                    stmt.setInt(1, Integer.parseInt(jsonObj.getString(CRED_PROF_ID)));

                    var rowsDeleted = stmt.executeUpdate();

                    if(rowsDeleted > 0)
                    {
                        LOGGER.info("{} rows deleted from credential_profile", rowsDeleted);

                        msg.reply(SUCCESS);
                    }
                    else
                    {
                        LOGGER.info("cred.profile.id = {} not found", jsonObj.getString(CRED_PROF_ID));

                        msg.reply(FAILED);
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

                if(!contexts.isEmpty())
                {
                    msg.reply(contexts);
                }
                else
                {
                    msg.fail(500, new JsonObject().put(ERROR, "Error in running discovery").put(ERR_MESSAGE, "Context is empty!").put(ERR_STATUS_CODE, 500).toString());
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
                    LOGGER.info("Discovery status changed to 1 for disc.profile.id: {}",jsonObj.getInteger(DISC_PROF_ID));
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
                        LOGGER.info("Mapping added for [discId:credId]: [{}:{}]",jsonObj.getInteger(DISC_PROF_ID), jsonObj.getInteger(CRED_PROF_ID));
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
                            LOGGER.info("Mapping added for [discId:credId]: [{}:{}]",jsonObj.getInteger(DISC_PROF_ID), jsonObj.getInteger(CRED_PROF_ID));
                        }
                    }
                    // if mapping for credential profile & discovery profile exists
                    else
                    {
                        LOGGER.info("Mapping already exists for [discId:credId]: [{}:{}]",jsonObj.getInteger(DISC_PROF_ID), jsonObj.getInteger(CRED_PROF_ID));
                    }

                }
            } catch(SQLException e)
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
                    discoveryProfile.put(DISC_PROF_ID, discProfileRS.getInt(DISC_PROF_ID)).put(DISC_NAME, discProfileRS.getString(DISC_NAME)).put(OBJECT_IP, discProfileRS.getString(OBJECT_IP)).put(SNMP_PORT, discProfileRS.getInt(SNMP_PORT)).put(IS_PROVISIONED, discProfileRS.getInt(IS_PROVISIONED)).put(IS_DISCOVERED, discProfileRS.getInt(IS_DISCOVERED)).put(CREDENTIALS, discProfileRS.getString(CREDENTIALS));
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
                    credentialProfile.put(CRED_PROF_ID, credProfileRS.getInt(CRED_PROF_ID)).put(CRED_NAME, credProfileRS.getString(CRED_NAME)).put(PROTOCOL, credProfileRS.getString(PROTOCOL)).put(VERSION, credProfileRS.getString(VERSION)).put(SNMP_COMMUNITY, credProfileRS.getString(SNMP_COMMUNITY));
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

}
