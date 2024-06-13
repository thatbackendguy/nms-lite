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

        // for metric polling
        vertx.setPeriodic(60_000, timerId ->
        {
            LOGGER.trace("Initiating metric polling cycle...");

            try
            {

                var context = new JsonArray();

                for (var monitor : ConfigDB.provisionedDevices.values())
                {
                    if (monitor != null && Utils.isAvailable(monitor.getString(OBJECT_IP)))
                    {
                        context.add(monitor.put(PLUGIN_NAME, NETWORK).put(REQUEST_TYPE, COLLECT));
                    }
                }

                if (!context.isEmpty())
                {
                    var encodedString = Base64.getEncoder().encodeToString(context.toString().getBytes());

                    LOGGER.trace("POLL METRICS: {}", encodedString);

                    // sending poll event on interval to Requester
                    eventBus.send(POLL_METRICS_EVENT, encodedString);
                }

            }
            catch (Exception exception)
            {
                LOGGER.error(ERROR_CONTAINER, exception.getMessage());
            }
        });

        // for availability
        vertx.setPeriodic(30_000, timerId ->
        {
            LOGGER.trace("Initiating check availability cycle...");

            try
            {
                var context = new JsonArray();

                var discoveryProfiles = ( ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE)) ).getJsonArray(RESULT, new JsonArray());

                if (!discoveryProfiles.isEmpty())
                {
                    for (var profile : discoveryProfiles)
                    {
                        var discoveryProfile = new JsonObject(profile.toString());

                        if (discoveryProfile.containsKey(OBJECT_IP) && discoveryProfile.containsKey(DISCOVERY_PROFILE_ID))
                        {
                            context.add(new JsonObject().put(OBJECT_IP, discoveryProfile.getString(OBJECT_IP))
                                    .put(DISCOVERY_PROFILE_ID, discoveryProfile.getInteger(DISCOVERY_PROFILE_ID))
                                    .put(REQUEST_TYPE, AVAILABILITY)
                                    .put(PLUGIN_NAME, NETWORK));
                        }
                    }
                }

                if (!context.isEmpty())
                {
                    var encodedString = Base64.getEncoder().encodeToString(context.toString().getBytes());

                    LOGGER.trace("CHECK AVAILABILITY: {}", encodedString);

                    // sending availability event on interval to Requester
                    eventBus.send(CHECK_AVAILABILITY, encodedString);
                }
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
    public void stop(Promise<Void> stopPromise)
    {

        stopPromise.complete();

        LOGGER.info("Scheduler undeployed successfully");
    }

}
