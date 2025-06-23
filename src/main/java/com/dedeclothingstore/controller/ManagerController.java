package com.dedeclothingstore.controller;
import com.dedeclothingstore.util.DatabaseConnection;
import com.dedeclothingstore.model.Transaction;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Alert;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ManagerController {
    @FXML
    private TableColumn<Transaction, Timestamp> colDate;
    @FXML
    private TableColumn<Transaction, String> colCashier;
    @FXML
    private TableColumn<Transaction, Double> colTotal;
    @FXML
    private TableColumn<Transaction, String> colMethod;
    @FXML
    private TableView<Transaction> transactionTable;
    @FXML
    private DatePicker startDate, endDate;
    @FXML
    private LineChart<String, Number> salesChart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;


    private Connection conn;

    public ManagerController() {
        conn = DatabaseConnection.getConnection();
    }

    @FXML
    public void initialize() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));
        colCashier.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        loadTransactions();
        loadSalesChart();
    }

    private void loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT * FROM transactions ORDER BY transaction_date DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                transactions.add(new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getInt("user_id"),
                        rs.getTimestamp("transaction_date"),
                        rs.getDouble("total_price"),
                        rs.getString("payment_method")
                ));
            }
            transactionTable.getItems().setAll(transactions);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void filterTransactions() {
        LocalDate start = startDate.getValue();
        LocalDate end = endDate.getValue();

        if (start == null || end == null) {
            showAlert("Pilih rentang tanggal terlebih dahulu.");
            return;
        }

        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT * FROM transactions WHERE transaction_date BETWEEN ? AND ? ORDER BY transaction_date DESC";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transactions.add(new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getInt("user_id"),
                        rs.getTimestamp("transaction_date"),
                        rs.getDouble("total_price"),
                        rs.getString("payment_method")
                ));
            }
            transactionTable.getItems().setAll(transactions);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSalesChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Pendapatan Harian");

        String query = "SELECT DATE(transaction_date) AS tanggal, SUM(total_price) AS total FROM transactions GROUP BY tanggal ORDER BY tanggal";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                series.getData().add(new XYChart.Data<>(rs.getString("tanggal"), rs.getDouble("total")));
            }
            salesChart.getData().add(series);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Dashboard Manajer");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

