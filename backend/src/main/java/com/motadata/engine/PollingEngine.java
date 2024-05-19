package com.motadata.engine;

import com.motadata.Config;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static com.motadata.contants.Constants.*;

public class PollingEngine extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingEngine.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        var eventBus = vertx.eventBus();

        vertx.setPeriodic(Config.POLLING_INTERVAL, timerId -> {
            try
            {
                LOGGER.trace("Polling started, requesting for provision devices...");

                eventBus.request(GET_PROVISIONED_DEVICES_EVENT, EMPTY_STRING, asyncResult -> {
                    try
                    {
                        if(asyncResult.succeeded())
                        {
                            if(!asyncResult.result().body().toString().isEmpty())
                            {
                                LOGGER.trace("polling context build success: {}", asyncResult.result().body().toString());

                                var encodedString = Base64.getEncoder().encodeToString(asyncResult.result().body().toString().getBytes());

                                LOGGER.trace("Polling initiated\t{}", encodedString);

                                var processBuilder = new ProcessBuilder(GO_PLUGIN_ENGINE_PATH, encodedString);

                                processBuilder.redirectErrorStream(true);

                                LOGGER.trace("Initiating process builder");

                                var process = processBuilder.start();

                                var isCompleted = process.waitFor(25, TimeUnit.SECONDS); // Wait for 25 seconds

                                if(!isCompleted)
                                {
                                    process.destroyForcibly();

                                    LOGGER.warn("Polling failed, Timed out");
                                }
                                else
                                {
                                    var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                                    var line = "";

                                    var processBuffer = Buffer.buffer();

                                    while((line = reader.readLine()) != null)
                                    {
                                        processBuffer.appendString(line);
                                    }

                                    LOGGER.trace("Process completed, Decoding & sending result...");

                                    var decodedString = new String(Base64.getDecoder().decode(processBuffer.toString()));

                                    LOGGER.trace(decodedString);

                                    eventBus.send(STORE_POLLED_DATA_EVENT, new JsonArray(decodedString));
                                }

                            }
                        }
                        else
                        {
                            LOGGER.warn(asyncResult.cause().getMessage());
                        }

                    } catch(InterruptedException | IOException exception)
                    {
                        LOGGER.error("Error while polling context: {}", exception.getMessage());
                    }
                });

            } catch(Exception exception)
            {
                LOGGER.error("Exception occurred in polling: {}", exception.getMessage(), exception);
            }
        });

        startPromise.complete();
    }
}
