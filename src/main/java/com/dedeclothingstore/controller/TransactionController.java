package com.dedeclothingstore.controller;

import com.dedeclothingstore.database.DatabaseConnection;
import com.dedeclothingstore.model.Product;
import com.dedeclothingstore.model.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionController {
    private Connection conn;

    public TransactionController() {
        conn = DatabaseConnection.getConnection();
    }

    public List<Product> getAvailableProducts() {
        List<Product> products = new ArrayList<>();
        String query = "SELECT product_id, name, category, price, stock_quantity FROM products";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    private void updateStock(int productId, int quantity) {
        String query = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
            System.out.println("Stok produk berhasil diperbarui!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean addTransaction(Transaction transaction, int productId, int quantity) {
        String query = "INSERT INTO transactions (user_id, total_price, payment_method) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, transaction.getUserId());
            stmt.setDouble(2, transaction.getTotalPrice());
            stmt.setString(3, transaction.getPaymentMethod());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int transactionId = rs.getInt(1);
                    boolean detailsSaved = addTransactionDetail(transactionId, productId, quantity, transaction.getTotalPrice());

                    if (detailsSaved) {
                        updateStock(productId, quantity);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    private boolean addTransactionDetail(int transactionId, int productId, int quantity, double subtotal) {
        String query = "INSERT INTO transaction_details (transaction_id, product_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, transactionId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.setDouble(4, subtotal);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getUserNameById(int userId) {
        String query = "SELECT name FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown User"; // Jika tidak ditemukan
    }

    public boolean isUserExists(int userId) {
        String query = "SELECT 1 FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // return true jika user ditemukan
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}