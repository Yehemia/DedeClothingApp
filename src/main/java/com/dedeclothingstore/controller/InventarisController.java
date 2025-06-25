package com.dedeclothingstore.controller;

import com.dedeclothingstore.model.Product;
import com.dedeclothingstore.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class InventarisController {
    // Deklarasi FXML dari file .fxml
    @FXML
    private TableView<Product> productTable;
    @FXML
    private TextField searchField, nameField, priceField, stockField, skuField, colorField, supplierField;
    @FXML
    private ComboBox<String> categoryComboBox, sizeComboBox, itemsPerPageCombo;
    @FXML
    private Button saveButton, updateButton, cancelButton, prevPageBtn, nextPageBtn;
    @FXML
    private Label lowStockLabel, pageInfoLabel;
    @FXML
    private TableColumn<Product, String> colName, colSKU, colCategory, colSize, colColor, colSupplier, colDate;
    @FXML
    private TableColumn<Product, Double> colPrice;
    @FXML
    private TableColumn<Product, Integer> colStock;
    @FXML
    private TableColumn<Product, Void> colAction;

    // Deklarasi untuk VBox yang berisi form
    @FXML
    private VBox formContainer;

    // Variabel instance
    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final Connection conn;
    private int currentPage = 1;
    private int itemsPerPage = 10;
    private int totalPages = 1;
    private Product selectedProduct;

    public InventarisController() {
        conn = DatabaseConnection.getConnection();
    }

    @FXML
    public void initialize() {
        // Setup UI
        setupTableColumns();
        setupPagination();
        setupFormControls();

        // Muat data awal
        loadProducts();
        checkLowStockItems();

        // Listener untuk mengubah item per halaman
        itemsPerPageCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                itemsPerPage = Integer.parseInt(newVal);
                currentPage = 1;
                loadProducts();
            }
        });
    }

    // --- METODE SETUP UI ---

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
        addActionButtonsToTable();
    }

    private void setupPagination() {
        itemsPerPageCombo.setItems(FXCollections.observableArrayList("5", "10", "20", "50"));
        itemsPerPageCombo.setValue("10");
        pageInfoLabel.setText(String.format("Halaman %d dari %d", currentPage, totalPages));
        prevPageBtn.setGraphic(new FontIcon(FontAwesomeSolid.ANGLE_LEFT));
        nextPageBtn.setGraphic(new FontIcon(FontAwesomeSolid.ANGLE_RIGHT));
    }

    private void setupFormControls(){
        categoryComboBox.setItems(FXCollections.observableArrayList("Kemeja", "Kaos", "Celana", "Jaket", "Aksesoris"));
        sizeComboBox.setItems(FXCollections.observableArrayList("S", "M", "L", "XL", "XXL", "All Size"));

        nameField.setPromptText("Contoh: Kemeja Lengan Panjang");
        skuField.setPromptText("Opsional, contoh: KLP-001");
        colorField.setPromptText("Opsional, contoh: Biru Navy");
        priceField.setPromptText("Hanya angka, contoh: 150000");
        stockField.setPromptText("Hanya angka, contoh: 50");
        supplierField.setPromptText("Opsional");

        saveButton.setGraphic(new FontIcon(FontAwesomeSolid.SAVE));
        updateButton.setGraphic(new FontIcon(FontAwesomeSolid.PENCIL_ALT));
        cancelButton.setGraphic(new FontIcon(FontAwesomeSolid.TIMES));
    }


    // --- LOGIKA FORM (TAMPIL/SEMBUNYI) ---

    @FXML
    private void showAddForm() {
        formContainer.setVisible(true);
        formContainer.setManaged(true);
        resetForm();
        saveButton.setDisable(false);
        updateButton.setDisable(true);
        cancelButton.setDisable(false);
    }

    private void editProduct(Product product) {
        formContainer.setVisible(true);
        formContainer.setManaged(true);

        selectedProduct = product;
        nameField.setText(product.getName());
        skuField.setText(product.getSku());
        categoryComboBox.setValue(product.getCategory());
        sizeComboBox.setValue(product.getSize());
        colorField.setText(product.getColor());
        priceField.setText(String.valueOf(product.getPrice()));
        stockField.setText(String.valueOf(product.getStockQuantity()));
        supplierField.setText(product.getSupplier());

        saveButton.setDisable(true);
        updateButton.setDisable(false);
        cancelButton.setDisable(false);
    }

    @FXML
    private void cancelEdit() {
        formContainer.setVisible(false);
        formContainer.setManaged(false);
        resetForm();
        saveButton.setDisable(true);
        updateButton.setDisable(true);
        cancelButton.setDisable(true);
    }

    @FXML
    private void resetForm() {
        selectedProduct = null;
        nameField.clear();
        skuField.clear();
        if(!categoryComboBox.getItems().isEmpty()) categoryComboBox.getSelectionModel().selectFirst();
        if(!sizeComboBox.getItems().isEmpty()) sizeComboBox.getSelectionModel().selectFirst();
        colorField.clear();
        priceField.clear();
        stockField.clear();
        supplierField.clear();
    }


    // --- LOGIKA DATABASE (CRUD) ---

    @FXML
    private void loadProducts() {
        productList.clear();
        String countQuery = "SELECT COUNT(*) FROM products";
        String query = "SELECT * FROM products ORDER BY product_id DESC LIMIT ? OFFSET ?";
        try {
            try (Statement countStmt = conn.createStatement();
                 ResultSet countRs = countStmt.executeQuery(countQuery)) {
                if (countRs.next()) {
                    int totalItems = countRs.getInt(1);
                    totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, itemsPerPage);
                stmt.setInt(2, (currentPage - 1) * itemsPerPage);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    productList.add(createProductFromResultSet(rs));
                }
            }
            productTable.setItems(productList);
            updatePaginationControls();
        } catch (SQLException e) {
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
        productList.clear();
        String query = "SELECT * FROM products WHERE name LIKE ? OR sku LIKE ? OR category LIKE ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            String keyword = "%" + searchKeyword + "%";
            stmt.setString(1, keyword);
            stmt.setString(2, keyword);
            stmt.setString(3, keyword);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                productList.add(createProductFromResultSet(rs));
            }
            currentPage = 1;
            totalPages = 1;
            updatePaginationControls();
        } catch (SQLException e) {
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
        String query = "INSERT INTO products (name, sku, category, size, color, price, stock_quantity, supplier, entry_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            setProductStatementValues(stmt);
            stmt.setString(9, LocalDate.now().format(DateTimeFormatter.ISO_DATE));
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Sukses", "Produk berhasil disimpan!", Alert.AlertType.INFORMATION);
                cancelEdit();
                loadProducts();
                checkLowStockItems();
            }
        } catch (SQLException e) {
            showAlert("Error", "Gagal menyimpan produk: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void updateProduct() {
        if (selectedProduct == null || !validateInputs()) return;
        String query = "UPDATE products SET name=?, sku=?, category=?, size=?, color=?, price=?, stock_quantity=?, supplier=? WHERE product_id=?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            setProductStatementValues(stmt);
            stmt.setInt(9, selectedProduct.getProductId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                showAlert("Sukses", "Produk berhasil diupdate!", Alert.AlertType.INFORMATION);
                cancelEdit();
                loadProducts();
                checkLowStockItems();
            }
        } catch (SQLException e) {
            showAlert("Error", "Gagal mengupdate produk: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void deleteProduct(Product product) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Apakah Anda yakin ingin menghapus produk " + product.getName() + "?", ButtonType.OK, ButtonType.CANCEL);
        confirmation.setTitle("Konfirmasi Hapus");
        confirmation.setHeaderText(null);
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String query = "DELETE FROM products WHERE product_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, product.getProductId());
                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    showAlert("Sukses", "Produk berhasil dihapus!", Alert.AlertType.INFORMATION);
                    loadProducts();
                    checkLowStockItems();
                }
            } catch (SQLException e) {
                showAlert("Error", "Gagal menghapus produk: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }


    // --- METODE BANTUAN (HELPER) ---

    private void updatePaginationControls() {
        pageInfoLabel.setText(String.format("Halaman %d dari %d", currentPage, totalPages));
        prevPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage >= totalPages);
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
        if (nameField.getText().trim().isEmpty() || priceField.getText().trim().isEmpty() || stockField.getText().trim().isEmpty()) {
            showAlert("Peringatan", "Field Nama, Harga, dan Stok wajib diisi!", Alert.AlertType.WARNING);
            return false;
        }
        try {
            Double.parseDouble(priceField.getText());
            Integer.parseInt(stockField.getText());
        } catch (NumberFormatException e) {
            showAlert("Peringatan", "Harga dan Stok harus berupa angka!", Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    private Product createProductFromResultSet(ResultSet rs) throws SQLException {
        return new Product(
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
    }

    private void addActionButtonsToTable() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button("", new FontIcon(FontAwesomeSolid.PENCIL_ALT));
            private final Button btnDelete = new Button("", new FontIcon(FontAwesomeSolid.TRASH));
            private final HBox pane = new HBox(5, btnEdit, btnDelete);
            {
                btnEdit.getStyleClass().addAll("btn", "btn-success");
                btnDelete.getStyleClass().addAll("btn", "btn-danger");
                btnEdit.setOnAction(event -> editProduct(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> deleteProduct(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void checkLowStockItems() {
        String query = "SELECT COUNT(*) FROM products WHERE stock_quantity < 5";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                int lowStockCount = rs.getInt(1);
                lowStockLabel.setText(lowStockCount > 0 ? "Peringatan: Ada " + lowStockCount + " produk dengan stok rendah!" : "");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}