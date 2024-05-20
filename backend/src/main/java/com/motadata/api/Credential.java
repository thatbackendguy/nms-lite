package com.motadata.api;

import com.motadata.Bootstrap;
import com.motadata.database.ConfigDB;
import com.motadata.utils.Utils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.motadata.api.APIServer.LOGGER;
import static com.motadata.constants.Constants.*;

public class Credential
{
    private final EventBus eventBus;

    private final Vertx vertx;

    private final Router subRouter;

    public Credential()
    {
        this.vertx = Bootstrap.getVertx();

        this.eventBus = Bootstrap.getVertx().eventBus();

        this.subRouter = Router.router(vertx);
    }

    public void init(Router router)
    {

        router.route("/credential/*").subRouter(subRouter);

        subRouter.route(HttpMethod.POST, URL_SEPARATOR).handler(this::addCredential);

        subRouter.route(HttpMethod.GET, URL_SEPARATOR).handler(this::getAllCredentials);

        subRouter.route(HttpMethod.GET, URL_SEPARATOR + COLON_SEPARATOR + CREDENTIAL_PROFILE_ID_PARAMS).handler(this::getCredential);

        subRouter.route(HttpMethod.PUT, URL_SEPARATOR + COLON_SEPARATOR + CREDENTIAL_PROFILE_ID_PARAMS).handler(this::updateCredential);

        subRouter.route(HttpMethod.DELETE, URL_SEPARATOR + COLON_SEPARATOR + CREDENTIAL_PROFILE_ID_PARAMS).handler(this::deleteCredential);
    }

    public void addCredential(RoutingContext routingContext)
    {
        LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        routingContext.request().bodyHandler(buffer -> {

            var reqJSON = buffer.toJsonObject();

            var response = new JsonObject();

            if(reqJSON.containsKey(CREDENTIAL_NAME) && reqJSON.containsKey(VERSION) && reqJSON.containsKey(SNMP_COMMUNITY))
            {
                if(Utils.validateRequestBody(reqJSON))
                {
                    var object = new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE).put(DATA, reqJSON);

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
                    response.put(STATUS, FAILED).put(ERROR, "Error in creating credential profile").put(ERR_MESSAGE, "Empty values are not allowed").put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                }
            }
            else
            {
                response.put(STATUS, FAILED).put(ERROR, "Error in creating credential profile").put(ERR_MESSAGE, INVALID_REQUEST_BODY).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
            }

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

        });
    }

    public void getAllCredentials(RoutingContext routingContext)
    {
        LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var response = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE));

        if(response.getJsonArray(RESULT).isEmpty())
        {
            response.remove(RESULT);

            response.put(STATUS, FAILED).put(ERROR, "No credential profiles found").put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase()).put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
        }
        else
        {
            response.put(STATUS, SUCCESS);
        }

        routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());
    }

    public void getCredential(RoutingContext routingContext)
    {
        LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var credProfileId = routingContext.request().getParam(CREDENTIAL_PROFILE_ID_PARAMS);

        var response = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE).put(DATA, new JsonObject().put(CREDENTIAL_PROFILE_ID, credProfileId)));

        if(response.isEmpty())
        {
            response.put(STATUS, FAILED).put(ERROR, "No credential profile found for ID: " + credProfileId).put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase()).put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
        }
        else
        {
            response.put(STATUS, SUCCESS);
        }

        routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());
    }

    public void updateCredential(RoutingContext routingContext)
    {
        LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var credProfileId = routingContext.request().getParam(CREDENTIAL_PROFILE_ID_PARAMS);

        routingContext.request().bodyHandler(buffer -> {

            var response = new JsonObject();

            var reqJSON = buffer.toJsonObject().put(CREDENTIAL_PROFILE_ID, credProfileId);

            if(reqJSON.containsKey(VERSION) && reqJSON.containsKey(SNMP_COMMUNITY))
            {
                if(Utils.validateRequestBody(reqJSON))
                {
                    var object = new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE).put(DATA, reqJSON);

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
                    response.put(STATUS, FAILED).put(ERROR, "Error in updating credential profile").put(ERR_MESSAGE, "Empty values are not allowed").put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                }
            }
            else
            {
                response.put(STATUS, FAILED).put(ERROR, "Error in updating credential profile").put(ERR_MESSAGE, "Either version or community field not available!").put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
            }

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());
        });
    }

    public void deleteCredential(RoutingContext routingContext)
    {
        LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var credProfileId = routingContext.request().getParam(CREDENTIAL_PROFILE_ID_PARAMS);

        var response = ConfigDB.delete(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE).put(DATA, new JsonObject().put(CREDENTIAL_PROFILE_ID, credProfileId)));

        if(response.isEmpty())
        {
            response.put(STATUS, FAILED).put(ERROR, "No credential profile found for ID: " + credProfileId).put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase()).put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
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
}
