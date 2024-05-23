package com.motadata.engine;

import com.motadata.Bootstrap;
import com.motadata.config.Config;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;

import static com.motadata.constants.Constants.*;
import static com.motadata.config.Config.*;

public class StoreRemoteMetrics extends AbstractVerticle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(StoreRemoteMetrics.class);

    private static final String METRICS_INSERT_QUERY = "INSERT INTO network_interface (`object.ip`,`interface.index`,`interface.name`,`interface.operational.status`,`interface.admin.status`,`interface.description`,`interface.sent.error.packet`,`interface.received.error.packet`,`interface.sent.octets`,`interface.received.octets`,`interface.speed`,`interface.alias`,`interface.physical.address`,`poll.time`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

    private final ZContext context = new ZContext();

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {

        var socket = context.createSocket(SocketType.PULL);

        socket.bind("tcp://*:"+Config.ZMQ_PORT);

        var vertx = Bootstrap.getVertx();

        vertx.setPeriodic(Config.POLLING_INTERVAL, timerId ->
        {
            LOGGER.trace("Initiating polling from agent");

            vertx.executeBlocking(handler ->
            {
                while (true)
                {
                    var message = socket.recvStr(ZMQ.DONTWAIT);

                    if (message != null)
                    {
                        var decodedString = new String(Base64.getDecoder().decode(message));

                        var result = new JsonArray(decodedString);

                        for (var data : result)
                        {
                            var monitor = new JsonObject(data.toString());

                            var interfaceData = monitor.getJsonObject(RESULT).getJsonArray(INTERFACE);

                            var objectIp = monitor.getString(OBJECT_IP);

                            var pollTime = monitor.getString(POLL_TIME);

                            try (var conn = DriverManager.getConnection(DB_URI, DB_USERNAME, DB_PASS); var insertPolledMetrics = conn.prepareStatement(METRICS_INSERT_QUERY))
                            {
                                for (var singleInterface : interfaceData)
                                {
                                    var interfaceMetrics = new JsonObject(singleInterface.toString());

                                    insertPolledMetrics.setString(1, objectIp);

                                    insertPolledMetrics.setInt(2, interfaceMetrics.getInteger(INTERFACE_INDEX, -1));
                                    insertPolledMetrics.setString(3, interfaceMetrics.getString(INTERFACE_NAME, ""));
                                    insertPolledMetrics.setInt(4, interfaceMetrics.getInteger(INTERFACE_OPERATIONAL_STATUS, 1));
                                    insertPolledMetrics.setInt(5, interfaceMetrics.getInteger(INTERFACE_ADMIN_STATUS, -1));
                                    insertPolledMetrics.setString(6, interfaceMetrics.getString(INTERFACE_DESCRIPTION, ""));
                                    insertPolledMetrics.setInt(7, interfaceMetrics.getInteger(INTERFACE_SENT_ERROR_PACKET, -1));
                                    insertPolledMetrics.setInt(8, interfaceMetrics.getInteger(INTERFACE_RECEIVED_ERROR_PACKET, -1));
                                    insertPolledMetrics.setInt(9, interfaceMetrics.getInteger(INTERFACE_SENT_OCTETS, 0));
                                    insertPolledMetrics.setInt(10, interfaceMetrics.getInteger(INTERFACE_RECEIVED_OCTETS, -1));
                                    insertPolledMetrics.setInt(11, interfaceMetrics.getInteger(INTERFACE_SPEED, -1));
                                    insertPolledMetrics.setString(12, interfaceMetrics.getString(INTERFACE_ALIAS, ""));
                                    insertPolledMetrics.setString(13, interfaceMetrics.getString(INTERFACE_PHYSICAL_ADDRESS, ""));
                                    insertPolledMetrics.setString(14, pollTime);

                                    insertPolledMetrics.addBatch();
                                }

                                var rowsInserted = insertPolledMetrics.executeBatch();

                                LOGGER.trace("{} rows data inserted for {}", rowsInserted.length, objectIp);

                            }
                            catch (SQLException sqlException)
                            {
                                LOGGER.error(ERROR_CONTAINER, sqlException.getMessage());
                            }
                        }
                    }
                    else
                    {
                        LOGGER.trace("Messaging queue empty");

                        break;
                    }
                }

            });
        });

        startPromise.complete();
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception
    {

        stopPromise.complete();
    }

}