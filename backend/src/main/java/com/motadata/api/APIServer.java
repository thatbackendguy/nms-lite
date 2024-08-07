package com.motadata.api;

import com.motadata.config.Config;
import com.motadata.constants.Constants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import io.vertx.ext.web.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.motadata.constants.Constants.*;

public class APIServer extends AbstractVerticle
{

    protected static final Logger LOGGER = LoggerFactory.getLogger(APIServer.class);

    private HttpServer server = null;

    private ErrorHandler errorHandler()
    {

        return ErrorHandler.create(vertx);
    }

    @Override
    public void start(Promise<Void> startPromise)
    {

        try
        {
            var router = Router.router(vertx);

            server = vertx.createHttpServer(new HttpServerOptions().setHost(Config.HOST).setPort(Config.PORT));

            new Credential().init(router);

            new Discovery().init(router);

            new PolledResult().init(router);

            // FOR RUNTIME EXCEPTION HANDLING FAILURES
            router.route().failureHandler(errorHandler());

            // GET: "/"
            router.route(URL_SEPARATOR).handler(routingContext ->
            {

                LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                        .path(), routingContext.request().remoteAddress());

                routingContext.json(new JsonObject().put(STATUS, SUCCESS)
                        .put(MESSAGE, "Welcome to Network Monitoring System!"));
            });

            server.requestHandler(router).listen(httpServerAsyncResult ->
            {

                if (httpServerAsyncResult.succeeded())
                {
                    startPromise.complete();

                    LOGGER.info("API Server is up and running");

                    LOGGER.info(String.format("HTTP Server is now listening on http://%s:%d/", Config.HOST, Config.PORT));
                }
                else
                {
                    startPromise.fail(httpServerAsyncResult.cause());

                    LOGGER.info("Failed to start the API Engine, port unavailable!");
                }
            });
        }
        catch (Exception exception)
        {
            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());

            startPromise.fail(exception);
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise)
    {

        if (server != null)
        {
            LOGGER.debug("Closing API Server");

            server.close();
        }

        stopPromise.complete();

        LOGGER.info("API Server undeployed successfully");
    }

}
