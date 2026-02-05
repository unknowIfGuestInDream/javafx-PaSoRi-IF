/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import com.tlcsdm.pasori.model.LogEntry;
import com.tlcsdm.pasori.model.SerialPortConfig;

import java.util.function.Consumer;

/**
 * Interface defining the contract for serial port handler implementations.
 * This interface supports future refactoring where each serial port (PaSoRi and AntennaIF)
 * can be handled by its own dedicated class with specific data processing logic.
 */
public interface SerialPortHandler {

    /**
     * Connect to the serial port with the given configuration.
     *
     * @param config the serial port configuration
     * @return true if connection was successful, false otherwise
     */
    boolean connect(SerialPortConfig config);

    /**
     * Disconnect from the serial port.
     */
    void disconnect();

    /**
     * Send data to the serial port.
     *
     * @param data the byte array to send
     * @return the number of bytes written, or -1 on error
     */
    int sendData(byte[] data);

    /**
     * Check if the handler is connected to a serial port.
     *
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Get the name of this handler (e.g., "PaSoRi", "AntennaIF").
     *
     * @return the handler name
     */
    String getName();

    /**
     * Get the direction for incoming data logs (data received from this device).
     *
     * @return the log direction for received data
     */
    LogEntry.Direction getReceiveLogDirection();

    /**
     * Get the direction for outgoing data logs (data sent to this device).
     *
     * @return the log direction for sent data
     */
    LogEntry.Direction getSendLogDirection();

    /**
     * Get the current port name if connected.
     *
     * @return the port name or null if not connected
     */
    String getPortName();

    /**
     * Set callback for data received from this port.
     *
     * @param callback the callback function
     */
    void setDataReceivedCallback(Consumer<byte[]> callback);

    /**
     * Set callback for errors.
     *
     * @param callback the callback function
     */
    void setErrorCallback(Consumer<String> callback);
}
