/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.controller;

import com.tlcsdm.pasori.PaSoRiApplication;
import com.tlcsdm.pasori.config.AppSettings;
import com.tlcsdm.pasori.config.I18N;
import com.tlcsdm.pasori.model.LogEntry;
import com.tlcsdm.pasori.model.SerialPortConfig;
import com.tlcsdm.pasori.service.CommunicationBridgeService;
import com.tlcsdm.pasori.service.DriverChecker;
import com.tlcsdm.pasori.service.SerialPortService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main controller for the PaSoRi IF Tool application.
 * Manages serial port connections and communication bridging.
 */
public class MainController implements Initializable {

    // PaSoRi connection controls (SDK-based)
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
    @FXML private TextArea logTextArea;
    @FXML private VBox logContainer;
    @FXML private Button clearLogBtn;
    @FXML private CheckBox autoScrollCheck;
    @FXML private CheckBox filterPasoriToAntennaCheck;
    @FXML private CheckBox filterAntennaToPasoriCheck;
    @FXML private CheckBox filterSystemCheck;

    // Manual send controls
    @FXML private TextField sendDataField;
    @FXML private Button sendBtn;

    private final CommunicationBridgeService bridgeService;
    private final ObservableList<Integer> baudRates;
    private final java.util.List<LogEntry> allLogEntries = new java.util.LinkedList<>();

    private Stage primaryStage;
    private ResourceBundle resources;
    private InlineCssTextArea styledLogArea;
    private VirtualizedScrollPane<InlineCssTextArea> logScrollPane;

    private static final int MAX_LOG_ENTRIES = 1000;
    private static final String ICON_PATH = "/com/tlcsdm/pasori/images/logo.png";
    private static final String CONTROLSFX_CSS_FIX_PATH = "/com/tlcsdm/pasori/css/controlsfx-fix.css";
    private static final String PREF_EXPORT_LOG_DIR = "exportLogDir";

