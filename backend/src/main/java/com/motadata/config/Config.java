package com.motadata.config;

import com.motadata.constants.Constants;
import io.vertx.core.json.JsonObject;
import static com.motadata.Bootstrap.LOGGER;

import java.io.FileInputStream;
import java.io.IOException;

public class Config
{
    private Config()
    {

    }

    public static String HOST;

    public static int PORT;

    public static String GO_PLUGIN_ENGINE_PATH;

    public static String GO_COLLECTOR_PATH;

    static
    {
        var config = loadConfig();

        HOST = config.getString(Constants.HOST, "127.0.0.1");

        PORT = config.getInteger(Constants.PORT, 8080);

        GO_PLUGIN_ENGINE_PATH = config.getString(Constants.GO_PLUGIN_ENGINE_PATH, System.getProperty("user.dir") + "/plugins/plugin-engine");

        GO_COLLECTOR_PATH = config.getString(Constants.GO_COLLECTOR_PATH, System.getProperty("user.dir") + "/plugins/collector");

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