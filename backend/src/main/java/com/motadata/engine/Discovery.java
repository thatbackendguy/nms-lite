package com.motadata.engine;

import com.motadata.Bootstrap;
import com.motadata.database.ConfigDB;
import com.motadata.utils.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.motadata.constants.Constants.*;

public class Discovery extends AbstractVerticle
{
    static final Logger LOGGER = LoggerFactory.getLogger(Discovery.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        var eventBus = Bootstrap.getVertx().eventBus();

        eventBus.localConsumer(RUN_DISCOVERY_EVENT, msg -> {
            try
            {

                var results = Utils.spawnPluginEngine(msg.body().toString());

                for(var result : results)
                {
                    var monitor = new JsonObject(result.toString());

                    LOGGER.trace(monitor.toString());

                    if(monitor.getString(STATUS).equals(SUCCESS))
                    {

                        var discoveryProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, Long.parseLong(monitor.getString(DISCOVERY_PROFILE_ID))))).getJsonObject(RESULT);

                        if(!discoveryProfile.getBoolean(IS_DISCOVERED, false))
                        {
                            var credentialProfileId = Long.parseLong(monitor.getString(CREDENTIAL_PROFILE_ID));

                            discoveryProfile.put(IS_DISCOVERED, true);

                            discoveryProfile.put(RESULT,monitor.getJsonObject(RESULT));

                            discoveryProfile.put(CREDENTIAL_PROFILE_ID, credentialProfileId);

                            Utils.incrementCounter(credentialProfileId);
                        }

                        LOGGER.trace("Monitor status updated");
                    }
                }
            } catch(Exception exception)
            {
                LOGGER.error(ERROR_CONTAINER, exception.getMessage());
            }
        });

        startPromise.complete();

        LOGGER.info("Discovery engine started");
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception
    {
        stopPromise.complete();
    }
}
