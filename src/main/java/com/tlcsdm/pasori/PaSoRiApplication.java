/*
 * Copyright (c) 2026, 梦里不知身是客
 * Licensed under the MIT License
 */
package com.tlcsdm.pasori;

import com.tlcsdm.pasori.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

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
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        
        Scene scene = new Scene(root, 900, 700);
        
        primaryStage.setTitle("PaSoRi IF Tool - Serial Communication Bridge");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
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
