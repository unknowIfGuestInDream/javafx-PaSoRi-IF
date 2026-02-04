/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.config;

import com.dlsc.preferencesfx.util.StorageHandlerImpl;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

/**
 * Custom StorageHandler that provides centered initial position for the preferences dialog.
 * <p>
 * This addresses the issue where PreferencesFX dialog appears at a fixed default position (700, 500)
 * on first open, which can be inappropriate on different screen sizes. This handler calculates
 * the center position of the primary screen when no saved position exists.
 */
public class CenteredStorageHandler extends StorageHandlerImpl {

    private static final double DEFAULT_WIDTH = 1000;
    private static final double DEFAULT_HEIGHT = 700;

    // Flag to track if positions have been loaded before
    private boolean positionLoaded = false;
    private double initialPosX;
    private double initialPosY;

    public CenteredStorageHandler(Class<?> saveClass) {
        super(saveClass);
        calculateCenteredPosition();
    }

    /**
     * Calculates the centered position based on primary screen bounds.
     */
    private void calculateCenteredPosition() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        initialPosX = (screenBounds.getWidth() - DEFAULT_WIDTH) / 2 + screenBounds.getMinX();
        initialPosY = (screenBounds.getHeight() - DEFAULT_HEIGHT) / 2 + screenBounds.getMinY();
    }

    @Override
    public double loadWindowPosX() {
        // Check if there's a saved value in preferences
        double saved = super.loadWindowPosX();
        // If the saved value is the hardcoded default (700), use our calculated center position
        if (!positionLoaded && saved == 700) {
            return initialPosX;
        }
        return saved;
    }

    @Override
    public double loadWindowPosY() {
        // Check if there's a saved value in preferences
        double saved = super.loadWindowPosY();
        // If the saved value is the hardcoded default (500), use our calculated center position
        if (!positionLoaded && saved == 500) {
            return initialPosY;
        }
        positionLoaded = true;
        return saved;
    }
}
