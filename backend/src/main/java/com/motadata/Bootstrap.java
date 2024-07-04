package com.motadata;

import com.motadata.api.APIServer;
import com.motadata.config.Config;
import com.motadata.constants.Constants;
import com.motadata.engine.Requester;
import com.motadata.engine.ResponseReceiver;
import com.motadata.engine.Scheduler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Bootstrap
{

    public static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    private static final Vertx vertx = Vertx.vertx();

    private static final ArrayList<String> deploymentIds = new ArrayList<>();

    public static void main(String[] args)
    {

        try
        {
            var pluginEngine = new ProcessBuilder(Config.GO_PLUGIN_ENGINE_PATH);

            var pluginProcess = pluginEngine.start();

            LOGGER.trace("Go Plugin engine started: {}", Config.GO_PLUGIN_ENGINE_PATH);

            var collector = new ProcessBuilder(Config.GO_COLLECTOR_PATH);

            var collectorProcess = collector.start();

            LOGGER.trace("Go collector started: {}", Config.GO_COLLECTOR_PATH);

            vertx.deployVerticle(Scheduler.class.getName(), new DeploymentOptions().setInstances(1)).compose(id ->
            {
                deploymentIds.add(id);

                return vertx.deployVerticle(Requester.class.getName(), new DeploymentOptions().setInstances(1));

            }).compose(id ->
            {
                deploymentIds.add(id);

                return vertx.deployVerticle(ResponseReceiver.class.getName(), new DeploymentOptions().setInstances(1));

            }).compose(id ->
            {
                deploymentIds.add(id);

                return vertx.deployVerticle(APIServer.class.getName(), new DeploymentOptions().setInstances(3));

            }).onSuccess(id ->
            {
                deploymentIds.add(id);

                LOGGER.info("All verticles deployed");

            }).onFailure(exception -> LOGGER.error("Deployment failed: ", exception));

            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {

                LOGGER.trace("Cleanup process in progress");

                deploymentIds.forEach(vertx::undeploy);

                pluginProcess.destroyForcibly();

                collectorProcess.destroyForcibly();

                LOGGER.trace("Cleanup process successful");

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