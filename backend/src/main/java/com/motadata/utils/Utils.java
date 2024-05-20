package com.motadata.utils;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.motadata.contants.Constants.*;


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

        var fileName = data.getString(OBJECT_IP, "localhost") + ".text";

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

}
