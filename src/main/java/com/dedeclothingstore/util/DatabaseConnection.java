// src/main/java/com/dedeclothingstore/util/DatabaseConnection.java
package com.dedeclothingstore.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/dede_db"; //
    private static final String USER = "root"; //
    private static final String PASSWORD = ""; //
    private static Connection conn;

    private DatabaseConnection() {}

    public static Connection getConnection() {
        if (conn == null) {
            try {
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Koneksi ke database berhasil dibuat!");
            } catch (SQLException e) {
                System.err.println("Koneksi gagal: " + e.getMessage());
                return null;
            }
        }
        return conn;
    }
}