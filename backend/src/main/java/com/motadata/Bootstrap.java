package com.motadata;

import com.motadata.api.APIServer;
//import com.motadata.engine.DiscoveryEngine;
//import com.motadata.engine.PollingEngine;
//import com.motadata.manager.ConfigServiceManager;
import com.motadata.engine.Discovery;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
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
//                .compose(id -> {
//                    LOGGER.info("Config Service Manager is up and running");
//
//                    return vertx.deployVerticle(DiscoveryEngine.class.getName(), workerOptions);
//                })
//                .compose(id -> {
//                    LOGGER.info("Discovery engine is up and running");
//
//                    return vertx.deployVerticle(PollingEngine.class.getName(), workerOptions);
//                })
                .compose(id -> {
                    LOGGER.info("Polling engine is up and running");

                    return vertx.deployVerticle(APIServer.class.getName());
                })
                .onSuccess(id -> {
                    LOGGER.info("API engine is up and running");
                })
                .onFailure(err -> LOGGER.error("Deployment failed: {}", err.getMessage()));

//        vertx.deployVerticle(APIServer.class.getName());
    }

    public static Vertx getVertx()
    {
        return vertx;
    }
}