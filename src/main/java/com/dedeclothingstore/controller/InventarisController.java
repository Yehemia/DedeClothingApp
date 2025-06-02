package com.dedeclothingstore.controller;

import com.dedeclothingstore.database.DatabaseConnection;
import com.dedeclothingstore.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class InventarisController {
    @FXML private TableView<Product> productTable;
    @FXML private TextField searchField, nameField, priceField, stockField, skuField, colorField, supplierField;
    @FXML private ComboBox<String> categoryComboBox, sizeComboBox, itemsPerPageCombo;
    @FXML private Button saveButton, updateButton, cancelButton, prevPageBtn, nextPageBtn;
    @FXML private Label lowStockLabel, pageInfoLabel;

    // Table columns
    @FXML private TableColumn<Product, String> colName, colSKU, colCategory, colSize, colColor, colSupplier, colDate;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, Void> colAction;

    @FXML
    private VBox formContainer;


    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private Connection conn;
    private int currentPage = 1;
    private int itemsPerPage = 10;
    private int totalPages = 1;
    private Product selectedProduct;

    public InventarisController() {
        conn = DatabaseConnection.getConnection();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        loadProducts();
        setupPagination();
        checkLowStockItems();


        sizeComboBox.getSelectionModel().selectFirst();
        categoryComboBox.getSelectionModel().selectFirst();
        itemsPerPageCombo.getSelectionModel().select(1); // Default to 10 items per page

        itemsPerPageCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            try {
                itemsPerPage = Integer.parseInt(newVal);
                currentPage = 1;
                loadProducts();
            } catch (NumberFormatException e) {
                System.err.println("Invalid number: " + newVal);
            }
        });

    }

    private void setupTableColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSKU.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colColor.setCellValueFactory(new PropertyValueFactory<>("color"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("entryDate"));

        // Add edit and delete buttons to action column
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Hapus");

            {
                editButton.setStyle("-fx-background-color: #4a6fa5; -fx-text-fill: white;");
                deleteButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");

                editButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    editProduct(product);
                });

                deleteButton.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    deleteProduct(product);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });
    }

    private void setupPagination() {
        updatePaginationControls();
    }

    private void updatePaginationControls() {
        pageInfoLabel.setText(String.format("Halaman %d dari %d", currentPage, totalPages));

        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void loadProducts() {
        productList.clear();
        String query = "SELECT * FROM products LIMIT ? OFFSET ?";
        String countQuery = "SELECT COUNT(*) FROM products";

        try {
            // Get total count for pagination
            Statement countStmt = conn.createStatement();
            ResultSet countRs = countStmt.executeQuery(countQuery);
            if (countRs.next()) {
                int totalItems = countRs.getInt(1);
                totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            }

            // Get paginated data
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, itemsPerPage);
            stmt.setInt(2, (currentPage - 1) * itemsPerPage);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("sku"),
                        rs.getString("category"),
                        rs.getString("size"),
                        rs.getString("color"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity"),
                        rs.getString("supplier"),
                        rs.getString("entry_date")
                );
                productList.add(product);
            }

            productTable.setItems(productList);
            updatePaginationControls();
            checkLowStockItems();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data produk: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void searchProduct() {
        String searchKeyword = searchField.getText().trim();
        if (searchKeyword.isEmpty()) {
            loadProducts();
            return;
        }

        String query = "SELECT * FROM products WHERE name LIKE ? OR sku LIKE ? OR category LIKE ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, "%" + searchKeyword + "%");
            stmt.setString(2, "%" + searchKeyword + "%");
            stmt.setString(3, "%" + searchKeyword + "%");

            ResultSet rs = stmt.executeQuery();
            productList.clear();

            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("sku"),
                        rs.getString("category"),
                        rs.getString("size"),
                        rs.getString("color"),
                        rs.getDouble("price"),
                        rs.getInt("stock_quantity"),
                        rs.getString("supplier"),
                        rs.getString("entry_date")
                );
                productList.add(product);
            }

            productTable.setItems(productList);
            currentPage = 1;
            totalPages = 1;
            updatePaginationControls();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal mencari produk: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void resetSearch() {
        searchField.clear();
        currentPage = 1;
        loadProducts();
    }

    @FXML
    private void saveProduct() {
        if (!validateInputs()) return;

        String query = "INSERT INTO products (name, sku, category, size, color, price, stock_quantity, " +
                "supplier, entry_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            setProductStatementValues(stmt);
            stmt.setString(9, LocalDate.now().format(DateTimeFormatter.ISO_DATE));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Sukses", "Produk berhasil disimpan!", Alert.AlertType.INFORMATION);
                resetForm();
                loadProducts();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal menyimpan produk: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void updateProduct() {
        if (selectedProduct == null || !validateInputs()) return;

        String query = "UPDATE products SET name=?, sku=?, category=?, size=?, color=?, price=?, " +
                "stock_quantity=?, supplier=? WHERE product_id=?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            setProductStatementValues(stmt);
            stmt.setInt(9, selectedProduct.getProductId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Sukses", "Produk berhasil diupdate!", Alert.AlertType.INFORMATION);
                cancelEdit();
                loadProducts();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal mengupdate produk: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setProductStatementValues(PreparedStatement stmt) throws SQLException {
        stmt.setString(1, nameField.getText().trim());
        stmt.setString(2, skuField.getText().trim());
        stmt.setString(3, categoryComboBox.getValue());
        stmt.setString(4, sizeComboBox.getValue());
        stmt.setString(5, colorField.getText().trim());
        stmt.setDouble(6, Double.parseDouble(priceField.getText()));
        stmt.setInt(7, Integer.parseInt(stockField.getText()));
        stmt.setString(8, supplierField.getText().trim());
    }

    private boolean validateInputs() {
        if (nameField.getText().trim().isEmpty() ||
                skuField.getText().trim().isEmpty() ||
                priceField.getText().trim().isEmpty() ||
                stockField.getText().trim().isEmpty()) {
            showAlert("Peringatan", "Field dengan tanda * wajib diisi!", Alert.AlertType.WARNING);
            return false;
        }

        try {
            double price = Double.parseDouble(priceField.getText());
            if (price <= 0) {
                showAlert("Peringatan", "Harga harus lebih besar dari 0!", Alert.AlertType.WARNING);
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Peringatan", "Harga harus berupa angka!", Alert.AlertType.WARNING);
            return false;
        }

        try {
            int stock = Integer.parseInt(stockField.getText());
            if (stock < 0) {
                showAlert("Peringatan", "Stok tidak boleh negatif!", Alert.AlertType.WARNING);
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Peringatan", "Stok harus berupa bilangan bulat!", Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    @FXML
    private void editProduct(Product product) {
        selectedProduct = product;

        nameField.setText(product.getName());
        skuField.setText(product.getSku());
        categoryComboBox.setValue(product.getCategory());
        sizeComboBox.setValue(product.getSize());
        colorField.setText(product.getColor());
        priceField.setText(String.valueOf(product.getPrice()));
        stockField.setText(String.valueOf(product.getStockQuantity()));
        supplierField.setText(product.getSupplier());

        saveButton.setVisible(false);
        updateButton.setVisible(true);
        cancelButton.setVisible(true);
    }

    @FXML
    private void cancelEdit() {
        selectedProduct = null;
        resetForm();
        saveButton.setVisible(true);
        updateButton.setVisible(false);
        cancelButton.setVisible(false);
    }

    @FXML
    private void deleteProduct(Product product) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Konfirmasi Hapus");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Apakah Anda yakin ingin menghapus produk " + product.getName() + "?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String query = "DELETE FROM products WHERE product_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, product.getProductId());
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    showAlert("Sukses", "Produk berhasil dihapus!", Alert.AlertType.INFORMATION);
                    loadProducts();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Gagal menghapus produk: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void resetForm() {
        nameField.clear();
        skuField.clear();
        categoryComboBox.getSelectionModel().selectFirst();
        sizeComboBox.getSelectionModel().selectFirst();
        colorField.clear();
        priceField.clear();
        stockField.clear();
        supplierField.clear();
    }

    @FXML
    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadProducts();
        }
    }

    @FXML
    private void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadProducts();
        }
    }

    @FXML
    private void exportData() {
        showAlert("Info", "Cape kapan kapan aja deh, butuh support system ", Alert.AlertType.INFORMATION);
    }

    private void checkLowStockItems() {
        String query = "SELECT COUNT(*) FROM products WHERE stock_quantity < 5";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next() && rs.getInt(1) > 0) {
                lowStockLabel.setText("Peringatan: Ada " + rs.getInt(1) + " produk dengan stok rendah!");
            } else {
                lowStockLabel.setText("");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showAddForm() {
        formContainer.setVisible(true);   // Tampilkan form
        resetForm();                      // Kosongkan semua input
        saveButton.setVisible(true);     // Tampilkan tombol Simpan
        updateButton.setVisible(false);  // Sembunyikan tombol Update
    }

    private void addActionButtonsToTable() {
        colAction.setCellFactory(param -> new TableCell<Product, Void>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnEdit, btnDelete);

            {
                btnEdit.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    editProduct(product);
                });

                btnDelete.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    deleteProduct(product);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}