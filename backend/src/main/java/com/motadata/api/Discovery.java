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

import static com.motadata.api.APIServer.LOGGER;

import static com.motadata.constants.Constants.*;

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

        subRouter.route(HttpMethod.GET, URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::getDiscovery);

        subRouter.route(HttpMethod.PUT, URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::updateDiscovery);

        subRouter.route(HttpMethod.DELETE, URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::deleteDiscovery);

        subRouter.route(HttpMethod.POST, URL_SEPARATOR + RUN).handler(this::runDiscovery);

        subRouter.route(HttpMethod.GET, URL_SEPARATOR + RESULT + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::getDiscoveryResult);

        subRouter.route(HttpMethod.POST, URL_SEPARATOR + PROVISION + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::provisionDevice);

        subRouter.route(HttpMethod.DELETE, URL_SEPARATOR + PROVISION + URL_SEPARATOR + COLON_SEPARATOR + PROVISION_ID_PARAMS).handler(this::unProvisionDevice);
    }

    public void addDiscovery(RoutingContext routingContext)
    {
        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer -> {

                var requestBody = buffer.toJsonObject();

                var response = new JsonObject();

                if(requestBody.containsKey(DISCOVERY_NAME) && requestBody.containsKey(OBJECT_IP) && requestBody.containsKey(PORT) && requestBody.containsKey(CREDENTIALS))
                {
                    if(Utils.validateRequestBody(requestBody))
                    {
                        var areCredentialsValid = false;

                        for(var credentialProfileId : requestBody.getJsonArray(CREDENTIALS))
                        {
                            var entries = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE).put(DATA, new JsonObject().put(CREDENTIAL_PROFILE_ID, credentialProfileId)));

                            if(entries.containsKey(RESULT))
                            {
                                areCredentialsValid = true;
                            }
                            else
                            {
                                areCredentialsValid = false;

                                break;
                            }

                        }

                        if(areCredentialsValid)
                        {
                            var object = new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, requestBody);

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
                            response.put(STATUS, FAILED).put(ERROR, "Error in creating discovery profile").put(ERR_MESSAGE, "One or more credential profiles not found!").put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code());
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

        } catch(Exception exception)
        {
            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    public void getAllDiscoveries(RoutingContext routingContext)
    {
        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var response = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE));

            if(response.getJsonArray(RESULT).isEmpty())
            {
                response.remove(RESULT);

                response.put(STATUS, FAILED).put(ERROR, "No discovery profiles found").put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase()).put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
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

        } catch(Exception exception)
        {
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(new JsonObject().put(STATUS, FAILED).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()).put(ERR_MESSAGE, exception.getMessage()).toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }

    }

    public void getDiscovery(RoutingContext routingContext)
    {
        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

            var response = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId)));

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

        } catch(Exception exception)
        {
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(new JsonObject().put(STATUS, FAILED).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()).put(ERR_MESSAGE, exception.getMessage()).toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    public void updateDiscovery(RoutingContext routingContext)
    {
        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

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

        } catch(Exception exception)
        {
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(new JsonObject().put(STATUS, FAILED).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()).put(ERR_MESSAGE, exception.getMessage()).toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }

    }

    public void deleteDiscovery(RoutingContext routingContext)
    {
        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

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

        } catch(Exception exception)
        {
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(new JsonObject().put(STATUS, FAILED).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()).put(ERR_MESSAGE, exception.getMessage()).toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    public void runDiscovery(RoutingContext routingContext)
    {
        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer -> {

                var requestObjects = buffer.toJsonArray();

                var contexts = new JsonArray();

                vertx.executeBlocking(handler -> {

                    for(var object : requestObjects)
                    {
                        var monitor = new JsonObject(object.toString());

                        var discoveryProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, monitor.getString(DISCOVERY_PROFILE_ID)))).getJsonObject(RESULT).put(DISCOVERY_PROFILE_ID, monitor.getString(DISCOVERY_PROFILE_ID));

                        if(!discoveryProfile.isEmpty() && Utils.pingCheck(discoveryProfile.getString(OBJECT_IP)))
                        {
                            discoveryProfile.put(REQUEST_TYPE, DISCOVERY).put(PLUGIN_NAME, NETWORK);

                            var credentialProfileIds = discoveryProfile.getJsonArray(CREDENTIALS);

                            var credentialProfiles = new JsonArray();

                            for(var credentialId : credentialProfileIds)
                            {
                                var credentialProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE).put(DATA, new JsonObject().put(CREDENTIAL_PROFILE_ID, credentialId))).getJsonObject(RESULT).put(CREDENTIAL_PROFILE_ID, credentialId);

                                credentialProfiles.add(credentialProfile);
                            }

                            discoveryProfile.put(CREDENTIALS, credentialProfiles);

                            contexts.add(discoveryProfile);
                        }
                    }

                    var encodedString = Base64.getEncoder().encodeToString(contexts.toString().getBytes());

                    eventBus.send(RUN_DISCOVERY_EVENT, encodedString);

                    handler.complete();

                }, resultHandler -> {

                    if(resultHandler.succeeded())
                    {
                        LOGGER.trace("Discovery context sent to discovery engine");
                    }
                });

                routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Discovery ran successfully!").toString());
            });

        } catch(Exception exception)
        {
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(new JsonObject().put(STATUS, FAILED).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()).put(ERR_MESSAGE, exception.getMessage()).toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    public void getDiscoveryResult(RoutingContext routingContext)
    {
        try
        {
            var response = new JsonObject();

            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

            var discoveryProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId))).getJsonObject(RESULT);

            if(discoveryProfile != null)
            {
                if(discoveryProfile.getJsonObject(RESULT).getBoolean(IS_DISCOVERED, false))
                {
                    response.put(STATUS, SUCCESS).put(MESSAGE,"Device discovered successfully");
                }
                else
                {
                    response.put(STATUS, FAILED).put(ERROR, "No discovery result found for ID: " + discProfileId).put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase()).put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
                }
            }
            else
            {
                response.put(STATUS, FAILED).put(ERROR, "No discovery result found for ID: " + discProfileId).put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase()).put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
            }

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

        } catch(Exception exception)
        {
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(new JsonObject().put(STATUS, FAILED).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()).put(ERR_MESSAGE, exception.getMessage()).toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    public void provisionDevice(RoutingContext routingContext)
    {
        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

            var response = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId)));

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
                for(var values : ConfigDB.provisionedDevices.values())
                {
                    if(Long.parseLong(discProfileId) == values.getLong(DISCOVERY_PROFILE_ID))
                    {
                        routingContext.response().setStatusCode(HttpResponseStatus.CONFLICT.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, "Unable to provision device").put(ERR_MESSAGE, "Device already provisioned").put(ERR_STATUS_CODE, HttpResponseStatus.CONFLICT.code()).toString());

                        return;
                    }
                }

                var provisionId = Utils.getId();

                var discoveryProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId))).getJsonObject(RESULT);

                var credentialProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE).put(DATA, new JsonObject().put(CREDENTIAL_PROFILE_ID, Long.parseLong(discoveryProfile.getString(CREDENTIAL_PROFILE_ID))))).getJsonObject(RESULT);

                ConfigDB.provisionedDevices.put(provisionId, new JsonObject().put(OBJECT_IP, discoveryProfile.getString(OBJECT_IP)).put(PORT, discoveryProfile.getInteger(PORT)).put(SNMP_COMMUNITY, credentialProfile.getString(SNMP_COMMUNITY)).put(VERSION, credentialProfile.getString(VERSION)).put(CREDENTIAL_PROFILE_ID, Long.parseLong(discoveryProfile.getString(CREDENTIAL_PROFILE_ID))));

                routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, String.format("Device provisioned successfully! Provision ID: %d", provisionId)).toString());

                return;
            }

            routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());

        } catch(Exception exception)
        {
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(new JsonObject().put(STATUS, FAILED).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()).put(ERR_MESSAGE, exception.getMessage()).toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    public void unProvisionDevice(RoutingContext routingContext)
    {
        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

            var provisionId = Long.parseLong(routingContext.request().getParam(PROVISION_ID_PARAMS));

            if(ConfigDB.provisionedDevices.containsKey(provisionId))
            {
                var credentialProfileId = Long.parseLong(ConfigDB.provisionedDevices.get(provisionId).getString(CREDENTIAL_PROFILE_ID));

                Utils.decrementCounter(credentialProfileId);

                ConfigDB.provisionedDevices.remove(provisionId);

                routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Device unprovisioned successfully").toString());

            }
            else
            {
                routingContext.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, "Unable to un-provision device").put(ERR_MESSAGE, "No provisioned device found").put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code()).toString());
            }
        } catch(Exception exception)
        {
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(new JsonObject().put(STATUS, FAILED).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()).put(ERR_MESSAGE, exception.getMessage()).toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }
}
