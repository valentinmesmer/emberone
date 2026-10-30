/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userdocument.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.storage.service.ObjectStorageDeletionOutboxService;
import com.emberone.platform.storage.service.ObjectStorageService;
import com.emberone.platform.storage.service.ObjectStorageTransactionCoordinator;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import com.emberone.userdocument.domain.UserDocument;
import com.emberone.userdocument.repository.UserDocumentRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserDocumentServiceTest {

    @Mock UserDocumentRepository repository;
    @Mock UserDocumentLinkResolver linkResolver;
    private final UserDocumentSystemWriter systemWriter = new UserDocumentSystemWriter();
    @Mock ObjectStorageService storageService;
    @Mock ObjectStorageDeletionOutboxService deletionOutboxService;
    @Mock ObjectStorageTransactionCoordinator objectStorageTransactionCoordinator;
    @Mock TokenService tokenService;
    @Mock UserRepository userRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    private UserDocumentService service;

    @BeforeEach
    void setUp() {
        service =
                new UserDocumentService(
                        repository,
                        linkResolver,
                        systemWriter,
                        storageService,
                        objectStorageTransactionCoordinator,
                        deletionOutboxService,
                        tokenService,
                        userRepository,
                        membershipRepository);
        ReflectionTestUtils.setField(service, "userDocumentsBucket", "documents");
    }

    @Test
    void uploadNormalizesMetadataAndDeduplicatesTags() {
        when(tokenService.getTenantId()).thenReturn(7L);
        when(repository.save(any()))
                .thenAnswer(
                        invocation -> {
                            UserDocument document = invocation.getArgument(0);
                            document.setId(5L);
                            return document;
                        });
        MockMultipartFile file =
                new MockMultipartFile("file", "proof.pdf", "application/pdf", "data".getBytes());

        var dto =
                service.uploadDocument(
                        1L, file, " Proof ", " Description ", List.of(" HR ", "HR", " "));

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getTitle()).isEqualTo("Proof");
        assertThat(dto.getFileName()).isEqualTo("Proof.pdf");
        assertThat(dto.getTags()).containsExactly("HR");
        var captor = org.mockito.ArgumentCaptor.forClass(UserDocument.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStorageKey())
                .startsWith("tenants/7/users/1/documents/")
                .endsWith(".pdf");
        assertThat(captor.getValue().getFileName()).isEqualTo("Proof.pdf");
    }

    @Test
    void markAllViewedUpdatesOnlyCurrentUser() {

        service.markAllViewed(1L);

        verify(repository).markAllViewedForUser(eq(1L), any(Instant.class));
    }

    @Test
    void organizationAssertionEnforcesTenantAndMembership() {
        when(tokenService.getTenantId()).thenReturn(7L);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(8L).build()));
        assertThatThrownBy(() -> service.assertUserBelongsToOrganization(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(User.builder().id(1L).tenantId(7L).build()));
        when(membershipRepository.existsActiveByUser_IdAndOrganization_Id(
                        eq(1L), eq(10L), any(Instant.class)))
                .thenReturn(false);
        assertThatThrownBy(() -> service.assertUserBelongsToOrganization(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organization");
    }

    @Test
    void uploadRejectsUnsupportedContentType() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "payload.html",
                        "text/html",
                        "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> service.uploadDocument(1L, file, "Title", null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void deleteDocumentsForLinkTargetsEnqueuesS3AndRemovesDocumentsAndLinks() {
        Set<String> recordTypes =
                Set.of("CERTIFICATION_RECORD", "TRAINING_COURSE_RECORD", "DRIVING_LICENSE_RECORD");
        when(repository.findStorageKeysOfDocumentsOwnedByLinkTargets(
                        "DRIVING_LICENSE_RECORD", List.of(42L), recordTypes))
                .thenReturn(List.of("tenants/7/users/1/documents/proof.pdf"));
        when(repository.deleteDocumentsOwnedByLinkTargets(
                        "DRIVING_LICENSE_RECORD", List.of(42L), recordTypes))
                .thenReturn(1);

        int deleted =
                service.deleteDocumentsForLinkTargets(
                        7L, AppLinkType.DRIVING_LICENSE_RECORD, List.of(42L, 42L));

        assertThat(deleted).isEqualTo(1);
        verify(deletionOutboxService)
                .enqueueObjectDeletion(7L, "documents", "tenants/7/users/1/documents/proof.pdf");
        verify(repository).deleteLinksToTargets("DRIVING_LICENSE_RECORD", List.of(42L));
    }

    @Test
    void sourceLinkTargetsAreRetainedByAnyRemainingLink() {
        assertThat(UserDocumentService.retainingLinkTypes(AppLinkType.WORKFLOW))
                .containsExactlyInAnyOrder(
                        Arrays.stream(AppLinkType.values()).map(Enum::name).toArray(String[]::new));
        assertThat(UserDocumentService.retainingLinkTypes(AppLinkType.TRAINING_COURSE_RECORD))
                .containsExactlyInAnyOrder(
                        "CERTIFICATION_RECORD", "TRAINING_COURSE_RECORD", "DRIVING_LICENSE_RECORD");
    }

    @Test
    void deleteDocumentsForLinkTargetsRejectsCertificationDefinitionLinks() {
        assertThatThrownBy(
                        () ->
                                service.deleteDocumentsForLinkTargets(
                                        7L, AppLinkType.CERTIFICATION_RECORD, List.of(3L)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void deleteDocumentsForLinkTargetsNoOpWhenLinkIdsEmpty() {
        service.deleteDocumentsForLinkTargets(7L, AppLinkType.TRAINING_COURSE_RECORD, List.of());

        verifyNoInteractions(repository, deletionOutboxService);
    }

    @Test
    void deleteDocumentsByIdsEnqueuesS3AndDeletes() {
        when(repository.findStorageKeysByIdIn(List.of(9L)))
                .thenReturn(List.of("tenants/7/users/1/documents/cert.pdf"));
        when(repository.deleteByIdIn(List.of(9L))).thenReturn(1);

        assertThat(service.deleteDocumentsByIds(7L, List.of(9L))).isEqualTo(1);
        verify(deletionOutboxService)
                .enqueueObjectDeletion(7L, "documents", "tenants/7/users/1/documents/cert.pdf");
    }

    @Test
    void oversizedTagIsRejected() {
        when(tokenService.getTenantId()).thenReturn(7L);
        MockMultipartFile file =
                new MockMultipartFile("file", "proof.pdf", "application/pdf", "data".getBytes());

        assertThatThrownBy(
                        () ->
                                service.uploadDocument(
                                        1L, file, "Proof", null, List.of("x".repeat(129))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128");
    }
}
