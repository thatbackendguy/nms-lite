package com.motadata.utils;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;


public class Utils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static Connection getConnection()
    {
        Connection connection = null;

        try
        {
            connection = DriverManager.getConnection(Config.DATABASE_URI, Config.DATABASE_USERNAME, Config.DATABASE_PASSWORD);

        } catch(SQLException e)
        {
            LOGGER.error("failed to get database connection, reason : {}", e.getMessage());
        }

        return connection;

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
}
