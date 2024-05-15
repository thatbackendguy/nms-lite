package com.motadata.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.motadata.Bootstrap.LOGGER;

public class Jdbc
{
    private Jdbc(){}

    private static final String DB_URI = "jdbc:mysql://localhost:3306/nmsDB";

    private static final String DB_USER = "root";

    private static final String DB_PASS = "Root@1010";

    public static Connection getConnection()
    {
        Connection connection=null;

        try
        {
            connection = DriverManager.getConnection(DB_URI, DB_USER, DB_PASS);

        } catch(SQLException e)
        {
            LOGGER.error("{}",e.getMessage());
        }

        return connection;

    }


}
