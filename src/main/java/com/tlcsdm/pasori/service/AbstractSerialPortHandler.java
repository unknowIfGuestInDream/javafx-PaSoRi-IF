/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import com.tlcsdm.pasori.model.LogEntry;
import com.tlcsdm.pasori.model.SerialPortConfig;

import java.util.function.Consumer;

/**
 * Abstract base class for serial port handlers that provides common functionality.
 * This class wraps SerialPortService and adds handler-specific behavior.
 * <p>
 * Subclasses can override {@link #processReceivedData(byte[])} and {@link #processOutgoingData(byte[])}
 * to implement device-specific data processing. These methods are called automatically
 * when data is received or sent, allowing for protocol-specific transformations.
 */
public abstract class AbstractSerialPortHandler implements SerialPortHandler {

    protected final SerialPortService serialPortService;
    protected final String name;
    protected final LogEntry.Direction receiveLogDirection;
    protected final LogEntry.Direction sendLogDirection;
    
    private Consumer<byte[]> externalDataCallback;

    /**
     * Create a new serial port handler.
     *
     * @param name               the handler name for logging
     * @param receiveLogDirection the log direction for received data
     * @param sendLogDirection    the log direction for sent data
     */
    protected AbstractSerialPortHandler(String name, 
                                        LogEntry.Direction receiveLogDirection,
                                        LogEntry.Direction sendLogDirection) {
        this.serialPortService = new SerialPortService();
        this.name = name;
        this.receiveLogDirection = receiveLogDirection;
        this.sendLogDirection = sendLogDirection;
    }

    @Override
    public boolean connect(SerialPortConfig config) {
        return serialPortService.connect(config);
    }

    @Override
    public void disconnect() {
        serialPortService.disconnect();
    }

    @Override
    public int sendData(byte[] data) {
        byte[] processedData = processOutgoingData(data);
        return serialPortService.sendData(processedData);
    }

    @Override
    public boolean isConnected() {
        return serialPortService.isConnected();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LogEntry.Direction getReceiveLogDirection() {
        return receiveLogDirection;
    }

    @Override
    public LogEntry.Direction getSendLogDirection() {
        return sendLogDirection;
    }

    @Override
    public String getPortName() {
        return serialPortService.getPortName();
    }

    @Override
    public void setDataReceivedCallback(Consumer<byte[]> callback) {
        this.externalDataCallback = callback;
        // Wrap the callback to apply data processing
        serialPortService.setDataReceivedCallback(rawData -> {
            byte[] processedData = processReceivedData(rawData);
            if (externalDataCallback != null) {
                externalDataCallback.accept(processedData);
            }
        });
    }

    @Override
    public void setErrorCallback(Consumer<String> callback) {
        serialPortService.setErrorCallback(callback);
    }

    /**
     * Process received data from the device.
     * Subclasses can override this method to add device-specific processing,
     * such as protocol parsing or data transformation.
     *
     * @param data the raw data received from the device
     * @return the processed data (by default, returns the data unchanged)
     */
    protected byte[] processReceivedData(byte[] data) {
        return data;
    }

    /**
     * Process data before sending to the device.
     * Subclasses can override this method to add device-specific processing,
     * such as protocol formatting or data transformation.
     *
     * @param data the raw data to send
     * @return the processed data ready for transmission (by default, returns the data unchanged)
     */
    protected byte[] processOutgoingData(byte[] data) {
        return data;
    }

    /**
     * Get the underlying serial port service.
     * Provided for advanced use cases where direct access is needed.
     *
     * @return the SerialPortService instance
     */
    protected SerialPortService getSerialPortService() {
        return serialPortService;
    }
}
