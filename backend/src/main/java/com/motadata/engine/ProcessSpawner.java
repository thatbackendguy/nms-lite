package com.motadata.engine;

import com.motadata.Bootstrap;
import com.motadata.utils.Utils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.motadata.constants.Constants.*;

public class ProcessSpawner extends AbstractVerticle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessSpawner.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {

        var vertx = Bootstrap.getVertx();

        var eventBus = vertx.eventBus();

        eventBus.<String> localConsumer(RUN_DISCOVERY_EVENT, msg ->
        {
            vertx.executeBlocking(handler ->
            {
                try
                {
                    var results = Utils.spawnPluginEngine(msg.body());

                    eventBus.send(PARSE_DISCOVERY_EVENT, results);
                }
                catch (Exception exception)
                {
                    LOGGER.error(ERROR_CONTAINER, exception.getMessage());
                }
            });

        });

        eventBus.<String> localConsumer(POLL_METRICS_EVENT, msg ->
        {
            vertx.executeBlocking(handler ->
            {
                try
                {

                    var results = Utils.spawnPluginEngine(msg.body());

                    eventBus.send(PARSE_COLLECT_EVENT, results);
                }

                catch (Exception exception)
                {
                    LOGGER.error(ERROR_CONTAINER, exception.getMessage());
                }
            });
        });

        startPromise.complete();

        LOGGER.info("Process spawner started");
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception
    {

        stopPromise.complete();
    }

}
