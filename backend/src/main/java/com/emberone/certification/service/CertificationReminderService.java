/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import com.emberone.certification.domain.CertificationStatus;
import com.emberone.certification.dto.CertificationStatusItemDto;
import com.emberone.certification.dto.UserCertificationStatusDto;
import com.emberone.certification.event.CertificationAssignmentChangedEvent;
import com.emberone.certification.event.TenantCertDefinitionArchivedEvent;
import com.emberone.certification.repository.UserCertificationAssignmentRepository;
import com.emberone.platform.event.AfterCommitEventListener;
import com.emberone.platform.navigation.domain.AppLink;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.todo.domain.TodoParamKeys;
import com.emberone.todo.domain.TodoSourceEntityType;
import com.emberone.todo.domain.TodoType;
import com.emberone.todo.service.TodoOpenRequest;
import com.emberone.todo.service.TodoService;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scans users with certification assignments and raises a to-do (deduplicated) for every
 * requirement that is missing, expired, or expiring soon. Intended to be invoked once per tenant by
 * a scheduled batch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificationReminderService {

    private static final int USER_CHUNK_SIZE = 100;

    private final UserCertificationAssignmentRepository assignmentRepository;
    private final CertificationStatusService statusService;
    private final TodoService todoService;
    private final EntityManager entityManager;

    private static boolean needsReminder(
            CertificationStatusItemDto item, CertificationStatus status) {
        if (item.isArchived() || !item.isAssigned()) {
            return false;
        }
        return status == CertificationStatus.MISSING
                || status == CertificationStatus.EXPIRED
                || status == CertificationStatus.EXPIRING_SOON;
    }

    /**
     * Create reminder to-dos for the current tenant context. Deduplicates via the to-do source
     * (CERTIFICATION_RECORD + definition id) and assignee, so re-runs do not pile up duplicates.
     * Covers {@link CertificationStatus#MISSING}, {@link CertificationStatus#EXPIRED}, and {@link
     * CertificationStatus#EXPIRING_SOON}.
     */
    @Transactional
    public int createRemindersForTenant(Long tenantId) {
        int created = 0;
        int pageIndex = 0;
        Page<Long> userIdPage;
        do {
            userIdPage =
                    assignmentRepository.findDistinctUserIds(
                            PageRequest.of(pageIndex, USER_CHUNK_SIZE));
            for (Long userId : userIdPage.getContent()) {
                created += createRemindersForUser(tenantId, userId);
            }
            entityManager.clear();
            pageIndex++;
        } while (userIdPage.hasNext());
        if (created > 0) {
            log.info("Certification reminder sweep touched {} requirement(s)", created);
        }
        return created;
    }

    private int createRemindersForUser(Long tenantId, Long userId) {
        int created = 0;
        UserCertificationStatusDto status = statusService.getStatusForUser(userId);
        for (CertificationStatusItemDto item : status.getItems()) {
            CertificationStatus itemStatus = CertificationStatus.valueOf(item.getStatus());
            if (!needsReminder(item, itemStatus)) {
                continue;
            }
            openReminder(tenantId, userId, item, itemStatus);
            created++;
        }
        return created;
    }

    @AfterCommitEventListener
    public void onCertificationAssignmentChanged(CertificationAssignmentChangedEvent event) {
        syncRemindersForUser(event.tenantId(), event.userId());
    }

    /**
     * Cancels every open reminder tied to an archived certification definition. Sync in the same
     * transaction as archive so todo cleanup rolls back with the archive if anything fails.
     */
    @EventListener
    public void onTenantCertDefinitionArchived(TenantCertDefinitionArchivedEvent event) {
        todoService.cancelBySource(
                TodoSourceEntityType.CERTIFICATION_RECORD, event.tenantCertDefinitionId(), null);
    }

    /**
     * Re-evaluates a single user's certification reminders immediately (e.g. after
     * assignment/record changes). Opens reminders for actionable statuses and cancels obsolete open
     * reminders for the same definition - including orphans whose definition no longer appears in
     * status (assignment removed without historical records).
     */
    @Transactional
    public void syncRemindersForUser(Long tenantId, Long userId) {
        UserCertificationStatusDto status = statusService.getStatusForUser(userId);
        Set<Long> keepOpenDefinitionIds = new HashSet<>();
        for (CertificationStatusItemDto item : status.getItems()) {
            if (item.getTenantCertDefinitionId() == null) {
                continue;
            }
            CertificationStatus itemStatus = CertificationStatus.valueOf(item.getStatus());
            if (needsReminder(item, itemStatus)) {
                openReminder(tenantId, userId, item, itemStatus);
                keepOpenDefinitionIds.add(item.getTenantCertDefinitionId());
            } else {
                cancelReminder(userId, item.getTenantCertDefinitionId());
            }
        }
        todoService.cancelOpenByAssigneeTypeAndSourceExcept(
                userId,
                TodoType.CERTIFICATION_EXPIRING,
                TodoSourceEntityType.CERTIFICATION_RECORD,
                keepOpenDefinitionIds,
                null);
    }

    private void openReminder(
            Long tenantId,
            Long userId,
            CertificationStatusItemDto item,
            CertificationStatus status) {
        Map<String, Object> params = new HashMap<>();
        params.put(TodoParamKeys.PARTICIPANT_ID, userId); // differentiates dedup per user
        params.put(TodoParamKeys.CERT_DEFINITION_ID, item.getTenantCertDefinitionId());
        params.put(TodoParamKeys.CERT_DEFINITION_NAME, item.getCertDefinitionName());
        params.put(TodoParamKeys.STATUS, status.name());
        if (item.getExpiryDate() != null) {
            params.put("expiryDate", item.getExpiryDate().toString());
        }

        AppLink link =
                new AppLink(AppLinkType.CERTIFICATION_RECORD, item.getTenantCertDefinitionId());

        todoService.open(
                TodoOpenRequest.builder()
                        .tenantId(tenantId)
                        .type(TodoType.CERTIFICATION_EXPIRING)
                        .assigneeUserId(userId)
                        .params(params)
                        .link(link)
                        .sourceEntityType(TodoSourceEntityType.CERTIFICATION_RECORD)
                        .sourceEntityId(item.getTenantCertDefinitionId())
                        .build());
    }

    private void cancelReminder(Long userId, Long tenantCertDefinitionId) {
        todoService.cancelOpenBySourceParticipant(
                TodoSourceEntityType.CERTIFICATION_RECORD,
                tenantCertDefinitionId,
                TodoType.CERTIFICATION_EXPIRING,
                userId,
                null);
    }
}
