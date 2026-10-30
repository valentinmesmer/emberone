/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.workflow.domain.WorkflowType;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

class LocalizationConfigurationTest {

    @Test
    void preservesLeadingSpaceInApproverSuffix() {
        MessageSource messageSource = new LocalizationConfiguration().messageSource();

        assertThat(messageSource.getMessage("notification.approverSuffix", null, Locale.GERMAN))
                .isEqualTo(" von {names}.");
    }

    @Test
    void everyWorkflowTypeHasGermanLabel() {
        MessageSource messageSource = new LocalizationConfiguration().messageSource();
        LocalizationService localizationService = new LocalizationService(messageSource);

        for (WorkflowType type : WorkflowType.values()) {
            assertThat(
                            localizationService.getMessageOrNull(
                                    "workflow.type." + type.name(), SupportedLocale.DE))
                    .as("workflow.type.%s", type.name())
                    .isNotBlank();
        }
    }

    @Test
    void organizationJoinRequestDocumentPromotionInterpolatesName() {
        MessageSource messageSource = new LocalizationConfiguration().messageSource();
        LocalizationService localizationService = new LocalizationService(messageSource);

        assertThat(
                        localizationService.getInterpolatedMessage(
                                "workflow.documentPromotion.organizationJoinRequest.title",
                                Map.of("name", "FF Musterstadt"),
                                SupportedLocale.DE))
                .isEqualTo("Eintritt FF Musterstadt");
        assertThat(
                        localizationService.getMessageOrNull(
                                "workflow.documentPromotion.mainOrganizationChange.title",
                                SupportedLocale.DE))
                .isEqualTo("Hauptorganisation {name}");
    }
}
