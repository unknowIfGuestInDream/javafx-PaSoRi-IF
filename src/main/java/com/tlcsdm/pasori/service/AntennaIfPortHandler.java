/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import com.tlcsdm.pasori.model.LogEntry;

/**
 * Serial port handler implementation for the Panasonic アンテナIF device.
 * <p>
 * This class handles communication with the アンテナIF (Antenna Interface) device
 * via USB CDC-ACM serial port. Override {@link #processReceivedData(byte[])} and
 * {@link #processOutgoingData(byte[])} to add device-specific data processing,
 * protocol handling, or other functionality.
 * <p>
 * Example customization:
 * <pre>{@code
 * public class CustomAntennaHandler extends AntennaIfPortHandler {
 *     @Override
 *     protected byte[] processReceivedData(byte[] data) {
 *         // Parse antenna-specific protocol
 *         return parseProtocol(data);
 *     }
 * }
 * }</pre>
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
              LogEntry.Direction.PASORI_TO_ANTENNA,
              LogEntry.Direction.PASORI_TO_ANTENNA);
    }

    // Override processReceivedData and processOutgoingData as needed for アンテナIF-specific processing
}
