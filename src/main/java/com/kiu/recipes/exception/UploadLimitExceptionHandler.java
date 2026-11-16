package com.kiu.recipes.exception;

import com.kiu.recipes.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;

/**
 * "File too large" is raised while the multipart request is parsed, before Spring has picked a controller,
 * so it can only be caught by an advice that is not limited to particular controllers. It answers with the
 * JSON error body for {@code /api/**} and with an HTML error page for the browser UI.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class UploadLimitExceptionHandler {

    private static final String MESSAGE = "Uploaded file is too large";

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.PAYLOAD_TOO_LARGE;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/api/")) {
            return ResponseEntity.status(status).body(new ErrorResponse(
                    LocalDateTime.now(), status.value(), status.getReasonPhrase(), MESSAGE, null));
        }
        return WebExceptionHandler.page(status, MESSAGE + " (the limit is 5 MB). Go back and choose a smaller photo.");
    }
}
