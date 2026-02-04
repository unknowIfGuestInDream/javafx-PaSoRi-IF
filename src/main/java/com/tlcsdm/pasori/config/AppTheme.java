/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.config;

import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Theme;
import javafx.application.Application;

import java.util.prefs.Preferences;
import java.util.function.Supplier;

/**
 * Theme configuration for the application.
 * Supports AtlantaFX themes only to avoid CSS variable resolution issues
 * when switching between different theme families (e.g., from Modena to AtlantaFX).
 */
public enum AppTheme {
    
    ATLANTAFX_PRIMER_LIGHT("settings.theme.atlantafx.primerLight", PrimerLight::new),
    ATLANTAFX_PRIMER_DARK("settings.theme.atlantafx.primerDark", PrimerDark::new),
    ATLANTAFX_NORD_LIGHT("settings.theme.atlantafx.nordLight", NordLight::new),
    ATLANTAFX_NORD_DARK("settings.theme.atlantafx.nordDark", NordDark::new);

    private static final String PREF_KEY_THEME = "theme";
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppTheme.class);

    private final String displayNameKey;
    private final Supplier<Theme> themeSupplier;

    AppTheme(String displayNameKey, Supplier<Theme> themeSupplier) {
        if (themeSupplier == null) {
            throw new IllegalArgumentException("themeSupplier must not be null");
        }
        this.displayNameKey = displayNameKey;
        this.themeSupplier = themeSupplier;
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
        Application.setUserAgentStylesheet(themeSupplier.get().getUserAgentStylesheet());
    }

    /**
     * Get the saved theme from preferences.
     * Default is ATLANTAFX_PRIMER_LIGHT.
     */
    public static AppTheme getSavedTheme() {
        String themeName = PREFS.get(PREF_KEY_THEME, ATLANTAFX_PRIMER_LIGHT.name());
        try {
            return AppTheme.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            return ATLANTAFX_PRIMER_LIGHT;
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
