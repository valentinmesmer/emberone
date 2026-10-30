/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.emberone.notification.channel.EmailChannelSender;
import com.emberone.notification.channel.RecipientNotReachableException;
import com.emberone.notification.config.NotificationProperties;
import com.emberone.notification.domain.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxServiceTest {

    @Mock NotificationOutboxTransactionService transactionService;
    @Mock EmailChannelSender emailChannelSender;

    NotificationOutboxService outboxService;
    NotificationProperties properties;

    private static NotificationDelivery claimedEmailDelivery() {
        Notification notification =
                Notification.builder()
                        .id(10L)
                        .tenantId(7L)
                        .recipientUserId(42L)
                        .type(NotificationType.WORKFLOW_APPROVED)
                        .build();
        return NotificationDelivery.builder()
                .id(20L)
                .tenantId(7L)
                .notification(notification)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationDeliveryStatus.IN_PROGRESS)
                .attempts(0)
                .build();
    }

    @BeforeEach
    void setUp() {
        properties = new NotificationProperties();
        properties.setMaxDeliveryAttempts(3);
        properties.setOutboxBatchSize(50);
        outboxService =
                new NotificationOutboxService(
                        transactionService, List.of(emailChannelSender), properties);
        lenient().when(emailChannelSender.supports(NotificationChannel.EMAIL)).thenReturn(true);
    }

    @Test
    void processPendingDeliveryMarksSentOnSuccess() throws Exception {
        NotificationDelivery delivery = claimedEmailDelivery();
        when(transactionService.claimPendingDeliveryIds(any(), any(), anyInt()))
                .thenReturn(List.of(20L));
        when(transactionService.loadClaimedDeliveries(any())).thenReturn(List.of(delivery));

        outboxService.processPendingDeliveries();

        verify(emailChannelSender).send(delivery.getNotification(), delivery);
        verify(transactionService).markSent(20L, 1);
    }

    @Test
    void processPendingDeliveryRetriesUntilMaxAttempts() throws Exception {
        NotificationDelivery delivery = claimedEmailDelivery();
        delivery.setAttempts(2);
        when(transactionService.claimPendingDeliveryIds(any(), any(), anyInt()))
                .thenReturn(List.of(20L));
        when(transactionService.loadClaimedDeliveries(any())).thenReturn(List.of(delivery));
        doThrow(new RuntimeException("smtp down")).when(emailChannelSender).send(any(), any());

        outboxService.processPendingDeliveries();

        verify(transactionService).markFailed(20L, 3, "smtp down", false);
    }

    @Test
    void missingEmailAddressMarksFailedWithoutThrowing() throws Exception {
        NotificationDelivery delivery = claimedEmailDelivery();
        when(transactionService.claimPendingDeliveryIds(any(), any(), anyInt()))
                .thenReturn(List.of(20L));
        when(transactionService.loadClaimedDeliveries(any())).thenReturn(List.of(delivery));
        doThrow(new RecipientNotReachableException("No email address on file"))
                .when(emailChannelSender)
                .send(any(), any());

        outboxService.processPendingDeliveries();

        verify(transactionService).markFailed(20L, 1, "No email address on file", false);
    }

    @Test
    void keepsPendingWhenRetryableFailureBelowLimit() throws Exception {
        NotificationDelivery delivery = claimedEmailDelivery();
        when(transactionService.claimPendingDeliveryIds(any(), any(), anyInt()))
                .thenReturn(List.of(20L));
        when(transactionService.loadClaimedDeliveries(any())).thenReturn(List.of(delivery));
        doThrow(new RuntimeException("temporary")).when(emailChannelSender).send(any(), any());

        outboxService.processPendingDeliveries();

        verify(transactionService).markFailed(20L, 1, "temporary", true);
    }

    @Test
    void processesAtMostOneClaimBatchPerInvocation() {
        when(transactionService.claimPendingDeliveryIds(any(), any(), anyInt()))
                .thenReturn(List.of(20L));
        when(transactionService.loadClaimedDeliveries(any())).thenReturn(List.of());

        outboxService.processPendingDeliveries();

        verify(transactionService, times(1)).claimPendingDeliveryIds(any(), any(), anyInt());
    }

    @Test
    void claimUsesStaleCutoffFromProperties() {
        when(transactionService.claimPendingDeliveryIds(any(), any(), anyInt()))
                .thenReturn(List.of());

        outboxService.processPendingDeliveries();

        ArgumentCaptor<Instant> staleCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(transactionService)
                .claimPendingDeliveryIds(
                        any(Instant.class),
                        staleCaptor.capture(),
                        eq(properties.getOutboxBatchSize()));
        Instant expectedStale = Instant.now().minus(properties.getProcessingStaleAfter());
        assertThat(staleCaptor.getValue())
                .isBetween(expectedStale.minusSeconds(2), expectedStale.plusSeconds(2));
    }
}
