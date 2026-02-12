/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.service;

import com.sun.jna.Memory;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import com.tlcsdm.pasori.sdk.felica.FelicaErrorType;
import com.tlcsdm.pasori.sdk.felica.FelicaLibrary;
import com.tlcsdm.pasori.sdk.felica.FelicaLibrary.CardInformation;
import com.tlcsdm.pasori.sdk.felica.FelicaLibrary.Polling;
import com.tlcsdm.pasori.sdk.felica.RwErrorType;

import java.util.function.Consumer;

/**
 * Service for communicating with PaSoRi NFC reader/writer via the FeliCa SDK.
 *
 * <p>This service replaces serial port communication with direct SDK calls
 * to the FeliCa library (felica.dll) through JNA.</p>
 */
public class PaSoRiSdkService {

    /** IDm length in bytes */
    public static final int IDM_LENGTH = 8;
    /** PMm length in bytes */
    public static final int PMM_LENGTH = 8;
    /** Default system code for polling (wildcard: 0xFFFF) */
    private static final byte[] DEFAULT_SYSTEM_CODE = {(byte) 0xFF, (byte) 0xFF};

    private volatile boolean initialized = false;
    private volatile boolean readerOpened = false;
    private Consumer<String> errorCallback;
    private Consumer<String> infoCallback;

    private FelicaLibrary felicaLib;

    /**
     * Initialize the FeliCa library.
     *
     * @return true if initialization succeeded
     */
    public boolean initializeLibrary() {
        if (initialized) {
            notifyInfo("FeliCa library already initialized");
            return true;
        }
        try {
            felicaLib = FelicaLibrary.INSTANCE;
            boolean result = felicaLib.initialize_library();
            if (result) {
                initialized = true;
                notifyInfo("FeliCa library initialized successfully");
            } else {
                notifyError("Failed to initialize FeliCa library: " + getLastErrorDescription());
            }
            return result;
        } catch (UnsatisfiedLinkError e) {
            notifyError("FeliCa library (felica.dll) not found: " + e.getMessage());
            return false;
        }
    }

    /**
     * Dispose of the FeliCa library resources.
     *
     * @return true if disposal succeeded
     */
    public boolean disposeLibrary() {
        if (!initialized || felicaLib == null) {
            return true;
        }
        if (readerOpened) {
            closeReader();
        }
        boolean result = felicaLib.dispose_library();
        initialized = false;
        if (result) {
            notifyInfo("FeliCa library disposed");
        } else {
            notifyError("Failed to dispose FeliCa library");
        }
        return result;
    }

    /**
     * Auto-detect and open the PaSoRi reader/writer.
     *
     * @return true if the reader was opened successfully
     */
    public boolean openReaderAuto() {
        if (!initialized || felicaLib == null) {
            notifyError("FeliCa library not initialized");
            return false;
        }
        if (readerOpened) {
            notifyInfo("Reader/Writer already opened");
            return true;
        }
        boolean result = felicaLib.open_reader_writer_auto();
        if (result) {
            readerOpened = true;
            notifyInfo("PaSoRi reader/writer opened (auto-detect)");
        } else {
            notifyError("Failed to open reader/writer: " + getLastErrorDescription());
        }
        return result;
    }

    /**
     * Close the reader/writer connection.
     *
     * @return true if successfully closed
     */
    public boolean closeReader() {
        if (!initialized || felicaLib == null || !readerOpened) {
            readerOpened = false;
            return true;
        }
        boolean result = felicaLib.close_reader_writer();
        readerOpened = false;
        if (result) {
            notifyInfo("PaSoRi reader/writer closed");
        } else {
            notifyError("Failed to close reader/writer: " + getLastErrorDescription());
        }
        return result;
    }

    /**
     * Poll for FeliCa cards using the default wildcard system code (0xFFFF).
     *
     * @return card information array [IDm(8 bytes), PMm(8 bytes)], or null if no card found
     */
    public byte[][] pollCard() {
        return pollCard(DEFAULT_SYSTEM_CODE);
    }

    /**
     * Poll for FeliCa cards with a specific system code.
     *
     * @param systemCode the 2-byte system code for polling
     * @return card information array [IDm(8 bytes), PMm(8 bytes)], or null if no card found
     */
    public byte[][] pollCard(byte[] systemCode) {
        if (!readerOpened || felicaLib == null) {
            notifyError("Reader/Writer not opened");
            return null;
        }

        // Prepare polling structure
        Polling polling = new Polling();
        Memory systemCodeMem = new Memory(2);
        systemCodeMem.write(0, systemCode, 0, 2);
        polling.system_code = systemCodeMem;
        polling.time_slot = 0x00; // Single slot

        // Prepare card information structure
        CardInformation cardInfo = new CardInformation();
        Memory idmMem = new Memory(IDM_LENGTH);
        Memory pmmMem = new Memory(PMM_LENGTH);
        cardInfo.card_idm = idmMem;
        cardInfo.card_pmm = pmmMem;

        ByteByReference numCards = new ByteByReference();

        boolean result = felicaLib.polling_and_get_card_information(polling, numCards, cardInfo);

        if (result && numCards.getValue() > 0) {
            byte[] idm = idmMem.getByteArray(0, IDM_LENGTH);
            byte[] pmm = pmmMem.getByteArray(0, PMM_LENGTH);
            return new byte[][]{idm, pmm};
        }
        return null;
    }

    /**
     * Check if the FeliCa library is initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Check if the reader/writer is opened.
     *
     * @return true if opened
     */
    public boolean isReaderOpened() {
        return readerOpened;
    }

    /**
     * Set callback for error messages.
     *
     * @param callback the error callback
     */
    public void setErrorCallback(Consumer<String> callback) {
        this.errorCallback = callback;
    }

    /**
     * Set callback for informational messages.
     *
     * @param callback the info callback
     */
    public void setInfoCallback(Consumer<String> callback) {
        this.infoCallback = callback;
    }

    /**
     * Get a description of the last error from the SDK.
     *
     * @return error description string
     */
    public String getLastErrorDescription() {
        if (felicaLib == null) {
            return "Library not loaded";
        }
        IntByReference felicaError = new IntByReference();
        IntByReference rwError = new IntByReference();
        boolean result = felicaLib.get_last_error_types(felicaError, rwError);
        if (result) {
            FelicaErrorType fError = FelicaErrorType.fromCode(felicaError.getValue());
            RwErrorType rError = RwErrorType.fromCode(rwError.getValue());
            return "FeliCa: " + fError.getDescription() + " (" + fError.getCode() + "), "
                + "RW: " + rError.getDescription() + " (" + rError.getCode() + ")";
        }
        return "Unable to retrieve error information";
    }

    /**
     * Shut down the service, closing the reader and disposing the library.
     */
    public void shutdown() {
        disposeLibrary();
    }

    private void notifyError(String message) {
        if (errorCallback != null) {
            errorCallback.accept(message);
        }
    }

    private void notifyInfo(String message) {
        if (infoCallback != null) {
            infoCallback.accept(message);
        }
    }

    /**
     * Convert a byte array to a hex string for display.
     *
     * @param bytes the byte array
     * @return hex string representation
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i]));
            if (i < bytes.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
