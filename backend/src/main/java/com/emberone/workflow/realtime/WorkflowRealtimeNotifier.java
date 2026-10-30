/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.realtime;

import com.emberone.platform.realtime.event.RealtimeEvent;
import com.emberone.platform.realtime.publish.RealtimePublisher;
import com.emberone.workflow.domain.Workflow;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
public class WorkflowRealtimeNotifier {

    public static final String EVENT_UPDATED = "workflow.updated";
    public static final String EVENT_COMMENT_CREATED = "workflow.comment.created";

    private final ObjectProvider<RealtimePublisher> realtimePublisherProvider;
    private final WorkflowRecipientResolver recipientResolver;

    public void notifyUpdated(Workflow workflow) {
        publish(workflow, EVENT_UPDATED);
    }

    public void notifyCommentCreated(Workflow workflow) {
        publish(workflow, EVENT_COMMENT_CREATED);
    }

    private void publish(Workflow workflow, String eventType) {
        RealtimePublisher publisher = realtimePublisherProvider.getIfAvailable();
        if (publisher == null || workflow == null || workflow.getId() == null) {
            return;
        }
        Map<String, Long> entityIds = buildEntityIds(workflow);
        RealtimeEvent event = RealtimeEvent.thin(eventType, entityIds);
        publisher.publishToTopic(
                workflow.getTenantId(),
                WorkflowTopicAuthorizer.DOMAIN + "." + workflow.getId(),
                event);
        notifyRecipients(publisher, workflow, event);
    }

    private Map<String, Long> buildEntityIds(Workflow workflow) {
        Map<String, Long> entityIds = new LinkedHashMap<>();
        entityIds.put("workflowId", workflow.getId());
        Long organizationId = recipientResolver.resolveOrganizationId(workflow);
        if (organizationId != null) {
            entityIds.put("organizationId", organizationId);
        }
        return entityIds;
    }

    private void notifyRecipients(
            RealtimePublisher publisher, Workflow workflow, RealtimeEvent event) {
        Set<Long> recipients = recipientResolver.resolveRecipientUserIds(workflow);
        if (CollectionUtils.isEmpty(recipients)) {
            return;
        }
        for (Long userId : recipients) {
            publisher.publishToUser(
                    workflow.getTenantId(), userId, RealtimePublisher.WORKFLOWS_QUEUE, event);
        }
    }
}
