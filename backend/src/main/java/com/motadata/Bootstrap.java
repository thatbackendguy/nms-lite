package com.motadata;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;

import static com.motadata.Constants.*;

public class Bootstrap
{
    public static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args)
    {
        var vertx = Vertx.vertx();

        vertx.deployVerticle("com.motadata.ApiServer", handler->{
            if(handler.succeeded())
            {
                LOGGER.info("Server is up and running");
            }
        });

        vertx.deployVerticle("com.motadata.DbWorker", new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER), handler->{
            if(handler.succeeded())
            {
                LOGGER.info("DB Worker is up and running");
            }
        });

//        var json = new JsonObject();
//
//        json.put(REQUEST_TYPE, "Discovery");
//
//        json.put(OBJECT_IP, "172.16.8.3");
//
//        json.put(SNMP_PORT, 161);
//
//        json.put(COMMUNITY, "public");
//
//        String encodedString = Base64.getEncoder().encodeToString(json.toString().getBytes());
//
//        System.out.println(encodedString);
//
//        try
//        {
//            ProcessBuilder processBuilder = new ProcessBuilder("/home/yash/Documents/GitHub/nms-lite/plugin-engine/plugin-engine", encodedString);
//
//            processBuilder.redirectErrorStream(true);
//
//            Process process = processBuilder.start();
//
//            // Read the output of the command
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//
//            String line;
//
//            var buffer = Buffer.buffer();
//
//            while((line = reader.readLine()) != null)
//            {
//                buffer.appendString(line);
//            }
//
//            byte[] decodedBytes = Base64.getDecoder().decode(buffer.toString());
//
//            // Convert the byte array to a string
//            String decodedString = new String(decodedBytes);
//
//            System.out.println(decodedString);
//
//        } catch(IOException e)
//        {
//            e.printStackTrace();
//        }
    }
}
