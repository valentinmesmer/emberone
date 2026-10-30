/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.attachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.attachment.domain.EntityAttachment;
import com.emberone.attachment.domain.EntityAttachmentType;
import com.emberone.attachment.dto.EntityAttachmentDto;
import com.emberone.attachment.repository.EntityAttachmentRepository;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.storage.service.ObjectStorageDeletionOutboxService;
import com.emberone.platform.storage.service.ObjectStorageService;
import com.emberone.platform.storage.service.ObjectStorageTransactionCoordinator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class EntityAttachmentServiceTest {

    @Mock EntityAttachmentRepository repository;
    @Mock ObjectStorageService storageService;
    @Mock ObjectStorageDeletionOutboxService deletionOutboxService;

    private EntityAttachmentService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(7L);
        service =
                new EntityAttachmentService(
                        repository,
                        storageService,
                        new ObjectStorageTransactionCoordinator(
                                storageService, deletionOutboxService));
    }

    @Test
    void uploadNewUsesProvidedStorageKeyAndPersistsMetadata() {
        when(repository.save(any()))
                .thenAnswer(
                        invocation -> {
                            EntityAttachment attachment = invocation.getArgument(0);
                            attachment.setId(5L);
                            return attachment;
                        });
        MockMultipartFile file =
                new MockMultipartFile("file", "report.pdf", "application/pdf", "data".getBytes());

        var dto =
                service.uploadNew(
                        "attachments",
                        7L,
                        1L,
                        10L,
                        EntityAttachmentType.APPOINTMENT,
                        20L,
                        file,
                        "tenants/7/organizations/10/appointment/20/uuid.pdf");

        assertThat(dto.getId()).isEqualTo(5L);
        var captor = org.mockito.ArgumentCaptor.forClass(EntityAttachment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStorageKey())
                .isEqualTo("tenants/7/organizations/10/appointment/20/uuid.pdf");
        assertThat(captor.getValue().getUploadedBy()).isEqualTo(1L);
        verify(storageService).uploadFile(file, "attachments", captor.getValue().getStorageKey());
    }

    @Test
    void listByEntityIsSortedNewestFirst() {
        EntityAttachment older = attachment(1L);
        EntityAttachment newer = attachment(2L);
        when(repository.findByEntityTypeAndEntityId(EntityAttachmentType.WORKFLOW, 20L))
                .thenReturn(List.of(older, newer));

        assertThat(service.listByEntity(EntityAttachmentType.WORKFLOW, 20L))
                .extracting(EntityAttachmentDto::getId)
                .containsExactly(2L, 1L);
    }

    @Test
    void deleteRemovesDatabaseRowAndEnqueuesStorageDeletion() {
        EntityAttachment attachment = attachment(5L);

        service.delete("attachments", attachment);

        verify(repository).delete(attachment);
        verify(deletionOutboxService)
                .enqueueObjectDeletion(7L, "attachments", "tenants/7/attachments/key-5");
    }

    @Test
    void rejectsEmptyAndOversizedFilesBeforeStorage() {
        assertThatThrownBy(
                        () ->
                                service.uploadNew(
                                        "attachments",
                                        7L,
                                        1L,
                                        10L,
                                        EntityAttachmentType.WORKFLOW,
                                        20L,
                                        new MockMultipartFile("file", new byte[0]),
                                        "key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
        MockMultipartFile large =
                new MockMultipartFile(
                        "file",
                        "large.pdf",
                        "application/pdf",
                        new byte[(int) EntityAttachmentService.MAX_FILE_SIZE + 1]);
        assertThatThrownBy(
                        () ->
                                service.uploadNew(
                                        "attachments",
                                        7L,
                                        1L,
                                        10L,
                                        EntityAttachmentType.WORKFLOW,
                                        20L,
                                        large,
                                        "key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50MB");
    }

    @Test
    void moveCopiesObjectAndEnqueuesSourceDeletion() {
        EntityAttachment attachment = attachment(5L);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.move("attachments", attachment, "tenants/7/attachments/key-promoted");

        verify(storageService)
                .copyFile(
                        "attachments",
                        "tenants/7/attachments/key-5",
                        "attachments",
                        "tenants/7/attachments/key-promoted");
        verify(deletionOutboxService)
                .enqueueObjectDeletion(7L, "attachments", "tenants/7/attachments/key-5");
        assertThat(attachment.getStorageKey()).isEqualTo("tenants/7/attachments/key-promoted");
    }

    private EntityAttachment attachment(long id) {
        return EntityAttachment.builder()
                .id(id)
                .tenantId(7L)
                .entityType(EntityAttachmentType.WORKFLOW)
                .entityId(20L)
                .organizationId(10L)
                .fileName("file.pdf")
                .contentType("application/pdf")
                .sizeBytes(4L)
                .storageKey("tenants/7/attachments/key-" + id)
                .uploadedBy(1L)
                .build();
    }
}
