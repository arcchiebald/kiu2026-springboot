package com.kiu.recipes.exception;

/** Deleting the resource would orphan recipes that still reference it. */
public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String message) {
        super(message);
    }
}
