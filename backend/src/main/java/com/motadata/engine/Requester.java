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
            var workerExecutors = Executors.newFixedThreadPool(10);

            sender.bind("tcp://*:7777");

            resultPusher.bind("tcp://*:9999");

            var vertx = Bootstrap.getVertx();

            var eventBus = vertx.eventBus();

            eventBus.<String> localConsumer(DUMP_TO_FILE, msg ->
            {

                try
                {
                    workerExecutors.submit(() ->
                    {
                        // sending work on TCP Port: 9999
                        resultPusher.send(msg.body(), 0);

                        LOGGER.trace("Data pushed to tcp://*:9999: {}", msg.body());

                        try
                        {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException interruptedException)
                        {
                            LOGGER.error(ERROR_CONTAINER, interruptedException.toString());
                        }
                    });

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
                    workerExecutors.submit(() ->
                    {
                        // sending work on TCP Port: 7777
                        sender.send(msg.body(), 0);

                        LOGGER.trace("Discovery request sent: {}", msg.body());
                    });
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
                    workerExecutors.submit(() ->
                    {
                        // sending work on TCP Port: 7777
                        sender.send(msg.body(), 0);

                        LOGGER.trace("Collect request sent: {}", msg.body());

                    });

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
                    workerExecutors.submit(() ->
                    {
                        // sending work on TCP Port: 7777
                        sender.send(msg.body(), 0);

                        LOGGER.trace("Check availability request sent: {}", msg.body());
                    });
                }
                catch (Exception exception)
                {
                    LOGGER.error(ERROR_CONTAINER, exception.getMessage());
                }

            });

            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                sender.close();

                resultPusher.close();

                context.close();

                if (!workerExecutors.isShutdown())
                {
                    workerExecutors.shutdown();
                }
            }));

            startPromise.complete();

            LOGGER.info("Requester started");
        }
        catch (Exception exception)
        {
            LOGGER.error(ERROR_CONTAINER, exception.getMessage());
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception
    {

        stopPromise.complete();
    }

}
