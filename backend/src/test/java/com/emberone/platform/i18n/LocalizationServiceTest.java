/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

@ExtendWith(MockitoExtension.class)
class LocalizationServiceTest {

    @Mock MessageSource messageSource;
    @InjectMocks LocalizationService service;

    @Test
    void getMessageDelegatesUsingSupportedLocale() {
        when(messageSource.getMessage("key", null, Locale.GERMAN)).thenReturn("Wert");

        assertThat(service.getMessage("key", SupportedLocale.DE)).isEqualTo("Wert");
    }

    @Test
    void optionalMessageReturnsNullForMissingOrBlankValues() {
        when(messageSource.getMessage("missing", null, Locale.GERMAN))
                .thenThrow(new NoSuchMessageException("missing"));
        when(messageSource.getMessage("blank", null, Locale.GERMAN)).thenReturn("  ");

        assertThat(service.getMessageOrNull("missing", SupportedLocale.DE)).isNull();
        assertThat(service.getMessageOrNull("blank", SupportedLocale.DE)).isNull();
    }

    @Test
    void requiredMessagePropagatesMissingMessage() {
        when(messageSource.getMessage("missing", null, Locale.GERMAN))
                .thenThrow(new NoSuchMessageException("missing"));

        assertThatThrownBy(() -> service.getMessage("missing", SupportedLocale.DE))
                .isInstanceOf(NoSuchMessageException.class);
    }
}
