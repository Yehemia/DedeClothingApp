package com.dedeclothingstore.controller;

import com.dedeclothingstore.database.DatabaseConnection;
import com.dedeclothingstore.model.Product;
import com.dedeclothingstore.model.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionController {
    private final Connection conn;

    public TransactionController() {
        conn = DatabaseConnection.getConnection();
    }

    public boolean isUserExists(int userId) {
        String sql = "SELECT id FROM users WHERE id = ? AND role = 'kasir'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Product> getAvailableProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE stock_quantity > 0";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity")
                );
                products.add(product);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return products;
    }

    public boolean addTransactionBatch(Transaction transaction, List<Product> cartItems) {
        String insertTransaction = "INSERT INTO transactions (user_id, date, total, payment_method) VALUES (?, ?, ?, ?)";
        String insertDetail = "INSERT INTO transaction_details (transaction_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        String updateStock = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?";

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement transStmt = conn.prepareStatement(insertTransaction, Statement.RETURN_GENERATED_KEYS)) {
                transStmt.setInt(1, transaction.getUserId());
                transStmt.setTimestamp(2, transaction.getTransactionDate());
                transStmt.setDouble(3, transaction.getTotalPrice());
                transStmt.setString(4, transaction.getPaymentMethod());
                transStmt.executeUpdate();

                ResultSet generatedKeys = transStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int transactionId = generatedKeys.getInt(1);

                    try (PreparedStatement detailStmt = conn.prepareStatement(insertDetail);
                         PreparedStatement stockStmt = conn.prepareStatement(updateStock)) {
                        for (Product item : cartItems) {
                            detailStmt.setInt(1, transactionId);
                            detailStmt.setInt(2, item.getProductId());
                            detailStmt.setInt(3, item.getStockQuantity());
                            detailStmt.setDouble(4, item.getPrice());
                            detailStmt.addBatch();

                            stockStmt.setInt(1, item.getStockQuantity());
                            stockStmt.setInt(2, item.getProductId());
                            stockStmt.addBatch();
                        }
                        detailStmt.executeBatch();
                        stockStmt.executeBatch();
                    }
                } else {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
