package com.dedeclothingstore.controller;

import com.dedeclothingstore.model.User;
import com.dedeclothingstore.util.DatabaseConnection;
import com.dedeclothingstore.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;

    private Connection conn;

    public LoginController() {
        conn = DatabaseConnection.getConnection();
    }

    @FXML
    public void initialize() {
        loginButton.setGraphic(new FontIcon(FontAwesomeSolid.SIGN_IN_ALT));
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
                User loggedInUser = new User(
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("role"),
                        rs.getString("username"),
                        null
                );
                SessionManager.getInstance().login(loggedInUser);

                String role = loggedInUser.getRole();
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
                showAlert("Login Gagal", "Username atau password salah.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error Database", "Gagal terhubung ke database.");
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}