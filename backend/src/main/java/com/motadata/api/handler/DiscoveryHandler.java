package com.motadata.api.handler;

import com.motadata.utils.Utils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Base64;

import static com.motadata.api.ApiRouter.LOGGER;

import static com.motadata.contants.Constants.*;

public class DiscoveryHandler implements RoutesHandler
{
    private final EventBus eventBus;

    private final Vertx vertx;

    public DiscoveryHandler(Vertx vertx, EventBus eventBus)
    {
        this.vertx = vertx;

        this.eventBus = eventBus;
    }

    @Override
    public void setupRoutes(Router router)
    {

        router.route(HttpMethod.POST, DISCOVERY_ROUTE).handler(this::addDiscovery);

        router.route(HttpMethod.GET, DISCOVERY_ROUTE).handler(this::getAllDiscoveries);

        router.route(HttpMethod.GET, DISCOVERY_ROUTE + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::getDiscovery);

        router.route(HttpMethod.GET, DISCOVERY_ROUTE + URL_SEPARATOR + RUN + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::runDiscovery);

        router.route(HttpMethod.PUT, DISCOVERY_ROUTE + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::updateDiscovery);

        router.route(HttpMethod.DELETE, DISCOVERY_ROUTE + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::deleteDiscovery);

        router.route(HttpMethod.GET, DISCOVERY_ROUTE + URL_SEPARATOR + PROVISION + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::provisionDevice);

        router.route(HttpMethod.GET, DISCOVERY_ROUTE + URL_SEPARATOR + PROVISION).handler(this::getProvisionedDevices);

        router.route(HttpMethod.DELETE, DISCOVERY_ROUTE + URL_SEPARATOR + PROVISION + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::unProvisionDevice);
    }

    public void addDiscovery(RoutingContext routingContext)
    {
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

    }

    public void getAllDiscoveries(RoutingContext routingContext)
    {

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

    }

    public void getDiscovery(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

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

    }

    public void updateDiscovery(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

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

    }

    public void deleteDiscovery(RoutingContext routingContext)
    {

        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

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

    }

    public void runDiscovery(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

                /* Example:
                 [{"discovery.profile.id": 2,"credentials": [1,2,3,4,5]},{"discovery.profile.id": 3,"credentials": [9,4,5]}
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

    }

    public void provisionDevice(RoutingContext routingContext)
    {

        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

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

    }

    public void unProvisionDevice(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

        eventBus.request(UNPROVISION_DEVICE, discProfileId, ar -> {
            if(ar.succeeded())
            {
                routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Device provision stopped successfully!"));
            }
            else
            {
                routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error stopping provision").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
            }
        });
    }

    public void getProvisionedDevices(RoutingContext routingContext)
    {

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

    }
}
