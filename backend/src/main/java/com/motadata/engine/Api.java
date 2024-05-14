package com.motadata.engine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.ErrorHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;

import static com.motadata.Bootstrap.LOGGER;
import static com.motadata.utils.constants.Constants.*;

public class Api extends AbstractVerticle
{
    private ErrorHandler errorHandler()
    {
        return ErrorHandler.create(vertx);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        HttpServer server = vertx.createHttpServer();

        var eventBus = vertx.eventBus();

        Router mainRouter = Router.router(vertx);

        Router credentialRouter = Router.router(vertx);

        Router discoveryRouter = Router.router(vertx);

        Router provisionRouter = Router.router(vertx);

        // for handling failures
        mainRouter.route().failureHandler(errorHandler());

        //--------------------------------------------------------------------------------------------------------------
        // GET: "/"
        mainRouter.route("/").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Welcome to Network Monitoring System!"));
        });

        //--------------------------------------------------------------------------------------------------------------
        // CREDENTIAL PROFILE SUB-ROUTER
        mainRouter.route("/credential/*").subRouter(credentialRouter);

        // DISCOVERY PROFILE SUB-ROUTER
        mainRouter.route("/discovery/*").subRouter(discoveryRouter);

        // PROVISION SUB-ROUTER
        mainRouter.route("/provision/*").subRouter(provisionRouter);

        //--------------------------------------------------------------------------------------------------------------
        // create credential profile
        credentialRouter.route(HttpMethod.POST, "/add").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.request().bodyHandler(buffer -> {

                JsonObject reqJSON = buffer.toJsonObject().put(TABLE_NAME, "credential_profile");

                eventBus.request(INSERT_EVENT, reqJSON, ar -> {

                    if(ar.succeeded())
                    {
                        if(ar.result().body().equals("success"))
                        {
                            ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profile added successfully!"));
                        }
                    }
                    else
                    {
                        ctx.response().setStatusCode(500).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Insertion Error").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, 500)).encode());
                    }
                });
            });
        });

        // get-all credential profiles
        credentialRouter.route(HttpMethod.GET, "/get-all").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            eventBus.request(GET_ALL_EVENT, new JsonObject().put(TABLE_NAME, "credential_profile"), ar -> {

                if(ar.succeeded())
                {
                    ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profiles fetched successfully!").put("credential.profiles", ar.result().body()));
                }
                else
                {
                    ctx.response().setStatusCode(500).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, 500)).encode());
                }
            });
        });

        // get credential profile
        credentialRouter.route(HttpMethod.GET, "/get/:credProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var credProfileId = ctx.request().getParam("credProfileId");

            eventBus.request(GET_EVENT, new JsonObject().put(TABLE_NAME, "credential_profile").put(CRED_PROF_ID, credProfileId), ar -> {

                if(ar.succeeded())
                {
                    ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profile fetched successfully!").put(RESULT, ar.result().body()));
                }
                else
                {
                    ctx.response().setStatusCode(500).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, 500)).encode());
                }
            });
        });

        // UPDATE CREDENTIAL PROFILE
        credentialRouter.route(HttpMethod.PUT, "/update/:credProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var credProfileId = ctx.request().getParam("credProfileId");

            ctx.request().bodyHandler(buffer -> {

                JsonObject reqJSON = buffer.toJsonObject().put(CRED_PROF_ID, credProfileId).put(TABLE_NAME, "credential_profile");

                eventBus.request(UPDATE_EVENT, reqJSON, ar -> {

                    if(ar.succeeded())
                    {
                        ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profile updated successfully!").put("result", ar.result().body()));
                    }
                    else
                    {
                        ctx.response().setStatusCode(500).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error updating data").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, 500)).encode());
                    }
                });
            });
        });

        // DELETE CREDENTIAL PROFILE
        credentialRouter.route(HttpMethod.DELETE, "/delete/:credProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var credProfileId = ctx.request().getParam("credProfileId");

            eventBus.request(DELETE_EVENT, new JsonObject().put(CRED_PROF_ID, credProfileId).put(TABLE_NAME, "credential_profile"), ar -> {

                if(ar.succeeded())
                {
                    if(ar.result().body().equals(SUCCESS))
                    {
                        ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profile: " + credProfileId + " deleted successfully!"));
                    }
                    else
                    {
                        ctx.response().setStatusCode(404).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Deletion Error").put(ERR_STATUS_CODE, 404).put(ERR_MESSAGE, "Credential profile: " + credProfileId + " not found!")).encode());
                    }
                }
                else
                {
                    ctx.response().setStatusCode(500).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Deletion Error").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, 500)).encode());
                }
            });
        });

        //--------------------------------------------------------------------------------------------------------------
        // create discovery profile
        discoveryRouter.route(HttpMethod.POST, "/add").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.request().bodyHandler(buffer -> {

                JsonObject reqJSON = buffer.toJsonObject().put(TABLE_NAME, "discovery_profile");

                eventBus.request(INSERT_EVENT, reqJSON, ar -> {

                    if(ar.succeeded())
                    {
                        if(ar.result().body().equals("success"))
                        {
                            ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile added successfully!"));
                        }
                    }
                    else
                    {
                        ctx.response().setStatusCode(500).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Insertion Error").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, 500)).encode());
                    }
                });
            });
        });

        discoveryRouter.route(HttpMethod.GET, "/get-all").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            eventBus.request(GET_ALL_EVENT, new JsonObject().put(TABLE_NAME, "discovery_profile"), ar -> {

                if(ar.succeeded())
                {
                    ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profiles fetched successfully!").put("discovery.profiles", ar.result().body()));
                }
                else
                {
                    ctx.response().setStatusCode(500).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, 500)).encode());
                }
            });
        });

        // get discovery profile
        discoveryRouter.route(HttpMethod.GET, "/get/:discProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var discProfileId = ctx.request().getParam("discProfileId");

            eventBus.request(GET_EVENT, new JsonObject().put(DISC_PROF_ID, discProfileId).put(TABLE_NAME, "discovery_profile"), ar -> {

                if(ar.succeeded())
                {
                    ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile fetched successfully!").put(RESULT, ar.result().body()));
                }
                else
                {
                    ctx.response().setStatusCode(500).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, 500)).encode());
                }
            });
        });

        // UPDATE DISCOVERY PROFILE
        discoveryRouter.route(HttpMethod.PUT, "/update/:discProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var discProfileId = ctx.request().getParam("discProfileId");

            ctx.request().bodyHandler(buffer -> {

                var reqJSON = buffer.toJsonObject().put(DISC_PROF_ID, discProfileId).put(TABLE_NAME, "discovery_profile");

                eventBus.request(UPDATE_EVENT, reqJSON, ar -> {

                    if(ar.succeeded())
                    {
                        ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile updated successfully!").put("result", ar.result().body()));
                    }
                    else
                    {
                        ctx.response().setStatusCode(500).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error updating data").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, 500)).encode());
                    }
                });
            });
        });

        // DELETE DISCOVERY PROFILE
        discoveryRouter.route(HttpMethod.DELETE, "/delete/:discProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var discProfileId = ctx.request().getParam("discProfileId");

            eventBus.request(DELETE_EVENT, new JsonObject().put(DISC_PROF_ID, discProfileId).put(TABLE_NAME, "discovery_profile"), ar -> {

                if(ar.succeeded())
                {
                    if(ar.result().body().equals(SUCCESS))
                    {
                        ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile: " + discProfileId + " deleted successfully!"));
                    }
                    else
                    {
                        ctx.response().setStatusCode(404).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Deletion Error").put(ERR_STATUS_CODE, 404).put(ERR_MESSAGE, "Discovery profile: " + discProfileId + " not found!")).encode());
                    }
                }
                else
                {
                    ctx.response().setStatusCode(500).putHeader("Content-Type", "application/json").end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Deletion Error").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, 500)).encode());
                }
            });
        });

        //--------------------------------------------------------------------------------------------------------------
        // RUN DISCOVERY
        discoveryRouter.route(HttpMethod.POST, "/run").handler(ctx -> {
            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.request().bodyHandler(buffer -> {

                var reqArray = buffer.toJsonArray();

                eventBus.request(MAKE_DISCOVERY_CONTEXT, reqArray, ar -> {

                    if(ar.succeeded())
                    {
                        LOGGER.info("context build success: {}", ar.result().body().toString());

                        String encodedString = Base64.getEncoder().encodeToString(ar.result().body().toString().getBytes());

                        LOGGER.trace("Execute Blocking initiated\n{}", encodedString);

                        eventBus.request(RUN_DISCOVERY, encodedString, res -> {
                            ctx.json(new JsonArray(res.result().body().toString()));
                        });

                    }
                    else
                    {
                        LOGGER.info(ar.cause().getMessage());

                        ctx.response().setStatusCode(500).end(ar.cause().getMessage());
                    }
                });
            });

        });


        //--------------------------------------------------------------------------------------------------------------
        // TODO /provision/create
        // TODO /provision/get/:discoveryId
        // TODO /provision/get-all
        // TODO /provision/delete

        server.requestHandler(mainRouter).listen(8080, res -> {

            if(res.succeeded())
            {
                LOGGER.info("Server is now listening on http://localhost:8080/");

                startPromise.complete();
            }
            else
            {
                LOGGER.info("Failed to start the server");

                startPromise.fail(res.cause());
            }
        });
    }
}
