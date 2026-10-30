/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboardingdocument.service.UserEntryDocumentFulfillmentService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingEntryDocumentReuseServiceTest {

    @Mock UserEntryDocumentFulfillmentService fulfillmentService;

    @InjectMocks OnboardingEntryDocumentReuseService service;

    private static final OnboardingStepFulfillmentContext CONTEXT =
            new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L);

    @Test
    void findReusableTenantConfigKeysDelegatesToUserFulfillmentTable() {
        when(fulfillmentService.findFulfilledTenantConfigKeys(42L, List.of("DOC")))
                .thenReturn(Set.of("DOC"));

        Set<String> reusable = service.findReusableTenantConfigKeys(CONTEXT, List.of("DOC"));

        assertThat(reusable).containsExactly("DOC");
    }

    @Test
    void areAllTenantReusableDelegatesToUserFulfillmentTable() {
        when(fulfillmentService.areAllTenantFulfilled(42L, List.of("DOC", "DSGVO")))
                .thenReturn(true);

        assertThat(service.areAllTenantReusable(CONTEXT, List.of("DOC", "DSGVO"))).isTrue();
    }
}
