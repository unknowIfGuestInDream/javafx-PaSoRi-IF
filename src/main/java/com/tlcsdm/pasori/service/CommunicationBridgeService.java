/*
 * Copyright (c) 2026, 梦里不知身是客
 * Licensed under the MIT License
 */
package com.tlcsdm.pasori.service;

import com.tlcsdm.pasori.model.LogEntry;
import com.tlcsdm.pasori.model.SerialPortConfig;

import java.util.function.Consumer;

/**
 * Bridge service that manages communication between PaSoRi device and アンテナIF device.
 * 
 * This service:
 * - Receives data from PaSoRi and forwards to アンテナIF
 * - Receives data from アンテナIF and forwards to PaSoRi
 * - Logs all communication for debugging purposes
 */
public class CommunicationBridgeService {

    private final SerialPortService pasoriService;
    private final SerialPortService antennaIfService;
    
    private Consumer<LogEntry> logCallback;
    private volatile boolean bridgingEnabled = false;

    public CommunicationBridgeService() {
        this.pasoriService = new SerialPortService();
        this.antennaIfService = new SerialPortService();
        
        setupCallbacks();
    }

    private void setupCallbacks() {
        // Forward data from PaSoRi to アンテナIF
        pasoriService.setDataReceivedCallback(data -> {
            log(LogEntry.Direction.PASORI_TO_ANTENNA, data);
            if (bridgingEnabled && antennaIfService.isConnected()) {
                antennaIfService.sendData(data);
            }
        });

        // Forward data from アンテナIF to PaSoRi
        antennaIfService.setDataReceivedCallback(data -> {
            log(LogEntry.Direction.ANTENNA_TO_PASORI, data);
            if (bridgingEnabled && pasoriService.isConnected()) {
                pasoriService.sendData(data);
            }
        });

        // Error callbacks
        pasoriService.setErrorCallback(error -> 
            log(LogEntry.Direction.SYSTEM, "PaSoRi Error: " + error));
        antennaIfService.setErrorCallback(error -> 
            log(LogEntry.Direction.SYSTEM, "アンテナIF Error: " + error));
    }

    /**
     * Connect to PaSoRi device.
     * 
     * @param config serial port configuration
     * @return true if connection successful
     */
    public boolean connectPaSoRi(SerialPortConfig config) {
        boolean result = pasoriService.connect(config);
        if (result) {
            log(LogEntry.Direction.SYSTEM, "PaSoRi connected on " + config.getPortName());
        }
        return result;
    }

    /**
     * Disconnect from PaSoRi device.
     */
    public void disconnectPaSoRi() {
        pasoriService.disconnect();
        log(LogEntry.Direction.SYSTEM, "PaSoRi disconnected");
    }

    /**
     * Connect to アンテナIF device via USB CDC-ACM.
     * 
     * @param config serial port configuration
     * @return true if connection successful
     */
    public boolean connectAntennaIf(SerialPortConfig config) {
        boolean result = antennaIfService.connect(config);
        if (result) {
            log(LogEntry.Direction.SYSTEM, "アンテナIF connected on " + config.getPortName());
        }
        return result;
    }

    /**
     * Disconnect from アンテナIF device.
     */
    public void disconnectAntennaIf() {
        antennaIfService.disconnect();
        log(LogEntry.Direction.SYSTEM, "アンテナIF disconnected");
    }

    /**
     * Enable or disable automatic data bridging between devices.
     * 
     * @param enabled true to enable bridging
     */
    public void setBridgingEnabled(boolean enabled) {
        this.bridgingEnabled = enabled;
        log(LogEntry.Direction.SYSTEM, "Data bridging " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Check if bridging is enabled.
     * 
     * @return true if bridging is enabled
     */
    public boolean isBridgingEnabled() {
        return bridgingEnabled;
    }

    /**
     * Manually send data to PaSoRi device.
     * 
     * @param data the data to send
     * @return bytes written or -1 on error
     */
    public int sendToPaSoRi(byte[] data) {
        log(LogEntry.Direction.ANTENNA_TO_PASORI, data);
        return pasoriService.sendData(data);
    }

    /**
     * Manually send data to アンテナIF device.
     * 
     * @param data the data to send
     * @return bytes written or -1 on error
     */
    public int sendToAntennaIf(byte[] data) {
        log(LogEntry.Direction.PASORI_TO_ANTENNA, data);
        return antennaIfService.sendData(data);
    }

    /**
     * Check if PaSoRi is connected.
     * 
     * @return true if connected
     */
    public boolean isPaSoRiConnected() {
        return pasoriService.isConnected();
    }

    /**
     * Check if アンテナIF is connected.
     * 
     * @return true if connected
     */
    public boolean isAntennaIfConnected() {
        return antennaIfService.isConnected();
    }

    /**
     * Set the log callback for communication logging.
     * 
     * @param callback the callback function
     */
    public void setLogCallback(Consumer<LogEntry> callback) {
        this.logCallback = callback;
    }

    /**
     * Shutdown all connections.
     */
    public void shutdown() {
        bridgingEnabled = false;
        pasoriService.disconnect();
        antennaIfService.disconnect();
        log(LogEntry.Direction.SYSTEM, "Communication bridge shutdown");
    }

    private void log(LogEntry.Direction direction, byte[] data) {
        if (logCallback != null) {
            logCallback.accept(new LogEntry(direction, data));
        }
    }

    private void log(LogEntry.Direction direction, String message) {
        if (logCallback != null) {
            logCallback.accept(new LogEntry(direction, message, false));
        }
    }
}
