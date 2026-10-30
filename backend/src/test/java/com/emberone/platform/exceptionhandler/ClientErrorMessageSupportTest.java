/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.exceptionhandler;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.testsupport.ExceptionOriginTestSupport;
import org.junit.jupiter.api.Test;

class ClientErrorMessageSupportTest {

    @Test
    void exposesApplicationIllegalArgumentMessages() {
        assertThat(
                        ClientErrorMessageSupport.shouldExposeClientMessage(
                                ExceptionOriginTestSupport.applicationIllegalArgument(
                                        "File is empty")))
                .isTrue();
    }

    @Test
    void hidesFrameworkIllegalArgumentMessages() {
        assertThat(
                        ClientErrorMessageSupport.shouldExposeClientMessage(
                                ExceptionOriginTestSupport.frameworkIllegalArgument(
                                        "Cache 'effectiveStandardByOrgType' does not allow 'null' values")))
                .isFalse();
    }

    @Test
    void neverExposesIllegalStateExceptionMessages() {
        assertThat(
                        ClientErrorMessageSupport.shouldExposeClientMessage(
                                new IllegalStateException("No tenantId found in token")))
                .isFalse();
    }
}
