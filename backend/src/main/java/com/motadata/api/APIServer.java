package com.motadata.api;

import com.motadata.Config;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.motadata.contants.Constants.*;

public class APIServer extends AbstractVerticle
{
    public static final Logger LOGGER = LoggerFactory.getLogger(APIServer.class);

    private ErrorHandler errorHandler()
    {
        return ErrorHandler.create(vertx);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        var server = vertx.createHttpServer(new HttpServerOptions().setHost(Config.HOST).setPort(Config.PORT));

        var router = Router.router(vertx);

        new Credential().init(router);

        new Discovery().init(router);

        // FOR HANDLING FAILURES
        router.route().failureHandler(errorHandler());

        // GET: "/"
        router.route(URL_SEPARATOR).handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Welcome to Network Monitoring System!"));
        });

        server.requestHandler(router).listen(httpServerAsyncResult -> {

            if(httpServerAsyncResult.succeeded())
            {
                startPromise.complete();

                LOGGER.info(String.format("HTTP Server is now listening on http://%s:%d/", Config.HOST, Config.PORT));
            }
            else
            {
                startPromise.fail(httpServerAsyncResult.cause());

                LOGGER.info("Failed to start the API Engine, port unavailable!");
            }
        });
    }
}
