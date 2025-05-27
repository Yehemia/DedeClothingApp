package com.dedeclothingstore.controller;

import com.dedeclothingstore.database.DatabaseConnection;
import com.dedeclothingstore.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventarisController {
    @FXML
    private TableView<Product> productTable;
    @FXML
    private TextField searchField, nameField, categoryField, priceField, stockField;
    @FXML
    private TableColumn<Product, String> colName;
    @FXML
    private TableColumn<Product, String> colCategory;
    @FXML
    private TableColumn<Product, Double> colPrice;
    @FXML
    private TableColumn<Product, Integer> colStock;


    private Connection conn;

    public InventarisController() {
        conn = DatabaseConnection.getConnection();
    }
    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        loadProducts();
    }

    @FXML
    private void loadProducts() {
        System.out.println("Memuat data produk...");
        ObservableList<Product> products = FXCollections.observableArrayList();
        String query = "SELECT * FROM products";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity")
                );
                products.add(product);
            }
            productTable.setItems(products);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




            @FXML
    private void searchProduct() {
        String searchKeyword = searchField.getText();
        List<Product> products = new ArrayList<>();
        String query = "SELECT * FROM products WHERE name LIKE ?;";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, "%" + searchKeyword + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity")
                ));
            }
            productTable.getItems().setAll(products);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void saveProduct() {
        String name = nameField.getText();
        String category = categoryField.getText();
        double price;
        int stock;

        try {
            price = Double.parseDouble(priceField.getText());
            stock = Integer.parseInt(stockField.getText());
        } catch (NumberFormatException e) {
            showAlert("Harga dan Stok harus berupa angka!");
            return;
        }

        String query = "INSERT INTO products (name, category, price, stock_quantity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setDouble(3, price);
            stmt.setInt(4, stock);
            stmt.executeUpdate();
            showAlert("Produk berhasil disimpan!");
            loadProducts();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Inventaris");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

