/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori;

/**
 * Launcher class for the PaSoRi-IF application.
 * This class serves as the entry point when running from a shaded/fat JAR.
 * It allows the JavaFX runtime to be properly initialized without requiring
 * the main class to extend Application.
 */
public class Launcher {
    
    public static void main(String[] args) {
        PaSoRiApplication.main(args);
    }
}
