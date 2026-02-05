/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import com.tlcsdm.pasori.model.LogEntry;

/**
 * Serial port handler implementation for the Panasonic アンテナIF device.
 * <p>
 * This class handles communication with the アンテナIF (Antenna Interface) device
 * via USB CDC-ACM serial port. It can be extended in the future to add
 * device-specific data processing, protocol handling, or other functionality.
 */
public class AntennaIfPortHandler extends AbstractSerialPortHandler {

    /**
     * Default handler name for アンテナIF device.
     */
    public static final String HANDLER_NAME = "AntennaIF";

    /**
     * Create a new AntennaIF port handler.
     */
    public AntennaIfPortHandler() {
        super(HANDLER_NAME,
              LogEntry.Direction.PASORI_TO_ANTENNA,  // Data received from AntennaIF goes TO Antenna direction for sending
              LogEntry.Direction.PASORI_TO_ANTENNA); // Data sent to AntennaIF
    }

    /**
     * Process received data from アンテナIF.
     * Override this method in subclasses to add device-specific processing.
     *
     * @param data the raw data received from the device
     * @return the processed data (by default, returns the data unchanged)
     */
    protected byte[] processReceivedData(byte[] data) {
        // Default implementation: pass through unchanged
        // Future: Add アンテナIF-specific protocol parsing here
        return data;
    }

    /**
     * Process data before sending to アンテナIF.
     * Override this method in subclasses to add device-specific processing.
     *
     * @param data the raw data to send
     * @return the processed data ready for transmission
     */
    protected byte[] processOutgoingData(byte[] data) {
        // Default implementation: pass through unchanged
        // Future: Add アンテナIF-specific protocol formatting here
        return data;
    }
}
