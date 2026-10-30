/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.testsupport;

public final class ExceptionOriginTestSupport {

    private ExceptionOriginTestSupport() {}

    public static IllegalArgumentException applicationIllegalArgument(String message) {
        IllegalArgumentException ex = new IllegalArgumentException(message);
        ex.setStackTrace(
                new StackTraceElement[] {
                    new StackTraceElement(
                            "com.emberone.user.service.UserService",
                            "validate",
                            "UserService.java",
                            42)
                });
        return ex;
    }

    public static IllegalArgumentException frameworkIllegalArgument(String message) {
        IllegalArgumentException ex = new IllegalArgumentException(message);
        ex.setStackTrace(
                new StackTraceElement[] {
                    new StackTraceElement(
                            "org.springframework.cache.interceptor.CacheAspectSupport",
                            "execute",
                            "CacheAspectSupport.java",
                            100)
                });
        return ex;
    }
}
