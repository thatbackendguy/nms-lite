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

import static com.motadata.constants.Constants.*;
import static com.motadata.api.APIServer.LOGGER;

public class Provision
{

    private final EventBus eventBus;

    private final Vertx vertx;

    private final Router provisionSubRouter;

    public Provision()
    {

        this.vertx = Bootstrap.getVertx();

        this.eventBus = Bootstrap.getVertx().eventBus();

        this.provisionSubRouter = Router.router(vertx);
    }

    public void init(Router router)
    {

        router.route("/provision/*").subRouter(provisionSubRouter);

        provisionSubRouter.route(HttpMethod.GET, URL_SEPARATOR + "devices").handler(this::getProvisionedDevice);

        provisionSubRouter.route(HttpMethod.POST, URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS)
                .handler(this::provisionDevice);

        provisionSubRouter.route(HttpMethod.DELETE, URL_SEPARATOR + COLON_SEPARATOR + PROVISION_ID_PARAMS)
                .handler(this::unProvisionDevice);
    }

    private void getProvisionedDevice(RoutingContext routingContext)
    {

        try
        {
            var response = new JsonObject();

            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            var result = new JsonArray();

            for (var device : ConfigDB.provisionedDevices.values())
            {
                result.add(device.getString(OBJECT_IP));
            }

            if (result.isEmpty())
            {
                response.put(STATUS, FAILED)
                        .put(ERROR, "No provisioned devices found")
                        .put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase())
                        .put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
            }
            else
            {
                response.put(STATUS, SUCCESS).put(RESULT, result);
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

    private void provisionDevice(RoutingContext routingContext)
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
                for (var values : ConfigDB.provisionedDevices.values())
                {
                    if (Long.parseLong(discProfileId) == values.getLong(DISCOVERY_PROFILE_ID))
                    {
                        routingContext.response()
                                .setStatusCode(HttpResponseStatus.CONFLICT.code())
                                .putHeader(CONTENT_TYPE, APP_JSON)
                                .end(new JsonObject().put(STATUS, FAILED)
                                        .put(ERROR, "Unable to provision device")
                                        .put(ERR_MESSAGE, "Device already provisioned")
                                        .put(ERR_STATUS_CODE, HttpResponseStatus.CONFLICT.code())
                                        .toString());

                        return;
                    }
                }

                var provisionId = Utils.getId();

                var discoveryProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE)
                        .put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId))).getJsonObject(RESULT);

                if (discoveryProfile.containsKey(CREDENTIAL_PROFILE_ID))
                {
                    var credentialProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE)
                                    .put(DATA, new JsonObject().put(CREDENTIAL_PROFILE_ID, Long.parseLong(discoveryProfile.getString(CREDENTIAL_PROFILE_ID)))))
                            .getJsonObject(RESULT);

                    ConfigDB.provisionedDevices.put(provisionId, new JsonObject().put(OBJECT_IP, discoveryProfile.getString(OBJECT_IP))
                            .put(PORT, discoveryProfile.getInteger(PORT))
                            .put(SNMP_COMMUNITY, credentialProfile.getString(SNMP_COMMUNITY))
                            .put(VERSION, credentialProfile.getString(VERSION))
                            .put(CREDENTIAL_PROFILE_ID, Long.parseLong(discoveryProfile.getString(CREDENTIAL_PROFILE_ID))));

                    routingContext.response()
                            .setStatusCode(HttpResponseStatus.OK.code())
                            .putHeader(CONTENT_TYPE, APP_JSON)
                            .end(new JsonObject().put(STATUS, SUCCESS)
                                    .put(MESSAGE, String.format("Device provisioned successfully! Provision ID: %d", provisionId))
                                    .toString());
                }
                else
                {
                    routingContext.response()
                            .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                            .putHeader(CONTENT_TYPE, APP_JSON)
                            .end(new JsonObject().put(STATUS, FAILED)
                                    .put(ERROR, "Unable to provision device")
                                    .put(ERR_MESSAGE, "Device is not discovered yet, first run discovery!")
                                    .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())
                                    .toString());
                }
                return;
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

    private void unProvisionDevice(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            var provisionId = Long.parseLong(routingContext.request().getParam(PROVISION_ID_PARAMS));

            if (ConfigDB.provisionedDevices.containsKey(provisionId))
            {
                var credentialProfileId = Long.parseLong(ConfigDB.provisionedDevices.get(provisionId)
                        .getString(CREDENTIAL_PROFILE_ID));

                Utils.decrementCounter(credentialProfileId);

                ConfigDB.provisionedDevices.remove(provisionId);

                routingContext.response()
                        .setStatusCode(HttpResponseStatus.OK.code())
                        .putHeader(CONTENT_TYPE, APP_JSON)
                        .end(new JsonObject().put(STATUS, SUCCESS)
                                .put(MESSAGE, "Device unprovisioned successfully")
                                .toString());

            }
            else
            {
                routingContext.response()
                        .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                        .putHeader(CONTENT_TYPE, APP_JSON)
                        .end(new JsonObject().put(STATUS, FAILED)
                                .put(ERROR, "Unable to un-provision device")
                                .put(ERR_MESSAGE, "No provisioned device found")
                                .put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code())
                                .toString());
            }
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
