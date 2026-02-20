/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.model;

import com.tlcsdm.pasori.config.I18N;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a log entry for serial communication.
 */
public class LogEntry {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public enum Direction {
        PASORI_TO_ANTENNA("log.direction.pasoriToAntenna"),
        ANTENNA_TO_PASORI("log.direction.antennaToPasori"),
        SYSTEM("log.direction.system");

        private final String i18nKey;

        Direction(String i18nKey) {
            this.i18nKey = i18nKey;
        }

        public String getDisplayName() {
            return I18N.get(i18nKey);
        }

        public String getI18nKey() {
            return i18nKey;
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

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    /**
     * Converts the log entry to a string.
     *
     * @param showTimestamp whether to include the timestamp
     * @return the formatted log entry string
     */
    public String toString(boolean showTimestamp) {
        if (showTimestamp) {
            return String.format("[%s] [%s] %s",
                timestamp.format(FORMATTER),
                direction.getDisplayName(),
                data);
        } else {
            return String.format("[%s] %s",
                direction.getDisplayName(),
                data);
        }
    }

    @Override
    public String toString() {
        return toString(true);
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
