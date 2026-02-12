/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.sdk.felica;

/**
 * FeliCa Access Library error types.
 * Mapped from felica_error.h - enumeration_felica_error_type.
 */
public enum FelicaErrorType {

    FELICA_ERROR_NOT_OCCURRED(1000, "Error has not occurred"),
    FELICA_UNKNOWN_ERROR(1001, "Unknown error"),
    FELICA_ILLEGAL_ARGUMENT(1002, "Illegal argument"),
    FELICA_MEMORY_ALLOCATION_ERROR(1003, "Memory allocation error"),
    FELICA_THREAD_CREATION_ERROR(1004, "Thread creation error"),
    FELICA_LIBRARY_NOT_INITIALIZED(1005, "Library not initialized"),
    FELICA_LIBRARY_ALREADY_INITIALIZED(1006, "Library already initialized"),
    FELICA_INVALID_FILE_NAME(1007, "Invalid file name"),
    FELICA_FILE_NOT_FOUND(1008, "File not found"),
    FELICA_FILE_OPEN_ERROR(1009, "File open error"),
    FELICA_FILE_NOT_OPENED(1010, "File not opened"),
    FELICA_FILE_ALREADY_OPENED(1011, "File already opened"),
    FELICA_INVALID_DIRECTORY_NAME(1012, "Invalid directory name"),
    FELICA_DIRECTORY_NOT_FOUND(1013, "Directory not found"),
    FELICA_DIRECTORY_OPEN_ERROR(1014, "Directory open error"),
    FELICA_DIRECTORY_NOT_OPENED(1015, "Directory not opened"),
    FELICA_DIRECTORY_ALREADY_OPENED(1016, "Directory already opened"),
    FELICA_INVALID_COMMUNICATIONS_PORT_NAME(1017, "Invalid communications port name"),
    FELICA_COMMUNICATIONS_PORT_NOT_FOUND(1018, "Communications port not found"),
    FELICA_COMMUNICATIONS_PORT_OPEN_ERROR(1019, "Communications port open error"),
    FELICA_COMMUNICATIONS_PORT_NOT_OPENED(1020, "Communications port not opened"),
    FELICA_COMMUNICATIONS_PORT_ALREADY_OPENED(1021, "Communications port already opened"),
    FELICA_INVALID_TIME_OUT(1022, "Invalid timeout"),
    FELICA_INVALID_BAUD_RATE(1023, "Invalid baud rate"),
    FELICA_INVALID_RETRY_COUNT(1024, "Invalid retry count"),
    FELICA_READER_WRITER_CONTROL_LIBRARY_NOT_FOUND(1025, "Reader/Writer control library not found"),
    FELICA_READER_WRITER_CONTROL_LIBRARY_LOAD_ERROR(1026, "Reader/Writer control library load error"),
    FELICA_READER_WRITER_OPEN_ERROR(1027, "Reader/Writer open error"),
    FELICA_READER_WRITER_OPEN_AUTO_ERROR(1028, "Reader/Writer auto-open error"),
    FELICA_READER_WRITER_NOT_OPENED(1029, "Reader/Writer not opened"),
    FELICA_READER_WRITER_ALREADY_OPENED(1030, "Reader/Writer already opened"),
    FELICA_READER_WRITER_RECONNECT_ERROR(1031, "Reader/Writer reconnect error"),
    FELICA_POLLING_ERROR(1036, "Polling error");

    private final int code;
    private final String description;

    FelicaErrorType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Find the error type by its native code value.
     *
     * @param code the native error code
     * @return the matching error type, or FELICA_UNKNOWN_ERROR if not found
     */
    public static FelicaErrorType fromCode(int code) {
        for (FelicaErrorType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return FELICA_UNKNOWN_ERROR;
    }
}
