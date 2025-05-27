package com.dedeclothingstore.controller;

import com.dedeclothingstore.database.DatabaseConnection;
import com.dedeclothingstore.model.TransactionDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionDetailController {
    private Connection conn;

    public TransactionDetailController() {
        conn = DatabaseConnection.getConnection();
    }

    // Ambil semua detail transaksi berdasarkan ID transaksi
    public List<TransactionDetail> getTransactionDetails(int transactionId) {
        List<TransactionDetail> details = new ArrayList<>();
        String query = "SELECT * FROM transaction_details WHERE transaction_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, transactionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                details.add(new TransactionDetail(
                        rs.getInt("detail_id"),
                        rs.getInt("transaction_id"),
                        rs.getInt("product_id"),
                        rs.getInt("quantity"),
                        rs.getDouble("subtotal")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return details;
    }

    // Tambah detail transaksi baru
    public boolean addTransactionDetail(TransactionDetail detail) {
        String query = "INSERT INTO transaction_details (transaction_id, product_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, detail.getTransactionId());
            stmt.setInt(2, detail.getProductId());
            stmt.setInt(3, detail.getQuantity());
            stmt.setDouble(4, detail.getSubtotal());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
