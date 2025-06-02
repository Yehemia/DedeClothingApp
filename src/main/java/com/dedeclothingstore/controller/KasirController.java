package com.dedeclothingstore.controller;

import com.dedeclothingstore.database.DatabaseConnection;
import com.dedeclothingstore.model.Product;
import com.dedeclothingstore.model.Transaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class KasirController {
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

    private final TransactionController transactionController = new TransactionController();
    private final int userId = LoginController.getCurrentUserId();
    private Connection conn;

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
    }

    private void loadProducts() {
        List<Product> products = transactionController.getAvailableProducts();
        productTable.getItems().setAll(products);
    }

    @FXML
    private void handleAddToCart() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            showAlert("Pilih produk terlebih dahulu!");
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

        Product item = new Product(
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

        cartItems.add(item);
        updateTotal();
    }

    private void updateTotal() {
        double total = 0;
        for (Product item : cartItems) {
            total += item.getPrice() * item.getStockQuantity();
        }
        totalLabel.setText("Rp " + total);
    }

    @FXML
    private void handlePayment() {
        if (cartItems.isEmpty()) {
            showAlert("Keranjang kosong!");
            return;
        }

        if (!transactionController.isUserExists(userId)) {
            showAlert("User tidak valid, silakan login ulang!");
            return;
        }

        String method = paymentMethod.getSelectionModel().getSelectedItem();
        if (method == null) {
            showAlert("Pilih metode pembayaran!");
            return;
        }

        double total = 0;
        for (Product item : cartItems) {
            total += item.getPrice() * item.getStockQuantity();
        }

        Transaction transaction = new Transaction(userId, new Timestamp(System.currentTimeMillis()), total, method);
        boolean success = transactionController.addTransactionBatch(transaction, cartItems);

        if (success) {
            showAlert("Transaksi berhasil!");
            cartItems.clear();
            updateTotal();
            loadProducts();
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
