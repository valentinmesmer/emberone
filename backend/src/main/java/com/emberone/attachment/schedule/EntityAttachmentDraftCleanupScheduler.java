/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.attachment.schedule;

import com.emberone.attachment.service.EntityAttachmentDraftCleanupService;
import com.emberone.platform.tenant.TenantBatchRunner;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EntityAttachmentDraftCleanupScheduler {

    private final TenantBatchRunner tenantBatchRunner;
    private final EntityAttachmentDraftCleanupService draftCleanupService;

    @Scheduled(cron = "${emberone.attachments.draft-cleanup-cron}", zone = "UTC")
    @SchedulerLock(name = "attachmentDraftCleanup")
    public void purgeStaleDraftAttachments() {
        tenantBatchRunner.forEachTenant(draftCleanupService::purgeStaleDrafts);
    }
}
