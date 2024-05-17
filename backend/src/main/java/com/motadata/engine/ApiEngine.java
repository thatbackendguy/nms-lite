package com.motadata.engine;

import com.motadata.utils.Config;
import com.motadata.utils.Utils;
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

        var credential = Router.router(vertx);

        var discovery = Router.router(vertx);

        var provision = Router.router(vertx);

        var metrics = Router.router(vertx);

        var snmp = Router.router(vertx);

        // FOR HANDLING FAILURES
        router.route().failureHandler(errorHandler());

        //--------------------------------------------------------------------------------------------------------------

        // CREDENTIAL PROFILE SUB-ROUTER
        router.route("/credential/*").subRouter(credential);

        // DISCOVERY PROFILE SUB-ROUTER
        router.route("/discovery/*").subRouter(discovery);

        // PROVISION SUB-ROUTER
        router.route("/provision/*").subRouter(provision);

        // METRICS SUB-ROUTER
        router.route("/metrics/*").subRouter(metrics);

        // NETWORK DEVICE - SNMP
        metrics.route("/snmp/*").subRouter(snmp);

        //--------------------------------------------------------------------------------------------------------------

        // GET: "/"
        router.route("/").handler(ctx -> {

            LOGGER.info(REQ_CONTAINER, ctx.request().method(), ctx.request().path(), ctx.request().remoteAddress());

            ctx.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Welcome to Network Monitoring System!"));
        });

        // GET STORED INTERFACE DATA - /metrics/snmp/get-data
        snmp.route(HttpMethod.GET, "/get-data").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer -> {

                var requestObject = buffer.toJsonObject().put(TABLE_NAME, NETWORK_INTERFACE_TABLE).put(EVENT_NAME, GET_INTERFACE_METRICS);

                if(Utils.validateRequestBody(requestObject))
                {
                    eventBus.request(GET_EVENT, requestObject, messageAsyncResult -> {

                        if(messageAsyncResult.succeeded())
                        {
                            routingContext.json(new JsonObject(messageAsyncResult.result().body().toString()).put(STATUS, SUCCESS));
                        }
                        else
                        {
                            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, messageAsyncResult.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                        }
                    });
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, INVALID_REQUEST_BODY).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }

            });

        });

        // GET AVAILABLE INTERFACES - /metrics/snmp/get-interfaces
        snmp.route(HttpMethod.GET, "/get-interfaces").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer -> {

                var requestObject = buffer.toJsonObject().put(TABLE_NAME, NETWORK_INTERFACE_TABLE).put(EVENT_NAME, GET_INTERFACES);

                if(Utils.validateRequestBody(requestObject))
                {
                    eventBus.request(GET_EVENT, requestObject, messageAsyncResult -> {

                        if(messageAsyncResult.succeeded())
                        {
                            routingContext.json(new JsonObject(messageAsyncResult.result().body().toString()).put(STATUS, SUCCESS));
                        }
                        else
                        {
                            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, messageAsyncResult.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                        }
                    });
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, INVALID_REQUEST_BODY).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }

            });

        });

        //--------------------------------------------------------------------------------------------------------------

        // CREATE CREDENTIAL PROFILE
        credential.route(HttpMethod.POST, "/add").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer -> {

                var reqJSON = buffer.toJsonObject().put(TABLE_NAME, CREDENTIAL_PROFILE_TABLE);

                if(Utils.validateRequestBody(reqJSON))
                {
                    eventBus.request(INSERT_EVENT, reqJSON, asyncResult -> {

                        if(asyncResult.succeeded())
                        {
                            var resultObj = new JsonObject(asyncResult.result().body().toString());

                            routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, String.format("Credential profile added successfully! ID: %d", resultObj.getInteger(CREDENTIAL_PROFILE_ID))));
                        }
                        else
                        {
                            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Insertion Error").put(ERR_MESSAGE, asyncResult.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                        }
                    });
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, INVALID_REQUEST_BODY).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }

            });
        });

        // GET-ALL CREDENTIAL PROFILES
        credential.route(HttpMethod.GET, "/get-all").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            eventBus.request(GET_ALL_EVENT, new JsonObject().put(TABLE_NAME, CREDENTIAL_PROFILE_TABLE), ar -> {

                if(ar.succeeded())
                {
                    routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profiles fetched successfully!").put("result", ar.result().body()));
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }
            });


        });

        // GET CREDENTIAL PROFILE
        credential.route(HttpMethod.GET, "/get/:credProfileId").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var credProfileId = routingContext.request().getParam("credProfileId");

            eventBus.request(GET_EVENT, new JsonObject().put(TABLE_NAME, CREDENTIAL_PROFILE_TABLE).put(CREDENTIAL_PROFILE_ID, credProfileId), ar -> {

                if(ar.succeeded())
                {
                    routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profile fetched successfully!").put(RESULT, ar.result().body()));
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }
            });


        });

        // UPDATE CREDENTIAL PROFILE
        credential.route(HttpMethod.PUT, "/update/:credProfileId").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var credProfileId = routingContext.request().getParam("credProfileId");

            routingContext.request().bodyHandler(buffer -> {

                var reqJSON = buffer.toJsonObject().put(CREDENTIAL_PROFILE_ID, credProfileId).put(TABLE_NAME, CREDENTIAL_PROFILE_TABLE);

                if(Utils.validateRequestBody(reqJSON))
                {
                    eventBus.request(UPDATE_EVENT, reqJSON, ar -> {

                        if(ar.succeeded())
                        {
                            routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profile updated successfully!").put("result", ar.result().body()));
                        }
                        else
                        {
                            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error updating data").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                        }
                    });
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, INVALID_REQUEST_BODY).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }

            });


        });

        // DELETE CREDENTIAL PROFILE
        credential.route(HttpMethod.DELETE, "/delete/:credProfileId").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var credProfileId = routingContext.request().getParam("credProfileId");

            eventBus.request(DELETE_EVENT, new JsonObject().put(CREDENTIAL_PROFILE_ID, credProfileId).put(TABLE_NAME, CREDENTIAL_PROFILE_TABLE), ar -> {

                if(ar.succeeded())
                {
                    if(ar.result().body().equals(SUCCESS))
                    {
                        routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Credential profile: " + credProfileId + " deleted successfully!"));
                    }
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Deletion Error").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }
            });


        });

        //--------------------------------------------------------------------------------------------------------------

        // CREATE DISCOVERY PROFILE
        discovery.route(HttpMethod.POST, "/add").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer -> {

                var reqJSON = buffer.toJsonObject().put(TABLE_NAME, DISCOVERY_PROFILE_TABLE);

                if(Utils.validateRequestBody(reqJSON))
                {

                    eventBus.request(INSERT_EVENT, reqJSON, asyncResult -> {

                        if(asyncResult.succeeded())
                        {
                            var resultObj = new JsonObject(asyncResult.result().body().toString());

                            routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, String.format("Discovery profile added successfully! ID: %d", resultObj.getInteger(DISCOVERY_PROFILE_ID))));
                        }
                        else
                        {
                            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Insertion Error").put(ERR_MESSAGE, asyncResult.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                        }
                    });
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, INVALID_REQUEST_BODY).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }
            });


        });

        // GET ALL DISCOVERY PROFILES
        discovery.route(HttpMethod.GET, "/get-all").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            eventBus.request(GET_ALL_EVENT, new JsonObject().put(TABLE_NAME, DISCOVERY_PROFILE_TABLE), ar -> {

                if(ar.succeeded())
                {
                    routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profiles fetched successfully!").put("discovery.profiles", ar.result().body()));
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }
            });

        });

        // GET DISCOVERY PROFILE
        discovery.route(HttpMethod.GET, "/get/:discProfileId").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam("discProfileId");

            eventBus.request(GET_EVENT, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId).put(TABLE_NAME, DISCOVERY_PROFILE_TABLE), asyncResult -> {

                if(asyncResult.succeeded())
                {
                    routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile fetched successfully!").put(RESULT, asyncResult.result().body()));
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, asyncResult.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }
            });

        });

        // UPDATE DISCOVERY PROFILE
        discovery.route(HttpMethod.PUT, "/update/:discProfileId").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam("discProfileId");

            routingContext.request().bodyHandler(buffer -> {

                var reqJSON = buffer.toJsonObject().put(DISCOVERY_PROFILE_ID, discProfileId).put(TABLE_NAME, DISCOVERY_PROFILE_TABLE);

                if(Utils.validateRequestBody(reqJSON))
                {
                    eventBus.request(UPDATE_EVENT, reqJSON, asyncResult -> {

                        if(asyncResult.succeeded())
                        {
                            routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile updated successfully!").put("result", asyncResult.result().body()));
                        }
                        else
                        {
                            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error updating data").put(ERR_MESSAGE, asyncResult.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                        }
                    });
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, INVALID_REQUEST_BODY).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }
            });

        });

        // DELETE DISCOVERY PROFILE
        discovery.route(HttpMethod.DELETE, "/delete/:discProfileId").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam("discProfileId");

            eventBus.request(DELETE_EVENT, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId).put(TABLE_NAME, DISCOVERY_PROFILE_TABLE), asyncResult -> {

                if(asyncResult.succeeded())
                {
                    if(asyncResult.result().body().equals(SUCCESS))
                    {
                        routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery profile: " + discProfileId + " deleted successfully!"));
                    }
                    else
                    {
                        routingContext.response().setStatusCode(404).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Deletion Error").put(ERR_STATUS_CODE, 404).put(ERR_MESSAGE, "Discovery profile: " + discProfileId + " not found!")).toString());
                    }
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Deletion Error").put(ERR_MESSAGE, asyncResult.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }
            });

        });

        //--------------------------------------------------------------------------------------------------------------

        // RUN DISCOVERY
        discovery.route(HttpMethod.POST, "/run").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

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
            routingContext.request().bodyHandler(buffer -> {

                var reqArray = buffer.toJsonArray();

                eventBus.request(MAKE_DISCOVERY_CONTEXT_EVENT, reqArray, ar -> {

                    if(ar.succeeded())
                    {
                        LOGGER.debug("context build success: {}", ar.result().body().toString());

                        String encodedString = Base64.getEncoder().encodeToString(ar.result().body().toString().getBytes());

                        LOGGER.trace("Execute Blocking initiated\t{}", encodedString);

                        eventBus.request(RUN_DISCOVERY_EVENT, encodedString, res -> {
                            routingContext.json(new JsonArray(res.result().body().toString()));
                        });

                    }
                    else
                    {
                        LOGGER.debug(ar.cause().getMessage());

                        routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(ar.cause().getMessage());
                    }
                });
            });

        });

        //--------------------------------------------------------------------------------------------------------------

        // REQUEST TO PROVISION DEVICE
        provision.route(HttpMethod.POST, "/add/:discProfileId").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam("discProfileId");

            eventBus.request(UPDATE_EVENT, new JsonObject().put(DISCOVERY_PROFILE_ID, Integer.parseInt(discProfileId)).put(TABLE_NAME, PROVISION_DEVICE), ar -> {

                if(ar.succeeded())
                {
                    routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Device provisioned successfully!"));
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error provisioning device").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }

            });

        });

        // GET ALL PROVISION DEVICES LIST
        provision.route(HttpMethod.GET, "/get-all").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            eventBus.request(GET_ALL_EVENT, new JsonObject().put(TABLE_NAME, GET_PROVISIONED_DEVICES_EVENT), ar -> {
                if(ar.succeeded())
                {
                    routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Provisioned devices fetched successfully!").put(RESULT, ar.result().body()));
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }
            });

        });

        // REMOVE PROVISION/ STOP POLLING
        provision.route(HttpMethod.DELETE, "/stop/:discProfileId").handler(routingContext -> {

            LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam("discProfileId");

            eventBus.request(STOP_POLLING_EVENT, discProfileId, ar -> {
                if(ar.succeeded())
                {
                    routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Device provision stopped successfully!"));
                }
                else
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error stopping provision").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                }
            });

        });

        server.requestHandler(router).listen(httpServerAsyncResult -> {

            if(httpServerAsyncResult.succeeded())
            {
                LOGGER.info(String.format("HTTP Server is now listening on http://%s:%d/", Config.HOST, Config.PORT));

                startPromise.complete();
            }
            else
            {
                LOGGER.info("Failed to start the API Engine, port unavailable!");

                startPromise.fail(httpServerAsyncResult.cause());
            }
        });


    }
}
