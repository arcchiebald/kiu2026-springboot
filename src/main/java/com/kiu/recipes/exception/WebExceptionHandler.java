package com.kiu.recipes.exception;

import com.kiu.recipes.controller.web.RecipeWebController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Error pages for the server-rendered UI (controllers in {@code controller.web}). Form problems are handled
 * inside the controllers; this only catches what escapes them (a missing record, a bad URL parameter).
 * The REST API keeps its JSON errors in {@link GlobalExceptionHandler}.
 */
@ControllerAdvice(basePackageClasses = RecipeWebController.class)
public class WebExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException ex) {
        return page(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({InvalidRequestException.class, MethodArgumentTypeMismatchException.class})
    public ModelAndView handleBadRequest(Exception ex) {
        String message = ex instanceof MethodArgumentTypeMismatchException m
                ? "Invalid value for '" + m.getName() + "'"
                : ex.getMessage();
        return page(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({DuplicateResourceException.class, ResourceInUseException.class})
    public ModelAndView handleConflict(RuntimeException ex) {
        return page(HttpStatus.CONFLICT, ex.getMessage());
    }

    static ModelAndView page(HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        return mav;
    }
}
