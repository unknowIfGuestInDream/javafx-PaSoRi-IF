/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IF communication protocol framing, parsing and BCC calculation.
 */
class IfProtocolTest {

    // ---- BCC Calculation Tests ----

    @Test
    void testCalculateBcc() {
        // XOR of bytes should yield the BCC
        byte[] data = {0x10, 0x00};
        assertEquals(0x10, IfProtocol.calculateBcc(data));
    }

    // ---- Frame Building Tests ----

    @Test
    void testBuildOpenCommand() {
        // Expected: STX(02) CMD(10) LEN(00) BCC(10) ETX(03)
        byte[] frame = IfProtocol.buildOpenCommand();
        assertNotNull(frame);
        assertEquals(5, frame.length);
        assertEquals(IfProtocol.STX, frame[0]);
        assertEquals(IfProtocol.CMD_OPEN, frame[1]);
        assertEquals(0x00, frame[2]); // LEN = 0
        assertEquals(0x10, frame[3]); // BCC = XOR(0x10, 0x00) = 0x10
        assertEquals(IfProtocol.ETX, frame[4]);
        assertBccValid(frame);
    }

    @Test
    void testBuildOpenResponse() {
        // Expected: STX(02) RES(11) LEN(01) ResCode(00) BCC ETX(03)
        byte[] frame = IfProtocol.buildOpenResponse(IfProtocol.RES_CODE_OK);
        assertNotNull(frame);
        assertEquals(6, frame.length);
        assertEquals(IfProtocol.STX, frame[0]);
        assertEquals(IfProtocol.RES_OPEN, frame[1]);
        assertEquals(0x01, frame[2]); // LEN = 1
        assertEquals(IfProtocol.RES_CODE_OK, frame[3]);
        assertEquals(IfProtocol.ETX, frame[5]);
        assertBccValid(frame);
    }

    @Test
    void testBuildCloseCommand() {
        // Expected: STX(02) CMD(20) LEN(00) BCC(20) ETX(03)
        byte[] frame = IfProtocol.buildCloseCommand();
        assertNotNull(frame);
        assertEquals(5, frame.length);
        assertEquals(IfProtocol.STX, frame[0]);
        assertEquals(IfProtocol.CMD_CLOSE, frame[1]);
        assertEquals(0x00, frame[2]); // LEN = 0
        assertEquals(0x20, frame[3]); // BCC = XOR(0x20, 0x00) = 0x20
        assertEquals(IfProtocol.ETX, frame[4]);
        assertBccValid(frame);
    }

    @Test
    void testBuildCloseResponse() {
        byte[] frame = IfProtocol.buildCloseResponse(IfProtocol.RES_CODE_OK);
        assertNotNull(frame);
        assertEquals(6, frame.length);
        assertEquals(IfProtocol.RES_CLOSE, frame[1]);
        assertBccValid(frame);
    }

    @Test
    void testBuildCardAccessCommand() {
        byte[] felicaData = {0x06, 0x00, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00};
        byte[] frame = IfProtocol.buildCardAccessCommand(felicaData);
        assertNotNull(frame);
        assertEquals(5 + felicaData.length, frame.length);
        assertEquals(IfProtocol.CMD_CARD_ACCESS, frame[1]);
        assertEquals(felicaData.length, frame[2] & 0xFF); // LEN
        // Verify data copied correctly
        for (int i = 0; i < felicaData.length; i++) {
            assertEquals(felicaData[i], frame[3 + i]);
        }
        assertBccValid(frame);
    }

    @Test
    void testBuildCardAccessResponse() {
        byte[] responseData = {0x07, 0x01, 0x00, 0x01, 0x02, 0x03, 0x04};
        byte[] frame = IfProtocol.buildCardAccessResponse(responseData);
        assertNotNull(frame);
        assertEquals(IfProtocol.RES_CARD_ACCESS, frame[1]);
        assertBccValid(frame);
    }

    @Test
    void testBuildSetParameterCommand() {
        byte subCmd = 0x01;
        byte[] params = {0x00, 0x64}; // timeout = 100
        byte[] frame = IfProtocol.buildSetParameterCommand(subCmd, params);
        assertNotNull(frame);
        assertEquals(IfProtocol.CMD_SET_PARAMETER, frame[1]);
        assertEquals(3, frame[2] & 0xFF); // LEN = 1 (subCmd) + 2 (params)
        assertEquals(subCmd, frame[3]);
        assertBccValid(frame);
    }

    @Test
    void testBuildSetParameterResponse() {
        byte[] frame = IfProtocol.buildSetParameterResponse(IfProtocol.RES_CODE_OK);
        assertNotNull(frame);
        assertEquals(IfProtocol.RES_SET_PARAMETER, frame[1]);
        assertBccValid(frame);
    }

