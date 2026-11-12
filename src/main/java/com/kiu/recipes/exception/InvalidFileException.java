package com.kiu.recipes.exception;

/** The uploaded file is missing, empty or not an accepted type. */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
