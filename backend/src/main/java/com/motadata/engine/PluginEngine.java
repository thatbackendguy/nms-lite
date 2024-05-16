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

import static com.motadata.Bootstrap.LOGGER;
import static com.motadata.utils.Constants.*;

public class PluginEngine extends AbstractVerticle
{

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        var eventBus = vertx.eventBus();

        eventBus.localConsumer(RUN_DISCOVERY,msg->{
            try
            {

                ProcessBuilder processBuilder = new ProcessBuilder("/home/yash/Documents/GitHub/nms-lite/plugin-engine/plugin-engine", msg.body().toString());

                processBuilder.redirectErrorStream(true);

                LOGGER.trace("Initiating process builder");

                Process process = processBuilder.start();

                // Read the output of the command
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;

                var processBuffer = Buffer.buffer();

                while((line = reader.readLine()) != null)
                {
                    processBuffer.appendString(line);
                }

                LOGGER.trace("Process completed, Decoding & sending result...");

                var decodedString = new String(Base64.getDecoder().decode(processBuffer.toString()));

                var results = new JsonArray(decodedString);

                for(var result: results)
                {
                    var monitor = new JsonObject(result.toString());

                    if(monitor.getString(STATUS).equals(SUCCESS))
                    {
                        eventBus.send(POST_DISC_SUCCESS, new JsonObject().put(DISC_PROF_ID, monitor.getInteger(DISC_PROF_ID)).put(CRED_PROF_ID, monitor.getInteger(CRED_PROF_ID)));
                    }
                }

                msg.reply(decodedString);

            } catch(IOException err)
            {
                msg.fail(500, err.getMessage());
            }
        });

        // TODO: if not, change to 5 * 60 * 1000 for 5 minutes
        vertx.setPeriodic(20 * 1000, timerId -> {

            LOGGER.trace("Polling started, requesting for provision devices..");

            eventBus.request(PROVISION_DEVICES, "",ar -> {
                try
                {
                    LOGGER.debug("polling context build success: {}", ar.result().body().toString());

                    var encodedString = Base64.getEncoder().encodeToString(ar.result().body().toString().getBytes());

                    LOGGER.trace("Polling initiated\t{}", encodedString);

                    var processBuilder = new ProcessBuilder("/home/yash/Documents/GitHub/nms-lite/plugin-engine/plugin-engine", encodedString);

                    processBuilder.redirectErrorStream(true);

                    LOGGER.trace("Initiating process builder");

                    var process = processBuilder.start();

                    // Read the output of the command
                    var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    var line = "";

                    var processBuffer = Buffer.buffer();

                    while((line = reader.readLine()) != null)
                    {
                        processBuffer.appendString(line);
                    }

                    LOGGER.trace("Process completed, Decoding & sending result...");

                    var decodedString = new String(Base64.getDecoder().decode(processBuffer.toString()));

                    LOGGER.debug(decodedString);

                    eventBus.send(POLL_DATA_STORE, decodedString);


                } catch(IOException err)
                {
                    LOGGER.error("Error while polling context", err);
                }
            });
        });


        startPromise.complete();
    }
}
