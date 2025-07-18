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

import static com.motadata.constants.Constants.*;

public class ResponseReceiver extends AbstractVerticle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseReceiver.class);

    private static final ZContext context = new ZContext();

    private static final ZMQ.Socket receiver = context.createSocket(SocketType.PULL);

    @Override
    public void start(Promise<Void> startPromise)
    {

        try
        {
            var vertx = Bootstrap.getVertx();

            var eventBus = vertx.eventBus();

            receiver.bind("tcp://*:8888");

            eventBus.<String> localConsumer(RESPONSE_MANAGER_EVENT, message ->
            {

                if (message != null && !message.body().isEmpty())
                {
                    var contextResult = Utils.decodeBase64ToJsonObject(message.body());

                    if (!contextResult.isEmpty())
                    {
                        if (contextResult.containsKey(REQUEST_TYPE))
                        {
                            switch (contextResult.getString(REQUEST_TYPE))
                            {
                                case COLLECT ->

                                {
                                    LOGGER.trace("Collect result received: {}", contextResult);

                                    eventBus.send(DUMP_TO_FILE, contextResult.toString());
                                }

                                case DISCOVERY ->

                                {
                                    LOGGER.trace("Discovery result received: {}", contextResult);

                                    ResponseParser.discoveryResult(contextResult);
                                }

                                case AVAILABILITY ->

                                {
                                    LOGGER.trace("Availability result received: {}", contextResult);

                                    ConfigDB.availableDevices.put(contextResult.getString(OBJECT_IP), contextResult.getJsonObject(RESULT)
                                            .getString("is.available", "down"));
                                }

                            }
                        }
                    }
                }

            });

            new Thread(() ->
            {
                try
                {
                    while (!Thread.currentThread().isInterrupted())
                    {
                        var message = receiver.recvStr(0);

                        eventBus.send(RESPONSE_MANAGER_EVENT, message);

                    }
                }
                catch (ZMQException zmqException)
                {
                    if (context.isClosed())
                    {
                        LOGGER.info("Context was closed");
                    }
                    else
                    {
                        LOGGER.error("ZMQException occurred: ", zmqException);
                    }
                }
                catch (Exception exception)
                {
                    LOGGER.error("Exception in receiver thread: ", exception);
                }
            }).start();

            startPromise.complete();

            LOGGER.info("Response Receiver started successfully");
        }
        catch (Exception exception)
        {
            LOGGER.error("Failed to start Response Receiver: ", exception);

            startPromise.fail(exception);
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise)
    {

        if (receiver != null)
        {
            receiver.close();

            LOGGER.debug("Socket at TCP port 8888 closed.");
        }

        context.close();

        stopPromise.complete();

        LOGGER.info("Response Receiver undeployed successfully");
    }

}
