package com.dedeclothingstore;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

public class MainApp extends Application {
    private static final double APP_WIDTH = 1280;
    private static final double APP_HEIGHT = 720;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/com/dedeclothingstore/fxml/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), APP_WIDTH, APP_HEIGHT);
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());

        stage.setTitle("Dede Clothing Store");
        stage.setScene(scene);
        stage.setMinWidth(APP_WIDTH);
        stage.setMinHeight(APP_HEIGHT);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}