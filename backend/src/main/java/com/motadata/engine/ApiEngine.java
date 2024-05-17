package com.motadata.engine;

import com.motadata.utils.Config;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import static com.motadata.utils.Constants.*;

public class ApiEngine extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiEngine.class);

    private ErrorHandler errorHandler()
    {
        return ErrorHandler.create(vertx);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        var server = vertx.createHttpServer(new HttpServerOptions().setHost(Config.HOST).setPort(Config.PORT));

        var eventBus = vertx.eventBus();

        var router = Router.router(vertx);

        var credentialRouter = Router.router(vertx);

        var discoveryRouter = Router.router(vertx);

        var provisionRouter = Router.router(vertx);

        var metricsRouter = Router.router(vertx);

        var snmpRouter = Router.router(vertx);

        // FOR HANDLING FAILURES
        router.route().failureHandler(errorHandler());

        //--------------------------------------------------------------------------------------------------------------

        // CREDENTIAL PROFILE SUB-ROUTER
        router.route("/credential/*").subRouter(credentialRouter);

        // DISCOVERY PROFILE SUB-ROUTER
        router.route("/discovery/*").subRouter(discoveryRouter);

        // PROVISION SUB-ROUTER
        router.route("/provision/*").subRouter(provisionRouter);

        // METRICS SUB-ROUTER
        router.route("/metrics/*").subRouter(metricsRouter);

        // NETWORK DEVICE - SNMP
        metricsRouter.route("/snmp/*").subRouter(snmpRouter);

        //--------------------------------------------------------------------------------------------------------------

        // GET: "/"
        router.route("/").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Welcome to Network Monitoring System!"));
        });

        // GET STORED INTERFACE DATA - /metrics/snmp/get-data
        snmpRouter.route(HttpMethod.GET, "/get-data").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer -> {

                eventBus.request(GET_EVENT, buffer.toJsonObject().put(TABLE_NAME, NETWORK_INTERFACE_TABLE), messageAsyncResult -> {

                    if(messageAsyncResult.succeeded())
                    {
                        routingContext.json(new JsonObject(messageAsyncResult.result().body().toString()).put(STATUS, SUCCESS));
                    }
                    else
                    {
                        routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, messageAsyncResult.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                    }
                });
            });

        });

        //--------------------------------------------------------------------------------------------------------------

        // CREATE CREDENTIAL PROFILE
        credentialRouter.route(HttpMethod.POST, "/add").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.request().bodyHandler(buffer -> {

                JsonObject reqJSON = buffer.toJsonObject().put(TABLE_NAME, CREDENTIAL_PROFILE_TABLE);

                eventBus.request(INSERT_EVENT, reqJSON, ar -> {

                    if(ar.succeeded())
                    {
                        if(ar.result().body().equals(SUCCESS))
                        {
                            ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profile added successfully!"));
                        }
                    }
                    else
                    {
                        ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Insertion Error").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                    }
                });
            });
        });

        // GET-ALL CREDENTIAL PROFILES
        credentialRouter.route(HttpMethod.GET, "/get-all").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            eventBus.request(GET_ALL_EVENT, new JsonObject().put(TABLE_NAME, CREDENTIAL_PROFILE_TABLE), ar -> {

                if(ar.succeeded())
                {
                    ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profiles fetched successfully!").put("result", ar.result().body()));
                }
                else
                {
                    ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                }
            });


        });

        // GET CREDENTIAL PROFILE
        credentialRouter.route(HttpMethod.GET, "/get/:credProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var credProfileId = ctx.request().getParam("credProfileId");

            eventBus.request(GET_EVENT, new JsonObject().put(TABLE_NAME, CREDENTIAL_PROFILE_TABLE).put(CREDENTIAL_PROFILE_ID, credProfileId), ar -> {

                if(ar.succeeded())
                {
                    ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profile fetched successfully!").put(RESULT, ar.result().body()));
                }
                else
                {
                    ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                }
            });


        });

        // UPDATE CREDENTIAL PROFILE
        credentialRouter.route(HttpMethod.PUT, "/update/:credProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var credProfileId = ctx.request().getParam("credProfileId");

            ctx.request().bodyHandler(buffer -> {

                JsonObject reqJSON = buffer.toJsonObject().put(CREDENTIAL_PROFILE_ID, credProfileId).put(TABLE_NAME, CREDENTIAL_PROFILE_TABLE);

                eventBus.request(UPDATE_EVENT, reqJSON, ar -> {

                    if(ar.succeeded())
                    {
                        ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profile updated successfully!").put("result", ar.result().body()));
                    }
                    else
                    {
                        ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error updating data").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                    }
                });
            });


        });

        // DELETE CREDENTIAL PROFILE
        credentialRouter.route(HttpMethod.DELETE, "/delete/:credProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var credProfileId = ctx.request().getParam("credProfileId");

            eventBus.request(DELETE_EVENT, new JsonObject().put(CREDENTIAL_PROFILE_ID, credProfileId).put(TABLE_NAME, CREDENTIAL_PROFILE_TABLE), ar -> {

                if(ar.succeeded())
                {
                    if(ar.result().body().equals(SUCCESS))
                    {
                        ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profile: " + credProfileId + " deleted successfully!"));
                    }
                }
                else
                {
                    ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Deletion Error").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                }
            });


        });

        //--------------------------------------------------------------------------------------------------------------

        // CREATE DISCOVERY PROFILE
        discoveryRouter.route(HttpMethod.POST, "/add").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.request().bodyHandler(buffer -> {

                JsonObject reqJSON = buffer.toJsonObject().put(TABLE_NAME, DISCOVERY_PROFILE_TABLE);

                eventBus.request(INSERT_EVENT, reqJSON, ar -> {

                    if(ar.succeeded())
                    {
                        if(ar.result().body().equals(SUCCESS))
                        {
                            ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile added successfully!"));
                        }
                    }
                    else
                    {
                        ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Insertion Error").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                    }
                });
            });


        });

        // GET ALL DISCOVERY PROFILES
        discoveryRouter.route(HttpMethod.GET, "/get-all").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            eventBus.request(GET_ALL_EVENT, new JsonObject().put(TABLE_NAME, DISCOVERY_PROFILE_TABLE), ar -> {

                if(ar.succeeded())
                {
                    ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profiles fetched successfully!").put("discovery.profiles", ar.result().body()));
                }
                else
                {
                    ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                }
            });

        });

        // GET DISCOVERY PROFILE
        discoveryRouter.route(HttpMethod.GET, "/get/:discProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var discProfileId = ctx.request().getParam("discProfileId");

            eventBus.request(GET_EVENT, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId).put(TABLE_NAME, DISCOVERY_PROFILE_TABLE), ar -> {

                if(ar.succeeded())
                {
                    ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile fetched successfully!").put(RESULT, ar.result().body()));
                }
                else
                {
                    ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                }
            });

        });

        // UPDATE DISCOVERY PROFILE
        discoveryRouter.route(HttpMethod.PUT, "/update/:discProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var discProfileId = ctx.request().getParam("discProfileId");

            ctx.request().bodyHandler(buffer -> {

                var reqJSON = buffer.toJsonObject().put(DISCOVERY_PROFILE_ID, discProfileId).put(TABLE_NAME, DISCOVERY_PROFILE_TABLE);

                eventBus.request(UPDATE_EVENT, reqJSON, ar -> {

                    if(ar.succeeded())
                    {
                        ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile updated successfully!").put("result", ar.result().body()));
                    }
                    else
                    {
                        ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error updating data").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                    }
                });
            });

        });

        // DELETE DISCOVERY PROFILE
        discoveryRouter.route(HttpMethod.DELETE, "/delete/:discProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var discProfileId = ctx.request().getParam("discProfileId");

            eventBus.request(DELETE_EVENT, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId).put(TABLE_NAME, DISCOVERY_PROFILE_TABLE), ar -> {

                if(ar.succeeded())
                {
                    if(ar.result().body().equals(SUCCESS))
                    {
                        ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile: " + discProfileId + " deleted successfully!"));
                    }
                    else
                    {
                        ctx.response().setStatusCode(404).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Deletion Error").put(ERR_STATUS_CODE, 404).put(ERR_MESSAGE, "Discovery profile: " + discProfileId + " not found!")).toString());
                    }
                }
                else
                {
                    ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Deletion Error").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                }
            });

        });

        //--------------------------------------------------------------------------------------------------------------

        // RUN DISCOVERY
        discoveryRouter.route(HttpMethod.POST, "/run").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

                /* Example:
                 [
                     {
                        "discovery.profile.id": 2,
                        "credentials": [1,2,3,4,5]
                     },
                     {
                        "discovery.profile.id": 3,
                        "credentials": [9,4,5]
                     },
                 ]
                */
            ctx.request().bodyHandler(buffer -> {

                var reqArray = buffer.toJsonArray();

                eventBus.request(MAKE_DISCOVERY_CONTEXT_EVENT, reqArray, ar -> {

                    if(ar.succeeded())
                    {
                        LOGGER.debug("context build success: {}", ar.result().body().toString());

                        String encodedString = Base64.getEncoder().encodeToString(ar.result().body().toString().getBytes());

                        LOGGER.trace("Execute Blocking initiated\t{}", encodedString);

                        eventBus.request(RUN_DISCOVERY_EVENT, encodedString, res -> {
                            ctx.json(new JsonArray(res.result().body().toString()));
                        });

                    }
                    else
                    {
                        LOGGER.debug(ar.cause().getMessage());

                        ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(ar.cause().getMessage());
                    }
                });
            });

        });

        //--------------------------------------------------------------------------------------------------------------

        // REQUEST TO PROVISION DEVICE
        provisionRouter.route(HttpMethod.POST, "/add/:discProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var discProfileId = ctx.request().getParam("discProfileId");

            eventBus.request(UPDATE_EVENT, new JsonObject().put(DISCOVERY_PROFILE_ID, Integer.parseInt(discProfileId)).put(TABLE_NAME, PROVISION_DEVICE), ar -> {

                if(ar.succeeded())
                {
                    ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Device provisioned successfully!"));
                }
                else
                {
                    ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error provisioning device").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                }

            });

        });

        // GET ALL PROVISION DEVICES LIST
        provisionRouter.route(HttpMethod.GET, "/get-all").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            eventBus.request(GET_ALL_EVENT, new JsonObject().put(TABLE_NAME, GET_PROVISIONED_DEVICES_EVENT), ar -> {
                if(ar.succeeded())
                {
                    ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Provisioned devices fetched successfully!").put(RESULT, ar.result().body()));
                }
                else
                {
                    ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                }
            });

        });

        // REMOVE PROVISION/ STOP POLLING
        provisionRouter.route(HttpMethod.DELETE, "/stop/:discProfileId").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            var discProfileId = ctx.request().getParam("discProfileId");

            eventBus.request(STOP_POLLING_EVENT, discProfileId, ar -> {
                if(ar.succeeded())
                {
                    ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Device provision stopped successfully!"));
                }
                else
                {
                    ctx.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error stopping provision").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())).toString());
                }
            });

        });

        server.requestHandler(router).listen(res -> {

            if(res.succeeded())
            {
                LOGGER.info(String.format("HTTP Server is now listening on http://%s:%d/", Config.HOST, Config.PORT));

                startPromise.complete();
            }
            else
            {
                LOGGER.info("Failed to start the API Engine, port unavailable!");

                startPromise.fail(res.cause());
            }
        });


    }
}
