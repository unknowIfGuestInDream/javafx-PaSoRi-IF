/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import com.tlcsdm.pasori.model.LogEntry;

/**
 * Serial port handler implementation for the PaSoRi device.
 * <p>
 * This class handles communication with the PaSoRi device via serial port.
 * Override {@link #processReceivedData(byte[])} and {@link #processOutgoingData(byte[])}
 * to add PaSoRi-specific data processing, protocol handling, or other device-specific functionality.
 * <p>
 * Example customization:
 * <pre>{@code
 * public class CustomPaSoRiHandler extends PaSoRiPortHandler {
 *     @Override
 *     protected byte[] processReceivedData(byte[] data) {
 *         // Parse PaSoRi-specific protocol
 *         return parseProtocol(data);
 *     }
 * }
 * }</pre>
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
              LogEntry.Direction.ANTENNA_TO_PASORI,
              LogEntry.Direction.ANTENNA_TO_PASORI);
    }

    // Override processReceivedData and processOutgoingData as needed for PaSoRi-specific processing
}
