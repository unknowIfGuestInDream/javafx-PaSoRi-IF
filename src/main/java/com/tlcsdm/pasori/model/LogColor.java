/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.model;

import javafx.scene.paint.Color;

/**
 * Enumeration of available log colors for styling log entries.
 */
public enum LogColor {
    BLACK("Black", "#000000"),
    DARK_GRAY("Dark Gray", "#404040"),
    GRAY("Gray", "#808080"),
    BLUE("Blue", "#0066CC"),
    DARK_BLUE("Dark Blue", "#003366"),
    GREEN("Green", "#008800"),
    DARK_GREEN("Dark Green", "#006600"),
    RED("Red", "#CC0000"),
    DARK_RED("Dark Red", "#990000"),
    ORANGE("Orange", "#FF8800"),
    PURPLE("Purple", "#6600CC"),
    CYAN("Cyan", "#00AAAA"),
    BROWN("Brown", "#996633");

    private final String displayName;
    private final String hexColor;

    LogColor(String displayName, String hexColor) {
        this.displayName = displayName;
        this.hexColor = hexColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHexColor() {
        return hexColor;
    }

    public Color toFxColor() {
        return Color.web(hexColor);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
