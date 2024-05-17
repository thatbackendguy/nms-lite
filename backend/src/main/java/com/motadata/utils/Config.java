package com.motadata.utils;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class Config
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    private Config()
    {}

    public static int PORT = 8080;

    public static String DATABASE_URI = "jdbc:mysql://localhost:3306/configDB";

    public static String DATABASE_USERNAME = "root";

    public static String DATABASE_PASSWORD = "Root@1010";

    public static String HOST = "127.0.0.1";

    public static long POLLING_INTERVAL = 300000;

    static
    {
        var config = loadConfig();

        HOST = config.getString(Constants.HOST);

        PORT = config.getInteger(Constants.PORT);

        DATABASE_URI = config.getString(Constants.DATABASE_URI);

        DATABASE_USERNAME = config.getString(Constants.DATABASE_USERNAME);

        DATABASE_PASSWORD = config.getString(Constants.DATABASE_PASSWORD);

        POLLING_INTERVAL = config.getLong(Constants.POLLING_INTERVAL);

    }

    private static JsonObject loadConfig()
    {
        try(var inputStream = new FileInputStream("config.json"))
        {
            var buffer = inputStream.readAllBytes();

            var jsonText = new String(buffer);

            return new JsonObject(jsonText);

        }
        catch(IOException e)
        {
            LOGGER.error("Error reading configuration file: ",e);

            return new JsonObject();
        }
    }

}