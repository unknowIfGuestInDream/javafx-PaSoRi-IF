/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * IF communication protocol handler for message framing and parsing.
 *
 * <p>Message format:</p>
 * <pre>
 * | D0  | D1      | D2  | D3      | ...       | Dn-1 | Dn  |
 * | STX | CMD/RES | LEN | Data(0) | Data(m)   | BCC  | ETX |
 * </pre>
 *
 * <ul>
 *   <li>STX = 0x02, ETX = 0x03</li>
 *   <li>BCC: XOR of D1 through Dn-1 yields 0x00</li>
 *   <li>LEN: byte count of Data(0) through Data(m)</li>
 * </ul>
 */
public class IfProtocol {

    public static final byte STX = 0x02;
    public static final byte ETX = 0x03;

    // Command codes
    public static final byte CMD_OPEN = 0x10;
    public static final byte RES_OPEN = 0x11;
    public static final byte CMD_CLOSE = 0x20;
    public static final byte RES_CLOSE = 0x21;
    public static final byte CMD_CARD_ACCESS = 0x30;
    public static final byte RES_CARD_ACCESS = 0x31;
    public static final byte CMD_SET_PARAMETER = 0x40;
    public static final byte RES_SET_PARAMETER = 0x41;
    public static final byte RES_EXCEPTION = (byte) 0xF1;

    // Response codes
    public static final byte RES_CODE_OK = 0x00;
    public static final byte RES_CODE_ERROR = 0x01;

    // Exception error codes (Data(1) in exception response)
    public static final byte ERR_CARD_ACCESS_BEFORE_OPEN = 0x01;
    public static final byte ERR_CARD_ACCESS_TIMEOUT = 0x10;

    private IfProtocol() {
        // Utility class
    }

    /**
     * Parsed IF protocol message.
     */
    public static class Message {
        private final byte command;
        private final byte[] data;

        public Message(byte command, byte[] data) {
            this.command = command;
            this.data = data != null ? data : new byte[0];
        }

        public byte getCommand() {
            return command;
        }

        public byte[] getData() {
            return data;
        }

        public int getDataLength() {
            return data.length;
        }
    }

    /**
     * Build a protocol frame from command and data.
     *
     * @param command the CMD/RES byte
     * @param data    the data bytes (may be null or empty)
     * @return the complete framed message
     */
    public static byte[] buildFrame(byte command, byte[] data) {
        int dataLen = (data != null) ? data.length : 0;
        // STX(1) + CMD(1) + LEN(1) + Data(dataLen) + BCC(1) + ETX(1) = 5 + dataLen
        byte[] frame = new byte[5 + dataLen];

        frame[0] = STX;
        frame[1] = command;
        frame[2] = (byte) dataLen;

        if (data != null && dataLen > 0) {
            System.arraycopy(data, 0, frame, 3, dataLen);
        }

        // Calculate BCC: XOR of D1 through Dn-1 (command, len, data bytes) should yield 0x00
        // So BCC = XOR(command, len, data[0], ..., data[m])
        byte bcc = 0;
        for (int i = 1; i < frame.length - 2; i++) {
            bcc ^= frame[i];
        }
        frame[frame.length - 2] = bcc;
        frame[frame.length - 1] = ETX;

        return frame;
    }

    /**
     * Build an Open command frame.
     *
     * @return the Open command frame
     */
    public static byte[] buildOpenCommand() {
        return buildFrame(CMD_OPEN, null);
    }

    /**
     * Build an Open response frame.
     *
     * @param resCode the response code
     * @return the Open response frame
     */
    public static byte[] buildOpenResponse(byte resCode) {
        return buildFrame(RES_OPEN, new byte[]{resCode});
    }

    /**
     * Build a Close command frame.
     *
     * @return the Close command frame
     */
    public static byte[] buildCloseCommand() {
        return buildFrame(CMD_CLOSE, null);
    }

    /**
     * Build a Close response frame.
     *
     * @param resCode the response code
     * @return the Close response frame
     */
    public static byte[] buildCloseResponse(byte resCode) {
        return buildFrame(RES_CLOSE, new byte[]{resCode});
    }

