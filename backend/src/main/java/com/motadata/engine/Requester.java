package com.motadata.engine;

import com.motadata.Bootstrap;
import com.motadata.database.ConfigDB;
import com.motadata.utils.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.motadata.constants.Constants.*;

public class Requester extends AbstractVerticle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Requester.class);

    private static final ZContext context = new ZContext();

    private static final ZMQ.Socket sender = context.createSocket(SocketType.PUSH);

    private static final ZMQ.Socket resultPusher = context.createSocket(SocketType.PUSH);

    @Override
    public void start(Promise<Void> startPromise)
    {

        try
        {
            sender.bind("tcp://*:7777");

            resultPusher.bind("tcp://*:9999");

            var vertx = Bootstrap.getVertx();

            var eventBus = vertx.eventBus();

            eventBus.<String> localConsumer(DUMP_TO_FILE, msg ->
            {

                try
                {

                    // sending work on TCP Port: 9999
                    resultPusher.send(msg.body(), ZMQ.DONTWAIT);

                    LOGGER.trace("Data pushed to tcp://*:9999: {}", msg.body());
                }
                catch (Exception exception)
                {
                    LOGGER.error(exception.getMessage(), exception);
                }

            });

            eventBus.<String> localConsumer(RUN_DISCOVERY_EVENT, msg ->
            {

                try
                {

                    // sending work on TCP Port: 7777
                    sender.send(msg.body(), ZMQ.DONTWAIT);

                    LOGGER.trace("Discovery request sent: {}", msg.body());

                }
                catch (Exception exception)
                {
                    LOGGER.error(exception.getMessage(), exception);
                }

            });

            eventBus.<String> localConsumer(POLL_METRICS_EVENT, msg ->
            {

                try
                {

                    // sending work on TCP Port: 7777
                    sender.send(msg.body(), ZMQ.DONTWAIT);

                    LOGGER.trace("Collect request sent: {}", msg.body());

                }
                catch (Exception exception)
                {
                    LOGGER.error(ERROR_CONTAINER, exception.getMessage());
                }
            });

            eventBus.<String> localConsumer(CHECK_AVAILABILITY, msg ->
            {

                try
                {
                    // sending work on TCP Port: 7777
                    sender.send(msg.body(), ZMQ.DONTWAIT);

                    LOGGER.trace("Check availability request sent: {}", msg.body());

                }
                catch (Exception exception)
                {
                    LOGGER.error(ERROR_CONTAINER, exception.getMessage());
                }

            });

            startPromise.complete();

            LOGGER.info("Requester started");
        }
        catch (Exception exception)
        {
            LOGGER.error("Exception occurred: ", exception);

            startPromise.fail(exception);
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise)
    {

        if (sender != null)
        {
            sender.close();

            LOGGER.debug("Socket at TCP port 7777 closed.");
        }

        if (resultPusher != null)
        {
            resultPusher.close();

            LOGGER.debug("Socket at TCP port 9999 closed.");
        }

        context.close();

        stopPromise.complete();

        LOGGER.info("Requester undeployed successfully");
    }

}
