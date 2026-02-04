/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.controller;

import com.fazecast.jSerialComm.SerialPort;
import com.tlcsdm.pasori.config.AppSettings;
import com.tlcsdm.pasori.config.I18N;
import com.tlcsdm.pasori.model.LogEntry;
import com.tlcsdm.pasori.model.SerialPortConfig;
import com.tlcsdm.pasori.service.CommunicationBridgeService;
import com.tlcsdm.pasori.service.SerialPortService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main controller for the PaSoRi IF Tool application.
 * Manages serial port connections and communication bridging.
 */
public class MainController implements Initializable {

    // PaSoRi connection controls
    @FXML private ComboBox<String> pasoriPortCombo;
    @FXML private ComboBox<Integer> pasoriBaudCombo;
    @FXML private Button pasoriConnectBtn;
    @FXML private Button pasoriDisconnectBtn;
    @FXML private Circle pasoriStatusIndicator;
    @FXML private Label pasoriStatusLabel;

    // アンテナIF connection controls
    @FXML private ComboBox<String> antennaPortCombo;
    @FXML private ComboBox<Integer> antennaBaudCombo;
    @FXML private Button antennaConnectBtn;
    @FXML private Button antennaDisconnectBtn;
    @FXML private Circle antennaStatusIndicator;
    @FXML private Label antennaStatusLabel;

    // Bridge controls
    @FXML private ToggleButton bridgeToggle;
    @FXML private Button refreshPortsBtn;

    // Communication log
    @FXML private ListView<String> logListView;
    @FXML private Button clearLogBtn;
    @FXML private CheckBox autoScrollCheck;

    // Manual send controls
    @FXML private TextField sendDataField;
    @FXML private ToggleGroup sendTargetGroup;
    @FXML private RadioButton sendToPasoriRadio;
    @FXML private RadioButton sendToAntennaRadio;
    @FXML private Button sendBtn;

    private final CommunicationBridgeService bridgeService;
    private final ObservableList<String> logItems;
    private final ObservableList<Integer> baudRates;

    private Stage primaryStage;
    private ResourceBundle resources;

    private static final int MAX_LOG_ENTRIES = 1000;

    public MainController() {
        this.bridgeService = new CommunicationBridgeService();
        this.logItems = FXCollections.observableArrayList();
        this.baudRates = FXCollections.observableArrayList(
            9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600
        );
    }

