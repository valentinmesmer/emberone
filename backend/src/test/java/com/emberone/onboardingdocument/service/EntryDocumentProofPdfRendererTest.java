/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentChannel;
import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentOutcome;
import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.onboardingdocument.domain.UserEntryDocumentFulfillment;
import com.emberone.onboardingdocument.domain.UserEntryDocumentFulfillmentEvent;
import com.emberone.platform.i18n.LocalizationConfiguration;
import com.emberone.platform.i18n.LocalizationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EntryDocumentProofPdfRendererTest {

    private final EntryDocumentProofPdfRenderer renderer =
            new EntryDocumentProofPdfRenderer(
                    new LocalizationService(new LocalizationConfiguration().messageSource()));

    @Test
    void renderProducesPdfBytes() {
        UserEntryDocumentFulfillment fulfillment =
                UserEntryDocumentFulfillment.builder()
                        .configKey("DSGVO")
                        .templateVersion(2)
                        .templateSha256Hex("a".repeat(64))
                        .outcome(EntryDocumentFulfillmentOutcome.ACCEPTED)
                        .mode(OnboardingDocumentFulfillmentMode.CONFIRMED)
                        .confirmedAt(Instant.parse("2024-06-01T10:00:00Z"))
                        .build();
        UserEntryDocumentFulfillmentEvent event =
                UserEntryDocumentFulfillmentEvent.builder()
                        .configKey("DSGVO")
                        .templateVersion(2)
                        .templateSha256Hex("a".repeat(64))
                        .confirmationText("Ich habe die Datenschutzhinweise gelesen.")
                        .outcome(EntryDocumentFulfillmentOutcome.ACCEPTED)
                        .channel(EntryDocumentFulfillmentChannel.SELF)
                        .confirmedAt(Instant.parse("2024-06-01T10:00:00Z"))
                        .actorUserId(5L)
                        .build();

        byte[] pdf =
                renderer.render(
                        List.of(fulfillment),
                        List.of(event),
                        Map.of(5L, "Max Mustermann"),
                        (configKey, organizationId) -> "Datenschutz",
                        organizationId -> "Organisation");

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
