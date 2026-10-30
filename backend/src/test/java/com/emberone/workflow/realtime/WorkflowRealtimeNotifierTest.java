/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.realtime.event.RealtimeEvent;
import com.emberone.platform.realtime.publish.RealtimePublisher;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class WorkflowRealtimeNotifierTest {

    @Mock ObjectProvider<RealtimePublisher> realtimePublisherProvider;
    @Mock RealtimePublisher publisher;
    @Mock WorkflowRecipientResolver recipientResolver;

    @InjectMocks WorkflowRealtimeNotifier notifier;

    private static Workflow workflow(Long id, Long tenantId, Long openerId) {
        return Workflow.builder()
                .id(id)
                .tenantId(tenantId)
                .workflowTypeId(1L)
                .status(WorkflowStatus.OPEN)
                .openedByUserId(openerId)
                .build();
    }

    @Test
    void notifyUpdatedPublishesTopicAndUserQueues() {
        when(realtimePublisherProvider.getIfAvailable()).thenReturn(publisher);
        Workflow workflow = workflow(1L, 7L, 42L);
        when(recipientResolver.resolveOrganizationId(workflow)).thenReturn(20L);
        when(recipientResolver.resolveRecipientUserIds(workflow)).thenReturn(Set.of(42L, 99L));

        notifier.notifyUpdated(workflow);

        ArgumentCaptor<RealtimeEvent> eventCaptor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(publisher).publishToTopic(eq(7L), eq("workflow.1"), eventCaptor.capture());
        verify(publisher)
                .publishToUser(eq(7L), eq(42L), eq(RealtimePublisher.WORKFLOWS_QUEUE), any());
        verify(publisher)
                .publishToUser(eq(7L), eq(99L), eq(RealtimePublisher.WORKFLOWS_QUEUE), any());

        RealtimeEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(WorkflowRealtimeNotifier.EVENT_UPDATED);
        assertThat(event.entityIds()).containsEntry("workflowId", 1L);
        assertThat(event.entityIds()).containsEntry("organizationId", 20L);
    }
}
