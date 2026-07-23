package com.pkcorporate.exception;

/**
 * Dedicated exception for Cloudinary upload/delete failures.
 * Kept as a BusinessException subtype so controllers return a clean API error.
 */
public class CloudinaryException extends BusinessException {

    public CloudinaryException(String message) {
        super(message);
    }

    public CloudinaryException(String message, Throwable cause) {
        super(message + ": " + (cause == null ? "" : cause.getMessage()));
    }
}

