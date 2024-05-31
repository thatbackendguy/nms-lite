package com.motadata.engine;

import com.motadata.Bootstrap;
import com.motadata.database.ConfigDB;
import com.motadata.utils.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import static com.motadata.constants.Constants.*;

public class Requester extends AbstractVerticle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Requester.class);

    private static final ZContext context = new ZContext();

    private static final ZMQ.Socket pusher = context.createSocket(SocketType.PUSH);

    private static final ZMQ.Socket puller = context.createSocket(SocketType.PULL);

    private static final ZMQ.Socket resultPusher = context.createSocket(SocketType.PUSH);

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {

        pusher.bind("tcp://*:7777");

        puller.bind("tcp://*:8888");

        resultPusher.bind("tcp://*:9999");

        var vertx = Bootstrap.getVertx();

        var eventBus = vertx.eventBus();

        eventBus.<String> localConsumer(DUMP_TO_FILE, msg ->
        {

            try
            {
                resultPusher.send(msg.body(), 0);

                LOGGER.trace("Data pushed to tcp://*:9999: {}", msg.body());

                Thread.sleep(100);

            }
            catch (Exception exception)
            {
                LOGGER.error(exception.getMessage(), exception);
            }

        });

        eventBus.<String> localConsumer(RUN_DISCOVERY_EVENT, msg ->
        {
            vertx.executeBlocking(handler ->
            {

                try
                {
                    pusher.send(msg.body(), 0);

                    LOGGER.trace("Discovery request sent: {}", msg.body());

                    var result = puller.recvStr(0);

                    LOGGER.trace("Discovery result received: {}", result);

                    // sending plugin engine result to ResponseParser
                    eventBus.send(PARSE_DISCOVERY_EVENT, Utils.decodeBase64ToJsonArray(result));

                }
                catch (Exception exception)
                {
                    LOGGER.error(exception.getMessage(), exception);
                }
            });

        });

        eventBus.<String> localConsumer(POLL_METRICS_EVENT, msg ->
        {
            vertx.executeBlocking(handler ->
            {
                try
                {
                    pusher.send(msg.body(), 0);

                    LOGGER.trace("Collect request sent: {}", msg.body());

                    var result = puller.recvStr(0);

                    LOGGER.trace("Collect result received: {}", result);

                    // sending metrics result to Requester to dump to file
                    eventBus.send(DUMP_TO_FILE, Utils.decodeBase64ToJsonArray(result).toString());

                }
                catch (Exception exception)
                {
                    LOGGER.error(ERROR_CONTAINER, exception.getMessage());
                }
            });
        });

        eventBus.<String> localConsumer(CHECK_AVAILABILITY, msg ->
        {
            vertx.executeBlocking(handler ->
            {
                try
                {
                    pusher.send(msg.body(), 0);

                    LOGGER.trace("Check availability request sent: {}", msg.body());

                    var reply = puller.recvStr(0);

                    LOGGER.trace("Availability result: {}", reply);

                    for (var result : Utils.decodeBase64ToJsonArray(reply))
                    {
                        var monitor = new JsonObject(result.toString());

                        ConfigDB.availableDevices.put(monitor.getString(OBJECT_IP), monitor.getJsonObject(RESULT)
                                .getString("is.available", "down"));

                    }

                }
                catch (Exception exception)
                {
                    LOGGER.error(ERROR_CONTAINER, exception.getMessage());
                }
            });
        });

        startPromise.complete();

        LOGGER.info("Process spawner started");
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception
    {

        stopPromise.complete();
    }

}