    @Test
    void testBuildExceptionResponse() {
        byte[] frame = IfProtocol.buildExceptionResponse(
            IfProtocol.CMD_CARD_ACCESS, IfProtocol.ERR_CARD_ACCESS_BEFORE_OPEN);
        assertNotNull(frame);
        assertEquals(IfProtocol.RES_EXCEPTION, frame[1]);
        assertEquals(2, frame[2] & 0xFF); // LEN = 2 (errorSourceCmd + errorCode)
        assertEquals(IfProtocol.CMD_CARD_ACCESS, frame[3]); // Data(0) = source cmd
        assertEquals(IfProtocol.ERR_CARD_ACCESS_BEFORE_OPEN, frame[4]); // Data(1) = error code
        assertBccValid(frame);
    }

    @Test
    void testBuildExceptionResponseTimeout() {
        byte[] frame = IfProtocol.buildExceptionResponse(
            IfProtocol.CMD_CARD_ACCESS, IfProtocol.ERR_CARD_ACCESS_TIMEOUT);
        assertNotNull(frame);
        assertEquals(IfProtocol.RES_EXCEPTION, frame[1]);
        assertEquals(IfProtocol.CMD_CARD_ACCESS, frame[3]);
        assertEquals(IfProtocol.ERR_CARD_ACCESS_TIMEOUT, frame[4]);
        assertBccValid(frame);
    }

    // ---- Frame Parsing Tests ----

    @Test
    void testParseOpenCommand() {
        byte[] frame = IfProtocol.buildOpenCommand();
        IfProtocol.Message msg = IfProtocol.parseFrame(frame);
        assertNotNull(msg);
        assertEquals(IfProtocol.CMD_OPEN, msg.getCommand());
        assertEquals(0, msg.getDataLength());
    }

    @Test
    void testParseOpenResponse() {
        byte[] frame = IfProtocol.buildOpenResponse(IfProtocol.RES_CODE_OK);
        IfProtocol.Message msg = IfProtocol.parseFrame(frame);
        assertNotNull(msg);
        assertEquals(IfProtocol.RES_OPEN, msg.getCommand());
        assertEquals(1, msg.getDataLength());
        assertEquals(IfProtocol.RES_CODE_OK, msg.getData()[0]);
    }

    @Test
    void testParseCardAccessCommand() {
        byte[] felicaData = {0x06, 0x00, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00};
        byte[] frame = IfProtocol.buildCardAccessCommand(felicaData);
        IfProtocol.Message msg = IfProtocol.parseFrame(frame);
        assertNotNull(msg);
        assertEquals(IfProtocol.CMD_CARD_ACCESS, msg.getCommand());
        assertEquals(felicaData.length, msg.getDataLength());
        assertArrayEquals(felicaData, msg.getData());
    }

    @Test
    void testParseSetParameterCommand() {
        byte subCmd = 0x01;
        byte[] params = {0x00, 0x64};
        byte[] frame = IfProtocol.buildSetParameterCommand(subCmd, params);
        IfProtocol.Message msg = IfProtocol.parseFrame(frame);
        assertNotNull(msg);
        assertEquals(IfProtocol.CMD_SET_PARAMETER, msg.getCommand());
        assertEquals(3, msg.getDataLength());
        assertEquals(subCmd, msg.getData()[0]);
    }

    @Test
    void testParseExceptionResponse() {
        byte[] frame = IfProtocol.buildExceptionResponse(
            IfProtocol.CMD_CARD_ACCESS, IfProtocol.ERR_CARD_ACCESS_TIMEOUT);
        IfProtocol.Message msg = IfProtocol.parseFrame(frame);
        assertNotNull(msg);
        assertEquals(IfProtocol.RES_EXCEPTION, msg.getCommand());
        assertEquals(2, msg.getDataLength());
        assertEquals(IfProtocol.CMD_CARD_ACCESS, msg.getData()[0]);
        assertEquals(IfProtocol.ERR_CARD_ACCESS_TIMEOUT, msg.getData()[1]);
    }

    @Test
    void testParseInvalidFrameNull() {
        assertNull(IfProtocol.parseFrame(null));
    }

    @Test
    void testParseInvalidFrameTooShort() {
        assertNull(IfProtocol.parseFrame(new byte[]{0x02, 0x10, 0x03}));
    }

    @Test
    void testParseInvalidFrameNoStx() {
        byte[] frame = {0x00, 0x10, 0x00, 0x10, 0x03};
        assertNull(IfProtocol.parseFrame(frame));
    }

    @Test
    void testParseInvalidFrameNoEtx() {
        byte[] frame = {0x02, 0x10, 0x00, 0x10, 0x00};
        assertNull(IfProtocol.parseFrame(frame));
    }

