/*
 * Copyright (c) 2026, 梦里不知身是客
 */
package com.tlcsdm.pasori.sdk.felica;

/**
 * FeliCa Reader/Writer Control Library error types.
 * Mapped from rw_error.h - enumeration_rw_error_type.
 */
public enum RwErrorType {

    RW_ERROR_NOT_OCCURRED(100, "Error has not occurred"),
    RW_UNKNOWN_ERROR(101, "Unknown error"),
    RW_ILLEGAL_ARGUMENT(102, "Illegal argument"),
    RW_MEMORY_ALLOCATION_ERROR(103, "Memory allocation error"),
    RW_THREAD_CREATION_ERROR(104, "Thread creation error"),
    RW_LIBRARY_NOT_INITIALIZED(105, "Library not initialized"),
    RW_LIBRARY_ALREADY_INITIALIZED(106, "Library already initialized"),
    RW_INVALID_FILE_NAME(107, "Invalid file name"),
    RW_FILE_NOT_FOUND(108, "File not found"),
    RW_FILE_OPEN_ERROR(109, "File open error"),
    RW_FILE_NOT_OPENED(110, "File not opened"),
    RW_FILE_ALREADY_OPENED(111, "File already opened"),
    RW_INVALID_DIRECTORY_NAME(112, "Invalid directory name"),
    RW_DIRECTORY_NOT_FOUND(113, "Directory not found"),
    RW_DIRECTORY_OPEN_ERROR(114, "Directory open error"),
    RW_DIRECTORY_NOT_OPENED(115, "Directory not opened"),
    RW_DIRECTORY_ALREADY_OPENED(116, "Directory already opened"),
    RW_INVALID_COMMUNICATIONS_PORT_NAME(117, "Invalid communications port name"),
    RW_COMMUNICATIONS_PORT_NOT_FOUND(118, "Communications port not found"),
    RW_COMMUNICATIONS_PORT_OPEN_ERROR(119, "Communications port open error"),
    RW_COMMUNICATIONS_PORT_NOT_OPENED(120, "Communications port not opened"),
    RW_COMMUNICATIONS_PORT_ALREADY_OPENED(121, "Communications port already opened"),
    RW_INVALID_TIME_OUT(122, "Invalid timeout"),
    RW_INVALID_BAUD_RATE(123, "Invalid baud rate"),
    RW_CARD_NOT_FOUND(157, "Card not found"),
    RW_INVALID_CARD_INDEX(158, "Invalid card index"),
    RW_CARD_STATUS_FLAG_ERROR(159, "Card status flag error"),
    RW_USB_COMMUNICATION_ERROR(176, "USB communication error"),
    RW_READER_WRITER_DISCONNECTED(177, "Reader/Writer disconnected"),
    RW_LOCK_TIMEOUT(187, "Lock timeout"),
    RW_READER_WRITER_VERSION_UNSUPPORTED(189, "Reader/Writer version unsupported"),
    RW_LIBRARY_IS_INVALID(190, "Library file is invalid");

    private final int code;
    private final String description;

    RwErrorType(int code, String description) {
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
     * @return the matching error type, or RW_UNKNOWN_ERROR if not found
     */
    public static RwErrorType fromCode(int code) {
        for (RwErrorType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return RW_UNKNOWN_ERROR;
    }
}
