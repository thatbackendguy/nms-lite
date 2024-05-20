package com.motadata.engine;

import com.motadata.Bootstrap;
import com.motadata.config.Config;
import com.motadata.database.ConfigDB;
import com.motadata.utils.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import static com.motadata.constants.Constants.*;

public class Polling extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Polling.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        var vertx = Bootstrap.getVertx();

        vertx.setPeriodic(Config.POLLING_INTERVAL, timerId -> {
            try
            {

                var context = new JsonArray();

                for(var monitor : ConfigDB.provisionedDevices.values())
                {
                    if(monitor != null)
                    {
                        context.add(monitor.put(PLUGIN_NAME, NETWORK).put(REQUEST_TYPE, COLLECT));
                    }
                }

                var encodedString = Base64.getEncoder().encodeToString(context.toString().getBytes());

                var results = Utils.spawnPluginEngine(encodedString);

                for(var result : results)
                {
                    var monitor = new JsonObject(result.toString());

                    LOGGER.trace(monitor.toString());

                    if(monitor.getString(STATUS).equals(SUCCESS))
                    {
                        Utils.writeToFile(vertx, monitor).onComplete(event -> LOGGER.trace("Result written to file"));
                    }
                }
            } catch(Exception exception)
            {
                LOGGER.error(ERROR_CONTAINER, exception.getMessage());
            }
        });

        startPromise.complete();

        LOGGER.info("Polling engine started successfully");
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception
    {
        stopPromise.complete();
    }
}