    @Test
    void testParseInvalidBcc() {
        byte[] frame = {0x02, 0x10, 0x00, 0x00, 0x03}; // BCC wrong (should be 0x10)
        assertNull(IfProtocol.parseFrame(frame));
    }

    @Test
    void testParseInvalidLength() {
        // Frame says LEN=2 but only has 1 data byte
        byte[] frame = {0x02, 0x10, 0x02, 0x00, 0x12, 0x03};
        assertNull(IfProtocol.parseFrame(frame));
    }

    // ---- Frame Accumulator Tests ----

    @Test
    void testFrameAccumulatorCompleteFrame() {
        IfProtocol.FrameAccumulator acc = new IfProtocol.FrameAccumulator();
        byte[] openCmd = IfProtocol.buildOpenCommand();
        byte[] result = acc.feed(openCmd);
        assertNotNull(result);
        assertArrayEquals(openCmd, result);
    }

    @Test
    void testFrameAccumulatorPartialFrames() {
        IfProtocol.FrameAccumulator acc = new IfProtocol.FrameAccumulator();
        byte[] openCmd = IfProtocol.buildOpenCommand();

        // Feed first 3 bytes
        byte[] part1 = new byte[3];
        System.arraycopy(openCmd, 0, part1, 0, 3);
        assertNull(acc.feed(part1));

        // Feed remaining bytes
        byte[] part2 = new byte[openCmd.length - 3];
        System.arraycopy(openCmd, 3, part2, 0, part2.length);
        byte[] result = acc.feed(part2);
        assertNotNull(result);
        assertArrayEquals(openCmd, result);
    }

    @Test
    void testFrameAccumulatorByteByByte() {
        IfProtocol.FrameAccumulator acc = new IfProtocol.FrameAccumulator();
        byte[] closeCmd = IfProtocol.buildCloseCommand();

        // Feed byte by byte
        for (int i = 0; i < closeCmd.length - 1; i++) {
            assertNull(acc.feed(new byte[]{closeCmd[i]}));
        }
        // Last byte should complete the frame
        byte[] result = acc.feed(new byte[]{closeCmd[closeCmd.length - 1]});
        assertNotNull(result);
        assertArrayEquals(closeCmd, result);
    }

    @Test
    void testFrameAccumulatorReset() {
        IfProtocol.FrameAccumulator acc = new IfProtocol.FrameAccumulator();
        // Feed partial data
        acc.feed(new byte[]{IfProtocol.STX, 0x10});
        acc.reset();
        // After reset, should start fresh
        byte[] openCmd = IfProtocol.buildOpenCommand();
        byte[] result = acc.feed(openCmd);
        assertNotNull(result);
        assertArrayEquals(openCmd, result);
    }

    @Test
    void testFrameAccumulatorNullInput() {
        IfProtocol.FrameAccumulator acc = new IfProtocol.FrameAccumulator();
        assertNull(acc.feed(null));
    }

    @Test
    void testFrameAccumulatorNewStxResetsBuffer() {
        IfProtocol.FrameAccumulator acc = new IfProtocol.FrameAccumulator();
        // Start a frame
        acc.feed(new byte[]{IfProtocol.STX, 0x10});
        // New STX resets - start the open command fresh
        byte[] openCmd = IfProtocol.buildOpenCommand();
        byte[] result = acc.feed(openCmd);
        assertNotNull(result);
        assertArrayEquals(openCmd, result);
    }

    // ---- Round-trip Tests ----

    @Test
    void testRoundTripOpenCommand() {
        byte[] frame = IfProtocol.buildOpenCommand();
        IfProtocol.Message msg = IfProtocol.parseFrame(frame);
        assertNotNull(msg);
        assertEquals(IfProtocol.CMD_OPEN, msg.getCommand());
        assertEquals(0, msg.getDataLength());
    }

    @Test
    void testRoundTripCardAccessWithData() {
        byte[] felicaData = new byte[64];
        for (int i = 0; i < felicaData.length; i++) {
            felicaData[i] = (byte) i;
        }
        byte[] frame = IfProtocol.buildCardAccessCommand(felicaData);
        IfProtocol.Message msg = IfProtocol.parseFrame(frame);
        assertNotNull(msg);
        assertEquals(IfProtocol.CMD_CARD_ACCESS, msg.getCommand());
        assertArrayEquals(felicaData, msg.getData());
    }

    // ---- Helper Methods ----

    /**
     * Assert that the BCC in a frame is valid (XOR of D1..Dn-1 = 0x00).
     */
    private void assertBccValid(byte[] frame) {
        byte bcc = 0;
        for (int i = 1; i < frame.length - 1; i++) {
            bcc ^= frame[i];
        }
        assertEquals(0x00, bcc, "BCC validation failed: XOR of D1..Dn-1 should be 0x00");
    }
}
