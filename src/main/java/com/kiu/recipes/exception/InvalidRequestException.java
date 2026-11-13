package com.kiu.recipes.exception;

/** The request is syntactically fine but semantically wrong (e.g. unsupported sort field). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
