package com.motadata.api;

import com.motadata.Bootstrap;
import com.motadata.constants.Constants;
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

import static com.motadata.constants.Constants.*;

public class Discovery extends APIServer
{

    private final EventBus eventBus;

    private final Vertx vertx;

    protected final Router discoverySubRouter;

    public Discovery()
    {

        this.vertx = Bootstrap.getVertx();

        this.eventBus = Bootstrap.getVertx().eventBus();

        this.discoverySubRouter = Router.router(vertx);
    }

    public void init()
    {

        router.route("/discovery/*").subRouter(discoverySubRouter);

        new Provision().init();

        discoverySubRouter.route(HttpMethod.POST, URL_SEPARATOR).handler(this::addDiscovery);

        discoverySubRouter.route(HttpMethod.GET, URL_SEPARATOR).handler(this::getAllDiscoveries);

        discoverySubRouter.route(HttpMethod.GET, URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS)
                .handler(this::getDiscovery);

        discoverySubRouter.route(HttpMethod.PUT, URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS)
                .handler(this::updateDiscovery);

        discoverySubRouter.route(HttpMethod.DELETE, URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS)
                .handler(this::deleteDiscovery);

        discoverySubRouter.route(HttpMethod.POST, URL_SEPARATOR + RUN).handler(this::runDiscovery);

        discoverySubRouter.route(HttpMethod.GET, URL_SEPARATOR + RESULT + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS)
                .handler(this::getDiscoveryResult);

    }

