/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.exceptionhandler;

import com.emberone.platform.exception.AccessDeniedException;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.exceptionhandler.AccessDeniedLogSupport;
import com.emberone.workflow.exception.WorkflowValidationException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(0)
@RestControllerAdvice(basePackages = "com.emberone.workflow")
public class WorkflowGlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Workflow resource not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(ConflictException ex) {
        log.warn("Workflow conflict: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(WorkflowValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(WorkflowValidationException ex) {
        log.warn("Workflow validation error: {}", ex.getMessage());
        return validationError(ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        AccessDeniedLogSupport.logAccessDenied(log, ex.getMessage());
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<Map<String, String>> validationError(String message, String code) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        if (code != null && !code.isBlank()) {
            body.put("code", code);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
