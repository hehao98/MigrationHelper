package edu.pku.migrationhelper.controller;

import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class RestResponseEntityExceptionHandler
        extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = { IllegalArgumentException.class })
    protected ResponseEntity<Object> handleIllegalArgument(
            RuntimeException ex, ServletWebRequest request) {
        Map<String, Object> response = getErrorResponse(
                HttpStatus.BAD_REQUEST, ex.getMessage(),
                request.getRequest().getRequestURI(),
                request.getRequest().getRequestURL().toString() + "?" + request.getRequest().getQueryString()
        );
        return handleExceptionInternal(ex, response,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { ResourceNotFoundException.class })
    protected ResponseEntity<Object> handleResourceNotFound(
            RuntimeException ex, ServletWebRequest request
    ) {
        Map<String, Object> response = getErrorResponse(
                HttpStatus.NOT_FOUND, ex.getMessage(),
                request.getRequest().getRequestURI(),
                request.getRequest().getRequestURL().toString() + "?" + request.getRequest().getQueryString()
        );
        return handleExceptionInternal(ex, response,
                new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    private Map<String, Object> getErrorResponse(HttpStatus status, String message, String path, String url) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", new Date());
        response.put("status", status.value());
        response.put("error", status.value() + " " + status.getReasonPhrase());
        response.put("message", message);
        response.put("path", path);
        response.put("url", url);
        return response;
    }
}
