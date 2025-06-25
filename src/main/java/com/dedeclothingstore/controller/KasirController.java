package com.dedeclothingstore.controller;

import com.dedeclothingstore.model.Product;
import com.dedeclothingstore.model.Transaction;
import com.dedeclothingstore.util.DatabaseConnection;
import com.dedeclothingstore.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KasirController {
    // ... (Deklarasi @FXML tidak berubah)
    @FXML
    private TableView<Product> productTable;
    @FXML
    private TableView<Product> cartTable;
    @FXML
    private TextField quantityField;
    @FXML
    private Label totalLabel;
    @FXML
    private ComboBox<String> paymentMethod;
    @FXML
    private TableColumn<Product, String> colName;
    @FXML
    private TableColumn<Product, Double> colPrice;
    @FXML
    private TableColumn<Product, Integer> colStock;
    @FXML
    private TableColumn<Product, String> cartName;
    @FXML
    private TableColumn<Product, Double> cartPrice;
    @FXML
    private TableColumn<Product, Integer> cartQty;

    private final ObservableList<Product> cartItems = FXCollections.observableArrayList();
    private final Connection conn;

    public KasirController() {
        conn = DatabaseConnection.getConnection();
    }

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));

        cartName.setCellValueFactory(new PropertyValueFactory<>("name"));
        cartPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        cartQty.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));

        cartTable.setItems(cartItems);
        loadProducts();

        paymentMethod.setItems(FXCollections.observableArrayList("Cash", "Cashless - QRIS", "Cashless - Kartu"));
        totalLabel.setText("Rp 0.00");
    }

    @FXML
    private void handleRemoveFromCart() {
        Product selectedItem = cartTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("Peringatan", "Pilih item di keranjang yang ingin dihapus.");
            return;
        }

        cartItems.remove(selectedItem);
        updateTotal();
    }

    private void loadProducts() {
        List<Product> products = getAvailableProducts();
        productTable.getItems().setAll(products);
    }

    @FXML
    private void handleAddToCart() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            showAlert("Peringatan", "Pilih produk terlebih dahulu!");
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText());
            if (quantity <= 0 || quantity > selectedProduct.getStockQuantity()) {
                showAlert("Peringatan", "Jumlah tidak valid atau melebihi stok yang tersedia!");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Peringatan", "Masukkan jumlah dalam format angka yang benar!");
            return;
        }
        Product itemForCart = new Product(
                selectedProduct.getProductId(),
                selectedProduct.getName(),
                selectedProduct.getSku(),
                selectedProduct.getCategory(),
                selectedProduct.getSize(),
                selectedProduct.getColor(),
                selectedProduct.getPrice(),
                quantity,
                selectedProduct.getSupplier(),
                selectedProduct.getEntryDate()
        );
        cartItems.add(itemForCart);
        updateTotal();
        quantityField.clear();
    }

    private void updateTotal() {
        double total = 0;
        for (Product item : cartItems) {
            total += item.getPrice() * item.getStockQuantity();
        }
        totalLabel.setText("Rp " + String.format("%,.2f", total));
    }

    @FXML
    private void handlePayment() {
        if (cartItems.isEmpty()) {
            showAlert("Peringatan", "Keranjang belanja masih kosong!");
            return;
        }
        int userId = SessionManager.getInstance().getCurrentUser().getUserId();
        if (!isUserExists(userId)) {
            showAlert("Error", "User kasir tidak valid. Silakan login ulang.");
            return;
        }
        String method = paymentMethod.getSelectionModel().getSelectedItem();
        if (method == null || method.isEmpty()) {
            showAlert("Peringatan", "Pilih metode pembayaran terlebih dahulu!");
            return;
        }
        double total = 0;
        for (Product item : cartItems) {
            total += item.getPrice() * item.getStockQuantity();
        }
        Transaction transaction = new Transaction(userId, new Timestamp(System.currentTimeMillis()), total, method);
        boolean success = addTransactionBatch(transaction, cartItems);
        if (success) {
            showAlert("Sukses", "Transaksi berhasil disimpan!");
            cartItems.clear();
            updateTotal();
            loadProducts();
        } else {
            showAlert("Error", "Transaksi gagal. Terjadi kesalahan pada database.");
        }
    }

    private boolean isUserExists(int userId) {
        String sql = "SELECT user_id FROM users WHERE user_id = ? AND role = 'Kasir'";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<Product> getAvailableProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE stock_quantity > 0";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
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

    private boolean addTransactionBatch(Transaction transaction, List<Product> cartItems) {
        String insertTransaction = "INSERT INTO transactions (user_id, transaction_date, total_price, payment_method) VALUES (?, ?, ?, ?)";
        String insertDetail = "INSERT INTO transaction_details (transaction_id, product_id, quantity, subtotal) VALUES (?, ?, ?, ?)";
        String updateStock = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";
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
                            detailStmt.setDouble(4, item.getPrice() * item.getStockQuantity());
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

    private void showAlert(String title, String message) {
        Alert.AlertType type = title.equals("Error") ? Alert.AlertType.ERROR : (title.equals("Peringatan") ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}