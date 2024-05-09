package com.motadata.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.motadata.Bootstrap.LOGGER;
import static com.motadata.Constants.*;

public class DatabaseConnection
{
    private DatabaseConnection(){}

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
