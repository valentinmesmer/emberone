/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.appointment.event.AppointmentDeletingEvent;
import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.service.EntityAttachmentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AppointmentDeletionAttachmentCleanupListenerTest {

    @Mock EntityAttachmentService entityAttachmentService;

    private AppointmentDeletionAttachmentCleanupListener listener;

    @BeforeEach
    void setUp() {
        listener = new AppointmentDeletionAttachmentCleanupListener(entityAttachmentService);
        ReflectionTestUtils.setField(listener, "attachmentsBucket", "attachments");
    }

    @Test
    void onAppointmentDeletingDeletesEachAttachment() {
        EntityAttachment first = EntityAttachment.builder().id(1L).build();
        EntityAttachment second = EntityAttachment.builder().id(2L).build();
        when(entityAttachmentService.findEntitiesByTypeAndId(EntityAttachmentType.APPOINTMENT, 99L))
                .thenReturn(List.of(first, second));

        listener.onAppointmentDeleting(new AppointmentDeletingEvent(7L, 10L, 99L));

        verify(entityAttachmentService).delete("attachments", first);
        verify(entityAttachmentService).delete("attachments", second);
    }
}
