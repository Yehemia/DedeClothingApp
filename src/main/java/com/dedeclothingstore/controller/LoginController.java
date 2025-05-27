package com.dedeclothingstore.controller;

import javafx.fxml.FXML;
import com.dedeclothingstore.database.DatabaseConnection;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.sql.*;


public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    private Connection conn;

    private static int currentUserId;

    public static int getCurrentUserId() {
        return currentUserId;
    }


    public static void setCurrentUserId(int currentUserId) {
        LoginController.currentUserId = currentUserId;
    }

    public LoginController() {
        conn = DatabaseConnection.getConnection();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        String query = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                setCurrentUserId(rs.getInt("user_id"));
                String role = rs.getString("role"); // Ambil peran user

                switch (role) {
                    case "Kasir":
                        loadScene("/com/dedeclothingstore/fxml/kasir.fxml");
                        break;
                    case "Manajer":
                        loadScene("/com/dedeclothingstore/fxml/manager.fxml");
                        break;
                    case "Staf Inventaris":
                        loadScene("/com/dedeclothingstore/fxml/inventaris.fxml");
                        break;
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Gagal");
                alert.setHeaderText(null);
                alert.setContentText("Username atau password salah.");
                alert.showAndWait();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}