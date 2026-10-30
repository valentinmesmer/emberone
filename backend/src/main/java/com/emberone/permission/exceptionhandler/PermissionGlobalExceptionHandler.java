/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permission.exceptionhandler;

import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.exceptionhandler.ClientErrorMessageSupport;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(
        basePackages = {
            "com.emberone.permission",
            "com.emberone.permissiongroup",
            "com.emberone.permissiongroupmapping",
            "com.emberone.userpermission"
        })
public class PermissionGlobalExceptionHandler {

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePermissionGroupNotFound(
            ResourceNotFoundException ex) {
        log.warn("Permission group not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(ConflictException ex) {
        log.warn("Permission conflict: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        if (ClientErrorMessageSupport.shouldExposeClientMessage(ex)) {
            log.warn("Illegal argument: {}", ex.getMessage());
            return error(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        log.error("Illegal argument (not exposed to client)", ex);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR, ClientErrorMessageSupport.GENERIC_UNEXPECTED);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(
            IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR, ClientErrorMessageSupport.GENERIC_UNEXPECTED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getAllErrors()
                .forEach(
                        (error) -> {
                            String fieldName = ((FieldError) error).getField();
                            String errorMessage = error.getDefaultMessage();
                            errors.put(fieldName, errorMessage);
                        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}
