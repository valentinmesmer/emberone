/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.exception.AppointmentAccessDeniedException;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.platform.security.TokenService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AppointmentDocumentServiceTest {

    @Mock EntityAttachmentService entityAttachmentService;
    @Mock TokenService tokenService;
    @Mock AppointmentRepository appointmentRepository;
    @Mock AppointmentAudienceService audienceService;

    private AppointmentDocumentService service;

    @BeforeEach
    void setUp() {
        service =
                new AppointmentDocumentService(
                        entityAttachmentService,
                        tokenService,
                        appointmentRepository,
                        audienceService);
        ReflectionTestUtils.setField(service, "attachmentsBucket", "attachments");
    }

    @Test
    void listAttachmentsRequireAudienceAccess() {
        Appointment appointment = Appointment.builder().id(20L).build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_Id(20L, 10L))
                .thenReturn(Optional.of(appointment));
        when(audienceService.canUserAccessAppointment(1L, appointment)).thenReturn(false);

        assertThatThrownBy(() -> service.listAttachments(10L, 20L))
                .isInstanceOf(AppointmentAccessDeniedException.class);
    }

    @Test
    void uploadAttachmentBuildsAppointmentScopedKey() {
        Appointment appointment = Appointment.builder().id(20L).build();
        when(tokenService.requireTenantId()).thenReturn(7L);
        when(appointmentRepository.findByIdAndOrganization_Id(20L, 10L))
                .thenReturn(Optional.of(appointment));
        when(audienceService.canUserAccessAppointment(1L, appointment)).thenReturn(true);
        MockMultipartFile file =
                new MockMultipartFile("file", "report.pdf", "application/pdf", "data".getBytes());
        EntityAttachment saved =
                EntityAttachment.builder()
                        .id(5L)
                        .tenantId(7L)
                        .entityType(EntityAttachmentType.APPOINTMENT)
                        .entityId(20L)
                        .organizationId(10L)
                        .fileName("report.pdf")
                        .contentType("application/pdf")
                        .sizeBytes(4L)
                        .storageKey("key")
                        .uploadedBy(1L)
                        .build();
        when(entityAttachmentService.uploadNew(
                        eq("attachments"),
                        eq(7L),
                        eq(1L),
                        eq(10L),
                        eq(EntityAttachmentType.APPOINTMENT),
                        eq(20L),
                        eq(file),
                        any()))
                .thenReturn(saved);
        when(entityAttachmentService.toDto(saved))
                .thenReturn(EntityAttachmentDto.builder().id(5L).build());

        service.uploadAttachment(1L, 10L, 20L, file);

        verify(entityAttachmentService)
                .uploadNew(
                        eq("attachments"),
                        eq(7L),
                        eq(1L),
                        eq(10L),
                        eq(EntityAttachmentType.APPOINTMENT),
                        eq(20L),
                        eq(file),
                        any());
    }

    @Test
    void uploadAttachmentRequiresAudienceAccess() {
        Appointment appointment = Appointment.builder().id(20L).build();
        when(tokenService.requireTenantId()).thenReturn(7L);
        when(appointmentRepository.findByIdAndOrganization_Id(20L, 10L))
                .thenReturn(Optional.of(appointment));
        when(audienceService.canUserAccessAppointment(1L, appointment)).thenReturn(false);
        MockMultipartFile file =
                new MockMultipartFile("file", "report.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(() -> service.uploadAttachment(1L, 10L, 20L, file))
                .isInstanceOf(AppointmentAccessDeniedException.class);
    }

    @Test
    void deleteAttachmentRequiresAudienceAccess() {
        Appointment appointment = Appointment.builder().id(20L).build();
        when(tokenService.requireUserId()).thenReturn(1L);
        when(appointmentRepository.findByIdAndOrganization_Id(20L, 10L))
                .thenReturn(Optional.of(appointment));
        when(audienceService.canUserAccessAppointment(1L, appointment)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteAttachment(10L, 20L, 5L))
                .isInstanceOf(AppointmentAccessDeniedException.class);
    }
}
