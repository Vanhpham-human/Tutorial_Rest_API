package com.fptaptech.catalog.common;

import com.fptaptech.catalog.product.ProductNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(CatalogException.class)
    ResponseEntity<ProblemDetail> handleCatalog(CatalogException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemResponses.create(
                ex.getStatus(), ex.getCode(), ex.getTitle(), ex.getMessage(),
                URI.create(request.getRequestURI()), ex.getErrors(), Instant.now(clock));
        return ResponseEntity.status(ex.getStatus())
                .header(HttpHeaders.CONTENT_TYPE, "application/problem+json")
                .body(problem);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<ProblemDetail> handleProductNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemResponses.create(
                HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found", ex.getMessage(),
                URI.create(request.getRequestURI()), List.of(), Instant.now(clock));
        problem.setProperty("productId", ex.getId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header(HttpHeaders.CONTENT_TYPE, "application/problem+json")
                .body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemResponses.create(
                HttpStatus.CONTENT_TOO_LARGE, "INVOICE_TOO_LARGE", "Invoice too large",
                "Hóa đơn vượt quá 5MB", instance(request), List.of(), Instant.now(clock));
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ex.getBody();
        problem.setType(URI.create(ProblemResponses.ERROR_BASE + "validation"));
        problem.setTitle("Validation failed");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("timestamp", Instant.now(clock));
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiFieldError(fieldError.getField(), String.valueOf(fieldError.getDefaultMessage())))
                .toList());
        if (problem.getInstance() == null) {
            problem.setInstance(instance(request));
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            if (problem.getInstance() == null) {
                problem.setInstance(instance(request));
            }
            if (problem.getProperties() == null || !problem.getProperties().containsKey("code")) {
                problem.setProperty("code", codeFor(status));
            }
            if (problem.getProperties() == null || !problem.getProperties().containsKey("timestamp")) {
                problem.setProperty("timestamp", Instant.now(clock));
            }
            HttpHeaders writable = new HttpHeaders();
            if (headers != null) {
                writable.addAll(headers);
            }
            writable.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
            headers = writable;
        }
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    private static URI instance(WebRequest request) {
        if (request instanceof ServletWebRequest servlet) {
            return URI.create(servlet.getRequest().getRequestURI());
        }
        return null;
    }

    private static String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "VALIDATION_FAILED";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 409 -> "CONFLICT";
            case 413 -> "INVOICE_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> "HTTP_" + status.value();
        };
    }
}
