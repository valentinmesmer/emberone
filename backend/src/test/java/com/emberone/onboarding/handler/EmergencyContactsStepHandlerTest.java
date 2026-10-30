/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.emergencycontact.dto.CreateEmergencyContactRequest;
import com.emberone.emergencycontact.repository.UserEmergencyContactRepository;
import com.emberone.emergencycontact.service.EmergencyContactService;
import com.emberone.onboarding.config.EmergencyContactsStepConfig;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.spi.OnboardingPromotionContext;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.platform.exception.ConflictException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmergencyContactsStepHandlerTest {

    @Mock UserEmergencyContactRepository emergencyContactRepository;
    @Mock EmergencyContactService emergencyContactService;

    @InjectMocks EmergencyContactsStepHandler handler;

    @Test
    void validateConfigRejectsZeroMinimum() {
        assertThatThrownBy(() -> handler.validateConfig(1L, new EmergencyContactsStepConfig(0)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void isFulfilledWhenCountMeetsMinimum() {
        when(emergencyContactRepository.countByUser_Id(42L)).thenReturn(2);

        boolean fulfilled =
                handler.isFulfilled(
                        new OnboardingStepFulfillmentContext(1L, 42L, 99L, 0L),
                        new EmergencyContactsStepConfig(2));

        assertThat(fulfilled).isTrue();
    }

    @Test
    void isNotFulfilledWhenCountBelowMinimum() {
        when(emergencyContactRepository.countByUser_Id(42L)).thenReturn(1);

        boolean fulfilled =
                handler.isFulfilled(
                        new OnboardingStepFulfillmentContext(1L, 42L, 99L, 0L),
                        new EmergencyContactsStepConfig(2));

        assertThat(fulfilled).isFalse();
    }

    @Test
    void isOnboardingFulfilledWhenDraftCountMeetsMinimum() {
        boolean fulfilled =
                handler.isOnboardingFulfilled(
                        new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L),
                        new EmergencyContactsStepConfig(1),
                        Map.of(
                                "contacts",
                                List.of(
                                        Map.of(
                                                "relationship",
                                                "PARENT",
                                                "firstName",
                                                "Ada",
                                                "lastName",
                                                "Lovelace",
                                                "street",
                                                "A",
                                                "city",
                                                "B",
                                                "postalCode",
                                                "1",
                                                "country",
                                                "DE",
                                                "phone",
                                                "+49111",
                                                "email",
                                                "ada@example.com"))));

        assertThat(fulfilled).isTrue();
    }

    @Test
    void isOnboardingNotFulfilledWhenDraftEmpty() {
        boolean fulfilled =
                handler.isOnboardingFulfilled(
                        new OnboardingStepFulfillmentContext(1L, 42L, 99L, 7L),
                        new EmergencyContactsStepConfig(1),
                        Map.of());

        assertThat(fulfilled).isFalse();
    }

    @Test
    void promoteCreatesContactsFromDraftWhenNotLiveFulfilled() {
        when(emergencyContactRepository.countByUser_Id(42L)).thenReturn(0);

        handler.promote(
                promotionContext(),
                new EmergencyContactsStepConfig(1),
                Map.of(
                        "contacts",
                        List.of(
                                Map.of(
                                        "relationship",
                                        "PARENT",
                                        "firstName",
                                        "Ada",
                                        "lastName",
                                        "Lovelace",
                                        "street",
                                        "A",
                                        "city",
                                        "B",
                                        "postalCode",
                                        "1",
                                        "country",
                                        "DE",
                                        "phone",
                                        "+49111",
                                        "email",
                                        "ada@example.com"))));

        verify(emergencyContactService)
                .createFromOnboarding(eq(1L), eq(42L), any(CreateEmergencyContactRequest.class));
    }

    @Test
    void promoteSkipsWhenAlreadyLiveFulfilled() {
        when(emergencyContactRepository.countByUser_Id(42L)).thenReturn(2);

        handler.promote(
                promotionContext(),
                new EmergencyContactsStepConfig(2),
                Map.of(
                        "contacts",
                        List.of(
                                Map.of(
                                        "relationship",
                                        "PARENT",
                                        "firstName",
                                        "Ada",
                                        "lastName",
                                        "Lovelace",
                                        "street",
                                        "A",
                                        "city",
                                        "B",
                                        "postalCode",
                                        "1",
                                        "country",
                                        "DE",
                                        "phone",
                                        "+49111",
                                        "email",
                                        "ada@example.com"))));

        verify(emergencyContactService, never())
                .createFromOnboarding(
                        anyLong(), anyLong(), any(CreateEmergencyContactRequest.class));
    }

    private static OnboardingPromotionContext promotionContext() {
        OnboardingInvitation invitation =
                OnboardingInvitation.builder()
                        .id(7L)
                        .tenantId(1L)
                        .userId(42L)
                        .targetOrganizationId(99L)
                        .build();
        return new OnboardingPromotionContext(invitation, 1L, 42L, 99L);
    }
}
