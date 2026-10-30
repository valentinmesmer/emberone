/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.management.domain.ManagementActivation;
import com.emberone.management.realtime.ManagementActivationRealtimeNotifier;
import com.emberone.management.repository.ManagementActivationRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagementActivationExpiryServiceTest {

    @Mock private ManagementActivationRepository activationRepository;
    @Mock private ManagementActivationRealtimeNotifier notifier;
    @InjectMocks private ManagementActivationExpiryService service;

    @Test
    void notifiesEachClaimedActivation() {
        ManagementActivation expired = ManagementActivation.builder().id(7L).userId(3L).build();
        when(activationRepository.claimExpiredForNotification(any(Instant.class)))
                .thenReturn(List.of(7L));
        when(activationRepository.findAllById(List.of(7L))).thenReturn(List.of(expired));

        service.notifyExpiredActivations();

        verify(notifier).notifyExpired(expired);
    }

    @Test
    void doesNothingWithoutExpiredActivations() {
        when(activationRepository.claimExpiredForNotification(any(Instant.class)))
                .thenReturn(List.of());

        service.notifyExpiredActivations();

        verify(notifier, never()).notifyExpired(any());
        verify(activationRepository, never()).findAllById(any());
    }
}
