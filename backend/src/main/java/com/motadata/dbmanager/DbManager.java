package com.motadata.dbmanager;

import com.motadata.utils.DatabaseConnection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

import static com.motadata.Bootstrap.*;
import static com.motadata.utils.constants.Constants.*;

public class DbManager extends AbstractVerticle
{
    private final String cpInsertQ = "INSERT INTO `nmsDB`.`credential_profile` (`cred.name`, `protocol`, `version`, `snmp.community`) VALUES (?,?,?,?);";

    private final String cpSelectQ = "SELECT * FROM `credential_profile` where `cred.profile.id`=?;";

    private final String cpSelectAllQ = "SELECT * FROM `credential_profile`";

    private final String cpUpdateQ = "UPDATE `credential_profile` SET `version` = ?, `snmp.community` = ? WHERE `cred.profile.id` = ?;";

    private final String dpInsertQ = "INSERT INTO `nmsDB`.`discovery_profile` (`disc.name`,`object.ip`,`snmp.port`,`credentials`) VALUES (?,?,?,?);";

    private final String dpSelectQ = "SELECT * FROM `discovery_profile` WHERE `disc.profile.id` = ?";

    private final String dpSelectAllQ = "SELECT * FROM `discovery_profile`";

    private final String dpUpdateQ = "UPDATE `discovery_profile` SET `disc.name` = ?, `snmp.port` = ? WHERE `disc.profile.id` = ?;";

    private final String dpUpdateIsDiscStatusQ = "UPDATE `discovery_profile` SET `is.discovered` = ? WHERE `disc.profile.id` = ?;";

    private final String cpDeleteQ = "DELETE FROM `credential_profile` WHERE `cred.profile.id` = ?;";

    private final String dpDeleteQ = "DELETE FROM `discovery_profile` WHERE `disc.profile.id` = ?;";

    private final String pmInsertQ = "INSERT INTO `nmsDB`.`profile_mapping` (`disc.profile.id`,`cred.profile.id`) VALUES (?,?);";

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        var eventBus = vertx.eventBus();

        // TODO: instead of msg.reply() use promise
        // TODO: add validations before inserting/updating
        // TODO: update the logic based on 3 tables


