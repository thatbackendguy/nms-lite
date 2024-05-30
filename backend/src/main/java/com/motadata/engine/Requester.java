package com.motadata.engine;

import com.motadata.Bootstrap;
import com.motadata.database.ConfigDB;
import com.motadata.utils.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
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

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {

        var vertx = Bootstrap.getVertx();

        var eventBus = vertx.eventBus();

        eventBus.<String> localConsumer(DUMP_TO_FILE, msg ->
        {
            try (var requester = context.createSocket(SocketType.PUSH))
            {
                if (requester.connect("tcp://localhost:9999"))
                {
                    requester.send(msg.body(), 0);

                    Thread.sleep(100);
                }


            }
            catch (Exception exception)
            {
                System.out.println(exception);
            }

        });

        eventBus.<String> localConsumer(RUN_DISCOVERY_EVENT, msg ->
        {
            vertx.executeBlocking(handler ->
            {

                try (var requester = context.createSocket(SocketType.REQ))
                {
                    requester.connect("tcp://localhost:7777");

                    requester.setReceiveTimeOut(30000);

                    requester.send(msg.body(), 0);

                    LOGGER.trace("Discovery request sent: {}", msg.body());

                    var reply = requester.recvStr(0);

                    // sending plugin engine result to ResponseParser
                    eventBus.send(PARSE_DISCOVERY_EVENT, Utils.decodeBase64ToJsonArray(reply));

                }
                catch (Exception exception)
                {
                    LOGGER.error(ERROR_CONTAINER, exception.getMessage());
                }
            });

        });

        eventBus.<String> localConsumer(POLL_METRICS_EVENT, msg ->
        {
            vertx.executeBlocking(handler ->
            {
                try (var requester = context.createSocket(SocketType.REQ))
                {
                    requester.connect("tcp://localhost:7777");

                    requester.setReceiveTimeOut(30000);

                    requester.send(msg.body(), 0);

                    LOGGER.trace("Collect request sent: {}", msg.body());

                    var reply = requester.recvStr(0);

                    // sending metrics result to Requester to dump to file
                    eventBus.send(DUMP_TO_FILE, Utils.decodeBase64ToJsonArray(reply).toString());

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
                try (var requester = context.createSocket(SocketType.REQ))
                {
                    requester.connect("tcp://localhost:7777");

                    requester.setReceiveTimeOut(30000);

                    requester.send(msg.body(), 0);

                    LOGGER.trace("Check availability request sent: {}", msg.body());

                    var reply = requester.recvStr(0);

                    for (var result : Utils.decodeBase64ToJsonArray(reply))
                    {
                        var monitor = new JsonObject(result.toString());

                        var discoveryProfile = ConfigDB.discoveryProfiles.get(monitor.getLong(DISCOVERY_PROFILE_ID));

                        discoveryProfile.put("is.available", monitor.getJsonObject(RESULT).getString("is.available"));
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
