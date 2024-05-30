package com.motadata;

import com.motadata.api.APIServer;
import com.motadata.config.Config;
import com.motadata.constants.Constants;
import com.motadata.engine.Requester;
import com.motadata.engine.ResponseParser;
import com.motadata.engine.Scheduler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    private static final Vertx vertx = Vertx.vertx();

    public static void main(String[] args)
    {

        try
        {

            vertx.deployVerticle(Scheduler.class.getName(), new DeploymentOptions().setInstances(1))

                    .compose(id -> vertx.deployVerticle(Requester.class.getName(), new DeploymentOptions().setInstances(2)))

                    .compose(id -> vertx.deployVerticle(ResponseParser.class.getName(), new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)))

                    .compose(id -> vertx.deployVerticle(APIServer.class.getName(), new DeploymentOptions().setInstances(4)))

                    .onSuccess(handler -> LOGGER.info("All verticles deployed"))

                    .onFailure(exception -> LOGGER.error("Deployment failed: {}", exception.getMessage()));

            var pluginEngine = new ProcessBuilder(Config.GO_PLUGIN_ENGINE_PATH).redirectErrorStream(true);

            var pluginProcess = pluginEngine.start();

            LOGGER.trace("Go Plugin engine started");

            var collector = new ProcessBuilder(Config.GO_COLLECTOR_PATH).redirectErrorStream(true);

            var collectorProcess = collector.start();

            LOGGER.trace("Go collector started");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {

                pluginProcess.destroyForcibly();

                collectorProcess.destroyForcibly();

            }));

        }
        catch (Exception exception)
        {
            LOGGER.error(Constants.ERROR_CONTAINER, exception);
        }
    }

    public static Vertx getVertx()
    {

        return vertx;
    }

}