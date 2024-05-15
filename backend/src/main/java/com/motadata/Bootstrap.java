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

        vertx.deployVerticle("com.motadata.engine.ApiEngine", handler->{
            if(handler.succeeded())
            {
                LOGGER.info("API engine is up and running");
            }
            else
            {
                LOGGER.error(handler.cause().getMessage());
            }
        });

        vertx.deployVerticle("com.motadata.manager.ConfigManager", new DeploymentOptions()
                .setThreadingModel(ThreadingModel.WORKER),
                handler->{
            if(handler.succeeded())
            {
                LOGGER.info("Config Manager is up and running");
            }
            else
            {
                LOGGER.error(handler.cause().getMessage());
            }
        });

        vertx.deployVerticle("com.motadata.engine.PluginEngine", new DeploymentOptions()
                .setThreadingModel(ThreadingModel.WORKER),
                handler->{
            if(handler.succeeded())
            {
                LOGGER.info("Plugin engine is up and running");
            }
            else
            {
                LOGGER.error(handler.cause().getMessage());
            }
        });
    }
}
