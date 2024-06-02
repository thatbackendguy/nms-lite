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
import org.zeromq.ZMQException;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.motadata.constants.Constants.*;

public class ResponseReceiver extends AbstractVerticle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseReceiver.class);

    private static final ZContext context = new ZContext();

    private static final ZMQ.Socket receiver = context.createSocket(SocketType.PULL);

    private final AtomicBoolean running = new AtomicBoolean(true);

    @Override
    public void start(Promise<Void> startPromise)
    {

        try
        {
            var vertx = Bootstrap.getVertx();

            var eventBus = vertx.eventBus();

            receiver.bind("tcp://*:8888");

            var workerThread = new Thread(() ->
            {
                try
                {
                    while (running.get() && !context.isClosed())
                    {
                        var message = receiver.recvStr(0);

                        if (message != null && !message.isEmpty())
                        {
                            var result = Utils.decodeBase64ToJsonArray(message);

                            if (!result.isEmpty())
                            {
                                for (var object : result)
                                {
                                    var contextResult = new JsonObject(object.toString());

                                    if (contextResult.containsKey(REQUEST_TYPE))
                                    {
                                        switch (contextResult.getString(REQUEST_TYPE))
                                        {
                                            case COLLECT:

                                                LOGGER.trace("Collect result received: {}", result);

                                                eventBus.send(DUMP_TO_FILE, result.toString());

                                                break;

                                            case DISCOVERY:

                                                LOGGER.trace("Discovery result received: {}", result);

                                                eventBus.send(PARSE_DISCOVERY_EVENT, result);

                                                break;

                                            case AVAILABILITY:

                                                LOGGER.trace("Availability result received: {}", result);

                                                ConfigDB.availableDevices.put(contextResult.getString(OBJECT_IP), contextResult.getJsonObject(RESULT)
                                                        .getString("is.available", "down"));

                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                catch (ZMQException zmqException)
                {
                    if (context.isClosed())
                    {
                        LOGGER.info("Context was closed, exiting worker thread.");
                    }
                    else
                    {
                        LOGGER.error("ZMQException occurred: ", zmqException);
                    }
                }
                catch (Exception exception)
                {
                    LOGGER.error("Exception in worker thread: ", exception);
                }
                finally
                {
                    receiver.close();
                }
            });

            workerThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                running.set(false);

                receiver.close();

                context.close();

                try
                {
                    workerThread.join();
                }
                catch (InterruptedException e)
                {
                    LOGGER.error("Error while waiting for worker thread to terminate: ", e);
                }
            }));

            startPromise.complete();

            LOGGER.info("Response Receiver started successfully");
        }
        catch (Exception exception)
        {
            LOGGER.error("Failed to start ResponseReceiver: ", exception);

            startPromise.fail(exception);
        }
    }

}
