package com.motadata.database;

import com.motadata.utils.Utils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

import static com.motadata.constants.Constants.*;

public class ConfigDB
{
    public static final ConcurrentHashMap<Long, JsonObject> provisionedDevices = new ConcurrentHashMap<>();

    static final Logger LOGGER = LoggerFactory.getLogger(ConfigDB.class);

    private static final ConcurrentHashMap<Long, JsonObject> credentialProfiles = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, JsonObject> discoveryProfiles = new ConcurrentHashMap<>();

    private ConfigDB()
    {}

    public static JsonObject create(JsonObject request)
    {
        LOGGER.trace("Create request: {}", request);

        var response = new JsonObject();

        try
        {
            var data = request.getJsonObject(DATA);

            switch(request.getString(REQUEST_TYPE))
            {
                case CREDENTIAL_PROFILE ->
                {

                    for(var credentialProfile : credentialProfiles.values())
                    {
                        if(data.getString(CREDENTIAL_NAME).equals(credentialProfile.getString(CREDENTIAL_NAME)))
                        {
                            response.put(ERROR, new JsonObject().put(ERROR, "INSERTION ERROR").put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code()).put(ERR_MESSAGE, String.format("error in saving %s, because credentialProfile name is already used", request.getString(REQUEST_TYPE))));

                            return response;
                        }
                    }

                    var id = Utils.getId();

                    credentialProfiles.put(id, data);

                    response.put(CREDENTIAL_PROFILE_ID, id).put(MESSAGE, "Credential profile created successfully");


                }
                case DISCOVERY_PROFILE ->
                {

                    for(var discoveryProfile : discoveryProfiles.values())
                    {
                        if(data.getString(DISCOVERY_NAME).equals(discoveryProfile.getString(DISCOVERY_NAME)))
                        {
                            response.put(ERROR, new JsonObject().put(ERROR, "INSERTION ERROR").put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code()).put(ERR_MESSAGE, String.format("error in saving %s, because discovery name is already used", request.getString(REQUEST_TYPE))));

                            return response;
                        }
                    }

                    var id = Utils.getId();

                    discoveryProfiles.put(id, data);

                    response.put(DISCOVERY_PROFILE_ID, id).put(MESSAGE, "Discovery profile created successfully");


                }
            }

        } catch(Exception exception)
        {
            response.put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, exception.getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERR_MESSAGE, "error in executing INSERT operation"));

            LOGGER.error(exception.getMessage());

        }
        return response;
    }

    public static JsonObject get(JsonObject request)
    {
        var response = new JsonObject();

        LOGGER.trace("Read request: {}", request);

        try
        {
            var data = request.getJsonObject(DATA);

            switch(request.getString(REQUEST_TYPE))
            {
                case CREDENTIAL_PROFILE ->
                {
                    if(data == null)
                    {
                        var credentialObjects = new JsonArray();

                        for(var id : credentialProfiles.keySet())
                        {
                            credentialObjects.add(new JsonObject().put(id.toString(), credentialProfiles.get(id)));
                        }

                        response.put(RESULT, credentialObjects);
                    }
                    else
                    {
                        if(credentialProfiles.containsKey(Long.parseLong(data.getString(CREDENTIAL_PROFILE_ID))))
                        {
                            response.put(RESULT, credentialProfiles.get(Long.parseLong(data.getString(CREDENTIAL_PROFILE_ID))));
                        }
                    }

                }
                case DISCOVERY_PROFILE ->
                {
                    if(data == null)
                    {
                        var discoveryObjects = new JsonArray();

                        for(var id : discoveryProfiles.keySet())
                        {
                            discoveryObjects.add(new JsonObject().put(id.toString(), discoveryProfiles.get(id)));
                        }

                        response.put(RESULT, discoveryObjects);
                    }
                    else
                    {
                        if(discoveryProfiles.containsKey(Long.parseLong(data.getString(DISCOVERY_PROFILE_ID))))
                        {
                            response.put(RESULT, discoveryProfiles.get(Long.parseLong(data.getString(DISCOVERY_PROFILE_ID))));
                        }
                    }
                }

            }

        } catch(Exception exception)
        {
            response.put(STATUS, FAILED);

            response.put(ERROR, new JsonObject().put(ERROR, exception.getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERR_MESSAGE, "error in executing GET operation"));

            LOGGER.error(exception.getMessage());
        }

        return response;
    }

    public static JsonObject update(JsonObject request)
    {
        var response = new JsonObject();

        LOGGER.trace("Update request: {}", request);

        try
        {
            var data = request.getJsonObject(DATA);

            switch(request.getString(REQUEST_TYPE))
            {
                case CREDENTIAL_PROFILE ->
                {

                    if(credentialProfiles.containsKey(Long.parseLong(data.getString(CREDENTIAL_PROFILE_ID))))
                    {
                        var credential = credentialProfiles.get(Long.parseLong(data.getString(CREDENTIAL_PROFILE_ID)));

                        credential.put(VERSION, data.getString(VERSION));

                        credential.put(SNMP_COMMUNITY, data.getString(SNMP_COMMUNITY));

                        response.put(MESSAGE, "Credential profile updated successfully!");
                    }
                    else
                    {
                        response.put(ERROR, new JsonObject().put(ERROR, "Error in updating credential profile").put(ERR_STATUS_CODE, HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE.code()).put(ERR_MESSAGE, String.format("No profile exists for ID: %s", data.getString(CREDENTIAL_PROFILE_ID))));
                    }
                }
                case DISCOVERY_PROFILE ->
                {

                    if(discoveryProfiles.containsKey(Long.parseLong(data.getString(DISCOVERY_PROFILE_ID))))
                    {
                        var discoveryProfile = discoveryProfiles.get(Long.parseLong(data.getString(DISCOVERY_PROFILE_ID)));

                        discoveryProfile.put(PORT, data.getInteger(PORT));

                        discoveryProfile.put(OBJECT_IP, data.getString(OBJECT_IP));

                        response.put(MESSAGE, "Discovery profile updated successfully!");
                    }
                    else
                    {
                        response.put(ERROR, new JsonObject().put(ERROR, "Error in updating discovery profile").put(ERR_STATUS_CODE, HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE.code()).put(ERR_MESSAGE, String.format("No profile exists for ID: %s", data.getString(DISCOVERY_PROFILE_ID))));
                    }
                }
            }


        } catch(Exception exception)
        {
            response.put(STATUS, FAILED);

            response.put(ERROR, new JsonObject().put(ERROR, exception.getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERR_MESSAGE, "error in executing UPDATE operation"));

            LOGGER.error(exception.getMessage());

        }
        return response;
    }

    public static JsonObject delete(JsonObject request)
    {
        var response = new JsonObject();

        LOGGER.trace("Delete request: {}", request);

        try
        {
            var data = request.getJsonObject(DATA);

            switch(request.getString(REQUEST_TYPE))
            {

                case CREDENTIAL_PROFILE ->
                {

                    if(credentialProfiles.containsKey(Long.parseLong(data.getString(CREDENTIAL_PROFILE_ID))))
                    {
                        credentialProfiles.remove(Long.parseLong(data.getString(CREDENTIAL_PROFILE_ID)));

                        response.put(MESSAGE, "Credential profile deleted successfully!");
                    }
                    else
                    {
                        response.put(ERROR, new JsonObject().put(ERROR, "Error in deleting credential profile").put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code()).put(ERR_MESSAGE, String.format("No profile exists for ID: %s", data.getString(CREDENTIAL_PROFILE_ID))));
                    }
                }

                case DISCOVERY_PROFILE ->
                {

                    var discoveryProfileId = Long.parseLong(data.getString(DISCOVERY_PROFILE_ID));

                    if(discoveryProfiles.containsKey(discoveryProfileId))
                    {
                        var credentialProfileId = Long.parseLong(discoveryProfiles.get(discoveryProfileId).getString(CREDENTIAL_PROFILE_ID));

                        Utils.decrementCounter(credentialProfileId);

                        discoveryProfiles.remove(discoveryProfileId);

                        response.put(MESSAGE, "Discovery profile deleted successfully!");
                    }
                    else
                    {
                        response.put(ERROR, new JsonObject().put(ERROR, "Error in deleting discovery profile").put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code()).put(ERR_MESSAGE, String.format("No profile exists for ID: %s", data.getString(DISCOVERY_PROFILE_ID))));
                    }
                }
            }
        } catch(Exception exception)
        {
            response.put(STATUS, FAILED);

            response.put(ERROR, new JsonObject().put(ERROR, exception.getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).put(ERR_MESSAGE, "error in executing UPDATE operation"));

            LOGGER.error(exception.getMessage());

        }
        return response;
    }
}