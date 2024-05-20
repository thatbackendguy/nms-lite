package com.motadata.utils;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.motadata.constants.Constants.*;


public class Utils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private static final AtomicLong counter = new AtomicLong(0);

    public static boolean validateRequestBody(JsonObject requestObject)
    {
        for(String key : requestObject.fieldNames())
        {
            var value = requestObject.getValue(key);

            if(value == null || (value instanceof String && ((String) value).isEmpty()))
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

    public static Future<Void> writeToFile(Vertx vertx, JsonObject data)
    {
        Promise<Void> promise = Promise.promise();

        var fileName = data.getString(OBJECT_IP, "localhost") + ".txt";

        var buffer = Buffer.buffer(data.encodePrettily());

        vertx.fileSystem().openBlocking(fileName, new OpenOptions().setAppend(true).setCreate(true)).write(buffer).onComplete(handler -> {
            LOGGER.info("Content written to file");

            promise.complete();

        }).onFailure(handler -> {
            LOGGER.warn("Error occurred while opening the file {}", handler.getCause().toString());

            promise.fail(handler.getCause());
        });

        return promise.future();
    }

    public static JsonArray spawnPluginEngine(String encodedString)
    {
        var decodedString = "";

        try
        {
            var processBuilder = new ProcessBuilder(GO_PLUGIN_ENGINE_PATH, encodedString).redirectErrorStream(true);

            LOGGER.trace("Initiating process builder");

            LOGGER.trace(encodedString);

            var process = processBuilder.start();

            var isCompleted = process.waitFor(25, TimeUnit.SECONDS); // Wait for 25 seconds

            if(!isCompleted)
            {
                process.destroyForcibly();

                LOGGER.error("Polling failed, Timed out");
            }
            else
            {
                var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;

                var processBuffer = Buffer.buffer();

                while((line = reader.readLine()) != null)
                {
                    processBuffer.appendString(line);
                }

                LOGGER.trace("Process completed, Decoding & sending result...");

                decodedString = new String(Base64.getDecoder().decode(processBuffer.toString()));

                LOGGER.info(decodedString);
            }
        } catch(IOException | InterruptedException exception)
        {
            LOGGER.error(ERROR_CONTAINER, exception.getMessage());
        }

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

            if(!isCompleted)
            {
                process.destroyForcibly();
            }
            else
            {
                var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;

                while((line = reader.readLine()) != null)
                {
                    if(line.contains("/0%"))
                    {
                        LOGGER.debug("{} device is UP!", objectIp);

                        return true;
                    }
                }
            }
        } catch(Exception exception)
        {
            LOGGER.error("Exception: {}", exception.getMessage());
        }

        LOGGER.debug("{} device is DOWN!", objectIp);

        return false;
    }

}
