package com.motadata.utils;

import com.motadata.database.ConfigDB;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Utils
{

    private static final ConcurrentMap<Long, AtomicInteger> counters = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private static final AtomicLong counter = new AtomicLong(0);

    public static boolean validateRequestBody(JsonObject requestObject)
    {

        for (String key : requestObject.fieldNames())
        {
            var value = requestObject.getValue(key);

            if (value == null || ( value instanceof String && ( (String) value ).isEmpty() ))
            {
                return false;
            }
        }
        return true;
    }

    public static long getId()
    {

        return counter.incrementAndGet();
    }

    public static JsonArray decodeBase64ToJsonArray(String encodedString)
    {

        var decodedString = "[]";

        decodedString = new String(Base64.getDecoder().decode(encodedString));

        return new JsonArray(decodedString);
    }

    public static boolean isAvailable(String objectIp)
    {

        if (ConfigDB.availableDevices.containsKey(objectIp) && ConfigDB.availableDevices.get(objectIp).equals("up"))
        {
            LOGGER.trace("{} device is UP!", objectIp);

            return true;
        }

        LOGGER.trace("{} device is DOWN!", objectIp);

        return false;
    }

    public static void incrementCounter(long credentialProfileId)
    {

        counters.computeIfAbsent(credentialProfileId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public static void decrementCounter(long credentialProfileId)
    {

        var counter = counters.get(credentialProfileId);

        if (counter != null)
        {
            counter.decrementAndGet();
        }
    }

    public static int getCounter(long credentialProfileId)
    {

        var counter = counters.get(credentialProfileId);

        return counter != null ? counter.get() : 0;
    }

}
