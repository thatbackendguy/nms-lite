package com.motadata.utils;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static com.motadata.contants.Constants.ERROR_CONTAINER;
import static com.motadata.contants.Constants.GO_PLUGIN_ENGINE_PATH;


public class ProcessUtils
{
    private ProcessUtils()
    {}

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessUtils.class);

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
}
