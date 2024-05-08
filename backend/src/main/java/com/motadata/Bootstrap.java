package com.motadata;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;

import static com.motadata.Constants.*;
public class Bootstrap
{
    public static void main(String[] args)
    {
        //        var vertx = Vertx.vertx();

        //        vertx.deployVerticle("com.motadata.ApiServer");

        var json = new JsonObject();

        json.put(REQUEST_TYPE, "Discovery");

        json.put(OBJECT_IP, "172.16.8.3");

        json.put(SNMP_PORT, 161);

        json.put(COMMUNITY, "public");

        String encodedString = Base64.getEncoder().encodeToString(json.toString().getBytes());

        System.out.println(encodedString);

        try
        {
            ProcessBuilder processBuilder = new ProcessBuilder("/home/yash/GolandProjects/nmslite/nmslite", encodedString);

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Read the output of the command
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            var buffer = Buffer.buffer();

            while((line = reader.readLine()) != null)
            {
                buffer.appendString(line);
            }

            byte[] decodedBytes = Base64.getDecoder().decode(buffer.toString());

            // Convert the byte array to a string
            String decodedString = new String(decodedBytes);

            System.out.println(decodedString);

        } catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