    private void addDiscovery(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer ->
            {

                var requestBody = buffer.toJsonObject();

                var response = new JsonObject();

                if (requestBody.containsKey(DISCOVERY_NAME) && requestBody.containsKey(OBJECT_IP) && requestBody.containsKey(PORT) && requestBody.containsKey(CREDENTIALS))
                {
                    try
                    {
                        if (Utils.validateRequestBody(requestBody) && requestBody.getInteger(PORT) > 0)
                        {
                            var areCredentialsValid = false;

                            for (var credentialProfileId : requestBody.getJsonArray(CREDENTIALS))
                            {
                                var entries = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE)
                                        .put(DATA, new JsonObject().put(CREDENTIAL_PROFILE_ID, credentialProfileId)));

                                if (entries.containsKey(RESULT))
                                {
                                    areCredentialsValid = true;
                                }
                                else
                                {
                                    areCredentialsValid = false;

                                    break;
                                }

                            }

                            if (areCredentialsValid)
                            {
                                var object = new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE)
                                        .put(DATA, requestBody);

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
                                        .put(ERROR, "Error in creating discovery profile")
                                        .put(ERR_MESSAGE, "One or more credential profiles not found!")
                                        .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                            }

                        }
                        else
                        {
                            response.put(STATUS, FAILED)
                                    .put(ERROR, "Error in creating discovery profile")
                                    .put(ERR_MESSAGE, "Empty/negative values are not allowed")
                                    .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                        }
                    }
                    catch (ClassCastException | NumberFormatException exception)
                    {
                        response.put(STATUS, FAILED)
                                .put(ERROR, "Error in creating discovery profile")
                                .put(ERR_MESSAGE, "Port must be number")
                                .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                    }
                }
                else
                {
                    response.put(STATUS, FAILED)
                            .put(ERROR, "Error in creating discovery profile")
                            .put(ERR_MESSAGE, INVALID_REQUEST_BODY)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                }

                routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

            });

        }
        catch (Exception exception)
        {
            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());

            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());
        }
    }

    private void getAllDiscoveries(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            var response = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE));

            if (response.getJsonArray(RESULT).isEmpty())
            {
                response.remove(RESULT);

                response.put(STATUS, FAILED)
                        .put(ERROR, "No discovery profiles found")
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

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }

    }

    private void getDiscovery(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

            var response = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE)
                    .put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId)));

            if (response.isEmpty())
            {
                response.put(STATUS, FAILED)
                        .put(ERROR, "No discovery profile found for ID: " + discProfileId)
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

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    private void updateDiscovery(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

            routingContext.request().bodyHandler(buffer ->
            {

                var response = new JsonObject();

                var requestBody = buffer.toJsonObject().put(DISCOVERY_PROFILE_ID, discProfileId);

                if (requestBody.containsKey(OBJECT_IP) && requestBody.containsKey(PORT))
                {
                    try
                    {
                        if (Utils.validateRequestBody(requestBody) && requestBody.getInteger(PORT) > 0)
                        {
                            var object = new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, requestBody);

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
                                    .put(ERROR, "Error in updating discovery profile")
                                    .put(ERR_MESSAGE, "Empty values are not allowed")
                                    .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                        }
                    }
                    catch (ClassCastException | NumberFormatException exception)
                    {
                        response.put(STATUS, FAILED)
                                .put(ERROR, "Error in creating discovery profile")
                                .put(ERR_MESSAGE, "Port must be number")
                                .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                    }
                }
                else
                {
                    response.put(STATUS, FAILED)
                            .put(ERROR, "Error in updating discovery profile")
                            .put(ERR_MESSAGE, "Either IP or port field not available!")
                            .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
                }

                routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

            });

        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }

    }

    private void deleteDiscovery(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

            var response = ConfigDB.delete(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE)
                    .put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId)));

            if (response.isEmpty())
            {
                response.put(STATUS, FAILED)
                        .put(ERROR, "No discovery profile found for ID: " + discProfileId)
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

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    private void runDiscovery(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer ->
            {

                var requestObjects = buffer.toJsonArray();

                var contexts = new JsonArray();

                vertx.executeBlocking(handler ->
                {

                    for (var object : requestObjects)
                    {
                        var monitor = new JsonObject(object.toString());

                        var discoveryProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE)
                                        .put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, monitor.getString(DISCOVERY_PROFILE_ID))))
                                .getJsonObject(RESULT)
                                .put(DISCOVERY_PROFILE_ID, monitor.getString(DISCOVERY_PROFILE_ID));

                        if (discoveryProfile.containsKey(RESULT) && discoveryProfile.containsKey(IS_DISCOVERED))
                        {
                            discoveryProfile.remove(RESULT);

                            discoveryProfile.remove(IS_DISCOVERED);
                        }

                        if (!discoveryProfile.isEmpty() && Utils.pingCheck(discoveryProfile.getString(OBJECT_IP)))
                        {
                            discoveryProfile.put(REQUEST_TYPE, DISCOVERY).put(PLUGIN_NAME, NETWORK);

                            var credentialProfileIds = discoveryProfile.getJsonArray(CREDENTIALS);

                            var credentialProfiles = new JsonArray();

                            for (var credentialId : credentialProfileIds)
                            {
                                var credentialProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE)
                                                .put(DATA, new JsonObject().put(CREDENTIAL_PROFILE_ID, credentialId)))
                                        .getJsonObject(RESULT)
                                        .put(CREDENTIAL_PROFILE_ID, credentialId);

                                credentialProfiles.add(credentialProfile);
                            }

                            discoveryProfile.put(CREDENTIALS, credentialProfiles);

                            contexts.add(discoveryProfile);
                        }
                    }

                    var encodedString = Base64.getEncoder().encodeToString(contexts.toString().getBytes());

                    eventBus.send(RUN_DISCOVERY_EVENT, encodedString);

                    handler.complete();

                    LOGGER.trace("Discovery context sent to discovery engine");

                });

                routingContext.response()
                        .putHeader(CONTENT_TYPE, APP_JSON)
                        .end(new JsonObject().put(STATUS, SUCCESS)
                                .put(MESSAGE, "Discovery ran successfully!")
                                .toString());
            });

        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    private void getDiscoveryResult(RoutingContext routingContext)
    {

        try
        {
            var response = new JsonObject();

            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

            var discoveryProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE)
                    .put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId))).getJsonObject(RESULT);

            if (discoveryProfile != null)
            {
                if (discoveryProfile.getBoolean(IS_DISCOVERED, false))
                {
                    response.put(STATUS, SUCCESS)
                            .put(MESSAGE, "Device discovered successfully")
                            .put(RESULT, discoveryProfile.getJsonObject(RESULT));
                }
                else
                {
                    response.put(STATUS, FAILED)
                            .put(ERROR, "No discovery result found for ID: " + discProfileId)
                            .put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase())
                            .put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
                }
            }
            else
            {
                response.put(STATUS, FAILED)
                        .put(ERROR, "No discovery result found for ID: " + discProfileId)
                        .put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase())
                        .put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
            }

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

}
