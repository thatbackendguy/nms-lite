package com.motadata.engine;

import com.motadata.Bootstrap;
import com.motadata.database.ConfigDB;
import com.motadata.utils.ProcessUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static com.motadata.contants.Constants.*;

public class Discovery extends AbstractVerticle
{
    static final Logger LOGGER = LoggerFactory.getLogger(Discovery.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        var eventBus = Bootstrap.getVertx().eventBus();

        eventBus.localConsumer(RUN_DISCOVERY_EVENT, msg -> {

            var results = ProcessUtils.spawnPluginEngine(msg.body().toString());

            for(var result : results)
            {
                var monitor = new JsonObject(result.toString());

                LOGGER.trace(monitor.toString());

                if(monitor.getString(STATUS).equals(SUCCESS))
                {
                    if(!ConfigDB.validCredentials.containsKey(Long.parseLong(monitor.getString(DISCOVERY_PROFILE_ID))))
                    {
                        var credentialProfile = ConfigDB.get(new JsonObject().put(REQUEST_TYPE, CREDENTIAL_PROFILE).put(DATA, new JsonObject().put(CREDENTIAL_PROFILE_ID, monitor.getString(CREDENTIAL_PROFILE_ID)))).getJsonObject(RESULT).put(CREDENTIAL_PROFILE_ID, monitor.getString(CREDENTIAL_PROFILE_ID));

                        ConfigDB.validCredentials.put(Long.parseLong(monitor.getString(DISCOVERY_PROFILE_ID)), new JsonObject().put(OBJECT_IP, monitor.getString(OBJECT_IP)).put(PORT, monitor.getInteger(PORT)).put(VERSION, credentialProfile.getString(VERSION)).put(SNMP_COMMUNITY, credentialProfile.getString(SNMP_COMMUNITY)));

                        LOGGER.trace("Discovered monitor saved to database");
                    }
                }
            }
        });

        startPromise.complete();

        LOGGER.info("Discovery engine started");
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception
    {
        stopPromise.complete();
    }
}
