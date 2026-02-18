/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Utility class for checking PaSoRi driver installation status.
 *
 * <p>The PaSoRi NFC reader/writer requires the Sony FeliCa port driver
 * (NFCPortWithDriver.exe) to be installed on the system.</p>
 */
public final class DriverChecker {

    private DriverChecker() {
        // Utility class
    }

    /**
     * Check whether the PaSoRi (FeliCa port) driver is installed on the system.
     *
     * <p>Attempts to load {@code felica.dll} via JNA. If the library cannot be
     * found on the system library path, the driver is considered not installed.</p>
     *
     * @return true if the driver is installed, false otherwise
     */
    public static boolean isDriverInstalled() {
        try {
            Native.load("felica", Library.class);
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }
}
