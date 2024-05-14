package com.motadata;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap
{
    public static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args)
    {
        var vertx = Vertx.vertx();

        vertx.deployVerticle("com.motadata.engine.Api", handler->{
            if(handler.succeeded())
            {
                LOGGER.info("Server is up and running");
            }
        });

        vertx.deployVerticle("com.motadata.dbmanager.DbManager", new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER), handler->{
            if(handler.succeeded())
            {
                LOGGER.info("DB Manager is up and running");
            }
        });

        vertx.deployVerticle("com.motadata.engine.Plugin", new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER), handler->{
            if(handler.succeeded())
            {
                LOGGER.info("Plugin engine is up and running");
            }
        });
    }
}
