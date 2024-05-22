package com.motadata;

import com.motadata.api.APIServer;
import com.motadata.constants.Constants;
import com.motadata.engine.ProcessSpawner;
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

                    .compose(id -> vertx.deployVerticle(ProcessSpawner.class.getName()))

                    .compose(id -> vertx.deployVerticle(ResponseParser.class.getName(), new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)))

                    .compose(id -> vertx.deployVerticle(APIServer.class.getName()))

                    .onSuccess(handler -> LOGGER.info("All verticles deployed"))

                    .onFailure(exception -> LOGGER.error("Deployment failed: {}", exception.getMessage()));

        }
        catch (Exception exception)
        {
            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    public static Vertx getVertx()
    {

        return vertx;
    }

}