    /**
     * Build a CardAccess command frame.
     *
     * @param felicaData the FeliCa data link layer data
     * @return the CardAccess command frame
     */
    public static byte[] buildCardAccessCommand(byte[] felicaData) {
        return buildFrame(CMD_CARD_ACCESS, felicaData);
    }

    /**
     * Build a CardAccess response frame.
     *
     * @param responseData the card response data
     * @return the CardAccess response frame
     */
    public static byte[] buildCardAccessResponse(byte[] responseData) {
        return buildFrame(RES_CARD_ACCESS, responseData);
    }

    /**
     * Build a SetParameter command frame.
     *
     * @param subCmd the sub-command byte
     * @param params the parameter bytes
     * @return the SetParameter command frame
     */
    public static byte[] buildSetParameterCommand(byte subCmd, byte[] params) {
        byte[] data;
        if (params != null && params.length > 0) {
            data = new byte[1 + params.length];
            data[0] = subCmd;
            System.arraycopy(params, 0, data, 1, params.length);
        } else {
            data = new byte[]{subCmd};
        }
        return buildFrame(CMD_SET_PARAMETER, data);
    }

    /**
     * Build a SetParameter response frame.
     *
     * @param resCode the response code
     * @return the SetParameter response frame
     */
    public static byte[] buildSetParameterResponse(byte resCode) {
        return buildFrame(RES_SET_PARAMETER, new byte[]{resCode});
    }

    /**
     * Build an exception response frame.
     *
     * @param errorSourceCmd the command code that caused the error
     * @param errorCode     the error code
     * @return the exception response frame
     */
    public static byte[] buildExceptionResponse(byte errorSourceCmd, byte errorCode) {
        return buildFrame(RES_EXCEPTION, new byte[]{errorSourceCmd, errorCode});
    }

    /**
     * Parse a complete frame into a Message.
     *
     * @param frame the raw frame bytes (including STX and ETX)
     * @return the parsed Message, or null if frame is invalid
     */
    public static Message parseFrame(byte[] frame) {
        if (frame == null || frame.length < 5) {
            return null;
        }

        // Check STX and ETX
        if (frame[0] != STX || frame[frame.length - 1] != ETX) {
            return null;
        }

        // Verify BCC: XOR of D1 through Dn-1 should be 0x00
        // D1 = frame[1], Dn-1 = frame[frame.length - 2]
        byte bcc = 0;
        for (int i = 1; i < frame.length - 1; i++) {
            bcc ^= frame[i];
        }
        if (bcc != 0x00) {
            return null;
        }

        byte command = frame[1];
        int len = frame[2] & 0xFF;

        // Verify frame length matches: STX(1) + CMD(1) + LEN(1) + Data(len) + BCC(1) + ETX(1) = 5 + len
        if (frame.length != 5 + len) {
            return null;
        }

        byte[] data = null;
        if (len > 0) {
            data = Arrays.copyOfRange(frame, 3, 3 + len);
        }

        return new Message(command, data);
    }

    /**
     * Accumulator for assembling frames from serial data stream.
     * Handles partial reads and frame boundary detection.
     */
    public static class FrameAccumulator {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private boolean inFrame = false;

        /**
         * Feed received bytes into the accumulator.
         *
         * @param data the received bytes
         * @return a complete frame if one was assembled, null otherwise
         */
        public byte[] feed(byte[] data) {
            if (data == null) {
                return null;
            }

            for (byte b : data) {
                if (b == STX) {
                    // Start of a new frame
                    buffer.reset();
                    buffer.write(b);
                    inFrame = true;
                } else if (b == ETX && inFrame) {
                    // End of frame
                    buffer.write(b);
                    byte[] frame = buffer.toByteArray();
                    buffer.reset();
                    inFrame = false;
                    return frame;
                } else if (inFrame) {
                    buffer.write(b);
                }
            }
            return null;
        }

        /**
         * Reset the accumulator state.
         */
        public void reset() {
            buffer.reset();
            inFrame = false;
        }
    }

    /**
     * Calculate BCC for verification.
     * XOR of all bytes from command through BCC should yield 0x00.
     *
     * @param data bytes from D1 to Dn-1
     * @return the calculated BCC value
     */
    public static byte calculateBcc(byte[] data) {
        byte bcc = 0;
        for (byte b : data) {
            bcc ^= b;
        }
        return bcc;
    }
}
