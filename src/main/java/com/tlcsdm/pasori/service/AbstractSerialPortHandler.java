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
 * Subclasses can override methods to implement device-specific data processing
 * when needed in the future.
 */
public abstract class AbstractSerialPortHandler implements SerialPortHandler {

    protected final SerialPortService serialPortService;
    protected final String name;
    protected final LogEntry.Direction receiveLogDirection;
    protected final LogEntry.Direction sendLogDirection;

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
        return serialPortService.sendData(data);
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
        serialPortService.setDataReceivedCallback(callback);
    }

    @Override
    public void setErrorCallback(Consumer<String> callback) {
        serialPortService.setErrorCallback(callback);
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
