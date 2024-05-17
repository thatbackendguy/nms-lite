package com.motadata;

import com.motadata.engine.ApiEngine;
import com.motadata.engine.DiscoveryEngine;
import com.motadata.engine.PollingEngine;
import com.motadata.manager.ConfigServiceManager;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args)
    {
        var vertx = Vertx.vertx();

        vertx.deployVerticle(ConfigServiceManager.class.getName(), new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER), configServiceManagerHandler -> {

            if(configServiceManagerHandler.succeeded())
            {
                LOGGER.info("Config Service Manager is up and running");

                vertx.deployVerticle(DiscoveryEngine.class.getName(), new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER), discoveryEngineHandler -> {

                    if(discoveryEngineHandler.succeeded())
                    {
                        LOGGER.info("Discovery engine is up and running");

                        vertx.deployVerticle(PollingEngine.class.getName(), new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER), pollingEngineHandler -> {

                            if(pollingEngineHandler.succeeded())
                            {
                                LOGGER.info("Polling engine is up and running");

                                vertx.deployVerticle(ApiEngine.class.getName(), apiEngineHandler -> {

                                    if(apiEngineHandler.succeeded())
                                    {
                                        LOGGER.info("API engine is up and running");
                                    }
                                    else
                                    {
                                        LOGGER.error(apiEngineHandler.cause().getMessage());
                                    }
                                });
                            }
                            else
                            {
                                LOGGER.error(pollingEngineHandler.cause().getMessage());
                            }
                        });
                    }
                    else
                    {
                        LOGGER.error(discoveryEngineHandler.cause().getMessage());
                    }
                });
            }
            else
            {
                LOGGER.error(configServiceManagerHandler.cause().getMessage());
            }
        });

    }
}