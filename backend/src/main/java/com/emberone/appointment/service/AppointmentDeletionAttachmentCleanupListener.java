/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import com.emberone.appointment.event.AppointmentDeletingEvent;
import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.service.EntityAttachmentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentDeletionAttachmentCleanupListener {

    private final EntityAttachmentService entityAttachmentService;

    @Value("${object-storage.attachments-bucket}")
    private String attachmentsBucket;

    /**
     * Runs synchronously in the deleting transaction so attachment + object-storage cleanup rolls
     * back together with the appointment delete if anything fails.
     */
    @EventListener
    public void onAppointmentDeleting(AppointmentDeletingEvent event) {
        List<EntityAttachment> attachments =
                entityAttachmentService.findEntitiesByTypeAndId(
                        EntityAttachmentType.APPOINTMENT, event.appointmentId());
        for (EntityAttachment attachment : attachments) {
            entityAttachmentService.delete(attachmentsBucket, attachment);
        }
    }
}
