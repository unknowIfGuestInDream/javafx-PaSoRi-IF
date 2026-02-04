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
 * This addresses the issue where PreferencesFX dialog appears at a fixed default position
 * on first open, which can be inappropriate on different screen sizes. This handler calculates
 * the center position of the primary screen when no saved position exists.
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
}
