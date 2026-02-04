/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.config;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Internationalization utility class for managing language resources.
 * Supports English, Chinese, and Japanese languages.
 */
public class I18N {

    private static final String BUNDLE_NAME = "com.tlcsdm.pasori.i18n.messages";
    private static final String PREF_KEY_LANGUAGE = "language";
    private static final Preferences PREFS = Preferences.userNodeForPackage(I18N.class);

    private static ResourceBundle bundle;
    private static Locale currentLocale;

    static {
        initializeLocale();
    }

    private I18N() {
        // Utility class
    }

    /**
     * Initialize locale from saved preferences or system default.
     */
    private static void initializeLocale() {
        String savedLanguage = PREFS.get(PREF_KEY_LANGUAGE, null);
        
        if (savedLanguage != null) {
            currentLocale = Locale.forLanguageTag(savedLanguage);
        } else {
            // Use system locale, fall back to English if not supported
            Locale systemLocale = Locale.getDefault();
            if (isSupportedLocale(systemLocale)) {
                currentLocale = systemLocale;
            } else {
                currentLocale = Locale.ENGLISH;
            }
            // Save the initial setting
            saveLanguagePreference(currentLocale);
        }
        
        loadBundle();
    }

    /**
     * Check if the locale is supported.
     */
    private static boolean isSupportedLocale(Locale locale) {
        String language = locale.getLanguage();
        return "en".equals(language) || "zh".equals(language) || "ja".equals(language);
    }

    /**
     * Load the resource bundle for the current locale.
     */
    private static void loadBundle() {
        bundle = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
    }

    /**
     * Get a localized string for the given key.
     *
     * @param key the resource key
     * @return the localized string, or the key itself if not found
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            // Return the key as fallback for missing translations
            return key;
        }
    }

    /**
     * Get a localized string for the given key with parameters.
     *
     * @param key the resource key
     * @param params the parameters to substitute
     * @return the localized string with parameters, or the key itself if not found
     */
    public static String get(String key, Object... params) {
        try {
            String pattern = bundle.getString(key);
            return MessageFormat.format(pattern, params);
        } catch (MissingResourceException e) {
            // Return the key as fallback for missing translations
            return key;
        } catch (IllegalArgumentException e) {
            // Return the pattern if formatting fails
            try {
                return bundle.getString(key);
            } catch (MissingResourceException ex) {
                return key;
            }
        }
    }

    /**
     * Get the current locale.
     * Returns the matching locale from getSupportedLocales() to ensure proper equality check.
     *
     * @return the current locale
     */
    public static Locale getCurrentLocale() {
        // Return the matching supported locale to ensure proper ComboBox selection
        for (Locale supported : getSupportedLocales()) {
            if (supported.getLanguage().equals(currentLocale.getLanguage())) {
                return supported;
            }
        }
        return currentLocale;
    }

    /**
     * Set the application locale.
     *
     * @param locale the new locale
     */
    public static void setLocale(Locale locale) {
        if (locale != null && isSupportedLocale(locale)) {
            currentLocale = locale;
            loadBundle();
            saveLanguagePreference(locale);
        }
    }

    /**
     * Set the application locale by language tag.
     *
     * @param languageTag the language tag (e.g., "en", "zh", "ja")
     */
    public static void setLocale(String languageTag) {
        setLocale(Locale.forLanguageTag(languageTag));
    }

    /**
     * Save the language preference.
     */
    private static void saveLanguagePreference(Locale locale) {
        PREFS.put(PREF_KEY_LANGUAGE, locale.toLanguageTag());
    }

    /**
     * Get supported locales.
     *
     * @return array of supported locales
     */
    public static Locale[] getSupportedLocales() {
        return new Locale[] {
            Locale.ENGLISH,
            Locale.SIMPLIFIED_CHINESE,
            Locale.JAPANESE
        };
    }

    /**
     * Get display name for a locale in its own language.
     *
     * @param locale the locale
     * @return the display name
     */
    public static String getDisplayName(Locale locale) {
        return locale.getDisplayLanguage(locale);
    }
}
