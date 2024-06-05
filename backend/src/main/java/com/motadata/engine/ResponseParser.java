package com.motadata.engine;

import com.motadata.database.ConfigDB;
import com.motadata.utils.Utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.motadata.constants.Constants.*;

public class ResponseParser
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseParser.class);

    public static void discoveryResult(JsonObject monitor)
    {

        LOGGER.trace("PARSE_DISCOVERY_EVENT: {}", monitor);

        try
        {

            LOGGER.trace(monitor.toString());

            if (monitor.getString(STATUS).equals(SUCCESS))
            {

                var discoveryProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, DISCOVERY_PROFILE)
                                .put(DATA, new JsonObject().put(DISCOVERY_PROFILE_ID, Long.parseLong(monitor.getString(DISCOVERY_PROFILE_ID)))))
                        .getJsonObject(RESULT);

                if (!discoveryProfile.getBoolean(IS_DISCOVERED, false))
                {
                    var credentialProfileId = Long.parseLong(monitor.getString(CREDENTIAL_PROFILE_ID));

                    discoveryProfile.put(IS_DISCOVERED, true);

                    discoveryProfile.put(RESULT, monitor.getJsonObject(RESULT));

                    discoveryProfile.put(CREDENTIAL_PROFILE_ID, credentialProfileId);

                    Utils.incrementCounter(credentialProfileId);
                }

                LOGGER.trace("Monitor status updated: {}", monitor.getString(OBJECT_IP, ""));
            }

        }
        catch (Exception exception)
        {
            LOGGER.error(ERROR_CONTAINER, exception.getMessage());
        }
    }

}
