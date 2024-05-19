package com.motadata.api;

import com.motadata.Bootstrap;
import com.motadata.database.ConfigDB;
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

import static com.motadata.api.APIServer.LOGGER;

import static com.motadata.contants.Constants.*;

public class Discovery
{
    private final EventBus eventBus;

    private final Vertx vertx;

    private final Router subRouter;

    public Discovery()
    {
        this.vertx = Bootstrap.getVertx();

        this.eventBus = Bootstrap.getVertx().eventBus();

        this.subRouter = Router.router(vertx);
    }

    public void init(Router router)
    {
        router.route("/discovery/*").subRouter(subRouter);

        subRouter.route(HttpMethod.POST, URL_SEPARATOR).handler(this::addDiscovery);

        subRouter.route(HttpMethod.GET, URL_SEPARATOR).handler(this::getAllDiscoveries);

        subRouter.route(HttpMethod.GET,   URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::getDiscovery);

        subRouter.route(HttpMethod.GET,   URL_SEPARATOR + RUN + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::runDiscovery);

        subRouter.route(HttpMethod.PUT,   URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::updateDiscovery);

        subRouter.route(HttpMethod.DELETE,   URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::deleteDiscovery);

        subRouter.route(HttpMethod.GET,   URL_SEPARATOR + PROVISION + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::provisionDevice);

        subRouter.route(HttpMethod.GET,   URL_SEPARATOR + PROVISION).handler(this::getProvisionedDevices);

        subRouter.route(HttpMethod.DELETE,   URL_SEPARATOR + PROVISION + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::unProvisionDevice);
    }

    public void addDiscovery(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        routingContext.request().bodyHandler(buffer -> {

            var reqJSON = buffer.toJsonObject();

            var response = new JsonObject();

            if(reqJSON.containsKey(DISCOVERY_NAME) && reqJSON.containsKey(OBJECT_IP) && reqJSON.containsKey(PORT))
            {
                if(Utils.validateRequestBody(reqJSON))
                {
                    var object = new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, reqJSON);

                    response = ConfigDB.create(object);

                    if(response.containsKey(ERROR))
                    {
                        response.put(STATUS, FAILED);
                    }
                    else
                    {
                        response.put(STATUS, SUCCESS);
                    }
                }
                else
                {
                    response.put(STATUS, FAILED).put(ERROR, "Error in creating discovery profile").put(ERR_MESSAGE, "Empty values are not allowed").put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                }
            }
            else
            {
                response.put(STATUS, FAILED).put(ERROR, "Error in creating discovery profile").put(ERR_MESSAGE, INVALID_REQUEST_BODY).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
            }

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

        });

    }

    public void getAllDiscoveries(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var response = ConfigDB.read(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE));

        if(response.getJsonArray(RESULT).isEmpty())
        {
            response.remove(RESULT);

            response.put(STATUS, FAILED).put(ERROR, "No discovery profiles found").put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase()).put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
        }
        else
        {
            response.put(STATUS, SUCCESS);
        }

        routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());
    }

    public void getDiscovery(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

        var response = ConfigDB.read(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId)));

        if(response.isEmpty())
        {
            response.put(STATUS, FAILED).put(ERROR, "No discovery profile found for ID: " + discProfileId).put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase()).put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
        }
        else
        {
            response.put(STATUS, SUCCESS);
        }

        routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());
    }

    public void updateDiscovery(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

        routingContext.request().bodyHandler(buffer -> {

            var response = new JsonObject();

            var reqJSON = buffer.toJsonObject().put(DISCOVERY_PROFILE_ID, discProfileId);

            if(reqJSON.containsKey(OBJECT_IP) && reqJSON.containsKey(PORT))
            {
                if(Utils.validateRequestBody(reqJSON))
                {
                    var object = new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, reqJSON);

                    response = ConfigDB.update(object);

                    if(response.containsKey(ERROR) || response.isEmpty())
                    {
                        response.put(STATUS, FAILED);
                    }
                    else
                    {
                        response.put(STATUS, SUCCESS);
                    }
                }
                else
                {
                    response.put(STATUS, FAILED).put(ERROR, "Error in updating discovery profile").put(ERR_MESSAGE, "Empty values are not allowed").put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                }
            }
            else
            {
                response.put(STATUS, FAILED).put(ERROR, "Error in updating discovery profile").put(ERR_MESSAGE, "Either IP or port field not available!").put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
            }

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());
        });

    }

    public void deleteDiscovery(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

        var response = ConfigDB.delete(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId)));

        if(response.isEmpty())
        {
            response.put(STATUS, FAILED).put(ERROR, "No discovery profile found for ID: " + discProfileId).put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase()).put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
        }
        else if(response.containsKey(ERROR))
        {
            response.put(STATUS, FAILED);
        }
        else
        {
            response.put(STATUS, SUCCESS);
        }

        routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());
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
