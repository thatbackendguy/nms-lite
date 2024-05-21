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

    static
    {
        var config = loadConfig();

        HOST = config.getString(Constants.HOST, "127.0.0.1");

        PORT = config.getInteger(Constants.PORT, 8080);

        POLLING_INTERVAL = config.getLong(Constants.POLLING_INTERVAL, 300_000L);

        GO_PLUGIN_ENGINE_PATH = config.getString(Constants.GO_PLUGIN_ENGINE_PATH, System.getProperty("user.dir") + "/pluginEngine/plugin-engine");

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