    /**
     * Set the primary stage reference for settings dialog.
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;

        // Setup baud rate options
        pasoriBaudCombo.setItems(baudRates);
        pasoriBaudCombo.setValue(115200);
        antennaBaudCombo.setItems(baudRates);
        antennaBaudCombo.setValue(115200);

        // Setup log view
        logListView.setItems(logItems);
        autoScrollCheck.setSelected(true);

        // Setup bridge service log callback
        bridgeService.setLogCallback(entry -> 
            Platform.runLater(() -> addLogEntry(entry.toString()))
        );

        // Initial port refresh
        refreshPorts();

        // Setup UI states
        updateConnectionButtons();

        // Setup send target group
        sendToPasoriRadio.setUserData("pasori");
        sendToAntennaRadio.setUserData("antenna");

        addLogEntry("[System] " + I18N.get("system.appStarted"));

        // Register for settings changes
        AppSettings.getInstance().setOnSettingsChanged(this::onLanguageChanged);
    }

    private void onLanguageChanged() {
        // Show restart required message
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(I18N.get("settings.title"));
            alert.setHeaderText(null);
            alert.setContentText("Language changed. Please restart the application for full effect.");
            alert.showAndWait();
        });
    }

    @FXML
    private void handleOpenSettings() {
        AppSettings.getInstance().getPreferencesFx().show(true);
    }

    @FXML
    private void handleExit() {
        shutdown();
        if (primaryStage != null) {
            primaryStage.close();
        }
        Platform.exit();
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18N.get("menu.about"));
        alert.setHeaderText("PaSoRi IF Tool");
        alert.setContentText("Version 1.0.0\n\nJavaFX tool for serial communication bridging between PaSoRi device and Panasonic USB CDC-ACM (アンテナIF仕様) device.");
        alert.showAndWait();
    }

    @FXML
    private void handleRefreshPorts() {
        refreshPorts();
    }

    @FXML
    private void handlePasoriConnect() {
        String portName = pasoriPortCombo.getValue();
        Integer baudRate = pasoriBaudCombo.getValue();

        if (portName == null || portName.isEmpty()) {
            showAlert(I18N.get("error.title"), I18N.get("error.selectPasoriPort"));
            return;
        }

        SerialPortConfig config = new SerialPortConfig();
        config.setPortName(portName);
        config.setBaudRate(baudRate != null ? baudRate : 115200);

        if (bridgeService.connectPaSoRi(config)) {
            updateConnectionButtons();
            updateStatusIndicator(pasoriStatusIndicator, pasoriStatusLabel, true, I18N.get("pasori.connected"));
        } else {
            showAlert(I18N.get("error.connectionError"), I18N.get("error.pasoriConnectFailed", portName));
        }
    }

    @FXML
    private void handlePasoriDisconnect() {
        bridgeService.disconnectPaSoRi();
        updateConnectionButtons();
        updateStatusIndicator(pasoriStatusIndicator, pasoriStatusLabel, false, I18N.get("pasori.disconnected"));
    }

    @FXML
    private void handleAntennaConnect() {
        String portName = antennaPortCombo.getValue();
        Integer baudRate = antennaBaudCombo.getValue();

        if (portName == null || portName.isEmpty()) {
            showAlert(I18N.get("error.title"), I18N.get("error.selectAntennaPort"));
            return;
        }

        SerialPortConfig config = new SerialPortConfig();
        config.setPortName(portName);
        config.setBaudRate(baudRate != null ? baudRate : 115200);

        if (bridgeService.connectAntennaIf(config)) {
            updateConnectionButtons();
            updateStatusIndicator(antennaStatusIndicator, antennaStatusLabel, true, I18N.get("antenna.connected"));
        } else {
            showAlert(I18N.get("error.connectionError"), I18N.get("error.antennaConnectFailed", portName));
        }
    }

    @FXML
    private void handleAntennaDisconnect() {
        bridgeService.disconnectAntennaIf();
        updateConnectionButtons();
        updateStatusIndicator(antennaStatusIndicator, antennaStatusLabel, false, I18N.get("antenna.disconnected"));
    }

    @FXML
    private void handleBridgeToggle() {
        boolean enabled = bridgeToggle.isSelected();
        bridgeService.setBridgingEnabled(enabled);
        bridgeToggle.setText(enabled ? I18N.get("bridge.on") : I18N.get("bridge.off"));
    }

    @FXML
    private void handleClearLog() {
        logItems.clear();
    }

    @FXML
    private void handleSendData() {
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

        Toggle selectedToggle = sendTargetGroup.getSelectedToggle();
        if (selectedToggle == null) {
            showAlert(I18N.get("error.title"), I18N.get("error.selectTargetDevice"));
            return;
        }

        String target = (String) selectedToggle.getUserData();
        int result;
        if ("pasori".equals(target)) {
            if (!bridgeService.isPaSoRiConnected()) {
                showAlert(I18N.get("error.title"), I18N.get("error.pasoriNotConnected"));
                return;
            }
            result = bridgeService.sendToPaSoRi(data);
        } else {
            if (!bridgeService.isAntennaIfConnected()) {
                showAlert(I18N.get("error.title"), I18N.get("error.antennaNotConnected"));
                return;
            }
            result = bridgeService.sendToAntennaIf(data);
        }

        if (result > 0) {
            sendDataField.clear();
        }
    }

    private void refreshPorts() {
        String[] portNames = SerialPortService.getAvailablePortNames();
        
        String selectedPasori = pasoriPortCombo.getValue();
        String selectedAntenna = antennaPortCombo.getValue();

        ObservableList<String> portList = FXCollections.observableArrayList(portNames);
        
        pasoriPortCombo.setItems(portList);
        antennaPortCombo.setItems(FXCollections.observableArrayList(portNames));

        // Restore selections if still available
        if (selectedPasori != null && portList.contains(selectedPasori)) {
            pasoriPortCombo.setValue(selectedPasori);
        }
        if (selectedAntenna != null && portList.contains(selectedAntenna)) {
            antennaPortCombo.setValue(selectedAntenna);
        }

        // Add port descriptions to log
        SerialPort[] ports = SerialPortService.getAvailablePorts();
        if (ports.length == 0) {
            addLogEntry("[System] " + I18N.get("system.noPortsFound"));
        } else {
            addLogEntry("[System] " + I18N.get("system.portsFound", ports.length));
            for (SerialPort port : ports) {
                addLogEntry("[System]   - " + port.getSystemPortName() + " (" + port.getDescriptivePortName() + ")");
            }
        }
    }

    private void updateConnectionButtons() {
        boolean pasoriConnected = bridgeService.isPaSoRiConnected();
        pasoriConnectBtn.setDisable(pasoriConnected);
        pasoriDisconnectBtn.setDisable(!pasoriConnected);
        pasoriPortCombo.setDisable(pasoriConnected);
        pasoriBaudCombo.setDisable(pasoriConnected);

        boolean antennaConnected = bridgeService.isAntennaIfConnected();
        antennaConnectBtn.setDisable(antennaConnected);
        antennaDisconnectBtn.setDisable(!antennaConnected);
        antennaPortCombo.setDisable(antennaConnected);
        antennaBaudCombo.setDisable(antennaConnected);

        // Update bridge toggle availability
        bridgeToggle.setDisable(!pasoriConnected || !antennaConnected);
    }

    private void updateStatusIndicator(Circle indicator, Label label, boolean connected, String text) {
        indicator.setFill(connected ? Color.LIMEGREEN : Color.GRAY);
        label.setText(text);
    }

    private void addLogEntry(String entry) {
        logItems.add(entry);
        
        // Limit log size
        while (logItems.size() > MAX_LOG_ENTRIES) {
            logItems.remove(0);
        }

        // Auto scroll to bottom
        if (autoScrollCheck.isSelected()) {
            logListView.scrollTo(logItems.size() - 1);
        }
    }

    private byte[] hexStringToByteArray(String hex) {
        // Remove spaces and convert to uppercase
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shutdown the controller and release resources.
     */
    public void shutdown() {
        bridgeService.shutdown();
    }
}
