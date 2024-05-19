package com.motadata.api.handler;

import com.motadata.utils.Utils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.motadata.api.ApiRouter.LOGGER;
import static com.motadata.contants.Constants.*;

public class CredentialHandler implements RoutesHandler
{
    private final EventBus eventBus;

    private final Vertx vertx;

    public CredentialHandler(Vertx vertx, EventBus eventBus)
    {
        this.vertx = vertx;

        this.eventBus = eventBus;
    }

    @Override
    public void setupRoutes(Router router)
    {

        router.route(HttpMethod.POST, CREDENTIAL_ROUTE).handler(this::addCredential);

        router.route(HttpMethod.GET, CREDENTIAL_ROUTE).handler(this::getAllCredentials);

        router.route(HttpMethod.GET, CREDENTIAL_ROUTE + URL_SEPARATOR + COLON_SEPARATOR + CREDENTIAL_PROFILE_ID_PARAMS).handler(this::getCredential);

        router.route(HttpMethod.PUT, CREDENTIAL_ROUTE + URL_SEPARATOR + COLON_SEPARATOR + CREDENTIAL_PROFILE_ID_PARAMS).handler(this::updateCredential);

        router.route(HttpMethod.DELETE, CREDENTIAL_ROUTE + URL_SEPARATOR + COLON_SEPARATOR + CREDENTIAL_PROFILE_ID_PARAMS).handler(this::deleteCredential);
    }

    public void addCredential(RoutingContext routingContext)
    {
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
    }

    public void getAllCredentials(RoutingContext routingContext)
    {
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

    }

    public void getCredential(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var credProfileId = routingContext.request().getParam(CREDENTIAL_PROFILE_ID_PARAMS);

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

    }

    public void updateCredential(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var credProfileId = routingContext.request().getParam(CREDENTIAL_PROFILE_ID_PARAMS);

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


    }

    public void deleteCredential(RoutingContext routingContext)
    {

        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var credProfileId = routingContext.request().getParam(CREDENTIAL_PROFILE_ID_PARAMS);

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


    }
}
