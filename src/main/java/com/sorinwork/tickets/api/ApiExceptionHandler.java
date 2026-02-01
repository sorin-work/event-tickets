package com.sorinwork.tickets.api;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** JSON error bodies for the API, e.g. {@code {"error": "Event not found"}}. */
@RestControllerAdvice(basePackages = "com.sorinwork.tickets.api")
public class ApiExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<Map<String, String>> notFound(NotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not found"));
  }

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<Map<String, String>> business(BusinessException e) {
    return ResponseEntity.unprocessableEntity().body(Map.of("error", e.getMessage()));
  }
}
