package com.motadata;

import com.motadata.utils.DatabaseConnection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static com.motadata.Bootstrap.*;
import static com.motadata.Constants.*;

public class DbWorker extends AbstractVerticle
{
    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        // FETCHING DATA FROM DB
        vertx.eventBus().localConsumer("get.data", msg -> {

            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals("discovery_profile"))
            {
                String dpSelectQ = "SELECT cp.`cred.name`,dp.`disc.name`, dp.`disc.profile.id`,dp.`cred.profile.id`, dp.`object.ip`, dp.`snmp.port`, cp.protocol, cp.version, cp.`snmp.community` FROM `discovery_profile` dp INNER JOIN `credential_profile` cp ON dp.`cred.profile.id` = cp.`cred.profile.id` WHERE dp.`object.ip` = ?;";

                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(dpSelectQ))
                {
                    stmt.setString(1, jsonObj.getString(OBJECT_IP));

                    ResultSet rs = stmt.executeQuery();

                    var result = new JsonObject();

                    while(rs.next())
                    {
                        result
                                .put(DISC_NAME, rs.getString(DISC_NAME))
                                .put(CRED_NAME, rs.getString(CRED_NAME))
                                .put(DISC_PROF_ID, rs.getString(DISC_PROF_ID))
                                .put(CRED_PROF_ID, rs.getString(CRED_PROF_ID))
                                .put(SNMP_PORT, rs.getString(SNMP_PORT))
                                .put(PROTOCOL, rs.getString(PROTOCOL))
                                .put(SNMP_COMMUNITY, rs.getString(SNMP_COMMUNITY))
                                .put(VERSION, rs.getString(VERSION));
                    }

                    jsonObj.remove(TABLE_NAME);

                    jsonObj.put(RESULT,result);

                    msg.reply(jsonObj.put(STATUS,SUCCESS));

                } catch(SQLException e)
                {
                    msg.reply(jsonObj.put(STATUS,FAILED).put(ERROR,e.getMessage()));
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals("credential_profile"))
            {
                String cpSelectQ = "SELECT * FROM `credential_profile`";

                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.createStatement())
                {

                    ResultSet rs = stmt.executeQuery(cpSelectQ);

                    var credProfiles  = new JsonArray();

                    while(rs.next())
                    {
                        credProfiles.add(new JsonObject()
                                .put(CRED_NAME, rs.getString(CRED_NAME))
                                .put(PROTOCOL, rs.getString(PROTOCOL))
                                .put(CRED_PROF_ID, rs.getString(CRED_PROF_ID))
                                .put(VERSION, rs.getString(VERSION))
                                .put(SNMP_COMMUNITY, rs.getString(SNMP_COMMUNITY)));
                    }

                    jsonObj.remove(TABLE_NAME);

                    jsonObj.put("credential_profiles", credProfiles);

                } catch(SQLException e)
                {
                    msg.reply(jsonObj.put(STATUS,FAILED).put(ERROR,e.getMessage()));
                }

                msg.reply(jsonObj.put(STATUS,SUCCESS));
            }
            else {
                msg.reply(jsonObj.put(STATUS,FAILED).put(ERROR,"Invalid table name!"));
            }
        });

        // INSERTING DATA TO DB
        vertx.eventBus().localConsumer("insert.data", msg->{
            var jsonObj = new JsonObject(msg.body().toString());

            if(jsonObj.getString(TABLE_NAME).equals("discovery_profile"))
            {
                jsonObj.remove(TABLE_NAME);

                String dpInsertQ = "INSERT INTO `nmsDB`.`discovery_profile` (`disc.name`,`disc.profile.id`,`object.ip`,`cred.profile.id`,`snmp.port`) VALUES (?,?,?,?,?);";

                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(dpInsertQ))
                {
                    stmt.setString(1, jsonObj.getString(DISC_NAME));
                    stmt.setString(2, UUID.randomUUID().toString());
                    stmt.setString(3, jsonObj.getString(OBJECT_IP));
                    stmt.setString(4, jsonObj.getString(CRED_PROF_ID));
                    stmt.setString(5, jsonObj.getString(SNMP_PORT));

                    var rowsInserted = stmt.executeUpdate();

                    LOGGER.info("{} rows inserted in {}", rowsInserted, jsonObj.getString(TABLE_NAME));

                    jsonObj.clear();

                    msg.reply(jsonObj.put(STATUS,SUCCESS).put(MESSAGE,"Discovery profile added successfully!"));
                }
                catch(SQLException e)
                {
                    msg.reply(jsonObj.put(STATUS,FAILED).put(ERROR,e.getMessage()));
                }
            }
            else if(jsonObj.getString(TABLE_NAME).equals("credential_profile"))
            {
                jsonObj.remove(TABLE_NAME);

                String cpInsertQ = "INSERT INTO `nmsDB`.`credential_profile` (`cred.profile.id`,`cred.name`, `protocol`, `version`, `snmp.community`) VALUES (?,?,?,?,?);";

                try(var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(cpInsertQ))
                {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, jsonObj.getString(CRED_NAME));
                    stmt.setString(3, jsonObj.getString(PROTOCOL));
                    stmt.setString(4, jsonObj.getString(VERSION));
                    stmt.setString(5, jsonObj.getString(SNMP_COMMUNITY));

                    var rowsInserted = stmt.executeUpdate();

                    LOGGER.info("{} rows inserted in {}", rowsInserted, jsonObj.getString(TABLE_NAME));

                    jsonObj.clear();

                    msg.reply(jsonObj.put(STATUS,SUCCESS).put(MESSAGE,"Credential profile added successfully!"));
                }
                catch(SQLException e)
                {
                    msg.reply(jsonObj.put(STATUS,FAILED).put(ERROR,e.getMessage()));
                }

            }
            else
            {
                msg.reply(jsonObj.put(STATUS,FAILED).put(ERROR,"Invalid table name!"));
            }

        });

        // DELETING DATA FROM DB
        vertx.eventBus().localConsumer("delete.data", msg -> {
            var jsonObj = new JsonObject(msg.body().toString());

            if (jsonObj.getString(TABLE_NAME).equals("discovery_profile"))
            {
                jsonObj.remove(TABLE_NAME);

                String dpDeleteQ = "DELETE FROM `discovery_profile` WHERE `object.ip` = ?;";

                try (var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(dpDeleteQ)) {
                    stmt.setString(1, jsonObj.getString(OBJECT_IP));

                    var rowsDeleted = stmt.executeUpdate();

                    LOGGER.info("{} rows deleted from {}", rowsDeleted, "discovery_profile");

                    jsonObj.clear();

                    msg.reply(jsonObj.put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile deleted successfully!"));

                }
                catch (SQLException e)
                {
                    msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, e.getMessage()));
                }
            }
            else if (jsonObj.getString(TABLE_NAME).equals("credential_profile"))
            {
                jsonObj.remove(TABLE_NAME);

                String cpDeleteQ = "DELETE FROM `credential_profile` WHERE `cred.name` = ?;";

                try (var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(cpDeleteQ))
                {
                    stmt.setString(1, jsonObj.getString(CRED_NAME));

                    var rowsDeleted = stmt.executeUpdate();

                    LOGGER.info("{} rows deleted from {}", rowsDeleted, "credential_profile");

                    jsonObj.clear();

                    msg.reply(jsonObj.put(STATUS, SUCCESS).put(MESSAGE, "Credential profile deleted successfully!"));

                }
                catch (SQLException e)
                {
                    msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, e.getMessage()));
                }
            }
            else
            {
                msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, "Invalid table name!"));
            }
        });

        // UPDATING DATA IN DB
        vertx.eventBus().localConsumer("update.data", msg -> {
            var jsonObj = new JsonObject(msg.body().toString());

            if (jsonObj.getString(TABLE_NAME).equals("discovery_profile"))
            {
                jsonObj.remove(TABLE_NAME);

                String dpUpdateQ = "UPDATE `discovery_profile` SET `disc.name` = ?, `snmp.port` = ? WHERE `object.ip` = ?;";

                try (var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(dpUpdateQ)) {
                    stmt.setString(1, jsonObj.getString(DISC_NAME));
                    stmt.setString(2, jsonObj.getString(SNMP_PORT));
                    stmt.setString(3, jsonObj.getString(OBJECT_IP));

                    var rowsUpdated = stmt.executeUpdate();

                    LOGGER.info("{} rows updated in {}", rowsUpdated, "discovery_profile");

                    jsonObj.clear();

                    msg.reply(jsonObj.put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile updated successfully!"));

                }
                catch (SQLException e)
                {
                    msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, e.getMessage()));
                }
            }
            else if (jsonObj.getString(TABLE_NAME).equals("credential_profile"))
            {
                jsonObj.remove(TABLE_NAME);

                String cpUpdateQ = "UPDATE `credential_profile` SET `protocol` = ?, `version` = ?, `snmp.community` = ? WHERE `cred.name` = ?;";

                try (var conn = DatabaseConnection.getConnection(); var stmt = conn.prepareStatement(cpUpdateQ))
                {
                    stmt.setString(1, jsonObj.getString(PROTOCOL));
                    stmt.setString(2, jsonObj.getString(VERSION));
                    stmt.setString(3, jsonObj.getString(SNMP_COMMUNITY));
                    stmt.setString(4, jsonObj.getString(CRED_NAME));

                    var rowsUpdated = stmt.executeUpdate();

                    LOGGER.info("{} rows updated in {}", rowsUpdated, "credential_profile");

                    jsonObj.clear();

                    msg.reply(jsonObj.put(STATUS, SUCCESS).put(MESSAGE, "Credential profile updated successfully!"));

                }
                catch (SQLException e)
                {
                    msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, e.getMessage()));
                }
            }
            else
            {
                msg.reply(jsonObj.put(STATUS, FAILED).put(ERROR, "Invalid table name!"));
            }
        });

        startPromise.complete();
    }
}
