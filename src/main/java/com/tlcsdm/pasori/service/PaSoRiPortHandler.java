/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import com.tlcsdm.pasori.model.LogEntry;

/**
 * Serial port handler implementation for the PaSoRi device.
 * <p>
 * This class handles communication with the PaSoRi device via serial port.
 * It can be extended in the future to add PaSoRi-specific data processing,
 * protocol handling, or other device-specific functionality.
 */
public class PaSoRiPortHandler extends AbstractSerialPortHandler {

    /**
     * Default handler name for PaSoRi device.
     */
    public static final String HANDLER_NAME = "PaSoRi";

    /**
     * Create a new PaSoRi port handler.
     */
    public PaSoRiPortHandler() {
        super(HANDLER_NAME, 
              LogEntry.Direction.ANTENNA_TO_PASORI,  // Data received from PaSoRi goes TO PaSoRi direction for sending
              LogEntry.Direction.ANTENNA_TO_PASORI); // Data sent to PaSoRi
    }

    /**
     * Process received data from PaSoRi.
     * Override this method in subclasses to add device-specific processing.
     *
     * @param data the raw data received from the device
     * @return the processed data (by default, returns the data unchanged)
     */
    protected byte[] processReceivedData(byte[] data) {
        // Default implementation: pass through unchanged
        // Future: Add PaSoRi-specific protocol parsing here
        return data;
    }

    /**
     * Process data before sending to PaSoRi.
     * Override this method in subclasses to add device-specific processing.
     *
     * @param data the raw data to send
     * @return the processed data ready for transmission
     */
    protected byte[] processOutgoingData(byte[] data) {
        // Default implementation: pass through unchanged
        // Future: Add PaSoRi-specific protocol formatting here
        return data;
    }
}
