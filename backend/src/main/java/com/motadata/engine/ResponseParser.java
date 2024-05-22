package com.motadata.engine;

import com.motadata.Bootstrap;
import com.motadata.database.ConfigDB;
import com.motadata.utils.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.motadata.constants.Constants.*;

public class ResponseParser extends AbstractVerticle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseParser.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {

        var vertx = Bootstrap.getVertx();

        var eventBus = vertx.eventBus();

        eventBus.<JsonArray> localConsumer(PARSE_DISCOVERY_EVENT, results ->
        {
            vertx.executeBlocking(handler ->
            {
                for (var result : results.body())
                {
                    var monitor = new JsonObject(result.toString());

                    LOGGER.trace(monitor.toString());

                    if (monitor.getString(STATUS).equals(SUCCESS))
                    {

                        var discoveryProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE)
                                        .put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, Long.parseLong(monitor.getString(DISCOVERY_PROFILE_ID)))))
                                .getJsonObject(RESULT);

                        if (!discoveryProfile.getBoolean(IS_DISCOVERED, false))
                        {
                            var credentialProfileId = Long.parseLong(monitor.getString(CREDENTIAL_PROFILE_ID));

                            discoveryProfile.put(IS_DISCOVERED, true);

                            discoveryProfile.put(RESULT, monitor.getJsonObject(RESULT));

                            discoveryProfile.put(CREDENTIAL_PROFILE_ID, credentialProfileId);

                            Utils.incrementCounter(credentialProfileId);
                        }

                        LOGGER.trace("Monitor status updated");
                    }
                }
            });

        });

        eventBus.<JsonArray> localConsumer(PARSE_COLLECT_EVENT, results ->
        {
            vertx.executeBlocking(handler ->
            {
                for (var result : results.body())
                {
                    var monitor = new JsonObject(result.toString());

                    LOGGER.trace(monitor.toString());

                    if (monitor.getString(STATUS).equals(SUCCESS))
                    {

                        Utils.writeToFile(vertx, monitor).onComplete(event -> LOGGER.trace("Result written to file"));

                    }
                }
            });
        });

        startPromise.complete();

        LOGGER.info("Response parser started");
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception
    {

        stopPromise.complete();
    }

}
