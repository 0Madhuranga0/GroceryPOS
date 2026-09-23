package com.gp.grocerypos.util;

import com.gp.grocerypos.config.AppConfig;
import com.gp.grocerypos.config.DatabaseConfig;
import com.gp.grocerypos.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Quick standalone test — verifies the database connection and prints
 * server info. Run this class directly to confirm connectivity before
 * starting the full application.
 *
 * Can be removed once Phase 2 is stable.
 */
public class DatabaseConnectionTest {

    public static void main(String[] args) {
        System.out.println("=== GroceryPOS Database Connection Test ===");

        try {
            AppConfig config = AppConfig.getInstance();
            DatabaseConfig dbConfig = config.getDatabaseConfig();

            System.out.println("Host     : " + dbConfig.getHost());
            System.out.println("Port     : " + dbConfig.getPort());
            System.out.println("Database : " + dbConfig.getDbName());
            System.out.println("User     : " + dbConfig.getUsername());
            System.out.println("JDBC URL : " + dbConfig.buildJdbcUrl());
            System.out.println();

            System.out.println("Attempting connection...");
            Connection conn = DatabaseConnection.getInstance().getConnection();

            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("SUCCESS!");
            System.out.println("Server   : " + meta.getDatabaseProductName()
                             + " " + meta.getDatabaseProductVersion());
            System.out.println("Driver   : " + meta.getDriverName()
                             + " " + meta.getDriverVersion());

            DatabaseConnection.getInstance().releaseConnection(conn);
            DatabaseConnection.getInstance().shutdown();

            System.out.println();
            System.out.println("Connection test PASSED. Database is ready.");

        } catch (Exception e) {
            System.err.println("FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
