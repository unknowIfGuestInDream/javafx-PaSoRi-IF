/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.sdk;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;

import java.util.Arrays;
import java.util.List;

/**
 * JNA interface for the FeliCa Access Library (felica.dll).
 * Mapped from felica.h / felica_basic_lite_s.h header files.
 *
 * <p>This interface provides Java bindings for the Sony FeliCa SDK,
 * enabling communication with PaSoRi NFC reader/writer devices.</p>
 */
public interface FelicaLibrary extends Library {

    /**
     * Load the FeliCa library instance.
     * The library name "felica" maps to felica.dll on Windows.
     */
    FelicaLibrary INSTANCE = Native.load("felica", FelicaLibrary.class);

    // ---- Structures ----

    /**
     * Reader/Writer mode configuration.
     * Maps to structure_reader_writer_mode in the C header.
     */
    @Structure.FieldOrder({"port_name", "baud_rate", "encryption_mode", "kar", "kbr"})
    class ReaderWriterMode extends Structure {
        public String port_name;
        public int baud_rate;
        public byte encryption_mode;
        public Pointer kar;
        public Pointer kbr;
    }

    /**
     * Polling parameters for card detection.
     * Maps to structure_polling in the C header.
     */
    @Structure.FieldOrder({"system_code", "time_slot"})
    class Polling extends Structure {
        /** System code (2 bytes) */
        public Pointer system_code;
        /** Time slot (0x00, 0x01, 0x03, 0x07, 0x0f) */
        public byte time_slot;
    }

    /**
     * Card information containing IDm and PMm.
     * Maps to structure_card_information in the C header.
     */
    @Structure.FieldOrder({"card_idm", "card_pmm"})
    class CardInformation extends Structure {
        /** Card IDm storage area (8 bytes) */
        public Pointer card_idm;
        /** Card PMm storage area (8 bytes) */
        public Pointer card_pmm;
    }

    /**
     * Device information structure.
     * Maps to structure_device_information in the C header.
     */
    @Structure.FieldOrder({"device_info_type", "device_info_connect"})
    class DeviceInformation extends Structure {
        /** USB reader/writer type */
        public byte device_info_type;
        /** USB reader/writer connection method (0x00: built-in, 0x01: external) */
        public byte device_info_connect;
    }

    // ---- Library lifecycle ----

    /**
     * Initialize the FeliCa library.
     *
     * @return true if successful
     */
    boolean initialize_library();

    /**
     * Dispose (cleanup) the FeliCa library.
     *
     * @return true if successful
     */
    boolean dispose_library();

    // ---- Error handling ----

    /**
     * Get the last error type.
     *
     * @param error_type pointer to receive the error type value
     * @return true if successful
     */
    boolean get_last_error_type(IntByReference error_type);

    /**
     * Get the last error types for both FeliCa and Reader/Writer libraries.
     *
     * @param felica_error_type pointer to receive FeliCa error type
     * @param rw_error_type pointer to receive Reader/Writer error type
     * @return true if successful
     */
    boolean get_last_error_types(IntByReference felica_error_type, IntByReference rw_error_type);

    // ---- Reader/Writer operations ----

    /**
     * Open reader/writer with specific mode configuration.
     *
     * @param reader_writer_mode the reader/writer mode configuration
     * @return true if successful
     */
    boolean open_reader_writer(ReaderWriterMode reader_writer_mode);

    /**
     * Automatically detect and open the reader/writer.
     *
     * @return true if successful
     */
    boolean open_reader_writer_auto();

    /**
     * Close the reader/writer connection.
     *
     * @return true if successful
     */
    boolean close_reader_writer();

    // ---- Transaction management ----

    /**
     * Acquire transaction lock on the reader/writer.
     *
     * @return true if successful
     */
    boolean transaction_lock();

    /**
     * Release transaction lock on the reader/writer.
     *
     * @return true if successful
     */
    boolean transaction_unlock();

    // ---- Device information ----

    /**
     * Get device information.
     *
     * @param device_information pointer to device information structure
     * @return true if successful
     */
    boolean get_device_information(DeviceInformation device_information);

    // ---- Timeout settings ----

    /**
     * Set timeout value in milliseconds.
     *
     * @param time_out timeout value in ms
     * @return true if successful
     */
    boolean set_time_out(int time_out);

    /**
     * Get current timeout value.
     *
     * @param time_out pointer to receive timeout value in ms
     * @return true if successful
     */
    boolean get_time_out(IntByReference time_out);

    /**
     * Set lock timeout for reader/writer access.
     *
     * @param lock_timeout timeout value in ms (0 = infinite wait)
     * @return true if successful
     */
    boolean set_lock_timeout(int lock_timeout);

    /**
     * Get lock timeout value.
     *
     * @param lock_timeout pointer to receive timeout value in ms
     * @return true if successful
     */
    boolean get_lock_timeout(IntByReference lock_timeout);

    /**
     * Set polling timeout for reader/writer.
     *
     * @param polling_timeout timeout value in ms
     * @return true if successful
     */
    boolean set_polling_timeout(int polling_timeout);

    /**
     * Get polling timeout value.
     *
     * @param polling_timeout pointer to receive timeout value in ms
     * @return true if successful
     */
    boolean get_polling_timeout(IntByReference polling_timeout);

    // ---- Card operations ----

    /**
     * Poll for cards and get card information.
     *
     * @param polling polling parameters (system code and time slot)
     * @param number_of_cards pointer to receive the number of cards found (1-3)
     * @param card_information pointer to receive the first card's information
     * @return true if successful (card found)
     */
    boolean polling_and_get_card_information(
        Polling polling,
        ByteByReference number_of_cards,
        CardInformation card_information
    );

    /**
     * Get card information for a specific card index (from last polling).
     *
     * @param card_index card index (1-3)
     * @param card_information pointer to receive card information
     * @return true if successful
     */
    boolean get_last_card_information(
        byte card_index,
        CardInformation card_information
    );
}
