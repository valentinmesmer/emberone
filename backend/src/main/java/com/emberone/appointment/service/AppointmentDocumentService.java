/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.service;

import com.emberone.appointment.domain.Appointment;
import com.emberone.appointment.exception.AppointmentAccessDeniedException;
import com.emberone.appointment.exception.AppointmentNotFoundException;
import com.emberone.appointment.repository.AppointmentRepository;
import com.emberone.attachment.AllowedAttachmentMediaTypes;
import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.attachment.service.EntityAttachmentService;
import com.emberone.platform.security.TokenService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentDocumentService {

    private final EntityAttachmentService entityAttachmentService;
    private final TokenService tokenService;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentAudienceService appointmentAudienceService;

    @Value("${object-storage.attachments-bucket}")
    private String attachmentsBucket;

    private static String appointmentStorageKey(
            Long tenantId, Long organizationId, Long appointmentId, String extension) {
        return "tenants/"
                + tenantId
                + "/organizations/"
                + organizationId
                + "/appointment/"
                + appointmentId
                + "/"
                + UUID.randomUUID()
                + extension;
    }

    @Transactional(readOnly = true)
    public List<EntityAttachmentDto> listAttachments(Long organizationId, Long appointmentId) {
        Long userId = tokenService.requireUserId();
        ensureUserCanRead(userId, organizationId, appointmentId);
        return entityAttachmentService.listByEntity(
                EntityAttachmentType.APPOINTMENT, appointmentId, organizationId);
    }

    @Transactional
    public EntityAttachmentDto uploadAttachment(
            Long userId, Long organizationId, Long appointmentId, MultipartFile file) {
        Long tenantId = tokenService.requireTenantId();
        ensureUserCanRead(userId, organizationId, appointmentId);
        EntityAttachmentService.validateContentType(file, AllowedAttachmentMediaTypes.VALUES);
        String extension = EntityAttachmentService.getFileExtension(file.getOriginalFilename());
        String storageKey =
                appointmentStorageKey(tenantId, organizationId, appointmentId, extension);
        EntityAttachment attachment =
                entityAttachmentService.uploadNew(
                        attachmentsBucket,
                        tenantId,
                        userId,
                        organizationId,
                        EntityAttachmentType.APPOINTMENT,
                        appointmentId,
                        file,
                        storageKey);
        log.info("Uploaded attachment {} for appointment {}", attachment.getId(), appointmentId);
        return entityAttachmentService.toDto(attachment);
    }

    @Transactional(readOnly = true)
    public InputStreamResource downloadAttachment(
            Long organizationId, Long appointmentId, Long attachmentId) {
        EntityAttachment attachment =
                requireReadableAttachment(organizationId, appointmentId, attachmentId);
        return entityAttachmentService.download(attachmentsBucket, attachment);
    }

    @Transactional
    public void deleteAttachment(Long organizationId, Long appointmentId, Long attachmentId) {
        Long userId = tokenService.requireUserId();
        ensureUserCanRead(userId, organizationId, appointmentId);
        EntityAttachment attachment =
                requireScopedAttachment(organizationId, appointmentId, attachmentId);
        entityAttachmentService.delete(attachmentsBucket, attachment);
        log.info("Deleted attachment {} for appointment {}", attachmentId, appointmentId);
    }

    @Transactional(readOnly = true)
    public String getContentType(Long organizationId, Long appointmentId, Long attachmentId) {
        return requireReadableAttachment(organizationId, appointmentId, attachmentId)
                .getContentType();
    }

    @Transactional(readOnly = true)
    public String getFileName(Long organizationId, Long appointmentId, Long attachmentId) {
        return requireReadableAttachment(organizationId, appointmentId, attachmentId).getFileName();
    }

    private EntityAttachment requireReadableAttachment(
            Long organizationId, Long appointmentId, Long attachmentId) {
        Long userId = tokenService.requireUserId();
        ensureUserCanRead(userId, organizationId, appointmentId);
        return requireScopedAttachment(organizationId, appointmentId, attachmentId);
    }

    private EntityAttachment requireScopedAttachment(
            Long organizationId, Long appointmentId, Long attachmentId) {
        return entityAttachmentService.requireScoped(
                attachmentId, EntityAttachmentType.APPOINTMENT, appointmentId, organizationId);
    }

    private void ensureUserCanRead(Long userId, Long organizationId, Long appointmentId) {
        Appointment appointment =
                appointmentRepository
                        .findByIdAndOrganization_Id(appointmentId, organizationId)
                        .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        if (!appointmentAudienceService.canUserAccessAppointment(userId, appointment)) {
            throw new AppointmentAccessDeniedException("User is not in the appointment audience");
        }
    }
}
