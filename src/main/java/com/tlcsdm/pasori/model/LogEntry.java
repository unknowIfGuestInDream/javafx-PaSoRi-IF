/*
 * Copyright (c) 2026, 梦里不知身是客
 * Licensed under the MIT License
 */
package com.tlcsdm.pasori.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a log entry for serial communication.
 */
public class LogEntry {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public enum Direction {
        PASORI_TO_ANTENNA("PaSoRi → アンテナIF"),
        ANTENNA_TO_PASORI("アンテナIF → PaSoRi"),
        SYSTEM("System");

        private final String displayName;

        Direction(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final LocalDateTime timestamp;
    private final Direction direction;
    private final String data;
    private final boolean isHex;

    public LogEntry(Direction direction, String data, boolean isHex) {
        this.timestamp = LocalDateTime.now();
        this.direction = direction;
        this.data = data;
        this.isHex = isHex;
    }

    public LogEntry(Direction direction, byte[] data) {
        this.timestamp = LocalDateTime.now();
        this.direction = direction;
        this.data = bytesToHex(data);
        this.isHex = true;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getData() {
        return data;
    }

    public boolean isHex() {
        return isHex;
    }

    @Override
    public String toString() {
        return String.format("[%s] [%s] %s", 
            timestamp.format(FORMATTER), 
            direction.getDisplayName(), 
            data);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i]));
            if (i < bytes.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
