package com.motadata.api;

import com.motadata.Bootstrap;
import com.motadata.constants.Constants;
import com.motadata.database.ConfigDB;
import com.motadata.utils.Utils;
import io.netty.handler.codec.http.HttpResponseStatus;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import static com.motadata.api.APIServer.LOGGER;

import static com.motadata.constants.Constants.*;

public class Credential
{
    private final Router credentialSubRouter;

    public Credential()
    {
        this.credentialSubRouter = Router.router(Bootstrap.getVertx());
    }

    public void init(Router router)
    {

        router.route("/credential/*").subRouter(credentialSubRouter);

        credentialSubRouter.route(HttpMethod.POST, URL_SEPARATOR).handler(this::addCredential);

        credentialSubRouter.route(HttpMethod.GET, URL_SEPARATOR).handler(this::getAllCredentials);

        credentialSubRouter.route(HttpMethod.GET, URL_SEPARATOR + COLON_SEPARATOR + CREDENTIAL_PROFILE_ID_PARAMS)
                .handler(this::getCredential);

        credentialSubRouter.route(HttpMethod.PUT, URL_SEPARATOR + COLON_SEPARATOR + CREDENTIAL_PROFILE_ID_PARAMS)
                .handler(this::updateCredential);

        credentialSubRouter.route(HttpMethod.DELETE, URL_SEPARATOR + COLON_SEPARATOR + CREDENTIAL_PROFILE_ID_PARAMS)
                .handler(this::deleteCredential);
    }

    private void addCredential(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer ->
            {

                var reqJSON = buffer.toJsonObject();

                var response = new JsonObject();

                if (reqJSON.containsKey(CREDENTIAL_NAME) && reqJSON.containsKey(VERSION) && reqJSON.containsKey(SNMP_COMMUNITY))
                {
                    if (Utils.validateRequestBody(reqJSON))
                    {
                        var object = new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE).put(DATA, reqJSON);

                        response = ConfigDB.create(object);

                        if (response.containsKey(ERROR))
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
                        response.put(STATUS, FAILED)
                                .put(ERROR, "Error in creating credential profile")
                                .put(ERR_MESSAGE, "Empty values are not allowed")
                                .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                    }
                }
                else
                {
                    response.put(STATUS, FAILED)
                            .put(ERROR, "Error in creating credential profile")
                            .put(ERR_MESSAGE, INVALID_REQUEST_BODY)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                }

                routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

            });

        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .putHeader(CONTENT_TYPE,APP_JSON)
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    private void getAllCredentials(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            var response = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE));

            if (response.getJsonArray(RESULT).isEmpty())
            {
                response.remove(RESULT);

                response.put(STATUS, FAILED)
                        .put(ERROR, "No credential profiles found")
                        .put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase())
                        .put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
            }
            else
            {
                response.put(STATUS, SUCCESS);
            }

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .putHeader(CONTENT_TYPE,APP_JSON)
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    private void getCredential(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            var credProfileId = routingContext.request().getParam(CREDENTIAL_PROFILE_ID_PARAMS);

            var response = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE)
                    .put(DATA, new JsonObject().put(CREDENTIAL_PROFILE_ID, credProfileId)));

            if (response.isEmpty())
            {
                response.put(STATUS, FAILED)
                        .put(ERROR, "No credential profile found for ID: " + credProfileId)
                        .put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase())
                        .put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
            }
            else
            {
                response.put(STATUS, SUCCESS);
            }

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .putHeader(CONTENT_TYPE,APP_JSON)
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    private void updateCredential(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            var credProfileId = routingContext.request().getParam(CREDENTIAL_PROFILE_ID_PARAMS);

            routingContext.request().bodyHandler(buffer ->
            {

                var response = new JsonObject();

                var reqJSON = buffer.toJsonObject().put(CREDENTIAL_PROFILE_ID, credProfileId);

                if (reqJSON.containsKey(VERSION) && reqJSON.containsKey(SNMP_COMMUNITY))
                {
                    if (Utils.validateRequestBody(reqJSON))
                    {
                        var object = new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE).put(DATA, reqJSON);

                        response = ConfigDB.update(object);

                        if (response.containsKey(ERROR) || response.isEmpty())
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
                        response.put(STATUS, FAILED)
                                .put(ERROR, "Error in updating credential profile")
                                .put(ERR_MESSAGE, "Empty values are not allowed")
                                .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                    }
                }
                else
                {
                    response.put(STATUS, FAILED)
                            .put(ERROR, "Error in updating credential profile")
                            .put(ERR_MESSAGE, "Either version or community field not available!")
                            .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                }

                routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());
            });

        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .putHeader(CONTENT_TYPE,APP_JSON)
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    private void deleteCredential(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            var credProfileId = routingContext.request().getParam(CREDENTIAL_PROFILE_ID_PARAMS);

            var response = new JsonObject();

            if (Utils.getCounter(Long.parseLong(credProfileId)) == 0)
            {
                response = ConfigDB.delete(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE)
                        .put(DATA, new JsonObject().put(CREDENTIAL_PROFILE_ID, credProfileId)));

                if (response.isEmpty())
                {
                    response.put(STATUS, FAILED)
                            .put(ERROR, "No credential profile found for ID: " + credProfileId)
                            .put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase())
                            .put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
                }
                else if (response.containsKey(ERROR))
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
                response.put(STATUS, FAILED)
                        .put(ERROR, "Credential profile is currently in use: " + credProfileId)
                        .put(ERR_MESSAGE, HttpResponseStatus.FORBIDDEN.reasonPhrase())
                        .put(ERR_STATUS_CODE, HttpResponseStatus.FORBIDDEN.code());
            }

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .putHeader(CONTENT_TYPE,APP_JSON)
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

}
