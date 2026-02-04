/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.config;

import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

import java.util.prefs.Preferences;

/**
 * Theme configuration for the application.
 * Supports JavaFX default theme and AtlantaFX themes.
 */
public enum AppTheme {
    
    LIGHT("settings.theme.light", null),
    ATLANTAFX_PRIMER_LIGHT("settings.theme.atlantafx.primerLight", PrimerLight.class.getName()),
    ATLANTAFX_PRIMER_DARK("settings.theme.atlantafx.primerDark", PrimerDark.class.getName()),
    ATLANTAFX_NORD_LIGHT("settings.theme.atlantafx.nordLight", NordLight.class.getName()),
    ATLANTAFX_NORD_DARK("settings.theme.atlantafx.nordDark", NordDark.class.getName());

    private static final String PREF_KEY_THEME = "theme";
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppTheme.class);

    private final String displayNameKey;
    private final String themeClass;

    AppTheme(String displayNameKey, String themeClass) {
        this.displayNameKey = displayNameKey;
        this.themeClass = themeClass;
    }

    /**
     * Get the display name key for i18n.
     */
    public String getDisplayNameKey() {
        return displayNameKey;
    }

    /**
     * Get the display name for the theme.
     */
    public String getDisplayName() {
        return I18N.get(displayNameKey);
    }

    /**
     * Apply this theme to the application.
     */
    public void apply() {
        if (themeClass != null) {
            Application.setUserAgentStylesheet(themeClass);
        } else {
            // Reset to default JavaFX theme
            Application.setUserAgentStylesheet(null);
        }
    }

    /**
     * Get the saved theme from preferences.
     * Default is LIGHT.
     */
    public static AppTheme getSavedTheme() {
        String themeName = PREFS.get(PREF_KEY_THEME, LIGHT.name());
        try {
            return AppTheme.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            return LIGHT;
        }
    }

    /**
     * Save the theme to preferences.
     */
    public static void saveTheme(AppTheme theme) {
        if (theme != null) {
            PREFS.put(PREF_KEY_THEME, theme.name());
        }
    }

    /**
     * Apply the saved theme.
     */
    public static void applySavedTheme() {
        getSavedTheme().apply();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