    public MainController() {
        this.bridgeService = new CommunicationBridgeService();
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

        // Setup baud rate options for Antenna IF only (PaSoRi uses SDK)
        antennaBaudCombo.setItems(baudRates);
        antennaBaudCombo.setValue(115200);

        // Setup styled log area for colored logs
        setupStyledLogArea();
        autoScrollCheck.setSelected(true);

        // Setup log filter checkbox listeners
        filterPasoriToAntennaCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshLogDisplay());
        filterAntennaToPasoriCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshLogDisplay());
        filterSystemCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshLogDisplay());

        // Setup bridge service log callback
        bridgeService.setLogCallback(entry -> 
            Platform.runLater(() -> addLogEntry(entry))
        );

        // Initial port refresh (for Antenna IF)
        refreshPorts();

        // Setup UI states
        updateConnectionButtons();

        // Check driver installation at startup by attempting to load felica.dll
        if (!DriverChecker.isDriverInstalled()) {
            addLogEntry(new LogEntry(LogEntry.Direction.SYSTEM, I18N.get("system.driverNotInstalled"), false));
            Platform.runLater(this::showDriverNotInstalledDialog);
        }

        addLogEntry(new LogEntry(LogEntry.Direction.SYSTEM, I18N.get("system.appStarted"), false));
    }

    /**
     * Setup the styled log area for colored log display.
     */
    private void setupStyledLogArea() {
        // Create inline CSS styled text area for colored logs
        styledLogArea = new InlineCssTextArea();
        styledLogArea.setEditable(false);
        styledLogArea.setWrapText(true);
        styledLogArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        
        // Create scroll pane wrapper
        logScrollPane = new VirtualizedScrollPane<>(styledLogArea);
        VBox.setVgrow(logScrollPane, javafx.scene.layout.Priority.ALWAYS);
        
        // Replace the placeholder TextArea with our styled area
        // This approach is used because RichTextFX components cannot be directly declared in FXML
        if (logTextArea != null && logTextArea.getParent() instanceof VBox parent) {
            int index = parent.getChildren().indexOf(logTextArea);
            if (index >= 0) {
                parent.getChildren().set(index, logScrollPane);
            }
        }
    }

    @FXML
    private void handleOpenSettings() {
        var preferencesFx = AppSettings.getInstance().getPreferencesFx();
        
        // Add listener to set icon and apply CSS fix when the settings dialog window appears
        ListChangeListener<Window> windowListener = change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Window window : change.getAddedSubList()) {
                        if (window instanceof Stage stage && stage != primaryStage) {
                            setDialogIcon(stage);
                            applyControlsFxCssFix(stage);
                        }
                    }
                }
            }
        };
        Window.getWindows().addListener(windowListener);
        
        try {
            preferencesFx.show(true);
        } finally {
            Window.getWindows().removeListener(windowListener);
        }
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
    private void handleRestart() {
        Platform.runLater(() -> {
            shutdown();
            if (primaryStage != null) {
                primaryStage.close();
            }
            try {
                PaSoRiApplication app = new PaSoRiApplication();
                app.init();
                app.start(new Stage());
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = e.toString();
                }
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18N.get("error.title"));
                alert.setHeaderText(null);
                alert.setContentText(I18N.get("error.restartFailed") + "\n" + errorMessage);
                alert.showAndWait();
                Platform.exit();
            }
        });
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18N.get("menu.about"));
        alert.setHeaderText("PaSoRi IF Tool");
        alert.setContentText(I18N.get("about.version") + "\n\n" + I18N.get("about.description"));
        
        // Set application icon to the dialog stage
        setDialogIcon((Stage) alert.getDialogPane().getScene().getWindow());
        
        alert.showAndWait();
    }

    @FXML
    private void handleExportLog() {
        if (allLogEntries.isEmpty()) {
            showAlert(I18N.get("error.title"), I18N.get("log.export.empty"));
            return;
        }

        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(MainController.class);
        String lastDir = prefs.get(PREF_EXPORT_LOG_DIR, System.getProperty("user.dir"));

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18N.get("menu.exportLog"));
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Log files (*.log)", "*.log"));
        fileChooser.setInitialFileName("pasori_log.log");

        File initialDir = new File(lastDir);
        if (initialDir.isDirectory()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file == null) {
            return;
        }

        // Remember the chosen directory
        prefs.put(PREF_EXPORT_LOG_DIR, file.getParentFile().getAbsolutePath());

        boolean showTimestamp = AppSettings.getInstance().isLogTimestampEnabled();
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            for (LogEntry entry : allLogEntries) {
                writer.println(entry.toString(showTimestamp));
            }
            addLogEntry(new LogEntry(LogEntry.Direction.SYSTEM,
                I18N.get("log.export.success", file.getAbsolutePath()), false));
        } catch (IOException e) {
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.toString();
            }
            showAlert(I18N.get("error.title"), I18N.get("log.export.failed", errorMessage));
        }
    }

    @FXML
    private void handleRefreshPorts() {
        refreshPorts();
    }

    @FXML
    private void handlePasoriConnect() {
        if (bridgeService.connectPaSoRi()) {
            updateConnectionButtons();
            updateStatusIndicator(pasoriStatusIndicator, pasoriStatusLabel, true, I18N.get("pasori.connected"));
        } else {
            showAlert(I18N.get("error.connectionError"), I18N.get("error.pasoriSdkConnectFailed"));
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
        styledLogArea.clear();
        allLogEntries.clear();
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

        if (!bridgeService.isAntennaIfConnected()) {
            showAlert(I18N.get("error.title"), I18N.get("error.antennaNotConnected"));
            return;
        }

        int result = bridgeService.sendToAntennaIf(data);
        if (result > 0) {
            sendDataField.clear();
        }
    }

    private void refreshPorts() {
        String[] portNames = SerialPortService.getAvailablePortNames();
        
        String selectedAntenna = antennaPortCombo.getValue();

        ObservableList<String> portList = FXCollections.observableArrayList(portNames);
        
        antennaPortCombo.setItems(portList);

        // Restore selection if still available
        if (selectedAntenna != null && portList.contains(selectedAntenna)) {
            antennaPortCombo.setValue(selectedAntenna);
        }

        // Add port descriptions to log
        com.fazecast.jSerialComm.SerialPort[] ports = SerialPortService.getAvailablePorts();
        if (ports.length == 0) {
            addLogEntry(new LogEntry(LogEntry.Direction.SYSTEM, I18N.get("system.noPortsFound"), false));
        } else {
            addLogEntry(new LogEntry(LogEntry.Direction.SYSTEM, I18N.get("system.portsFound", ports.length), false));
            for (com.fazecast.jSerialComm.SerialPort port : ports) {
                addLogEntry(new LogEntry(LogEntry.Direction.SYSTEM, 
                    "  - " + port.getSystemPortName() + " (" + port.getDescriptivePortName() + ")", false));
            }
        }
    }

    private void updateConnectionButtons() {
        boolean pasoriConnected = bridgeService.isPaSoRiConnected();
        pasoriConnectBtn.setDisable(pasoriConnected);
        pasoriDisconnectBtn.setDisable(!pasoriConnected);

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

    private void addLogEntry(LogEntry entry) {
        // Store the entry in the list
        allLogEntries.add(entry);
        
        // Limit log size - trim from the beginning if too many entries
        if (allLogEntries.size() > MAX_LOG_ENTRIES) {
            allLogEntries.remove(0);
        }
        
        // Check if this entry should be displayed based on current filter
        if (!isEntryVisible(entry)) {
            return;
        }

        // Get the color for this log entry
        String colorHex = AppSettings.getInstance().getLogColorHex(entry.getDirection());
        boolean showTimestamp = AppSettings.getInstance().isLogTimestampEnabled();
        String logText = entry.toString(showTimestamp);
        
        // Add newline if not the first entry
        int startPos = styledLogArea.getLength();
        if (startPos > 0) {
            styledLogArea.appendText("\n");
            startPos = styledLogArea.getLength();
        }
        
        // Append the log text with inline color styling
        styledLogArea.appendText(logText);
        int endPos = styledLogArea.getLength();
        
        // Apply color style to the appended text
        styledLogArea.setStyle(startPos, endPos, "-fx-fill: " + colorHex + ";");

        // Auto scroll to bottom
        if (autoScrollCheck.isSelected()) {
            styledLogArea.requestFollowCaret();
            styledLogArea.moveTo(styledLogArea.getLength());
        }
    }

    /**
     * Check if a log entry should be visible based on current filter settings.
     */
    private boolean isEntryVisible(LogEntry entry) {
        return switch (entry.getDirection()) {
            case PASORI_TO_ANTENNA -> filterPasoriToAntennaCheck.isSelected();
            case ANTENNA_TO_PASORI -> filterAntennaToPasoriCheck.isSelected();
            case SYSTEM -> filterSystemCheck.isSelected();
        };
    }

    /**
     * Refresh the log display based on current filter settings.
     */
    private void refreshLogDisplay() {
        styledLogArea.clear();
        
        boolean showTimestamp = AppSettings.getInstance().isLogTimestampEnabled();
        
        for (LogEntry entry : allLogEntries) {
            if (!isEntryVisible(entry)) {
                continue;
            }
            
            String colorHex = AppSettings.getInstance().getLogColorHex(entry.getDirection());
            String logText = entry.toString(showTimestamp);
            
            int startPos = styledLogArea.getLength();
            if (startPos > 0) {
                styledLogArea.appendText("\n");
                startPos = styledLogArea.getLength();
            }
            
            styledLogArea.appendText(logText);
            int endPos = styledLogArea.getLength();
            styledLogArea.setStyle(startPos, endPos, "-fx-fill: " + colorHex + ";");
        }
        
        // Auto scroll to bottom after refresh
        if (autoScrollCheck.isSelected()) {
            styledLogArea.requestFollowCaret();
            styledLogArea.moveTo(styledLogArea.getLength());
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

    /**
     * Show a warning dialog informing the user that the FeliCa SDK is not installed.
     */
    private void showDriverNotInstalledDialog() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(I18N.get("system.driverNotInstalled.title"));
        alert.setHeaderText(I18N.get("system.driverNotInstalled.header"));
        alert.setContentText(I18N.get("system.driverNotInstalled.content"));
        setDialogIcon((Stage) alert.getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Set the application icon on a dialog stage.
     */
    private void setDialogIcon(Stage stage) {
        try (InputStream iconStream = getClass().getResourceAsStream(ICON_PATH)) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            // Ignore if icon cannot be loaded
        }
    }

    /**
     * Apply CSS fix for ControlsFX components (like ToggleSwitch) to work with AtlantaFX themes.
     */
    private void applyControlsFxCssFix(Stage stage) {
        if (stage.getScene() != null) {
            var cssUrl = getClass().getResource(CONTROLSFX_CSS_FIX_PATH);
            if (cssUrl != null) {
                stage.getScene().getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Warning: ControlsFX CSS fix file not found at " + CONTROLSFX_CSS_FIX_PATH);
            }
        }
    }

    /**
     * Shutdown the controller and release resources.
     */
    public void shutdown() {
        bridgeService.shutdown();
    }
}
