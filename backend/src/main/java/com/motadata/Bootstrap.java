package com.motadata;

import com.motadata.api.ApiRouter;
import com.motadata.engine.DiscoveryEngine;
import com.motadata.engine.PollingEngine;
import com.motadata.manager.ConfigServiceManager;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Bootstrap
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args)
    {
        var vertx = Vertx.vertx();

        var workerOptions = new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER);

        deployVerticle(vertx, ConfigServiceManager.class.getName(), workerOptions)
                .compose(id -> {
                    LOGGER.info("Config Service Manager is up and running");

                    return deployVerticle(vertx, DiscoveryEngine.class.getName(), workerOptions);
                })
                .compose(id -> {
                    LOGGER.info("Discovery engine is up and running");

                    return deployVerticle(vertx, PollingEngine.class.getName(), workerOptions);
                })
                .compose(id -> {
                    LOGGER.info("Polling engine is up and running");

                    return deployVerticle(vertx, ApiRouter.class.getName(), new DeploymentOptions());
                })
                .onSuccess(id -> {
                    LOGGER.info("API engine is up and running");
                })
                .onFailure(err -> LOGGER.error("Deployment failed: {}", err.getMessage()));

    }

    private static Future<String> deployVerticle(Vertx vertx, String verticleName, DeploymentOptions options)
    {
        return vertx.deployVerticle(verticleName, options);
    }
}