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

public class Scheduler extends AbstractVerticle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {

        var vertx = Bootstrap.getVertx();

        var eventBus = vertx.eventBus();

        vertx.setPeriodic(Config.POLLING_INTERVAL, timerId ->
        {
            try
            {

                var context = new JsonArray();

                for (var monitor : ConfigDB.provisionedDevices.values())
                {
                    if (monitor != null)
                    {
                        context.add(monitor.put(PLUGIN_NAME, NETWORK).put(REQUEST_TYPE, COLLECT));
                    }
                }

                var encodedString = Base64.getEncoder().encodeToString(context.toString().getBytes());

               eventBus.send(POLL_METRICS_EVENT,encodedString);

            }
            catch (Exception exception)
            {
                LOGGER.error(ERROR_CONTAINER, exception.getMessage());
            }
        });

        startPromise.complete();

        LOGGER.info("Scheduler started successfully");
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception
    {

        stopPromise.complete();
    }

}
