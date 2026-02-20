/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.model;

import java.util.Locale;
import java.util.Objects;

/**
 * A wrapper class for Locale that displays the language name in its native form.
 * For example: English, 中文, 日本語
 */
public class DisplayLocale {

    private final Locale locale;

    public DisplayLocale(Locale locale) {
        this.locale = Objects.requireNonNull(locale, "Locale cannot be null");
    }

    /**
     * Get the wrapped Locale.
     *
     * @return the Locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the display name of the language in its own language.
     * For example: English, 中文, 日本語
     */
    @Override
    public String toString() {
        return locale.getDisplayLanguage(locale);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DisplayLocale other = (DisplayLocale) obj;
        return locale.getLanguage().equals(other.locale.getLanguage());
    }

    @Override
    public int hashCode() {
        return locale.getLanguage().hashCode();
    }
}
