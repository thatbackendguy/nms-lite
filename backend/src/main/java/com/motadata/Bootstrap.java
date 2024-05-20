package com.motadata;

import com.motadata.api.APIServer;
import com.motadata.engine.*;
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
        var workerOptions = new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER);

        vertx.deployVerticle(Discovery.class.getName(), workerOptions)

                .compose(id->vertx.deployVerticle(Polling.class.getName(), workerOptions))

                .compose(id -> vertx.deployVerticle(APIServer.class.getName()))

                .onFailure(err -> LOGGER.error("Deployment failed: {}", err.getMessage()));
    }

    public static Vertx getVertx()
    {
        return vertx;
    }
}