        // INSERTING DATA TO DB
        eventBus.localConsumer(INSERT_EVENT, msg -> {
            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals("discovery_profile"))
            {

                var credentialsDB = new JsonArray();

                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.createStatement())
                {
                    var rs = stmt.executeQuery(cpSelectAllQ);

                    while(rs.next())
                    {
                        credentialsDB.add(rs.getString(CRED_PROF_ID));
                    }

                } catch(SQLException e)
                {
                    LOGGER.error(e.getMessage());
                }

                var credentialsReq = new JsonArray(jsonObj.getString(CREDENTIALS));

                var credentialsReqSet = new HashSet<>(credentialsReq.stream().toList());

                var credentialsDBJsonSet = new HashSet<>(credentialsDB.stream().toList());

                if(!credentialsDBJsonSet.containsAll(credentialsReqSet))
                {
                    LOGGER.error("One or more credential IDs are not available in the database.");
                    msg.fail(500, "One or more credential IDs are not available in the database.");
                    return;
                }

                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(dpInsertQ))
                {
                    stmt.setString(1, jsonObj.getString(DISC_NAME));
                    stmt.setString(2, jsonObj.getString(OBJECT_IP));
                    stmt.setString(3, jsonObj.getString(SNMP_PORT));
                    stmt.setString(4, jsonObj.getJsonArray(CREDENTIALS).toString());

                    var rowsInserted = stmt.executeUpdate();

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
            else if(jsonObj.getString(TABLE_NAME).equals("credential_profile"))
            {
                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(cpInsertQ))
                {

                    stmt.setString(1, jsonObj.getString(CRED_NAME));
                    stmt.setString(2, jsonObj.getString(PROTOCOL));
                    stmt.setString(3, jsonObj.getString(VERSION));
                    stmt.setString(4, jsonObj.getString(SNMP_COMMUNITY));

                    var rowsInserted = stmt.executeUpdate();

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

            if(jsonObj.getString(TABLE_NAME).equals("credential_profile"))
            {
                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.createStatement())
                {
                    var rs = stmt.executeQuery(cpSelectAllQ);

                    var credentialProfiles = new JsonArray();

                    while(rs.next())
                    {
                        credentialProfiles.add(new JsonObject().put(CRED_NAME, rs.getString(CRED_NAME)).put(PROTOCOL, rs.getString(PROTOCOL)).put(CRED_PROF_ID, rs.getString(CRED_PROF_ID)).put(VERSION, rs.getString(VERSION)).put(SNMP_COMMUNITY, rs.getString(SNMP_COMMUNITY)));
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
            else if(jsonObj.getString(TABLE_NAME).equals("discovery_profile"))
            {
                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.createStatement())
                {
                    var rs = stmt.executeQuery(dpSelectAllQ);

                    var discoveryProfiles = new JsonArray();

                    while(rs.next())
                    {
                        discoveryProfiles.add(new JsonObject().put(DISC_PROF_ID, rs.getString(DISC_PROF_ID)).put(DISC_NAME, rs.getString(DISC_NAME)).put(OBJECT_IP, rs.getString(OBJECT_IP)).put(SNMP_PORT, rs.getString(SNMP_PORT)).put(IS_PROVISIONED, rs.getString(IS_PROVISIONED)).put(IS_DISCOVERED, rs.getString(IS_DISCOVERED)).put(CREDENTIALS, rs.getString(CREDENTIALS)));
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

            if(jsonObj.getString(TABLE_NAME).equals("discovery_profile"))
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
            else if(jsonObj.getString(TABLE_NAME).equals("credential_profile"))
            {
                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(cpSelectQ))
                {
                    stmt.setInt(1, Integer.parseInt(jsonObj.getString(CRED_PROF_ID)));

                    ResultSet rs = stmt.executeQuery();

                    var credentialProfile = new JsonObject();

                    while(rs.next())
                    {
                        credentialProfile.put(CRED_PROF_ID, rs.getString(CRED_PROF_ID)).put(CRED_NAME, rs.getString(CRED_NAME)).put(PROTOCOL, rs.getString(PROTOCOL)).put(VERSION, rs.getString(VERSION)).put(SNMP_COMMUNITY, rs.getString(SNMP_COMMUNITY));
                    }

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

            if(jsonObj.getString(TABLE_NAME).equals("discovery_profile"))
            {
                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(dpUpdateQ))
                {
                    stmt.setString(1, jsonObj.getString(DISC_NAME));

                    stmt.setString(2, jsonObj.getString(SNMP_PORT));

                    stmt.setInt(3, Integer.parseInt(jsonObj.getString(DISC_PROF_ID)));

                    var rowsUpdated = stmt.executeUpdate();

                    if(rowsUpdated > 0)
                    {
                        LOGGER.info("{} rows updated in credential_profile", rowsUpdated);

                        var discoveryProfile = getDiscoveryProfile(Integer.parseInt(jsonObj.getString(DISC_PROF_ID)));

                        // TODO: on success update the is.discovered to 0 & is.provisioned 0

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
            else if(jsonObj.getString(TABLE_NAME).equals("credential_profile"))
            {
                try(var conn = DatabaseConnection.getConnection(); var updateStmt = conn.prepareStatement(cpUpdateQ); var selectStmt = conn.prepareStatement(cpSelectQ);)
                {
                    updateStmt.setString(1, jsonObj.getString(VERSION));
                    updateStmt.setString(2, jsonObj.getString(SNMP_COMMUNITY));
                    updateStmt.setInt(3, Integer.parseInt(jsonObj.getString(CRED_PROF_ID)));

                    var rowsUpdated = updateStmt.executeUpdate();

                    if(rowsUpdated > 0)
                    {
                        LOGGER.info("{} rows updated in credential_profile", rowsUpdated);

                        selectStmt.setInt(1, Integer.parseInt(jsonObj.getString(CRED_PROF_ID)));

                        var rs = selectStmt.executeQuery();

                        var credentialProfile = new JsonObject();

                        while(rs.next())
                        {
                            credentialProfile.put(CRED_PROF_ID, rs.getString(CRED_PROF_ID)).put(CRED_NAME, rs.getString(CRED_NAME)).put(PROTOCOL, rs.getString(PROTOCOL)).put(VERSION, rs.getString(VERSION)).put(SNMP_COMMUNITY, rs.getString(SNMP_COMMUNITY));
                        }

                        //  TODO: on updating credential profile: change is.discovered to 0
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

            if(jsonObj.getString(TABLE_NAME).equals("discovery_profile"))
            {
                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(dpDeleteQ))
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
            else if(jsonObj.getString(TABLE_NAME).equals("credential_profile"))
            {
                // TODO: add validation before deleting credential profile

                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(cpDeleteQ))
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

            try(var conn = DatabaseConnection.getConnection(); var discSelectStmt = conn.prepareStatement(dpSelectQ); var cpSelectStmt = conn.prepareStatement(cpSelectQ))
            {
                for(var device : reqArray)
                {
                    discSelectStmt.setInt(1, Integer.parseInt(device.toString()));

                    var rs = discSelectStmt.executeQuery();

                    var monitor = new JsonObject().put(REQUEST_TYPE, DISCOVERY).put(PLUGIN_NAME, NETWORK);

                    var credentialStr = "";

                    if(!rs.isBeforeFirst())
                    {
                        msg.fail(500, "One of the discovery profile does not exists");

                        return;
                    }
                    else
                    {
                        while(rs.next())
                        {
                            monitor.put(OBJECT_IP, rs.getString(OBJECT_IP)).put(SNMP_PORT, rs.getInt(SNMP_PORT)).put(IS_PROVISIONED, rs.getInt(IS_PROVISIONED)).put(IS_DISCOVERED, rs.getInt(IS_DISCOVERED)).put(DISC_PROF_ID, rs.getInt(DISC_PROF_ID));

                            credentialStr = rs.getString(CREDENTIALS);
                        }

                        if(monitor.isEmpty())
                        {
                            throw new SQLException("No discovery profile found!");
                        }
                        else
                        {
                            if(monitor.getString(IS_DISCOVERED).equals(TRUE) || monitor.getString(IS_PROVISIONED).equals(TRUE))
                            {
                                msg.fail(500, "Device is already discovered");

                                return;
                            }


                            var credentialsArray = new JsonArray();

                            for(var credentialId : new JsonArray(credentialStr))
                            {

                                cpSelectStmt.setInt(1, Integer.parseInt(credentialId.toString()));

                                var credResultSet = cpSelectStmt.executeQuery();


                                while(credResultSet.next())
                                {
                                    var credentialObject = new JsonObject().put(CRED_PROF_ID, credResultSet.getInt(CRED_PROF_ID)).put(SNMP_COMMUNITY, credResultSet.getString(SNMP_COMMUNITY));

                                    credentialsArray.add(credentialObject);
                                }
                            }

                            monitor.put(CREDENTIALS, credentialsArray);
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
                    msg.fail(500, "Failed");
                }
            } catch(SQLException | NullPointerException e)
            {
                msg.fail(500, e.getMessage());
            }

        });

        // POST DISCOVERY SUCCESS
        eventBus.localConsumer(POST_DISC_SUCCESS, msg -> {
            var jsonObj = new JsonObject(msg.body().toString());

            try(var conn = DatabaseConnection.getConnection(); var pmStmt = conn.prepareStatement(pmInsertQ); var dpUpdateStmt = conn.prepareStatement(dpUpdateIsDiscStatusQ))
            {
                dpUpdateStmt.setInt(1, 1);

                dpUpdateStmt.setInt(2, jsonObj.getInteger(DISC_PROF_ID));

                var rowsUpdated = dpUpdateStmt.executeUpdate();

                if(rowsUpdated > 0)
                {
                    LOGGER.info("{} rows updated for discovery profile: {}", rowsUpdated, jsonObj.getInteger(DISC_PROF_ID));
                }

                pmStmt.setInt(1, jsonObj.getInteger(DISC_PROF_ID));

                pmStmt.setInt(2, jsonObj.getInteger(CRED_PROF_ID));

                var rowsInserted = pmStmt.executeUpdate();

                if(rowsInserted > 0)
                {
                    LOGGER.info("{} rows inserted in profile_mapping", rowsInserted);
                }

            } catch(SQLException e)
            {
                LOGGER.info("Error: {}", e.getMessage());
            }
        });

        startPromise.complete();
    }


    private JsonObject getDiscoveryProfile(int discoveryProfileId) throws SQLException
    {
        var discoveryProfile = new JsonObject();

        String dpSelectQ = "SELECT * FROM `discovery_profile` WHERE `disc.profile.id` = ?";

        try(var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(dpSelectQ))
        {
            stmt.setInt(1, discoveryProfileId);

            var rs = stmt.executeQuery();

            while(rs.next())
            {
                discoveryProfile.put(DISC_PROF_ID, rs.getString(DISC_PROF_ID)).put(DISC_NAME, rs.getString(DISC_NAME)).put(OBJECT_IP, rs.getString(OBJECT_IP)).put(SNMP_PORT, rs.getString(SNMP_PORT)).put(IS_PROVISIONED, rs.getString(IS_PROVISIONED)).put(IS_DISCOVERED, rs.getString(IS_DISCOVERED)).put(CREDENTIALS, rs.getString(CREDENTIALS));
            }

        }

        return discoveryProfile;
    }
}
