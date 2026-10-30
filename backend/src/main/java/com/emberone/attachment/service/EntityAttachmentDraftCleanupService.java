/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.attachment.service;

import com.emberone.attachment.config.AttachmentProperties;
import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityAttachmentDraftCleanupService {

    private static final List<EntityAttachmentType> DRAFT_TYPES =
            List.of(
                    EntityAttachmentType.WORKFLOW_DRAFT,
                    EntityAttachmentType.WORKFLOW_COMMENT_DRAFT);

    private final EntityAttachmentRepository entityAttachmentRepository;
    private final EntityAttachmentService entityAttachmentService;
    private final AttachmentProperties properties;

    @Value("${object-storage.workflows-bucket}")
    private String workflowsBucket;

    @Transactional
    public int purgeStaleDrafts(Long tenantId) {
        Instant cutoff = Instant.now().minus(Duration.ofHours(properties.getDraftRetentionHours()));
        List<EntityAttachment> stale =
                entityAttachmentRepository.findByEntityTypeInAndUploadedAtBefore(
                        DRAFT_TYPES, cutoff);
        for (EntityAttachment attachment : stale) {
            entityAttachmentService.delete(workflowsBucket, attachment);
        }
        if (!stale.isEmpty()) {
            log.info("Purged {} stale workflow draft attachment(s)", stale.size());
        }
        return stale.size();
    }
}
