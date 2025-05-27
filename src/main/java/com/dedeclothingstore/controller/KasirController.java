package com.dedeclothingstore.controller;

import com.dedeclothingstore.database.DatabaseConnection;
import com.dedeclothingstore.model.Product;
import com.dedeclothingstore.model.Transaction;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class KasirController {
    @FXML
    private TableView<Product> productTable;
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

    int userId = LoginController.getCurrentUserId();

    private Connection conn;
    private final TransactionController transactionController = new TransactionController(); // Gunakan TransactionController

    public KasirController() {
        conn = DatabaseConnection.getConnection();
    }

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));

        loadProducts();
    }

    private void loadProducts() {
        List<Product> products = transactionController.getAvailableProducts(); // Ambil produk dari TransactionController
        productTable.getItems().setAll(products);
    }

    @FXML
    private void handlePayment() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            showAlert("Pilih produk terlebih dahulu!");
            return;
        }

        if (!transactionController.isUserExists(userId)) {
            showAlert("User tidak valid, silakan login ulang!");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText());
            if (quantity <= 0 || quantity > selectedProduct.getStockQuantity()) {
                showAlert("Jumlah tidak valid!");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Masukkan jumlah yang valid!");
            return;
        }

        double total = selectedProduct.getPrice() * quantity;
        totalLabel.setText("Rp " + total);

        String method = paymentMethod.getSelectionModel().getSelectedItem();
        if (method == null) {
            showAlert("Pilih metode pembayaran!");
            return;
        }


        Transaction transaction = new Transaction(userId,new Timestamp(System.currentTimeMillis()), total, method); // Buat objek transaksi
        boolean success = transactionController.addTransaction(transaction, selectedProduct.getProductId(), quantity);

        if (success) {
            showAlert("Transaksi berhasil!");
            loadProducts(); // Perbarui daftar produk setelah transaksi
        } else {
            showAlert("Transaksi gagal, coba lagi!");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Kasir");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}