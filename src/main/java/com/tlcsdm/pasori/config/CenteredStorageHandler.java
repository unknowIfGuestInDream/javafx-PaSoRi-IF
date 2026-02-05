/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.config;

import com.dlsc.preferencesfx.util.StorageHandlerImpl;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

/**
 * Custom StorageHandler that provides centered initial position for the preferences dialog.
 * <p>
 * This addresses the issue where PreferencesFX dialog appears at a fixed default position
 * on first open, which can be inappropriate on different screen sizes. This handler calculates
 * the center position of the primary screen when no saved position exists.
 * <p>
 * Additionally, this handler properly serializes JavaFX Color objects to hex format strings
 * to avoid "Invalid color specification" errors when loading color preferences.
 */
public class CenteredStorageHandler extends StorageHandlerImpl {

    // PreferencesFX default values from com.dlsc.preferencesfx.util.Constants
    private static final double PREFERENCESFX_DEFAULT_WIDTH = 1000;
    private static final double PREFERENCESFX_DEFAULT_HEIGHT = 700;
    private static final double PREFERENCESFX_DEFAULT_POS_X = 700;
    private static final double PREFERENCESFX_DEFAULT_POS_Y = 500;

    private final double centeredPosX;
    private final double centeredPosY;
    private boolean initialLoadDone = false;

    public CenteredStorageHandler(Class<?> saveClass) {
        super(saveClass);
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        centeredPosX = (screenBounds.getWidth() - PREFERENCESFX_DEFAULT_WIDTH) / 2 + screenBounds.getMinX();
        centeredPosY = (screenBounds.getHeight() - PREFERENCESFX_DEFAULT_HEIGHT) / 2 + screenBounds.getMinY();
    }

    @Override
    public double loadWindowPosX() {
        double saved = super.loadWindowPosX();
        // If this is the first load and the saved value equals the hardcoded default,
        // return the calculated centered position instead
        if (!initialLoadDone && saved == PREFERENCESFX_DEFAULT_POS_X) {
            return centeredPosX;
        }
        return saved;
    }

    @Override
    public double loadWindowPosY() {
        double saved = super.loadWindowPosY();
        // If this is the first load and the saved value equals the hardcoded default,
        // return the calculated centered position instead
        if (!initialLoadDone && saved == PREFERENCESFX_DEFAULT_POS_Y) {
            // Mark initial load as done after both X and Y have been read
            initialLoadDone = true;
            return centeredPosY;
        }
        initialLoadDone = true;
        return saved;
    }

    /**
     * Saves an object to preferences, with special handling for Color objects.
     * Color objects are serialized to hex format (e.g., "#0066CC") to ensure
     * compatibility with JavaFX's Color.web() parsing.
     */
    @Override
    public void saveObject(String breadcrumb, Object object) {
        if (object instanceof Color color) {
            // Serialize Color to hex format for proper parsing later
            super.saveObject(breadcrumb, colorToHex(color));
        } else {
            super.saveObject(breadcrumb, object);
        }
    }

    /**
     * Loads an object from preferences, with special handling for Color objects.
     * Handles various color string formats and falls back to the default if parsing fails.
     */
    @Override
    public Object loadObject(String breadcrumb, Object defaultObject) {
        if (defaultObject instanceof Color defaultColor) {
            Object loaded = super.loadObject(breadcrumb, colorToHex(defaultColor));
            if (loaded instanceof String colorString) {
                return parseColor(colorString, defaultColor);
            }
            return defaultColor;
        }
        return super.loadObject(breadcrumb, defaultObject);
    }

    /**
     * Converts a Color object to a hex string format (e.g., "#0066CCFF" including alpha).
     */
    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255),
            (int) (color.getOpacity() * 255));
    }

    /**
     * Parses a color string to a Color object, handling various formats.
     * Falls back to the default color if parsing fails.
     */
    private Color parseColor(String colorString, Color defaultColor) {
        if (colorString == null || colorString.isBlank()) {
            return defaultColor;
        }
        try {
            // Try standard Color.web() parsing first (handles #RRGGBB and #RRGGBBAA formats)
            return Color.web(colorString);
        } catch (IllegalArgumentException e) {
            // Handle "0xRRGGBBAA" format from Color.toString()
            try {
                if (colorString.startsWith("0x") && colorString.length() == 10) {
                    // Convert "0xRRGGBBAA" format to "#RRGGBBAA"
                    String hex = "#" + colorString.substring(2);
                    return Color.web(hex);
                } else if (colorString.startsWith("0x") && colorString.length() == 8) {
                    // Convert "0xRRGGBB" format to "#RRGGBB"
                    String hex = "#" + colorString.substring(2);
                    return Color.web(hex);
                }
            } catch (IllegalArgumentException ignored) {
                // Fall through to default
            }
            return defaultColor;
        }
    }
}
