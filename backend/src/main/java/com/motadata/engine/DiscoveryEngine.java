package com.motadata.engine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static com.motadata.Bootstrap.LOGGER;
import static com.motadata.utils.Constants.*;
import static com.motadata.utils.Constants.CREDENTIAL_PROFILE_ID;

public class DiscoveryEngine extends AbstractVerticle
{
    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        var eventBus = vertx.eventBus();

        eventBus.localConsumer(RUN_DISCOVERY_EVENT, msg -> {
            try
            {
                var processBuilder = new ProcessBuilder("/home/yash/Documents/GitHub/nms-lite/plugin-engine/plugin-engine", msg.body().toString());

                processBuilder.redirectErrorStream(true);

                LOGGER.trace("Initiating process builder");

                var process = processBuilder.start();

                var isCompleted = process.waitFor(25, TimeUnit.SECONDS); // Wait for 25 seconds

                if(!isCompleted)
                {
                    process.destroyForcibly();

                    msg.fail(500, "Discovery failed, Timed out");
                }
                else
                {

                    var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    String line;

                    var processBuffer = Buffer.buffer();

                    while((line = reader.readLine()) != null)
                    {
                        processBuffer.appendString(line);
                    }

                    LOGGER.trace("Process completed, Decoding & sending result...");

                    var decodedString = new String(Base64.getDecoder().decode(processBuffer.toString()));

                    var results = new JsonArray(decodedString);

                    for(var result : results)
                    {
                        var monitor = new JsonObject(result.toString());

                        if(monitor.getString(STATUS).equals(SUCCESS))
                        {
                            eventBus.send(POST_DISCOVERY_SUCCESS_EVENT, new JsonObject().put(DISCOVERY_PROFILE_ID, monitor.getInteger(DISCOVERY_PROFILE_ID)).put(CREDENTIAL_PROFILE_ID, monitor.getInteger(CREDENTIAL_PROFILE_ID)));
                        }
                    }
                    msg.reply(decodedString);
                }

            } catch(IOException | InterruptedException err)
            {
                msg.fail(500, err.getMessage());
            }
        });

        startPromise.complete();
    }
}