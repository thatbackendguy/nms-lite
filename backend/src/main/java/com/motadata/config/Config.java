package com.motadata.config;

import com.motadata.constants.Constants;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class Config
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    private Config()
    {

    }

    public static String HOST;

    public static int PORT;

    public static long POLLING_INTERVAL;

    public static String GO_PLUGIN_ENGINE_PATH;

    public static int AGENT_MODE;

    public static final String DB_URI;

    public static final String DB_USERNAME;

    public static final String DB_PASS;

    static
    {
        var config = loadConfig();

        HOST = config.getString(Constants.HOST, "127.0.0.1");

        PORT = config.getInteger(Constants.PORT, 8080);

        POLLING_INTERVAL = config.getLong(Constants.POLLING_INTERVAL, 300_000L);

        GO_PLUGIN_ENGINE_PATH = config.getString(Constants.GO_PLUGIN_ENGINE_PATH, System.getProperty("user.dir") + "/pluginEngine/plugin-engine");

        AGENT_MODE = config.getInteger(Constants.AGENT_MODE, 0);

        DB_URI = config.getString(Constants.DATABASE_URI, "jdbc:mysql://localhost:3306/configDB");

        DB_USERNAME = config.getString(Constants.DATABASE_USERNAME, "root");

        DB_PASS = config.getString(Constants.DATABASE_PASSWORD, "Root@1010");

    }

    private static JsonObject loadConfig()
    {

        try (var inputStream = new FileInputStream("config.json"))
        {
            var buffer = inputStream.readAllBytes();

            var jsonText = new String(buffer);

            return new JsonObject(jsonText);

        }
        catch (IOException e)
        {
            LOGGER.error("Error reading configuration file: ", e);

            return new JsonObject();
        }
    }

}