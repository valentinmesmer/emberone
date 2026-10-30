/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.attachment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.config.AttachmentProperties;
import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EntityAttachmentDraftCleanupServiceTest {

    @Mock EntityAttachmentRepository entityAttachmentRepository;
    @Mock EntityAttachmentService entityAttachmentService;

    private EntityAttachmentDraftCleanupService service;

    @BeforeEach
    void setUp() {
        AttachmentProperties properties = new AttachmentProperties();
        properties.setDraftRetentionHours(12);
        service =
                new EntityAttachmentDraftCleanupService(
                        entityAttachmentRepository, entityAttachmentService, properties);
        ReflectionTestUtils.setField(service, "workflowsBucket", "workflows");
    }

    @Test
    void purgeStaleDraftsDeletesMatchingAttachments() {
        EntityAttachment draft =
                EntityAttachment.builder()
                        .id(1L)
                        .tenantId(10L)
                        .entityType(EntityAttachmentType.WORKFLOW_DRAFT)
                        .storageKey("key")
                        .build();
        when(entityAttachmentRepository.findByEntityTypeInAndUploadedAtBefore(
                        any(), any(Instant.class)))
                .thenReturn(List.of(draft));

        service.purgeStaleDrafts(10L);

        verify(entityAttachmentService).delete("workflows", draft);
    }
}
