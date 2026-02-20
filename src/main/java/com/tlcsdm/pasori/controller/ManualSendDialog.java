/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.controller;

import com.tlcsdm.pasori.config.I18N;
import com.tlcsdm.pasori.service.CommunicationBridgeService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dialog for manually sending hex data to the Antenna IF device.
 * Contains a send command input, send log, and receive log.
 */
public class ManualSendDialog {

    private static final String ICON_PATH = "/com/tlcsdm/pasori/images/logo.png";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Stage dialogStage;
    private final CommunicationBridgeService bridgeService;
    private final TextArea sendLogArea;
    private final TextArea receiveLogArea;
    private final TextField sendDataField;

    public ManualSendDialog(Stage ownerStage, CommunicationBridgeService bridgeService) {
        this.bridgeService = bridgeService;
        this.dialogStage = new Stage();
        this.sendLogArea = new TextArea();
        this.receiveLogArea = new TextArea();
        this.sendDataField = new TextField();

        setupDialog(ownerStage);
    }

    private void setupDialog(Stage ownerStage) {
        dialogStage.setTitle(I18N.get("dialog.manualSend.title"));
        dialogStage.initOwner(ownerStage);

        // Set icon
        try (InputStream iconStream = getClass().getResourceAsStream(ICON_PATH)) {
            if (iconStream != null) {
                dialogStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            // Ignore if icon cannot be loaded
        }

        // Build UI
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // Send controls
        sendDataField.setPromptText(I18N.get("dialog.manualSend.placeholder"));
        Button sendBtn = new Button(I18N.get("button.send"));
        sendBtn.setPrefWidth(80);
        sendBtn.setOnAction(e -> handleSend());
        sendDataField.setOnAction(e -> handleSend());

        HBox sendBox = new HBox(10, sendDataField, sendBtn);
        HBox.setHgrow(sendDataField, Priority.ALWAYS);
        sendBox.setAlignment(Pos.CENTER_LEFT);

        // Send log
        Label sendLogLabel = new Label(I18N.get("dialog.manualSend.sendLog"));
        sendLogLabel.setStyle("-fx-font-weight: bold;");
        Region sendSpacer = new Region();
        HBox.setHgrow(sendSpacer, Priority.ALWAYS);
        Button clearSendBtn = new Button(I18N.get("button.clear"));
        clearSendBtn.setOnAction(e -> sendLogArea.clear());
        HBox sendLogHeader = new HBox(10, sendLogLabel, sendSpacer, clearSendBtn);
        sendLogHeader.setAlignment(Pos.CENTER_LEFT);

        sendLogArea.setEditable(false);
        sendLogArea.setWrapText(true);
        sendLogArea.setStyle("-fx-font-family: monospace;");
        VBox.setVgrow(sendLogArea, Priority.ALWAYS);

        // Receive log
        Label receiveLogLabel = new Label(I18N.get("dialog.manualSend.receiveLog"));
        receiveLogLabel.setStyle("-fx-font-weight: bold;");
        Region receiveSpacer = new Region();
        HBox.setHgrow(receiveSpacer, Priority.ALWAYS);
        Button clearReceiveBtn = new Button(I18N.get("button.clear"));
        clearReceiveBtn.setOnAction(e -> receiveLogArea.clear());
        HBox receiveLogHeader = new HBox(10, receiveLogLabel, receiveSpacer, clearReceiveBtn);
        receiveLogHeader.setAlignment(Pos.CENTER_LEFT);

        receiveLogArea.setEditable(false);
        receiveLogArea.setWrapText(true);
        receiveLogArea.setStyle("-fx-font-family: monospace;");
        VBox.setVgrow(receiveLogArea, Priority.ALWAYS);

        root.getChildren().addAll(
            sendBox,
            sendLogHeader, sendLogArea,
            receiveLogHeader, receiveLogArea
        );

        Scene scene = new Scene(root, 600, 450);
        dialogStage.setScene(scene);
        dialogStage.setMinWidth(400);
        dialogStage.setMinHeight(300);

        // Cleanup on close
        dialogStage.setOnCloseRequest(e -> cleanup());
    }

    private void setupReceiveCallback() {
        bridgeService.setManualReceiveCallback(data ->
            Platform.runLater(() -> appendReceiveLog(data))
        );
    }

    private void handleSend() {
        String hexInput = sendDataField.getText().trim();
        if (hexInput.isEmpty()) {
            showAlert(I18N.get("error.title"), I18N.get("error.enterHexData"));
            return;
        }

        byte[] data;
        try {
            data = hexStringToByteArray(hexInput);
        } catch (IllegalArgumentException e) {
            showAlert(I18N.get("error.title"), I18N.get("error.invalidHexFormat"));
            return;
        }

        if (!bridgeService.isAntennaIfConnected()) {
            showAlert(I18N.get("error.title"), I18N.get("error.antennaNotConnected"));
            return;
        }

        int result = bridgeService.sendToAntennaIf(data);
        if (result > 0) {
            appendSendLog(data);
            sendDataField.clear();
        }
    }

    private void appendSendLog(byte[] data) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String hex = bytesToHex(data);
        sendLogArea.appendText("[" + timestamp + "] " + hex + "\n");
    }

    private void appendReceiveLog(byte[] data) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String hex = bytesToHex(data);
        receiveLogArea.appendText("[" + timestamp + "] " + hex + "\n");
    }

    private void cleanup() {
        bridgeService.setManualReceiveCallback(null);
    }

    /**
     * Show the dialog, or bring to front if already showing.
     */
    public void show() {
        if (dialogStage.isShowing()) {
            dialogStage.requestFocus();
        } else {
            setupReceiveCallback();
            dialogStage.show();
        }
    }

    /**
     * Check if the dialog is currently showing.
     */
    public boolean isShowing() {
        return dialogStage.isShowing();
    }

    private static byte[] hexStringToByteArray(String hex) {
        hex = hex.replaceAll("\\s+", "").toUpperCase();
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid hex character");
            }
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i]));
            if (i < bytes.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
