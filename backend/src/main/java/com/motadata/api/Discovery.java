package com.motadata.api;

import com.motadata.Bootstrap;
import com.motadata.database.ConfigDB;
import com.motadata.utils.ProcessUtils;
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

        subRouter.route(HttpMethod.GET, URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::getDiscovery);

        subRouter.route(HttpMethod.POST, URL_SEPARATOR + RUN).handler(this::runDiscovery);

        subRouter.route(HttpMethod.GET, URL_SEPARATOR + RESULT + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::getDiscoveryResult);

        subRouter.route(HttpMethod.PUT, URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::updateDiscovery);

        subRouter.route(HttpMethod.DELETE, URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::deleteDiscovery);

        subRouter.route(HttpMethod.POST, URL_SEPARATOR + PROVISION + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::provisionDevice);

        //        subRouter.route(HttpMethod.GET, URL_SEPARATOR + PROVISION).handler(this::getProvisionedDevices);

        //        subRouter.route(HttpMethod.DELETE, URL_SEPARATOR + PROVISION + URL_SEPARATOR + COLON_SEPARATOR + DISCOVERY_PROFILE_ID_PARAMS).handler(this::unProvisionDevice);
    }

    public void addDiscovery(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

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

    }

    public void getAllDiscoveries(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

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
    }

    public void getDiscovery(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

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
                 [{"discovery.profile.id": 2,"credentials": [1,2,3,4,5]}]
                */
        routingContext.request().bodyHandler(buffer -> {

            var requestObjects = buffer.toJsonArray();

            var contexts = new JsonArray();

            vertx.executeBlocking(handler -> {
                for(var object : requestObjects)
                {
                    var monitor = new JsonObject(object.toString());

                    //TODO: working on run discovery

                    var discoveryProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE).put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, monitor.getString(DISCOVERY_PROFILE_ID)))).getJsonObject(RESULT).put(DISCOVERY_PROFILE_ID, monitor.getString(DISCOVERY_PROFILE_ID));

                    if(!discoveryProfile.isEmpty() && ProcessUtils.pingCheck(discoveryProfile.getString(OBJECT_IP)))
                    {
                        discoveryProfile.put(REQUEST_TYPE, DISCOVERY).put(PLUGIN_NAME, NETWORK);

                        if(ConfigDB.validCredentials.containsKey(Long.parseLong(monitor.getString(DISCOVERY_PROFILE_ID))))
                        {
                            // TODO: device already discovered
                        }

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
    }

    public void getDiscoveryResult(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);

        var response = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERED_DEVICES).put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, discProfileId)));

        if(response.isEmpty())
        {
            response.put(STATUS, FAILED).put(ERROR, "No discovery result found for ID: " + discProfileId).put(ERR_MESSAGE, HttpResponseStatus.NOT_FOUND.reasonPhrase()).put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code());
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

    public void provisionDevice(RoutingContext routingContext)
    {

        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

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
            for(var values: ConfigDB.provisionedDevices.values())
            {
                if(Long.parseLong(discProfileId) == values)
                {
                    routingContext.response().setStatusCode(HttpResponseStatus.CONFLICT.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR,"Unable to provision device").put(ERR_MESSAGE,"Device already provisioned").put(ERR_STATUS_CODE,HttpResponseStatus.CONFLICT.code()).toString());

                    return;
                }
            }

            var provisionId = Utils.getId();

            ConfigDB.provisionedDevices.put(provisionId,Long.parseLong(discProfileId));

            routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE,String.format("Device provisioned successfully! Provision ID: %d",provisionId)).toString());

            return;
        }

        routingContext.response().putHeader(CONTENT_TYPE, APP_JSON).end(response.toString());
    }

    //    public void unProvisionDevice(RoutingContext routingContext)
    //    {
    //        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());
    //
    //        var discProfileId = routingContext.request().getParam(DISCOVERY_PROFILE_ID_PARAMS);
    //
    //        eventBus.request(UNPROVISION_DEVICE, discProfileId, ar -> {
    //            if(ar.succeeded())
    //            {
    //                routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Device provision stopped successfully!"));
    //            }
    //            else
    //            {
    //                routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error stopping provision").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
    //            }
    //        });
    //    }
    //
    //    public void getProvisionedDevices(RoutingContext routingContext)
    //    {
    //
    //        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());
    //
    //        eventBus.request(GET_ALL_EVENT, new JsonObject().put(TABLE_NAME, GET_PROVISIONED_DEVICES_EVENT), ar -> {
    //            if(ar.succeeded())
    //            {
    //                routingContext.json(new JsonObject().put(STATUS, SUCCESS).put(MESSAGE, "Provisioned devices fetched successfully!").put(RESULT, ar.result().body()));
    //            }
    //            else
    //            {
    //                routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data from DB").put(ERR_MESSAGE, ar.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
    //            }
    //        });
    //
    //    }
}
