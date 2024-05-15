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

        // TODO: Polling => before starting polling, check for is.discovered=1 (use join on profile_mapping)


        startPromise.complete();
    }
}
