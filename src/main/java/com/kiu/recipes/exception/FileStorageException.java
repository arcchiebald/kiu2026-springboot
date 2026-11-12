package com.kiu.recipes.exception;

/** The file was valid but could not be written to / read from disk. */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
