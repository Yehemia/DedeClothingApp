package com.dedeclothingstore.controller;

import com.dedeclothingstore.model.Transaction;
import com.dedeclothingstore.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label; // <-- Perubahan import
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

public class ManagerController {
    // ... (Deklarasi @FXML lain tidak berubah)
    @FXML
    private TableView<Transaction> transactionTable;
    @FXML
    private TableColumn<Transaction, Timestamp> colDate;
    @FXML
    private TableColumn<Transaction, String> colCashier;
    @FXML
    private TableColumn<Transaction, Double> colTotal;
    @FXML
    private TableColumn<Transaction, String> colMethod;
    @FXML
    private DatePicker startDate, endDate;
    @FXML
    private LineChart<String, Number> salesChart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;

    // --- PERUBAHAN DI SINI: Ganti Tile menjadi Label ---
    @FXML
    private Label totalRevenueLabel;
    @FXML
    private Label totalTransactionsLabel;
    @FXML
    private Label avgTransactionLabel;

    private Connection conn;
    private final ObservableList<Transaction> transactionList = FXCollections.observableArrayList();

    public ManagerController() {
        conn = DatabaseConnection.getConnection();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        loadAllData();
    }

    private void loadAllData() {
        loadDashboardData();
        loadTransactions();
        loadSalesChart();
    }

    private void setupTableColumns() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        colCashier.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        transactionTable.setItems(transactionList);
    }

    // --- PERUBAHAN DI SINI: Mengisi data ke Label ---
    private void loadDashboardData() {
        String query = "SELECT COUNT(*) as total_transaksi, SUM(total_price) as total_pendapatan, AVG(total_price) as rata_rata FROM transactions";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                double totalRevenue = rs.getDouble("total_pendapatan");
                long totalTransactions = rs.getLong("total_transaksi");
                double avgTransaction = rs.getDouble("rata_rata");

                // Format angka menjadi format mata uang Rupiah tanpa desimal
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
                currencyFormat.setMaximumFractionDigits(0);

                totalRevenueLabel.setText(currencyFormat.format(totalRevenue));
                totalTransactionsLabel.setText(String.valueOf(totalTransactions)); // Tampilkan sebagai angka bulat
                avgTransactionLabel.setText(currencyFormat.format(avgTransaction));
            }
        } catch (SQLException e) {
            showAlert("Error Database", "Gagal memuat data dashboard: " + e.getMessage());
        }
    }

    // ... (Sisa metode lainnya tetap sama persis seperti sebelumnya) ...
    @FXML
    public void loadTransactions() {
        transactionList.clear();
        String query = "SELECT * FROM transactions ORDER BY transaction_date DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                transactionList.add(createTransactionFromResultSet(rs));
            }
        } catch (SQLException e) {
            showAlert("Error Database", "Gagal memuat data transaksi: " + e.getMessage());
        }
    }

    @FXML
    private void filterTransactions() {
        LocalDate start = startDate.getValue();
        LocalDate end = endDate.getValue();
        if (start == null || end == null) {
            showAlert("Informasi", "Pilih rentang tanggal terlebih dahulu.");
            return;
        }
        if (start.isAfter(end)) {
            showAlert("Peringatan", "Tanggal mulai tidak boleh setelah tanggal akhir.");
            return;
        }
        transactionList.clear();
        String query = "SELECT * FROM transactions WHERE DATE(transaction_date) BETWEEN ? AND ? ORDER BY transaction_date DESC";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transactionList.add(createTransactionFromResultSet(rs));
            }
        } catch (SQLException e) {
            showAlert("Error Database", "Gagal memfilter data transaksi: " + e.getMessage());
        }
    }

    private void loadSalesChart() {
        salesChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Pendapatan Harian");
        String query = "SELECT DATE(transaction_date) AS tanggal, SUM(total_price) AS total FROM transactions GROUP BY tanggal ORDER BY tanggal ASC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(rs.getString("tanggal"), rs.getDouble("total")));
            }
            salesChart.getData().add(series);
        } catch (SQLException e) {
            showAlert("Error Chart", "Gagal memuat data grafik penjualan: " + e.getMessage());
        }
    }

    private Transaction createTransactionFromResultSet(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("transaction_id"),
                rs.getInt("user_id"),
                rs.getTimestamp("transaction_date"),
                rs.getDouble("total_price"),
                rs.getString("payment_method")
        );
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}