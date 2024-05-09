package com.motadata;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.ErrorHandler;

import static com.motadata.Bootstrap.LOGGER;
import static com.motadata.Constants.*;

public class ApiServer extends AbstractVerticle
{
    private ErrorHandler errorHandler()
    {
        return ErrorHandler.create(vertx);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        HttpServer server = vertx.createHttpServer();

        Router mainRouter = Router.router(vertx);

        Router discoveryRouter = Router.router(vertx);

        Router credentialRouter = Router.router(vertx);

        // for handling failures
        mainRouter.route().failureHandler(errorHandler());

        // GET: "/"
        mainRouter.route("/").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Welcome to Network Monitoring System!"));
        });

        // DISCOVERY PROFILE SUB-ROUTER
        mainRouter.route("/discovery/*").subRouter(discoveryRouter);

        // CREDENTIAL PROFILE SUB-ROUTER
        mainRouter.route("/credential-profile/*").subRouter(credentialRouter);

        // get discovery profile
        discoveryRouter.route(HttpMethod.GET, "/get/:ipAddress").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var ipAddress = ctx.request().getParam("ipAddress");

            vertx.eventBus().request("get.data", new JsonObject().put(OBJECT_IP, ipAddress).put(TABLE_NAME, "discovery_profile"), ar -> {

                if(ar.succeeded())
                {
                    ctx.json(new JsonObject(ar.result().body().toString()).put(STATUS, SUCCESS));
                }
            });
        });

        // create discovery profile
        discoveryRouter.route(HttpMethod.POST, "/add").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.request().bodyHandler(buffer -> {

                JsonObject reqJSON = buffer.toJsonObject().put(TABLE_NAME, "discovery_profile");

                vertx.eventBus().request("insert.data", reqJSON, ar -> {

                    if(ar.succeeded())
                    {
                        ctx.json(new JsonObject(ar.result().body().toString()));
                    }
                });
            });
        });

        // UPDATE DISCOVERY PROFILE
        discoveryRouter.route(HttpMethod.PUT, "/update/:ipAddress").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var ipAddress = ctx.request().getParam("ipAddress");

            ctx.request().bodyHandler(buffer -> {

                JsonObject reqJSON = buffer.toJsonObject()
                        .put(OBJECT_IP, ipAddress)
                        .put(TABLE_NAME, "discovery_profile");

                vertx.eventBus().request("update.data", reqJSON, ar -> {

                    if (ar.succeeded())
                    {
                        ctx.json(new JsonObject(ar.result().body().toString()));
                    }
                });
            });
        });

        // DELETE DISCOVERY PROFILE
        discoveryRouter.route(HttpMethod.DELETE, "/delete/:ipAddress").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var ipAddress = ctx.request().getParam("ipAddress");

            vertx.eventBus().request("delete.data", new JsonObject().put(OBJECT_IP, ipAddress).put(TABLE_NAME, "discovery_profile"), ar -> {

                if (ar.succeeded())
                {
                    ctx.json(new JsonObject(ar.result().body().toString()));
                }
            });
        });

        // CREDENTIAL PROFILE
        // get credential profile
        credentialRouter.route(HttpMethod.GET, "/get-all").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            vertx.eventBus().request("get.data", new JsonObject().put(TABLE_NAME, "credential_profile"), ar -> {

                if(ar.succeeded())
                {
                    ctx.json(new JsonObject(ar.result().body().toString()).put(STATUS, SUCCESS));
                }
            });
        });

        // create credential profile
        credentialRouter.route(HttpMethod.POST, "/add").handler(ctx->{

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.request().bodyHandler(buffer -> {

                JsonObject reqJSON = buffer.toJsonObject().put(TABLE_NAME, "credential_profile");

                vertx.eventBus().request("insert.data", reqJSON, ar -> {

                    if(ar.succeeded())
                    {
                        ctx.json(new JsonObject(ar.result().body().toString()));
                    }
                });
            });
        });

        // UPDATE CREDENTIAL PROFILE
        credentialRouter.route(HttpMethod.PUT, "/update/:credName").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var credName = ctx.request().getParam("credName");

            ctx.request().bodyHandler(buffer -> {

                JsonObject reqJSON = buffer.toJsonObject()
                        .put(CRED_NAME, credName)
                        .put(TABLE_NAME, "credential_profile");

                vertx.eventBus().request("update.data", reqJSON, ar -> {

                    if (ar.succeeded())
                    {
                        ctx.json(new JsonObject(ar.result().body().toString()));
                    }
                });
            });
        });

        // DELETE CREDENTIAL PROFILE
        credentialRouter.route(HttpMethod.DELETE, "/delete/:credName").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var credName = ctx.request().getParam("credName");

            vertx.eventBus().request("delete.data", new JsonObject().put(CRED_NAME, credName).put(TABLE_NAME, "credential_profile"), ar -> {

                if (ar.succeeded())
                {
                    ctx.json(new JsonObject(ar.result().body().toString()));
                }
            });
        });


        server.requestHandler(mainRouter).listen(8080, res -> {

            if(res.succeeded())
            {
                LOGGER.info("Server is now listening on http://localhost:8080/");

                startPromise.complete();
            }
            else
            {
                LOGGER.info("Failed to start the server");

                startPromise.fail("Failed to start the server");
            }
        });
    }
}
