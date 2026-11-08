/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.platform.ai.extraction.DocumentExtractionSchema;
import com.emberone.platform.ai.extraction.DocumentExtractionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiCapabilitiesServiceTest {

    @Mock DocumentExtractionService extractionService;
    @Mock DocumentExtractionSchema certSchema;

    AiProperties properties = new AiProperties();
    AiCapabilitiesService service;

    @BeforeEach
    void setUp() {
        service =
                new AiCapabilitiesService(
                        properties, new AiAvailabilityGuard(properties), extractionService);
    }

    @Test
    void nothingIsAvailableByDefault() {
        AiCapabilitiesDto dto = service.capabilitiesForCurrentUser();

        assertThat(dto.enabled()).isFalse();
        assertThat(dto.helpChat().available()).isFalse();
        assertThat(dto.documentExtraction().available()).isFalse();
        assertThat(dto.documentExtraction().types()).isEmpty();
    }

    @Test
    void offersExtractionPerProofType() {
        properties.setEnabled(true);
        when(extractionService.schemas()).thenReturn(List.of(certSchema));
        when(certSchema.type()).thenReturn("CERTIFICATION_RECORD");

        AiCapabilitiesDto dto = service.capabilitiesForCurrentUser();

        assertThat(dto.documentExtraction().available()).isTrue();
        assertThat(dto.documentExtraction().types().get("CERTIFICATION_RECORD").available())
                .isTrue();
    }
}
