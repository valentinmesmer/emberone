/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.exceptionhandler;

public final class ClientErrorMessageSupport {

    public static final String GENERIC_UNEXPECTED = "An unexpected error occurred";
    public static final String GENERIC_IDENTITY_UNAVAILABLE =
            "The identity service is temporarily unavailable.";

    private ClientErrorMessageSupport() {}

    public static boolean shouldExposeClientMessage(Throwable ex) {
        if (ex instanceof IllegalStateException) {
            return false;
        }
        StackTraceElement[] stack = ex.getStackTrace();
        if (stack.length == 0) {
            return false;
        }
        String origin = stack[0].getClassName();
        if (!origin.startsWith("com.emberone.")) {
            return false;
        }
        if (origin.contains(".cache.")) {
            return false;
        }
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        return !isInfrastructureMessage(message);
    }

    private static boolean isInfrastructureMessage(String message) {
        return message.contains("Cache '")
                || message.contains("RedisCache")
                || message.contains("does not allow 'null' values");
    }
}
