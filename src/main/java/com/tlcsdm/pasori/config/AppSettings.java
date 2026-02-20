/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.config;

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.tlcsdm.pasori.model.DisplayLocale;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Application settings management using PreferencesFX.
 */
public class AppSettings {

    private static AppSettings instance;

    private static final Preferences PREFS = Preferences.userNodeForPackage(AppSettings.class);
    private static final String PREF_LOG_TIMESTAMP = "logTimestamp";
    private static final String PREF_LOG_COLOR_PASORI_TO_ANTENNA = "logColorPasoriToAntenna";
    private static final String PREF_LOG_COLOR_ANTENNA_TO_PASORI = "logColorAntennaToPasori";
    private static final String PREF_LOG_COLOR_SYSTEM = "logColorSystem";

    private final ObjectProperty<DisplayLocale> languageProperty;
    private final ObjectProperty<AppTheme> themeProperty;
    private final BooleanProperty logTimestampProperty;
    private final ObjectProperty<Color> logColorPasoriToAntennaProperty;
    private final ObjectProperty<Color> logColorAntennaToPasoriProperty;
    private final ObjectProperty<Color> logColorSystemProperty;

    private PreferencesFx preferencesFx;

    private AppSettings() {
        // Initialize properties from saved preferences
        languageProperty = new SimpleObjectProperty<>(new DisplayLocale(I18N.getCurrentLocale()));
        themeProperty = new SimpleObjectProperty<>(AppTheme.getSavedTheme());

        // Log settings
        logTimestampProperty = new SimpleBooleanProperty(PREFS.getBoolean(PREF_LOG_TIMESTAMP, true));
        logColorPasoriToAntennaProperty = new SimpleObjectProperty<>(
            loadColor(PREF_LOG_COLOR_PASORI_TO_ANTENNA, Color.web("#0066CC")));
        logColorAntennaToPasoriProperty = new SimpleObjectProperty<>(
            loadColor(PREF_LOG_COLOR_ANTENNA_TO_PASORI, Color.web("#008800")));
        logColorSystemProperty = new SimpleObjectProperty<>(
            loadColor(PREF_LOG_COLOR_SYSTEM, Color.web("#808080")));

        // Add listeners to apply changes immediately
        languageProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                I18N.setLocale(newVal.getLocale());
                rebuildPreferences();
            }
        });

        themeProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                newVal.apply();
                AppTheme.saveTheme(newVal);
            }
        });

        // Log settings listeners
        logTimestampProperty.addListener((obs, oldVal, newVal) ->
            PREFS.putBoolean(PREF_LOG_TIMESTAMP, newVal));
        logColorPasoriToAntennaProperty.addListener((obs, oldVal, newVal) ->
            saveColor(PREF_LOG_COLOR_PASORI_TO_ANTENNA, newVal));
        logColorAntennaToPasoriProperty.addListener((obs, oldVal, newVal) ->
            saveColor(PREF_LOG_COLOR_ANTENNA_TO_PASORI, newVal));
        logColorSystemProperty.addListener((obs, oldVal, newVal) ->
            saveColor(PREF_LOG_COLOR_SYSTEM, newVal));
    }

    private Color loadColor(String key, Color defaultColor) {
        String saved = PREFS.get(key, null);
        if (saved != null) {
            try {
                return Color.web(saved);
            } catch (IllegalArgumentException e) {
                return defaultColor;
            }
        }
        return defaultColor;
    }

    private void saveColor(String key, Color color) {
        if (color != null) {
            PREFS.put(key, toHexString(color));
        }
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    /**
     * Get the singleton instance.
     */
    public static AppSettings getInstance() {
        if (instance == null) {
            instance = new AppSettings();
        }
        return instance;
    }

    /**
     * Get the language property.
     */
    public ObjectProperty<DisplayLocale> languageProperty() {
        return languageProperty;
    }

    /**
     * Get the theme property.
     */
    public ObjectProperty<AppTheme> themeProperty() {
        return themeProperty;
    }

    /**
     * Get the log timestamp property.
     */
    public BooleanProperty logTimestampProperty() {
        return logTimestampProperty;
    }

    /**
     * Get whether to show timestamps in log entries.
     */
    public boolean isLogTimestampEnabled() {
        return logTimestampProperty.get();
    }

    /**
     * Get the log color for PaSoRi to Antenna direction.
     */
    public ObjectProperty<Color> logColorPasoriToAntennaProperty() {
        return logColorPasoriToAntennaProperty;
    }

    /**
     * Get the log color for Antenna to PaSoRi direction.
     */
    public ObjectProperty<Color> logColorAntennaToPasoriProperty() {
        return logColorAntennaToPasoriProperty;
    }

    /**
     * Get the log color for system messages.
     */
    public ObjectProperty<Color> logColorSystemProperty() {
        return logColorSystemProperty;
    }

    /**
     * Get the hex color for a log direction.
     */
    public String getLogColorHex(com.tlcsdm.pasori.model.LogEntry.Direction direction) {
        return switch (direction) {
            case PASORI_TO_ANTENNA -> toHexString(logColorPasoriToAntennaProperty.get());
            case ANTENNA_TO_PASORI -> toHexString(logColorAntennaToPasoriProperty.get());
            case SYSTEM -> toHexString(logColorSystemProperty.get());
        };
    }

    /**
     * Create and show the settings dialog.
     */
    public PreferencesFx getPreferencesFx() {
        if (preferencesFx == null) {
            buildPreferences();
        }
        return preferencesFx;
    }

    private void buildPreferences() {
        List<DisplayLocale> supportedLocales = Arrays.stream(I18N.getSupportedLocales())
            .map(DisplayLocale::new)
            .toList();
        List<AppTheme> themes = Arrays.asList(AppTheme.values());

        preferencesFx = PreferencesFx.of(new CenteredStorageHandler(AppSettings.class),
            Category.of(I18N.get("settings.general"),
                Group.of(I18N.get("settings.languageAndTheme"),
                    Setting.of(I18N.get("settings.language"),
                        FXCollections.observableArrayList(supportedLocales),
                        languageProperty),
                    Setting.of(I18N.get("settings.theme"),
                        FXCollections.observableArrayList(themes),
                        themeProperty)
                ),
                Group.of(I18N.get("settings.logSettings"),
                    Setting.of(I18N.get("settings.logTimestamp"),
                        logTimestampProperty),
                    Setting.of(I18N.get("settings.logColorPasoriToAntenna"),
                        logColorPasoriToAntennaProperty),
                    Setting.of(I18N.get("settings.logColorAntennaToPasori"),
                        logColorAntennaToPasoriProperty),
                    Setting.of(I18N.get("settings.logColorSystem"),
                        logColorSystemProperty)
                )
            )
        ).persistWindowState(true)
         .saveSettings(true)
         .debugHistoryMode(false)
         .buttonsVisibility(false)
         .instantPersistent(true);
    }

    private void rebuildPreferences() {
        preferencesFx = null;
    }

    /**
     * Apply initial settings (called at application startup).
     */
    public void applyInitialSettings() {
        AppTheme.applySavedTheme();
    }
}
