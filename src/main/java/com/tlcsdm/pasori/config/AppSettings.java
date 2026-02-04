/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.config;

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Application settings management using PreferencesFX.
 */
public class AppSettings {

    private static AppSettings instance;

    private final ObjectProperty<Locale> languageProperty;
    private final ObjectProperty<AppTheme> themeProperty;
    
    private PreferencesFx preferencesFx;
    private Runnable onSettingsChanged;

    private AppSettings() {
        // Initialize properties from saved preferences
        languageProperty = new SimpleObjectProperty<>(I18N.getCurrentLocale());
        themeProperty = new SimpleObjectProperty<>(AppTheme.getSavedTheme());

        // Add listeners to apply changes immediately
        languageProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                I18N.setLocale(newVal);
                rebuildPreferences();
                if (onSettingsChanged != null) {
                    onSettingsChanged.run();
                }
            }
        });

        themeProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                newVal.apply();
                AppTheme.saveTheme(newVal);
            }
        });
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
     * Set callback for when settings change (for UI refresh).
     */
    public void setOnSettingsChanged(Runnable callback) {
        this.onSettingsChanged = callback;
    }

    /**
     * Get the language property.
     */
    public ObjectProperty<Locale> languageProperty() {
        return languageProperty;
    }

    /**
     * Get the theme property.
     */
    public ObjectProperty<AppTheme> themeProperty() {
        return themeProperty;
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
        List<Locale> supportedLocales = Arrays.asList(I18N.getSupportedLocales());
        List<AppTheme> themes = Arrays.asList(AppTheme.values());

        preferencesFx = PreferencesFx.of(AppSettings.class,
            Category.of(I18N.get("settings.general"),
                Group.of(I18N.get("settings.appearance"),
                    Setting.of(I18N.get("settings.language"),
                        FXCollections.observableArrayList(supportedLocales),
                        languageProperty),
                    Setting.of(I18N.get("settings.theme"),
                        FXCollections.observableArrayList(themes),
                        themeProperty)
                )
            )
        ).persistWindowState(true)
         .saveSettings(true)
         .debugHistoryMode(false)
         .buttonsVisibility(true);
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
