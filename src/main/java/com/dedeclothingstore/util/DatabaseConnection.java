package com.dedeclothingstore.util;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/dede_db";
    private static final String USER = "root"; // Sesuaikan dengan username MySQL-mu
    private static final String PASSWORD = ""; // Jika ada password, isi di sini

    public static Connection getConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Koneksi ke database berhasil!");
        } catch (SQLException e) {
            System.err.println("Koneksi gagal: " + e.getMessage());
        }
        return conn;
    }

    public static void main(String[] args) {
        getConnection(); // Tes koneksi
    }
}

