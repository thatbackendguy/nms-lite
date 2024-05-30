package com.motadata.utils;

import com.motadata.config.Config;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.motadata.constants.Constants.*;

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

    public static boolean pingCheck(String objectIp)
    {

        try
        {
            var processBuilder = new ProcessBuilder("fping", objectIp, "-c3", "-q");

            processBuilder.redirectErrorStream(true);

            var process = processBuilder.start();

            var isCompleted = process.waitFor(5, TimeUnit.SECONDS); // Wait for 5 seconds

            if (!isCompleted)
            {
                process.destroyForcibly();
            }
            else
            {
                var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;

                while (( line = reader.readLine() ) != null)
                {
                    if (line.contains("/0%"))
                    {

                        LOGGER.debug("{} device is UP!", objectIp);

                        return true;
                    }
                }
            }
        }
        catch (Exception exception)
        {
            LOGGER.error("Exception: {}", exception.getMessage());
        }

        LOGGER.debug("{} device is DOWN!", objectIp);

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
