/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori;

import com.tlcsdm.pasori.config.AppSettings;
import com.tlcsdm.pasori.config.I18N;
import com.tlcsdm.pasori.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Main JavaFX Application for PaSoRi-IF serial communication tool.
 * 
 * This application allows communication between:
 * - PaSoRi device (via SDK serial port)
 * - USB CDC-ACM device (アンテナIF仕様)
 */
public class PaSoRiApplication extends Application {

    private MainController controller;

    @Override
    public void init() {
        // Apply saved theme before UI is created
        AppSettings.getInstance().applyInitialSettings();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        loader.setResources(java.util.ResourceBundle.getBundle(
            "com.tlcsdm.pasori.i18n.messages", I18N.getCurrentLocale()));
        Parent root = loader.load();
        controller = loader.getController();
        controller.setPrimaryStage(primaryStage);
        
        Scene scene = new Scene(root, 900, 700);
        
        primaryStage.setTitle(I18N.get("app.title"));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        // Set application icon
        try (InputStream iconStream = getClass().getResourceAsStream("images/logo.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            } else {
                System.err.println("Warning: Application icon not found at images/logo.png");
            }
        }
        
        primaryStage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.shutdown();
            }
        });
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
